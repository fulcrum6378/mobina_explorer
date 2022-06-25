package ir.mahdiparastesh.mobinaexplorer.view

import android.icu.text.DecimalFormat
import com.google.gson.Gson
import ir.mahdiparastesh.mobinaexplorer.Fetcher
import ir.mahdiparastesh.mobinaexplorer.Panel
import ir.mahdiparastesh.mobinaexplorer.Panel.Action
import ir.mahdiparastesh.mobinaexplorer.json.Rest
import ir.mahdiparastesh.mobinaexplorer.room.Candidate
import ir.mahdiparastesh.mobinaexplorer.room.Database
import ir.mahdiparastesh.mobinaexplorer.room.Nominee

class UiWork(
    private val c: Panel,
    private val work: Action,
    private val input: Any? = null
) : Thread() {
    private lateinit var db: Database
    private lateinit var dao: Database.DAO

    override fun run() {
        db = Database.DbFile.build(c.c).also { dao = it.dao() }
        var async = false
        val obj: Any? = when (work) {
            Action.CANDIDATES -> arrayListOf<Candidate>().apply {
                when (c.m.listWhat) {
                    1 -> dao.rejCandidates()
                    2 -> dao.obcCandidates()
                    else -> dao.nrmCandidates()
                }.forEach { add(it.apply { nominee = dao.nomineeById(it.id) }) }
            }
            Action.UPDATE -> (input as Candidate).apply { dao.updateCandidate(this) }
            Action.CUSTOM_WORK -> (input as CustomWork).execute(dao)
            Action.SUMMARY -> arrayListOf<String>().apply {
                val noms = dao.nominees()
                val sum = noms.size
                add(sum.toString())
                add(if (sum != 0) sumCent(sum, noms.filter { it.anal }.size).toString() else "0")
                add(if (sum != 0) sumCent(sum, noms.filter { it.fllw }.size).toString() else "0")
            }.toTypedArray()
            Action.REPAIR -> (input as Nominee).apply {
                async = true
                Fetcher(
                    c, Fetcher.Endpoint.INFO.url.format(id), Fetcher.Listener(Panel.handler) { info ->
                        val newU =
                            Gson().fromJson(Fetcher.decode(info), Rest.ProfileInfo::class.java).user
                        user = newU.username
                        // name = newU.full_name
                        accs = !newU.is_private || newU.friendship_status?.following == true
                        // "friendship_status" appears to be always null in INFO
                        Thread {
                            dao.updateNominee(this)
                            Panel.handler?.obtainMessage(work.ordinal)?.sendToTarget()
                            db.close()
                        }.start()
                    }, onError = {
                        if (it.networkResponse?.statusCode == 404) Thread {
                            dao.deleteNominee(id)
                            dao.deleteCandidate(id)
                            Panel.handler?.obtainMessage(work.ordinal)?.sendToTarget()
                            db.close()
                        }.start()
                    })
            }
            else -> null
        }
        if (!async) {
            Panel.handler?.obtainMessage(work.ordinal, obj)?.sendToTarget()
            db.close()
        }
    }

    private fun sumCent(sum: Int, cent: Int) =
        DecimalFormat("#.##").format((100f / sum.toFloat()) * cent.toFloat())

    fun interface CustomWork {
        fun execute(dao: Database.DAO): Any?
    }
}
