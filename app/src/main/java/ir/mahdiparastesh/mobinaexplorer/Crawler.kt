package ir.mahdiparastesh.mobinaexplorer

import android.database.sqlite.SQLiteConstraintException
import android.icu.util.Calendar
import android.net.TrafficStats
import android.os.Process
import ir.mahdiparastesh.mobinaexplorer.room.Database
import ir.mahdiparastesh.mobinaexplorer.room.Nominee
import ir.mahdiparastesh.mobinaexplorer.room.Session

class Crawler(private val c: Explorer) : Thread() {
    private lateinit var db: Database
    lateinit var dao: Database.DAO
    private lateinit var session: Session
    private val preBytes = bytesSinceBoot()
    var running = false

    override fun run() {
        running = true
        session = Session(0, now(), 0, 0L, 0L)
        db = Database.DbFile.build(c).also { dao = it.dao() }
        carryOn()
    }

    fun carryOn() {
        if (!running) return
        val preNoms = dao.nominees()
        if (preNoms.isEmpty() || preNoms.all { it.anal })
            Inspector.search(c, proximity.random())
        else preNoms.filter { !it.anal } /**/.random()
            .apply { Inspector(c, this) }
    }

    fun nominate(nominee: Nominee) {
        try {
            dao.addNominee(nominee)
        } catch (ignored: SQLiteConstraintException) {
            dao.updateNominee(nominee.apply {
                val older = dao.nomineeById(nominee.id)
                anal = older.anal
                if (older.accs == accs) fllw = older.fllw
            })
        }
        session.nominees += 1L
    }

    override fun interrupt() {
        running = false
        signal(Signal.OFF)
        session.bytes = bytesSinceBoot() - preBytes
        if (session.bytes > 0) {
            session.end = now()
            dao.addSession(session)
        }
        db.close()
        /*try {
            super.interrupt()
        } catch (ignored: Exception) {
        }*/
    }

    enum class Signal(val s: String) {
        OFF(""),
        VOLLEY_ERROR("Volley Error: %s"),
        INSPECTING("Inspecting %s..."),
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
        const val MAX_FOLLOW = 1000L // Only for MAX_PROXIMITY
        const val MAX_POSTS_FACTOR = 108 // /= PROXIMITY
        const val HUMAN_DELAY = 7000L
        val proximity = arrayOf("rasht", "resht", "gilan", "رشت", "گیلان")
        val keywords = arrayOf("mobina", "مبینا", "1379", "79", "2000")

        fun bytesSinceBoot() = TrafficStats.getUidTxBytes(Process.myUid())

        fun now() = Calendar.getInstance().timeInMillis

        fun signal(status: Signal, vararg s: String) {
            Panel.handler?.obtainMessage(Panel.Action.SIGNAL.ordinal, status.s.format(*s))
                ?.sendToTarget()
        }
    }

    enum class Proximity(val max: Int?) {
        MIN_PROXIMITY(3), // {0, 1, 2, 3} All followers/following will be nominated
        MED_PROXIMITY(6), // {4, 5, 6} All followers/following will be searched
        MAX_PROXIMITY(9), // {7, 8, 9} Some followers/following will be searched
        OUT_OF_REACH(null) // 10+ step followers/following won't be fetched
    }
}
