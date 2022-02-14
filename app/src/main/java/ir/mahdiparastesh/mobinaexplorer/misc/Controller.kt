package ir.mahdiparastesh.mobinaexplorer.misc

import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ir.mahdiparastesh.mobinaexplorer.Explorer

class Controller : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        when (intent.action) {
            Explorer.Code.STOP.s -> with(c.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler) {
                cancel(Explorer.JOB_ID)
            }
        }
    }
}
