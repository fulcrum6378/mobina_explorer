package ir.mahdiparastesh.mobinaexplorer

import android.os.Handler
import android.os.Message
import com.android.volley.Request
import com.google.gson.Gson
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.IN_PLACE
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.MAX_DISTANCE
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.MED_DISTANCE
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.MIN_DISTANCE
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.keywords
import ir.mahdiparastesh.mobinaexplorer.Crawler.Companion.proximity
import ir.mahdiparastesh.mobinaexplorer.Crawler.Signal
import ir.mahdiparastesh.mobinaexplorer.Fetcher.Type
import ir.mahdiparastesh.mobinaexplorer.json.GraphQL.*
import ir.mahdiparastesh.mobinaexplorer.json.Rest
import ir.mahdiparastesh.mobinaexplorer.misc.Delayer
import ir.mahdiparastesh.mobinaexplorer.room.Candidate
import ir.mahdiparastesh.mobinaexplorer.room.Database
import ir.mahdiparastesh.mobinaexplorer.room.Nominee

class Inspector(private val c: Explorer, val nom: Nominee, forceAnalyze: Boolean = false) {
    private val db: Database
    private lateinit var dao: Database.DAO
    private lateinit var u: User
    private lateinit var timeline: Media
    private val allPosts = arrayListOf<EdgePost>()
    private val l = c.crawler.handling.looper
    private var qualified: Qualification? = null

    private val handler = object : Handler(l) {
        val ANALYZED = 0
        val FOLLOWERS = 1
        val FOLLOWING = 2

        @Suppress("UNCHECKED_CAST")
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                ANALYZED -> {
                    val newlyQualified = qualified != null && c.crawler.candidate(
                        Candidate(
                            nom.id, qualified?.res?.mobina() ?: -1f, qualified!!.where,
                            Crawler.now()
                        )
                    )
                    if (newlyQualified) c.crawler.signal(Signal.QUALIFIED, nom.user)
                    if (!nom.anal) dao.updateNominee(nom.analyzed())
                    val doNotWait = msg.obj == false
                    if (nom.accs && nom.proximity() != null && !nom.fllw) {
                        if (Explorer.strategy == Explorer.STRATEGY_COLLECT) {
                            if (!newlyQualified) c.crawler.signal(Signal.FOLLOWERS_W, nom.user, "0")
                            Delayer(l) { allFollow(Type.FOLLOWERS) }
                        } else bye(done = false, signal = !newlyQualified, wait = !doNotWait)
                    } else bye(signal = !newlyQualified, wait = !doNotWait)
                }
                FOLLOWERS -> {
                    c.crawler.signal(Signal.FOLLOWING_W, nom.user, "0")
                    Delayer(l) { allFollow(Type.FOLLOWING) }
                }
                FOLLOWING -> bye()
            }
        }

        private fun bye(done: Boolean = true, signal: Boolean = true, wait: Boolean = true) {
            if (done) dao.updateNominee(nom.followed())
            if (signal) c.crawler.signal(Signal.RESTING, nom.user)
            if (wait) Delayer(l) { Delayer(l) { c.crawler.carryOn() } }
            else c.crawler.carryOn()
        }
    }

    init {
        c.crawler.signal(Signal.INSPECTING, nom.user)
        db = Database.DbFile.build(c).also { dao = it.dao() }
        var shallFetch = true

        val scopes = arrayListOf<String?>(nom.user, nom.name)
        if (searchScopes(proximity, *scopes.toTypedArray()) && !forceAnalyze)
            revertProximity()
        if (searchScopes(keywords, *scopes.toTypedArray()) && !forceAnalyze)
            qualified = Qualification(null, Candidate.IN_PROFILE_TEXT)
        if (shallFetch && nom.anal && !forceAnalyze) {
            handler.obtainMessage(handler.ANALYZED, false).sendToTarget()
            shallFetch = false
        }

        if (shallFetch) Fetcher(c, Type.PROFILE.url.format(nom.user), Fetcher.Listener { baPro ->
            if (!c.crawler.running) return@Listener
            val profile = Fetcher.decode(baPro)

            try {
                u = Gson().fromJson(profile, Profile::class.java).graphql.user!!
                if (nom.accs && (u.is_private == true || u.blocked_by_viewer == true
                            || u.has_blocked_viewer == true)
                ) dao.updateNominee(nom.apply { accs = false })
                unknownError = 0
            } catch (e: Exception) { // JsonSyntaxException or NullPointerException
                invalidResult()
                return@Listener
            }
            timeline = u.edge_owner_to_timeline_media!!

            if (nom.name != u.full_name) {
                try {
                    nom.name = u.full_name
                } catch (e: NullPointerException) {
                    throw Exception(Gson().toJson(u))
                }
                dao.updateNominee(nom)
                scopes.removeLast()
                scopes.add(nom.name)
            }
            scopes.add(u.biography)
            if (searchScopes(proximity, *scopes.toTypedArray())) revertProximity()
            if (searchScopes(keywords, *scopes.toTypedArray()))
                qualified = Qualification(null, Candidate.IN_PROFILE_TEXT)
            else {
                handler.obtainMessage(handler.ANALYZED).sendToTarget()
                return@Listener
            }
            Delayer(l) {
                if (!c.crawler.running) return@Delayer
                c.crawler.signal(Signal.PROFILE_PHOTO, nom.user)
                Fetcher(c, u.profile_pic_url_hd ?: u.profile_pic_url, Fetcher.Listener { img ->
                    if (c.crawler.running) c.analyzer.Subject(img) { res ->
                        if (!res.isNullOrEmpty() && res.anyQualified()) {
                            qualified = Qualification(res, Candidate.IN_PROFILE)
                            handler.obtainMessage(handler.ANALYZED).sendToTarget()
                        } else fetchPosts(timeline.edges)
                    }
                })
            }
        })
    }

    private fun invalidResult() {
        unknownError++
        if (unknownError < Crawler.maxTryAgain) {
            c.crawler.signal(Signal.INVALID_RESULT)
            Crawler.handler?.obtainMessage(Fetcher.HANDLE_ERROR)?.sendToTarget()
        } else c.crawler.signal(Signal.UNKNOWN_ERROR)
    }

    @Suppress("SameParameterValue")
    private fun fetchPosts(initial: Array<EdgePost>?) {
        if (!c.crawler.running || !::timeline.isInitialized) return
        if (!nom.accs) {
            handler.obtainMessage(handler.ANALYZED).sendToTarget()
            return
        }
        if (initial != null) {
            c.crawler.signal(Signal.START_POSTS, nom.user)
            allPosts.addAll(initial)
            analPost()
            return
        } else if (!timeline.page_info.has_next_page || analyzedPosts >= nom.maxPosts()) {
            handler.obtainMessage(handler.ANALYZED).sendToTarget()
            return
        }
        c.crawler.signal(Signal.RESUME_POSTS, nom.user, allPosts.size.toString())
        Delayer(l) {
            if (c.crawler.running) Fetcher(c,
                Type.POSTS.url.format(nom.id, allPosts.size, timeline.page_info.end_cursor),
                Fetcher.Listener { graphQl ->
                    try {
                        timeline = Gson().fromJson(
                            Fetcher.decode(graphQl), Rest.GraphQLResponse::class.java
                        ).data.user!!.edge_owner_to_timeline_media!!
                        allPosts.addAll(timeline.edges)
                        analPost()
                    } catch (e: Exception) { // JsonSyntaxException or NullPointerException
                        invalidResult()
                        return@Listener
                    }
                })
        }
    }

    private var analyzedPosts = 0
    private fun analPost(i: Int = 0) {
        if (!c.crawler.running) return
        if (analyzedPosts >= nom.maxPosts()) {
            handler.obtainMessage(handler.ANALYZED).sendToTarget()
            return
        }
        if (i >= timeline.edges.size - 1) {
            fetchPosts(null)
            return
        }

        val node = timeline.edges[i].node
        if (searchScopes(proximity, node.location?.name, node.accessibility_caption))
            revertProximity()
        if (searchScopes(keywords, node.accessibility_caption) && qualified == null)
            qualified = Qualification(null, Candidate.IN_POST_TEXT.format(analyzedPosts))

        analSlide(i, arrayListOf(node.display_url).apply {
            node.edge_sidecar_to_children?.let { slider ->
                slider.edges.forEachIndexed { i, slide ->
                    if (i != 0) add(slide.node.display_url)
                }
            }
        }.toTypedArray())
    }

    private fun analSlide(i: Int, slides: Array<String>, ii: Int = 0) {
        if (!c.crawler.running) return
        if (ii >= slides.size || ii >= nom.maxSlides()) {
            analyzedPosts++
            analPost(i + 1)
            return
        }
        c.crawler.signal(Signal.ANALYZE_POST, nom.user, "${analyzedPosts + 1}", "${ii + 1}")
        Delayer(l) {
            Fetcher(c, slides[ii], Fetcher.Listener { img ->
                c.analyzer.Subject(img) { res ->
                    if (!res.isNullOrEmpty() && res.anyQualified()) {
                        qualified = Qualification(res, Candidate.IN_POST.format(analyzedPosts, ii))
                        handler.obtainMessage(handler.ANALYZED).sendToTarget()
                    } else analSlide(i, slides, ii + 1)
                }
            })
        }
    }

    @Suppress("LABEL_NAME_CLASH")
    private fun allFollow(type: Type, next_max_id: String = "") {
        if (!c.crawler.running) return
        if (next_max_id == "") numFollow = 0
        c.crawler.signal(
            if (type == Type.FOLLOWERS) Signal.FOLLOWERS else Signal.FOLLOWING,
            nom.user, numFollow.toString()
        )
        Fetcher(c, type.url.format(nom.id.toString(), next_max_id), Fetcher.Listener { flw ->
            val json = Gson().fromJson(Fetcher.decode(flw), Rest.Follow::class.java)
            Fetcher(
                c, Type.FRIENDSHIPS.url, Fetcher.Listener { friendship ->
                    if (!c.crawler.running) return@Listener
                    addFollow(
                        json.users.toMutableList().also { numFollow += it.size },
                        Gson().fromJson(
                            Fetcher.decode(friendship), Rest.Friendships::class.java
                        ).friendship_statuses
                    )

                    if (json.next_max_id == null || numFollow >= nom.maxFollow())
                        handler.obtainMessage(type.ordinal).sendToTarget()
                    else {
                        c.crawler.signal(
                            if (type == Type.FOLLOWERS) Signal.FOLLOWERS_W else Signal.FOLLOWING_W,
                            nom.user, numFollow.toString()
                        )
                        Delayer(l) { allFollow(type, json.next_max_id) }
                    }
                }, true, Request.Method.POST,
                preFriend + json.users.joinToString(sepFriendId) { it.pk }
            )
        })
    }

    private var numFollow = 0
    private fun addFollow(list: List<Rest.User>, fs: Map<String, Rest.Friendship>) {
        when (nom.proximity()) {
            IN_PLACE, MIN_DISTANCE -> list
            MED_DISTANCE, MAX_DISTANCE ->
                list.filter { preferredFollow(it, true) || preferredFollow(it, false) }
            else -> throw IllegalStateException("Unsupported proximity value!")
        }.forEach {
            val acs = !it.is_private || fs[it.pk]?.following == true
            c.crawler.nominate(
                Nominee(
                    it.pk.toLong(), it.username, it.full_name!!, acs,
                    (if (preferredFollow(it, true)) 0 else nom.step + 1).toByte(),
                    false, !acs
                )
            )
        }
    }

    private fun preferredFollow(ru: Rest.User, prx: Boolean) =
        searchScopes(if (prx) proximity else keywords, ru.username, ru.full_name)

    private fun revertProximity() {
        nom.step = 0
        dao.updateNominee(nom)
    }

    fun close() {
        db.close()
    }

    companion object {
        private const val preFriend = "user_ids="
        private const val sepFriendId = ","
        var unknownError = 0

        @Suppress("LABEL_NAME_CLASH")
        fun search(c: Explorer, word: String) = Fetcher(c, Type.SEARCH.url.format(word),
            Fetcher.Listener { search ->
                if (!c.crawler.running) return@Listener
                val users = Gson().fromJson(Fetcher.decode(search), Rest.Search::class.java)
                    .users.toMutableList()
                    .also { it.sortWith(Rest.Search.ItemUser.Sort()) }
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
                                    x.user.pk.toLong(), x.user.username, x.user.full_name!!,
                                    acs, 0, false, !acs
                                )
                            )
                        }
                        Delayer(c.crawler.handling.looper) { c.crawler.carryOn() }
                    }, true, Request.Method.POST,
                    preFriend + users.joinToString(sepFriendId) { it.user.pk }
                )
            })

        fun searchScopes(keywords: Array<String>, vararg scopes: String?): Boolean {
            for (wrd in scopes) if (!wrd.isNullOrBlank())
                for (kwd in keywords)
                    if (wrd.contains(kwd, true))
                        return true
            return false
        }
    }

    data class Qualification(val res: Analyzer.Results?, val where: String)
}
