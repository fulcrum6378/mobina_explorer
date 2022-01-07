package ir.mahdiparastesh.mobinaexplorer.view

import android.content.Context
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
                dao.candidates().forEach {
                    add(it.apply { nominee = dao.nomineeById(it.id) })
                }
            }
            Action.REJECT -> (input as Candidate).apply {
                rejected = true
                dao.updateCandidate(this)
            }
            Action.CUSTOM_WORK -> (input as CustomWork).execute(dao)
            else -> null
        }
        Panel.handler?.obtainMessage(work.ordinal, obj)?.sendToTarget()
        db.close()
    }

    fun interface CustomWork {
        fun execute(dao: Database.DAO): Any?
    }
}
