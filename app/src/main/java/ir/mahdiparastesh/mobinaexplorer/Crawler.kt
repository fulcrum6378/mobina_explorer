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
import ir.mahdiparastesh.mobinaexplorer.room.Candidate
import ir.mahdiparastesh.mobinaexplorer.room.Database
import ir.mahdiparastesh.mobinaexplorer.room.Nominee
import ir.mahdiparastesh.mobinaexplorer.room.Session
import java.io.InputStreamReader

class Crawler(private val c: Explorer) : Thread() {
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
                    HANDLE_ERROR -> carryOn()
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
        val preNoms = dao.nominees()
        if (preNoms.isEmpty() || preNoms.all { it.anal }) proximity.random().apply {
            signal(Signal.SEARCHING, this)
            Inspector.search(c, this)
        } else preNoms.filter { !it.anal || !it.fllw }.random()
            .apply { inspection = Inspector(c, this) }
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
        try {
            dao.addCandidate(candidate)
        } catch (ignored: SQLiteConstraintException) {
            dao.updateCandidate(candidate.apply {
                val older = dao.candidate(candidate.id)
                rejected = older.rejected
            })
        }
        Panel.handler?.obtainMessage(Panel.Action.REFRESH.ordinal)?.sendToTarget()
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
        SEARCHING("Searching for the keyword \"%s\"..."),
        INSPECTING("Inspecting %s\'s profile..."),
        SIGNED_OUT("You're signed out :("),
        PROFILE_PHOTO("Analyzing %s's profile photo..."),
        START_POSTS("Found nothing :( Inspecting %s's posts..."),
        RESUME_POSTS("Fetching more posts from %1\$s (currently %2\$s)..."),
        ANALYZE_POST("Analyzing %1\$s's post #%2\$s"),
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
        var un = ""

        fun signal(status: Signal, vararg s: String) {
            Explorer.handler.obtainMessage(Explorer.HANDLE_STATUS, status.s.format(*s))
                .sendToTarget()
        }

        fun bytesSinceBoot() = TrafficStats.getUidTxBytes(Process.myUid())

        fun now() = Calendar.getInstance().timeInMillis

        fun maxPosts(prx: Proximity) = when (prx) {
            Proximity.MIN_PROXIMITY -> 12
            Proximity.MED_PROXIMITY -> 8
            Proximity.MAX_PROXIMITY -> 4
            else -> 0
        }

        fun maxFollow(prx: Proximity) = when (prx) {
            Proximity.MIN_PROXIMITY -> 3000
            Proximity.MED_PROXIMITY -> 2000
            Proximity.MAX_PROXIMITY -> 1000
            else -> 0
        }

        const val HUMAN_DELAY = 5000L
        val proximity = arrayOf("rasht", "resht", "gilan", "رشت", "گیلان")
        val keywords = arrayOf("mobina", "مبینا", "1379", "79", "2000")
    }

    enum class Proximity(val max: Byte?) {
        MIN_PROXIMITY(3), // {0, 1, 2, 3} All followers/following will be nominated
        MED_PROXIMITY(6), // {4, 5, 6} All followers/following will be searched
        MAX_PROXIMITY(9), // {7, 8, 9} Some followers/following will be searched
        OUT_OF_REACH(null) // 10+ step followers/following won't be fetched
    }
}
