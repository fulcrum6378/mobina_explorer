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

class Inspector(private val c: Explorer, nom: Nominee) {
    private lateinit var u: User
    private lateinit var timeline: TimelineMedia
    private val allPosts = arrayListOf<EdgePost>()

    init {
        Panel.handler?.obtainMessage(Panel.Action.STATUS.ordinal, "Analyzing ${nom.user}")
            ?.sendToTarget()
        Fetcher(c, Type.PROFILE.url.format(nom.user), Fetcher.Listener { html ->
            // Human Simulation Games
            /*Fetcher(c, Type.HUMAN_CSS1.url, true) {}
            Fetcher(c, Type.HUMAN_CSS2.url, true) {}
            Fetcher(c, Type.HUMAN_CSS3.url, true) {}
            Fetcher(c, Type.HUMAN_CSS4.url, true) {}
            Fetcher(c, Type.HUMAN_CSS5.url, true) {}*/

            val cnf = Fetcher.decode(html).substringAfter(preConfig).substringBefore(posConfig)
            try {
                u = Gson().fromJson(cnf, PageConfig::class.java).entry_data
                    .ProfilePage[0].graphql.user
            } catch (ignored: Exception) {
                // TODO: Not Signed In
                return@Listener
            }
            timeline = u.edge_owner_to_timeline_media!!
            allPosts.addAll(timeline.edges)

            var lookAt = u.profile_pic_url_hd
            if (lookAt == null) lookAt = u.profile_pic_url
            if (lookAt != null) Fetcher(c, lookAt, Fetcher.Listener {
                c.analyzer.Subject(it) {
                    // TODO: if (it.isNullOrEmpty()) { TODO(); return; }
                    // TODO: COMPARE
                    /*c.crawler.dao.updateNominee(nom.apply { anal = true })
                    handler.obtainMessage(handler.ANALYZED).sendToTarget()*/
                }
            })// else Fetcher.Delayer { resumePosts() }
        })
    }

    private val handler = object : Handler(Looper.getMainLooper()) {
        val ANALYZED = 0
        val FOLLOWERS = 1
        val FOLLOWING = 2

        @Suppress("UNCHECKED_CAST")
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                ANALYZED ->
                    if (nom.step < Crawler.MAX_STEPS && u.is_private == false)
                        allFollow(Type.FOLLOWERS, mutableListOf())
                    else c.crawler.carryOn()
                FOLLOWERS -> {
                    addFollow(msg.obj as List<Rest.User>)
                    Fetcher.Delayer { allFollow(Type.FOLLOWING, mutableListOf()) }
                }
                FOLLOWING -> {
                    addFollow(msg.obj as List<Rest.User>)
                    Fetcher.Delayer { c.crawler.carryOn() }
                }
            }
        }

        fun addFollow(list: List<Rest.User>) {
            list.forEach {
                c.crawler.dao.addNominee(
                    Nominee(
                        it.pk.toLong(), it.username,
                        it.full_name, it.is_private,
                        nom.step, false
                    )
                )
            }
        }
    }

    private fun resumePosts() {
        if (!timeline.page_info.has_next_page)
            TODO()
        else Fetcher.Delayer {
            Fetcher(c,
                Type.POSTS.url.format(hash, u.id, allPosts.size, timeline.page_info.end_cursor),
                Fetcher.Listener { graphQl ->
                    u = Gson().fromJson(Fetcher.decode(graphQl), Rest.GraphQLResponse::class.java)
                        .data.user
                    timeline = u.edge_owner_to_timeline_media!!
                    allPosts.addAll(timeline.edges)
                    resumePosts()
                })
        }
    }

    private fun allFollow(type: Type, list: MutableList<Rest.User>, next_max_id: String = "") {
        Fetcher(c, type.url.format(u.id, next_max_id), Fetcher.Listener { str ->
            val json = Gson().fromJson(Fetcher.decode(str), Follow::class.java)
            list.addAll(json.users.toMutableList())
            if (json.next_max_id != null)
                Fetcher.Delayer { allFollow(type, list, json.next_max_id) }
            else handler.obtainMessage(type.ordinal, list).sendToTarget()
        })
    }

    companion object {
        const val preConfig = "<script type=\"text/javascript\">window._sharedData = "
        const val posConfig = ";</script>"
        const val hash = "8c2a529969ee035a5063f2fc8602a0fd"

        //const val preLib = "<link rel=\"preload\" href=\"/static/bundles/es6/ConsumerLibCommons.js/"
        //const val posLib = ".js\""
        //const val preHash = "o.pagination},queryId:\""
        //const val posHash = "\""

        fun search(c: Explorer, word: String) = Fetcher(c, Type.SEARCH.url.format(word),
            Fetcher.Listener { str ->
                for (x in Gson().fromJson(Fetcher.decode(str), Search::class.java).users.toMutableList()
                    .also { it.sortWith(Search.ItemUser.Sort()) }) c.crawler.dao.addNominee(
                    Nominee(
                        x.user.pk.toLong(), x.user.username, x.user.full_name, x.user.is_private,
                        0, false
                    )
                )
                c.crawler.carryOn()
            })
    }
}
