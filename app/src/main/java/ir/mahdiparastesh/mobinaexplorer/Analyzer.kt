package ir.mahdiparastesh.mobinaexplorer

import android.os.Handler
import android.os.Looper
import android.os.Message
import com.google.gson.Gson
import ir.mahdiparastesh.mobinaexplorer.Fetcher.Type
import ir.mahdiparastesh.mobinaexplorer.json.Follow
import ir.mahdiparastesh.mobinaexplorer.json.GraphQL.*
import ir.mahdiparastesh.mobinaexplorer.json.PageConfig
import ir.mahdiparastesh.mobinaexplorer.json.Rest
import ir.mahdiparastesh.mobinaexplorer.json.Search
import ir.mahdiparastesh.mobinaexplorer.room.Nominee

class Analyzer(private val c: Explorer, user: String, val step: Int) {
    private lateinit var u: User
    private lateinit var timeline: TimelineMedia
    private val allPosts = arrayListOf<EdgePost>()

    init {
        Panel.handler?.obtainMessage(Panel.Action.STATUS.ordinal, "Analyzing $user")
            ?.sendToTarget()
        Fetcher(c, Type.PROFILE.url.format(user)) { html ->
            // Human Simulation Games
            /*Fetcher(c, Type.HUMAN_CSS1.url, true) {}
            Fetcher(c, Type.HUMAN_CSS2.url, true) {}
            Fetcher(c, Type.HUMAN_CSS3.url, true) {}
            Fetcher(c, Type.HUMAN_CSS4.url, true) {}
            Fetcher(c, Type.HUMAN_CSS5.url, true) {}*/

            val cnf = html.substringAfter(preConfig).substringBefore(posConfig)
            try {
                u = Gson().fromJson(cnf, PageConfig::class.java).entry_data
                    .ProfilePage[0].graphql.user
            } catch (ignored: Exception) {
                // TODO: Not Signed In
                return@Fetcher
            }
            timeline = u.edge_owner_to_timeline_media!!
            allPosts.addAll(timeline.edges)

            if (u.profile_pic_url_hd != null) {
            } else if (u.profile_pic_url != null) {
            } else {
            }

            // TODO: ANALYZE PROFILE PHOTO
            // IF DIDN'T FIND ANYTHING: TODO(resumePosts())
            //if (step <= 10) allFollow(Type.FOLLOWERS, mutableListOf())

            handler.obtainMessage(handler.ANALYZED).sendToTarget()
        }
    }

    private val handler = object : Handler(Looper.getMainLooper()) {
        val ANALYZED = 0
        val FOLLOWERS = 1
        val FOLLOWING = 2

        @Suppress("UNCHECKED_CAST")
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                ANALYZED -> c.crawler.carryOn() // TODO: Change later

                FOLLOWERS, FOLLOWING -> (msg.obj as List<Rest.User>).forEach {
                    c.crawler.dao.addNominee(
                        Nominee(
                            it.pk.toLong(), it.username,
                            it.full_name, it.is_private,
                            step.toByte(), false
                        )
                    )
                }
            }
            if (msg.what == FOLLOWERS) allFollow(Type.FOLLOWING, mutableListOf())
            if (msg.what == FOLLOWING) c.crawler.carryOn()
        }
    }

    private fun resumePosts() {
        if (!timeline.page_info.has_next_page)
            Panel.handler?.obtainMessage(1, allPosts)?.sendToTarget()
        else object : Fetcher.Delayer() {
            override fun onFinish() {
                Fetcher(
                    c, Type.POSTS.url
                        .format(hash, u.id, allPosts.size, timeline.page_info.end_cursor)
                ) { graphQl ->
                    u = Gson().fromJson(graphQl, Rest.GraphQLResponse::class.java)
                        .data.user
                    timeline = u.edge_owner_to_timeline_media!!
                    allPosts.addAll(timeline.edges)
                    resumePosts()
                }
            }
        }
    }

    private fun allFollow(type: Type, list: MutableList<Rest.User>, next_max_id: String = "") {
        Fetcher(c, type.url.format(u.id, next_max_id)) { str ->
            val json = Gson().fromJson(str, Follow::class.java)
            list.addAll(json.users.toMutableList())
            if (json.next_max_id != null) object : Fetcher.Delayer() {
                override fun onFinish() {
                    allFollow(type, list, json.next_max_id)
                }
            } else handler.obtainMessage(type.ordinal, list).sendToTarget()
        }
    }

    companion object {
        const val preConfig = "<script type=\"text/javascript\">window._sharedData = "
        const val posConfig = ";</script>"
        const val hash = "8c2a529969ee035a5063f2fc8602a0fd"

        //const val preLib = "<link rel=\"preload\" href=\"/static/bundles/es6/ConsumerLibCommons.js/"
        //const val posLib = ".js\""
        //const val preHash = "o.pagination},queryId:\""
        //const val posHash = "\""

        fun search(c: Explorer, word: String) = Fetcher(c, Type.SEARCH.url.format(word)) { str ->
            for (x in Gson().fromJson(str, Search::class.java).users.toMutableList()
                .also { it.sortWith(Search.ItemUser.Sort()) }) c.crawler.dao.addNominee(
                Nominee(
                    x.user.pk.toLong(), x.user.username, x.user.full_name, x.user.is_private,
                    0, false
                )
            )
            c.crawler.carryOn()
        }
    }
}
