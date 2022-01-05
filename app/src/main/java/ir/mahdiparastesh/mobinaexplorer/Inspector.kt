package ir.mahdiparastesh.mobinaexplorer

import android.os.Handler
import android.os.Looper
import android.os.Message
import com.google.gson.Gson
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.MAX_FOLLOW
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.MAX_POSTS
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.keywords
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.proximity
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.signal
import ir.mahdiparastesh.mobinaexplorer.Crawler.Signal
import ir.mahdiparastesh.mobinaexplorer.Fetcher.Type
import ir.mahdiparastesh.mobinaexplorer.json.Follow
import ir.mahdiparastesh.mobinaexplorer.json.GraphQL.*
import ir.mahdiparastesh.mobinaexplorer.json.PageConfig
import ir.mahdiparastesh.mobinaexplorer.json.Rest
import ir.mahdiparastesh.mobinaexplorer.json.Search
import ir.mahdiparastesh.mobinaexplorer.room.Candidate
import ir.mahdiparastesh.mobinaexplorer.room.Nominee
import java.util.*

class Inspector(private val c: Explorer, val nom: Nominee) {
    private lateinit var u: User
    private lateinit var timeline: TimelineMedia
    private val allPosts = arrayListOf<EdgePost>()

    init {
        signal(Signal.INSPECTING, nom.user)
        Fetcher(c, Type.PROFILE.url.format(nom.user), Fetcher.Listener { html ->
            if (!c.crawler.running) return@Listener
            /*Fetcher(c, Type.HUMAN_CSS1.url, true) {}
            Fetcher(c, Type.HUMAN_CSS2.url, true) {}
            Fetcher(c, Type.HUMAN_CSS3.url, true) {}
            Fetcher(c, Type.HUMAN_CSS4.url, true) {}
            Fetcher(c, Type.HUMAN_CSS5.url, true) {}*/

            val cnf = Fetcher.decode(html).substringAfter(preConfig).substringBefore(posConfig)
            try {
                u = Gson().fromJson(cnf, PageConfig::class.java).entry_data.ProfilePage[0]
                    .graphql.user
            } catch (e: Exception) { // JsonSyntaxException
                signal(Signal.SIGNED_OUT)
                return@Listener
            }
            timeline = u.edge_owner_to_timeline_media!!
            val scopes = arrayOf(u.username, u.full_name, u.biography)
            if (searchScopes(true, *scopes)) revertProximity()
            if (searchScopes(false, *scopes)) {
                qualify(-1f, Candidate.IN_PROFILE_TEXT)
                return@Listener
            }

            if (!c.crawler.running) return@Listener
            var lookAt = u.profile_pic_url_hd
            if (lookAt == null) lookAt = u.profile_pic_url
            if (lookAt != null) {
                signal(Signal.PROFILE_PHOTO, nom.user)
                Fetcher(c, lookAt, Fetcher.Listener { img ->
                    if (c.crawler.running) c.analyzer.Subject(img) { res ->
                        if (res.isNullOrEmpty() || !res.anyQualified())
                            resumePosts(timeline.edges)
                        else qualify(res.best(), Candidate.IN_PROFILE)
                    }
                })
            } else resumePosts(timeline.edges)
        })
    }

    private fun qualify(score: Float, type: String) {
        signal(Signal.QUALIFIED, u.username.toString())
        c.crawler.dao.addCandidate(Candidate(u.id!!.toLong(), score, type))
        handler.obtainMessage(handler.ANALYZED).sendToTarget()
        Panel.handler?.obtainMessage(Panel.Action.REFRESH.ordinal)?.sendToTarget()
    }

    private val handler = object : Handler(Looper.getMainLooper()) {
        val ANALYZED = 0
        val FOLLOWERS = 1
        val FOLLOWING = 2

        @Suppress("UNCHECKED_CAST")
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                ANALYZED -> {
                    c.crawler.dao.updateNominee(nom.apply { anal = true })
                    if (nom.step < Crawler.MAX_PROXIMITY && u.is_private == false) {
                        signal(Signal.FOLLOWERS_W, u.username.toString(), "0")
                        Fetcher.Delayer { allFollow(Type.FOLLOWERS, mutableListOf()) }
                    } else Fetcher.Delayer {
                        signal(Signal.RESTING, u.username.toString())
                        c.crawler.carryOn()
                    }
                }
                FOLLOWERS -> {
                    signal(Signal.FOLLOWING_W, u.username.toString(), "0")
                    Fetcher.Delayer { allFollow(Type.FOLLOWING, mutableListOf()) }
                }
                FOLLOWING -> Fetcher.Delayer {
                    signal(Signal.RESTING, u.username.toString())
                    c.crawler.carryOn()
                }
            }
        }
    }

    private fun resumePosts(initial: Array<EdgePost>?) {
        if (!c.crawler.running) return
        if (initial != null) {
            signal(Signal.START_POSTS, u.username.toString())
            allPosts.addAll(timeline.edges)
            analLot(timeline.edges)
            return
        }

        if (!timeline.page_info.has_next_page || allPosts.size >= MAX_POSTS)
            handler.obtainMessage(handler.ANALYZED).sendToTarget()
        else Fetcher.Delayer {
            signal(Signal.RESUME_POSTS, u.username.toString(), allPosts.size.toString())
            if (c.crawler.running) Fetcher(c,
                Type.POSTS.url.format(hash, u.id, allPosts.size, timeline.page_info.end_cursor),
                Fetcher.Listener { graphQl ->
                    u = Gson().fromJson(Fetcher.decode(graphQl), Rest.GraphQLResponse::class.java)
                        .data.user
                    timeline = u.edge_owner_to_timeline_media!!
                    allPosts.addAll(timeline.edges)
                    analLot(timeline.edges)
                })
        }
    }

    private fun analLot(lot: Array<EdgePost>) {
        lot.forEachIndexed { i, post ->
            signal(
                Signal.ANALYZE_POST, u.username.toString(),
                (allPosts.size - 12 + i + 1).toString()
            )
            if (!c.crawler.running) return
            val scopes = arrayOf(post.node.accessibility_caption, post.node.location?.name)
            if (searchScopes(true, *scopes))
                revertProximity()
            if (searchScopes(false, *scopes)) {
                qualify(-1f, Candidate.IN_POST_TEXT)
                return@forEachIndexed
            }

            post.node.thumbnail_resources.forEachIndexed { ii, slide ->
                var end = false
                Fetcher(c, slide.src, Fetcher.Listener { img ->
                    c.analyzer.Subject(img) { res ->
                        if (res.isNullOrEmpty() || !res.anyQualified()) return@Subject
                        qualify(res.best(), Candidate.IN_POST.format(i.toString(), ii.toString()))
                        end = true
                    }
                })
                if (end) return@analLot
            }
        }
        resumePosts(null)
    }

    private fun allFollow(type: Type, list: MutableList<Rest.User>, next_max_id: String = "") {
        if (!c.crawler.running) return
        signal(
            if (type == Type.FOLLOWERS) Signal.FOLLOWERS else Signal.FOLLOWING,
            u.username.toString(), list.size.toString()
        )
        Fetcher(c, type.url.format(u.id, next_max_id), Fetcher.Listener { str ->
            val json = Gson().fromJson(Fetcher.decode(str), Follow::class.java)
            addFollow(json.users.toMutableList())
            list.addAll(json.users.toMutableList())
            if (json.next_max_id != null && list.size < MAX_FOLLOW) {
                signal(
                    if (type == Type.FOLLOWERS) Signal.FOLLOWERS_W else Signal.FOLLOWING_W,
                    u.username.toString(), list.size.toString()
                )
                Fetcher.Delayer { allFollow(type, list, json.next_max_id) }
            } else handler.obtainMessage(type.ordinal, list).sendToTarget()
        })
    }

    private fun addFollow(list: List<Rest.User>) {
        list.forEach {
            c.crawler.nominate(
                Nominee(
                    it.pk.toLong(), it.username,
                    it.full_name, it.is_private,
                    nom.step, false
                )
            )
        }
    }

    private fun searchScopes(prx: Boolean, vararg scopes: String?): Boolean {
        var ret = false
        (if (prx) proximity else keywords).forEach { kw ->
            if (scopes.filterNotNull().any { scope ->
                    scope.lowercase().contains(kw.lowercase())
                }) ret = true
        }
        return ret
    }

    private fun revertProximity() {
        nom.step = 0
        c.crawler.dao.updateNominee(nom)
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
                if (!c.crawler.running) return@Listener
                for (x in Gson().fromJson(
                    Fetcher.decode(str),
                    Search::class.java
                ).users.toMutableList()
                    .also { it.sortWith(Search.ItemUser.Sort()) }) c.crawler.nominate(
                    Nominee(
                        x.user.pk.toLong(), x.user.username, x.user.full_name, x.user.is_private,
                        0, false
                    )
                )
                c.crawler.carryOn()
            })
    }
}
