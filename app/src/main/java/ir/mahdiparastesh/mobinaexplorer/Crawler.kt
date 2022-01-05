package ir.mahdiparastesh.mobinaexplorer

import android.net.TrafficStats
import android.os.Process
import ir.mahdiparastesh.mobinaexplorer.room.Database
import ir.mahdiparastesh.mobinaexplorer.room.Session
import java.util.*

class Crawler(private val c: Explorer) : Thread() {
    private lateinit var db: Database
    lateinit var dao: Database.DAO
    private val session = Session(0, now(), 0, 0L)
    private val preBytes = bytesSinceBoot()
    var running = true

    override fun run() {
        db = Database.DbFile.build(c).also { dao = it.dao() }
        carryOn()
    }

    fun carryOn() {
        if (!running) return
        val preNoms = dao.nominees()
        if (preNoms.isEmpty() || preNoms.all { it.anal })
            Inspector.search(c, proxyKeywords.random())
        else preNoms.filter { !it.anal } /**/.random()
            .apply { Inspector(c, this) }
    }

    companion object {
        const val MAX_STEPS = 10
        val proxyKeywords = arrayOf("rasht", "resht", "gilan", "رشت", "گیلان")
        val impKeywords = arrayOf("1379", "79", "2000") // Adjectives
        val expKeywords = arrayOf("mobina", "مبینا") // Given Names

        fun bytesSinceBoot() = TrafficStats.getUidTxBytes(Process.myUid())

        fun now() = Calendar.getInstance().timeInMillis
    }

    override fun interrupt() {
        running = false
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
}
