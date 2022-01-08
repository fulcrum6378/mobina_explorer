package ir.mahdiparastesh.mobinaexplorer

import android.os.Handler
import android.os.Message
import com.android.volley.Request
import com.google.gson.Gson
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.MAX_SLIDES
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.keywords
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.proximity
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.signal
import ir.mahdiparastesh.mobinaexplorer.Crawler.Proximity.*
import ir.mahdiparastesh.mobinaexplorer.Crawler.Signal
import ir.mahdiparastesh.mobinaexplorer.Fetcher.Type
import ir.mahdiparastesh.mobinaexplorer.json.*
import ir.mahdiparastesh.mobinaexplorer.json.GraphQL.*
import ir.mahdiparastesh.mobinaexplorer.room.Candidate
import ir.mahdiparastesh.mobinaexplorer.room.Database
import ir.mahdiparastesh.mobinaexplorer.room.Nominee
import java.util.*

class Inspector(private val c: Explorer, val nom: Nominee, forceAnalyze: Boolean = false) {
    private var db: Database
    private var dao: Database.DAO
    private lateinit var u: User
    private lateinit var timeline: TimelineMedia
    private val allPosts = arrayListOf<EdgePost>()

    init {
        signal(Signal.INSPECTING, nom.user)
        db = Database.DbFile.build(c).also { dao = it.dao() }
        Fetcher(c, Type.PROFILE.url.format(nom.user), Fetcher.Listener { html ->
            if (!c.crawler.running) return@Listener
            if (nom.anal && !forceAnalyze) {
                handler.obtainMessage(handler.ANALYZED).sendToTarget()
                return@Listener
            }
            // Crawler.un = nom.user

            val cnf = Fetcher.decode(html).substringAfter(preConfig).substringBefore(posConfig)
            try {
                u = Gson().fromJson(cnf, PageConfig::class.java).entry_data.ProfilePage[0]
                    .graphql.user
                namusanSignedOut = 0
            } catch (e: Exception) { // JsonSyntaxException
                namusanSignedOut++
                if (namusanSignedOut < Crawler.maxTryAgain) {
                    signal(Signal.INVALID_RESULT)
                    Crawler.handler?.obtainMessage(Crawler.HANDLE_ERROR)?.sendToTarget()
                } else signal(Signal.SIGNED_OUT)
                return@Listener
            }
            timeline = u.edge_owner_to_timeline_media!!
            val scopes = arrayOf(u.username, u.full_name, u.biography)
            if (searchScopes(true, *scopes)) revertProximity()
            if (searchScopes(false, *scopes)) {
                qualify(null, Candidate.IN_PROFILE_TEXT)
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
                            fetchPosts(timeline.edges)
                        else qualify(res, Candidate.IN_PROFILE)
                    }
                })
            } else fetchPosts(timeline.edges)
        })
    }

    private fun qualify(res: Analyzer.Results?, type: String) {
        signal(Signal.QUALIFIED, nom.user)
        handler.obtainMessage(handler.ANALYZED).sendToTarget()
        c.crawler.candidate(Candidate(nom.id, res?.mobina() ?: -1f, type))
    }

    private val handler = object : Handler(c.crawler.handling.looper) {
        val ANALYZED = 0
        val FOLLOWERS = 1
        val FOLLOWING = 2

        @Suppress("UNCHECKED_CAST")
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                ANALYZED -> {
                    if (!nom.anal) {
                        nom.anal = true
                        dao.updateNominee(nom)
                    }
                    if (nom.accs && nom.proximity() != OUT_OF_REACH && !nom.fllw) {
                        signal(Signal.FOLLOWERS_W, nom.user, "0")
                        Fetcher.Delayer { allFollow(Type.FOLLOWERS, mutableListOf(), hashMapOf()) }
                    } else bye()
                }
                FOLLOWERS -> {
                    val res = msg.obj as List<Any>
                    addFollow(res[0] as List<Rest.User>, res[1] as HashMap<String, Rest.Friendship>)
                    signal(Signal.FOLLOWING_W, nom.user, "0")
                    Fetcher.Delayer { allFollow(Type.FOLLOWING, mutableListOf(), hashMapOf()) }
                }
                FOLLOWING -> {
                    val res = msg.obj as List<Any>
                    addFollow(res[0] as List<Rest.User>, res[1] as HashMap<String, Rest.Friendship>)
                    bye()
                }
            }
        }

        private fun bye() {
            nom.fllw = true
            dao.updateNominee(nom)
            signal(Signal.RESTING, nom.user)
            Fetcher.Delayer { Fetcher.Delayer { c.crawler.carryOn() } }
        }
    }

    private fun fetchPosts(initial: Array<EdgePost>?) {
        if (!c.crawler.running) return
        if (!nom.accs) {
            handler.obtainMessage(handler.ANALYZED).sendToTarget()
            return
        }
        if (initial != null) {
            signal(Signal.START_POSTS, nom.user)
            allPosts.addAll(timeline.edges)
            analPost()
            return
        } else if (!timeline.page_info.has_next_page || allPosts.size >= nom.maxPosts()) {
            handler.obtainMessage(handler.ANALYZED).sendToTarget()
            return
        }
        signal(Signal.RESUME_POSTS, nom.user, allPosts.size.toString())
        Fetcher.Delayer {
            if (c.crawler.running) Fetcher(c,
                Type.POSTS.url.format(hash, nom.id, allPosts.size, timeline.page_info.end_cursor),
                Fetcher.Listener { graphQl ->
                    u = Gson().fromJson(
                        Fetcher.decode(graphQl), Rest.GraphQLResponse::class.java
                    ).data.user
                    timeline = u.edge_owner_to_timeline_media!!
                    allPosts.addAll(timeline.edges)
                    analPost()
                })
        }
    }

    private var analyzedPosts = 0
    private fun analPost(i: Int = 0) {
        if (!c.crawler.running) return
        if (analyzedPosts > nom.maxPosts()) {
            handler.obtainMessage(handler.ANALYZED).sendToTarget()
            return
        }
        if (i >= timeline.edges.size - 1) {
            fetchPosts(null)
            return
        }

        val scopes = arrayOf(
            timeline.edges[i].node.accessibility_caption,
            timeline.edges[i].node.location?.name
        )
        if (searchScopes(true, *scopes))
            revertProximity()
        if (searchScopes(false, *scopes)) {
            qualify(null, Candidate.IN_POST_TEXT.format(analyzedPosts))
            return
        }

        val urls = arrayListOf(timeline.edges[i].node.display_url)
        timeline.edges[i].node.edge_sidecar_to_children?.let { slider ->
            slider.edges.forEach { urls.add(it.node.display_url) }
        }
        analSlide(i, urls.toTypedArray())
    }

    private fun analSlide(i: Int, slides: Array<String>, ii: Int = 0) {
        if (!c.crawler.running) return
        if (ii >= slides.size || ii >= MAX_SLIDES) {
            analyzedPosts++
            analPost(i + 1)
            return
        }
        signal(Signal.ANALYZE_POST, nom.user, "${analyzedPosts + 1}", "${ii + 1}")
        Fetcher(c, slides[ii], Fetcher.Listener { img ->
            c.analyzer.Subject(img) { res ->
                if (!res.isNullOrEmpty() && res.anyQualified()) {
                    qualify(res, Candidate.IN_POST.format(analyzedPosts, ii))
                    return@Subject
                }
                analSlide(i, slides, ii + 1)
            }
        })
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
            nom.user, list.size.toString()
        )
        Fetcher(c, type.url.format(nom.id.toString(), next_max_id), Fetcher.Listener { flw ->
            val json = Gson().fromJson(Fetcher.decode(flw), Follow::class.java)
            Fetcher(
                c, Type.FRIENDSHIPS.url, Fetcher.Listener { friendship ->
                    if (!c.crawler.running) return@Listener
                    friends.putAll(
                        Gson().fromJson(
                            Fetcher.decode(friendship), Rest.Friendships::class.java
                        ).friendship_statuses
                    )

                    list.addAll(json.users.toMutableList())
                    if (json.next_max_id == null || list.size >= nom.maxFollow())
                        handler.obtainMessage(type.ordinal, listOf(list, friends)).sendToTarget()
                    else {
                        signal(
                            if (type == Type.FOLLOWERS) Signal.FOLLOWERS_W else Signal.FOLLOWING_W,
                            nom.user, list.size.toString()
                        )
                        Fetcher.Delayer { allFollow(type, list, friends, json.next_max_id) }
                    }
                }, true, Request.Method.POST,
                preFriend + json.users.joinToString(sepFriendId) { it.pk }
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
                    false, !acs
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
        dao.updateNominee(nom)
    }

    fun close() {
        db.close()
    }

    companion object {
        const val preConfig = "<script type=\"text/javascript\">window._sharedData = "
        const val posConfig = ";</script>"
        const val hash = "8c2a529969ee035a5063f2fc8602a0fd"
        private const val preFriend = "user_ids="
        private const val sepFriendId = "," //%2C
        var namusanSignedOut = 0

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
                        ).friendship_statuses
                        for (x in users) {
                            val acs = !x.user.is_private || fs[x.user.pk]?.following == true
                            c.crawler.nominate(
                                Nominee(
                                    x.user.pk.toLong(), x.user.username, x.user.full_name,
                                    acs, 0, false, !acs
                                )
                            )
                        }
                        c.crawler.carryOn()
                    }, true, Request.Method.POST,
                    preFriend + users.joinToString(sepFriendId) { it.user.pk }
                )
            })
    }
}
