package ir.mahdiparastesh.mobinaexplorer

import android.net.TrafficStats
import android.os.Process
import androidx.room.Room
import ir.mahdiparastesh.mobinaexplorer.room.Database
import ir.mahdiparastesh.mobinaexplorer.room.DbFile
import ir.mahdiparastesh.mobinaexplorer.room.Session
import java.lang.Exception
import java.util.*

class Crawler(private val c: Explorer) : Thread() {
    private lateinit var db: Database
    lateinit var dao: Database.DAO
    private val session = Session(0, now(), 0, 0L)
    private val preBytes = bytesSinceBoot()

    override fun run() {
        db = Room.databaseBuilder(c, Database::class.java, DbFile.DATABASE)
            .allowMainThreadQueries()
            .build().also { dao = it.dao() }
        carryOn()
    }

    fun carryOn() {
        val preNoms = dao.nominees()
        if (preNoms.isEmpty() || preNoms.all { it.anal })
            Analyzer.search(c, keywords.random())
        else preNoms.filter { !it.anal }.forEachIndexed { i, nom ->
            Analyzer(c, nom.user, nom.step + 1, i == preNoms.size - 1)
        }
    }

    companion object {
        val keywords = arrayOf(
            "mobina", "rasht", "resht", "gilan", "مبینا", "رشت", "گیلان", "1379", "79", "2000"
        )

        fun bytesSinceBoot() = TrafficStats.getUidTxBytes(Process.myUid())
    }

    private fun now() = Calendar.getInstance().timeInMillis

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
