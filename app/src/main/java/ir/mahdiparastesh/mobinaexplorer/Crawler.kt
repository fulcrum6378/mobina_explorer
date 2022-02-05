package ir.mahdiparastesh.mobinaexplorer

import android.database.sqlite.SQLiteConstraintException
import android.icu.util.Calendar
import android.net.TrafficStats
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.os.Process
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import ir.mahdiparastesh.mobinaexplorer.json.Rest
import ir.mahdiparastesh.mobinaexplorer.misc.Delayer
import ir.mahdiparastesh.mobinaexplorer.room.Candidate
import ir.mahdiparastesh.mobinaexplorer.room.Database
import ir.mahdiparastesh.mobinaexplorer.room.Nominee
import ir.mahdiparastesh.mobinaexplorer.room.Session
import java.io.InputStreamReader

open class Crawler(private val c: Explorer) : Thread() {
    private lateinit var db: Database
    private lateinit var dao: Database.DAO
    private lateinit var session: Session
    lateinit var headers: HashMap<String, String>
    lateinit var handling: HandlerThread
    private val preBytes = bytesSinceBoot()
    private var inspection: Inspector? = null
    var running = false

    init {
        priority = MAX_PRIORITY
    }

    override fun run() {
        running = true
        session = Session(0, now(), 0, 0L, 0L)
        handling = HandlerThread(Explorer.Code.CRW_HANDLING.s).also { it.start() }
        handler = object : Handler(handling.looper) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    HANDLE_VOLLEY -> (msg.obj as Fetcher.Listener.Transit)
                        .apply { listener.onFinished(response) }
                    HANDLE_INTERRUPT -> {
                        dao.addSession(session)
                        db.close()
                        handling.quit()
                        handler = null
                    }
                    HANDLE_ML_KIT -> (msg.obj as Analyzer.Transit)
                        .apply { listener.onFinished(results) }
                    HANDLE_ERROR -> Delayer(handling.looper) { carryOn() }
                    HANDLE_STOP -> c.stopSelf()
                    HANDLE_NOT_FOUND -> if (inspection != null) {
                        dao.deleteNominee(inspection!!.nom.id)
                        dao.deleteCandidate(inspection!!.nom.id)
                        Delayer(handling.looper) { carryOn() }
                    }
                    HANDLE_REQ_REPAIR -> repair(msg.obj as Nominee)
                }
            }
        }
        db = Database.DbFile.build(c).also { dao = it.dao() }
        headers = Gson().fromJson(
            JsonReader(InputStreamReader(c.resources.assets.open("headers.json"))),
            HashMap::class.java
        )
        carryOn()
        //inspection = Inspector(c, dao.nominee(""), true)
    }

    fun carryOn() {
        inspection?.close()
        inspection = null
        if (!running) return
        var noNoms = if (!Panel.onlyPv) dao.noNominees() else dao.noPvNominees()
        if (Panel.onlyPv && noNoms.isEmpty()) {
            Panel.onlyPv = false
            noNoms = dao.noNominees()
            Panel.handler?.obtainMessage(Panel.Action.NO_REM_PV.ordinal)?.sendToTarget()
        }
        if (noNoms.isEmpty()) proximity.random().apply {
            signal(Signal.SEARCHING, this)
            Inspector.search(c, this)
        } else noNoms.random().apply {
            inspection = Inspector(c, this)
        }
    }

    fun nominate(nominee: Nominee) {
        try {
            dao.addNominee(nominee)
            session.nominees += 1L
        } catch (ignored: SQLiteConstraintException) {
            dao.updateNominee(nominee.apply {
                val older = dao.nomineeById(nominee.id)
                anal = older.anal
                if (older.accs == accs) fllw = older.fllw
            })
        }
    }

    fun candidate(candidate: Candidate) {
        newCandidate(candidate, dao)
        Panel.handler?.obtainMessage(Panel.Action.REFRESH.ordinal)?.sendToTarget()
    }

    fun signal(status: Signal, vararg s: String) {
        Explorer.handler.obtainMessage(Explorer.HANDLE_STATUS, status.s.format(*s))
            .sendToTarget()
        Panel.handler?.obtainMessage(Panel.Action.USER_LINK.ordinal, inspection?.nom?.user)
            ?.sendToTarget()
        if (status in arrayOf(Signal.SIGNED_OUT, Signal.VOLLEY_NOT_WORKING))
            handler?.obtainMessage(HANDLE_STOP)?.sendToTarget()
    }

    fun repair(nom: Nominee) {
        Fetcher(c, Fetcher.Type.POSTS.url.format(nom.id, 1, ""), Fetcher.Listener { graphQl ->
            try {
                val newUn = Gson().fromJson(
                    Fetcher.decode(graphQl), Rest.GraphQLResponse::class.java
                ).data.user.edge_owner_to_timeline_media!!.edges[0].node.owner.username
                dao.updateNominee(nom.apply { user = newUn })
            } catch (ignored: java.lang.Exception) {
                dao.deleteNominee(nom.id)
                dao.deleteCandidate(nom.id)
            }
        })
    }

    override fun interrupt() {
        running = false
        inspection?.close()
        signal(Signal.OFF)
        session.bytes = bytesSinceBoot() - preBytes
        if (session.bytes > 0) {
            session.end = now()
            handler?.obtainMessage(HANDLE_INTERRUPT)?.sendToTarget()
        }
        try {
            super.interrupt()
        } catch (ignored: Exception) {
        }
    }

    enum class Signal(val s: String) {
        OFF(""),
        VOLLEY_ERROR("Volley Error: %s"),
        VOLLEY_NOT_WORKING("Sorry, but Volley doesn't work at all. Error: %s"),
        PAGE_NOT_FOUND("Page not found..."),
        SEARCHING("Searching for the keyword \"%s\"..."),
        INSPECTING("Inspecting %s\'s profile..."),
        //USER_CHANGED("Their username has been changed! Preparing for reparation..."),
        INVALID_RESULT("Invalid result! Trying again..."),
        SIGNED_OUT("You're signed out :("),
        UNKNOWN_ERROR("Unknown error!!!"),
        PROFILE_PHOTO("Analyzing %s's profile photo..."),
        START_POSTS("Found nothing :( Inspecting %s's posts..."),
        RESUME_POSTS("Fetching more posts from %1\$s (currently %2\$s)..."),
        ANALYZE_POST("Analyzing %1\$s's post %2\$s, slide %3\$s"),
        FOLLOWERS("Fetching %1\$s's followers (%2\$s)..."),
        FOLLOWERS_W("Waiting before fetching %1\$s's followers (%2\$s)..."),
        FOLLOWING("Fetching %1\$s's following (%2\$s)..."),
        FOLLOWING_W("Waiting before fetching %1\$s's following (%2\$s)..."),
        QUALIFIED("%s was QUALIFIED!!!"),
        RESTING("Let's have a rest, please...")
    }

    companion object {
        var handler: Handler? = null
        const val HANDLE_VOLLEY = 0
        const val HANDLE_ML_KIT = 1
        const val HANDLE_INTERRUPT = 2
        const val HANDLE_ERROR = 3
        const val HANDLE_STOP = 4
        const val HANDLE_NOT_FOUND = 5
        const val HANDLE_REQ_REPAIR = 6

        fun newCandidate(can: Candidate, dao: Database.DAO) {
            try {
                dao.addCandidate(can)
            } catch (ignored: SQLiteConstraintException) {
                dao.updateCandidate(can.apply {
                    val older = dao.candidate(can.id)
                    rejected = older.rejected
                })
            }
        }

        fun bytesSinceBoot() = TrafficStats.getUidRxBytes(Process.myUid()) +
                TrafficStats.getUidTxBytes(Process.myUid())

        fun now() = Calendar.getInstance().timeInMillis

        fun maxPosts(prx: Byte?) = when (prx) {
            IN_PLACE -> 11
            MIN_PROXIMITY -> 8
            MED_PROXIMITY -> 6
            MAX_PROXIMITY -> 3
            else -> 0
        }

        fun maxFollow(prx: Byte?) = when (prx) {
            IN_PLACE -> 10000
            MIN_PROXIMITY -> 2000
            MED_PROXIMITY -> 1500
            MAX_PROXIMITY -> 1000
            else -> 0
        }

        fun maxSlides(prx: Byte?) = when (prx) {
            IN_PLACE -> 10
            MIN_PROXIMITY -> 8
            MED_PROXIMITY -> 6
            MAX_PROXIMITY -> 4
            else -> 0
        }

        const val HUMAN_DELAY = 5000L
        const val maxTryAgain = 2
        const val IN_PLACE = (0).toByte() // 0
        const val MIN_PROXIMITY = (3).toByte() // {1, 2, 3}
        const val MED_PROXIMITY = (6).toByte() // {4, 5, 6}
        const val MAX_PROXIMITY = (9).toByte() // {7, 8, 9}

        // 10+ step followers/following won't be fetched
        val proximity = arrayOf("rasht", "resht", "gilan", "guilan", "رشت", "گیلان", "گیلک")
        val superKeywords = arrayOf("mobina", "مبینا")
        val otherKeywords = arrayOf("1379", "79", "2000")

        @Suppress("SpellCheckingInspection")
        val antiKeywords = arrayOf(
            "abdul", "abolfazl", "ahmad", "ahmed", "ali", "babak", "benyamin", "davood", "david",
            "davod", "ebrahim", "ebi", "mohammad", "mohamad", "hosein", "hossein", "hoseyn",
            "hosseyn", "reza", "mahdi", "mehdi", "amir", "amin", "sadegh", "sadeq", "rahman",
            "kumar", "esmail", "saeed", "saed", "ghasem", "rahmat", "arman", "javad", "koorosh",
            "daryush", "saman",
            "احمد", "عبد", "ابوالفضل", "ابولفضل", "علی", "بابک", "بنیامین", "داود", "داوود",
            "ابراهیم", "ابی", "محمد", "حسین", "رضا", "مهدی", "امیر", "امین", "صادق", "رحمان",
            "اسماعیل", "اسی", "سعید", "قاسم", "رحمت", "آرمان", "جواد", "کوروش", "داریوش", "سامان",
            "0910", "0912", "0933"
        )
    }
}
