package ir.mahdiparastesh.mobinaexplorer.view

import android.content.Context
import android.icu.text.DecimalFormat
import ir.mahdiparastesh.mobinaexplorer.Panel
import ir.mahdiparastesh.mobinaexplorer.Panel.Action
import ir.mahdiparastesh.mobinaexplorer.room.Candidate
import ir.mahdiparastesh.mobinaexplorer.room.Database

class UiWork(
    private val c: Context,
    private val work: Action,
    private val input: Any? = null
) : Thread() {
    private lateinit var db: Database
    private lateinit var dao: Database.DAO

    override fun run() {
        db = Database.DbFile.build(c).also { dao = it.dao() }
        val obj: Any? = when (work) {
            Action.CANDIDATES -> arrayListOf<Candidate>().apply {
                (if (Panel.showRejected) dao.orCandidates() else dao.nrCandidates()).forEach {
                    add(it.apply { nominee = dao.nomineeById(it.id) })
                }
            }
            Action.REJECT -> (input as Candidate).apply {
                rejected = true
                dao.updateCandidate(this)
            }
            Action.ACCEPT -> (input as Candidate).apply {
                rejected = false
                dao.updateCandidate(this)
            }
            Action.CUSTOM_WORK -> (input as CustomWork).execute(dao)
            Action.SUMMARY -> arrayListOf<String>().apply {
                val noms = dao.nominees()
                val sum = noms.size
                add(sum.toString())
                add(if (sum != 0) sumCent(sum, noms.filter { it.anal }.size).toString() else "0")
                add(if (sum != 0) sumCent(sum, noms.filter { it.fllw }.size).toString() else "0")
            }.toTypedArray()
            else -> null
        }
        Panel.handler?.obtainMessage(work.ordinal, obj)?.sendToTarget()
        db.close()
    }

    private fun sumCent(sum: Int, cent: Int) =
        DecimalFormat("#.##").format((100f / sum.toFloat()) * cent.toFloat())

    fun interface CustomWork {
        fun execute(dao: Database.DAO): Any?
    }
}
