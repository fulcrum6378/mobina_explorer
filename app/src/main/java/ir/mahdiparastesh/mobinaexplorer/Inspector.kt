package ir.mahdiparastesh.mobinaexplorer

import android.os.Handler
import android.os.Looper
import android.os.Message
import com.android.volley.Request
import com.google.gson.Gson
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.MAX_FOLLOW
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.keywords
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.proximity
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.signal
import ir.mahdiparastesh.mobinaexplorer.Crawler.Proximity.*
import ir.mahdiparastesh.mobinaexplorer.Crawler.Signal
import ir.mahdiparastesh.mobinaexplorer.Fetcher.Type
import ir.mahdiparastesh.mobinaexplorer.json.Follow
import ir.mahdiparastesh.mobinaexplorer.json.GraphQL.*
import ir.mahdiparastesh.mobinaexplorer.json.PageConfig
import ir.mahdiparastesh.mobinaexplorer.json.Rest
import ir.mahdiparastesh.mobinaexplorer.json.Search
import ir.mahdiparastesh.mobinaexplorer.room.Candidate
import ir.mahdiparastesh.mobinaexplorer.room.Nominee
import java.lang.IllegalStateException
import java.util.*

class Inspector(private val c: Explorer, val nom: Nominee) {
    private lateinit var u: User
    private lateinit var timeline: TimelineMedia
    private val allPosts = arrayListOf<EdgePost>()

    init {
        signal(Signal.INSPECTING, nom.user)
        Fetcher(c, Type.PROFILE.url.format(nom.user), Fetcher.Listener { html ->
            if (!c.crawler.running) return@Listener
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
                    nom.anal = true
                    c.crawler.dao.updateNominee(nom)
                    if (nom.accs && nom.proximity() != OUT_OF_REACH) {
                        signal(Signal.FOLLOWERS_W, u.username.toString(), "0")
                        Fetcher.Delayer { allFollow(Type.FOLLOWERS, mutableListOf(), hashMapOf()) }
                    } else bye()
                }
                FOLLOWERS -> {
                    val res = msg.obj as List<Any>
                    addFollow(res[0] as List<Rest.User>, res[1] as HashMap<String, Rest.Friendship>)
                    signal(Signal.FOLLOWING_W, u.username.toString(), "0")
                    Fetcher.Delayer { allFollow(Type.FOLLOWING, mutableListOf(), hashMapOf()) }
                }
                FOLLOWING -> {
                    val res = msg.obj as List<Any>
                    addFollow(res[0] as List<Rest.User>, res[1] as HashMap<String, Rest.Friendship>)
                    bye()
                }
            }
        }

        private fun bye() = Fetcher.Delayer {
            nom.fllw = true
            c.crawler.dao.updateNominee(nom)
            signal(Signal.RESTING, u.username.toString())
            c.crawler.carryOn()
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

        if (!timeline.page_info.has_next_page || allPosts.size >= nom.maxPosts())
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

    @Suppress("LABEL_NAME_CLASH")
    private fun allFollow(
        type: Type,
        list: MutableList<Rest.User>,
        friends: HashMap<String, Rest.Friendship>,
        next_max_id: String = ""
    ) {
        if (!c.crawler.running) return
        signal(
            if (type == Type.FOLLOWERS) Signal.FOLLOWERS else Signal.FOLLOWING,
            u.username.toString(), list.size.toString()
        )
        Fetcher(c, type.url.format(u.id, next_max_id), Fetcher.Listener { flw ->
            val json = Gson().fromJson(Fetcher.decode(flw), Follow::class.java)
            Fetcher(
                c, Type.FRIENDSHIPS.url, Fetcher.Listener { friendship ->
                    if (!c.crawler.running) return@Listener
                    friends.putAll(
                        Gson().fromJson(
                            Fetcher.decode(friendship), Rest.Friendships::class.java
                        ).friendships
                    )

                    list.addAll(json.users.toMutableList())
                    if (json.next_max_id == null || (nom.proximity() == MAX_PROXIMITY && list.size < MAX_FOLLOW))
                        handler.obtainMessage(type.ordinal, listOf(list, friends))
                            .sendToTarget()
                    else {
                        signal(
                            if (type == Type.FOLLOWERS) Signal.FOLLOWERS_W else Signal.FOLLOWING_W,
                            u.username.toString(), list.size.toString()
                        )
                        Fetcher.Delayer { allFollow(type, list, friends, json.next_max_id) }
                    }
                }, true, Request.Method.POST,
                json.users.joinToString(friendIdSeparator) { it.pk }
            )
        })
    }

    private fun addFollow(list: List<Rest.User>, fs: HashMap<String, Rest.Friendship>) {
        when (nom.proximity()) {
            MIN_PROXIMITY -> list
            MED_PROXIMITY, MAX_PROXIMITY ->
                list.filter { preferredFollow(it, true) || preferredFollow(it, false) }
            else -> throw IllegalStateException(
                "While adding followers/following, \"nom\"\'s " +
                        "proximity must only be one of MIN_PROXIMITY, MED_PROXIMITY or MAX_PROXIMITY."
            )
        }.forEach {
            val acs = !it.is_private || fs[it.pk]?.following == true
            c.crawler.nominate(
                Nominee(
                    it.pk.toLong(), it.username, it.full_name, acs,
                    (if (preferredFollow(it, true)) 0 else nom.step + 1).toByte(),
                    false, acs
                )
            )
        }
    }

    private fun preferredFollow(ru: Rest.User, prx: Boolean) =
        searchScopes(prx, ru.username, ru.full_name)

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
        private const val friendIdSeparator = "%2C"

        @Suppress("LABEL_NAME_CLASH")
        fun search(c: Explorer, word: String) = Fetcher(c, Type.SEARCH.url.format(word),
            Fetcher.Listener { search ->
                if (!c.crawler.running) return@Listener
                val users = Gson().fromJson(Fetcher.decode(search), Search::class.java)
                    .users.toMutableList()
                    .also { it.sortWith(Search.ItemUser.Sort()) }
                Fetcher(
                    c, Type.FRIENDSHIPS.url, Fetcher.Listener { friendship ->
                        if (!c.crawler.running) return@Listener
                        val fs = Gson().fromJson(
                            Fetcher.decode(friendship), Rest.Friendships::class.java
                        ).friendships
                        for (x in users) {
                            val acs = !x.user.is_private || fs[x.user.pk]?.following == true
                            c.crawler.nominate(
                                Nominee(
                                    x.user.pk.toLong(), x.user.username, x.user.full_name,
                                    acs, 0, false, acs
                                )
                            )
                        }
                        c.crawler.carryOn()
                    }, true, Request.Method.POST,
                    users.joinToString(friendIdSeparator) { it.user.pk }
                )
            })
    }
}
