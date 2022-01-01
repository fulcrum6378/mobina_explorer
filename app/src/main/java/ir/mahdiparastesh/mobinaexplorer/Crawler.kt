package ir.mahdiparastesh.mobinaexplorer

import android.net.TrafficStats
import android.os.Process
import androidx.room.Room
import ir.mahdiparastesh.mobinaexplorer.Fun.Companion.now
import ir.mahdiparastesh.mobinaexplorer.room.Database
import ir.mahdiparastesh.mobinaexplorer.room.DbFile
import ir.mahdiparastesh.mobinaexplorer.room.Session

class Crawler(private val c: Explorer) : Thread() {
    private lateinit var db: Database
    lateinit var dao: Database.DAO
    private val session = Session(0, now(), 0, 0L)
    private val preBytes = bytesSinceBoot()
    var running = true

    override fun run() {
        db = Room.databaseBuilder(c, Database::class.java, DbFile.DATABASE)
            .allowMainThreadQueries()
            .build().also { dao = it.dao() }
        carryOn()
    }

    fun carryOn() {
        val preNoms = dao.nominees()
        if (preNoms.isEmpty() || preNoms.all { it.anal })
            Analyzer.search(c, expKeywords.random())
        else preNoms.filter { !it.anal }.forEachIndexed { i, nom ->
            Analyzer(c, nom.user, nom.step + 1, i == preNoms.size - 1)
        }
    }

    companion object {
        val impKeywords = arrayOf(
             "rasht", "resht", "gilan", "رشت", "گیلان", "1379", "79", "2000"
        )
        val expKeywords = arrayOf("mobina", "مبینا")

        fun bytesSinceBoot() = TrafficStats.getUidTxBytes(Process.myUid())
    }

    override fun interrupt() {
        session.bytes = bytesSinceBoot() - preBytes
        if (session.bytes > 0) {
            session.end = now()
            dao.addSession(session)
        }
        db.close()
        try {
            super.interrupt()
        } catch (ignored: Exception) {
        }
    }
}
