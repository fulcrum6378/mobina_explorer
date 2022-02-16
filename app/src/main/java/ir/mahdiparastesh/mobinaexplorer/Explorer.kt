package ir.mahdiparastesh.mobinaexplorer

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import ir.mahdiparastesh.mobinaexplorer.misc.Controller

@SuppressLint("UnspecifiedImmutableFlag")
class Explorer : JobService() {
    lateinit var c: Context
    private var wakeLock: PowerManager.WakeLock? = null
    lateinit var analyzer: Analyzer
    lateinit var crawler: Crawler

    @SuppressLint("WakelockTimeout")
    override fun onStartJob(params: JobParameters): Boolean {
        state.value = State.CHANGING
        c = applicationContext

        // In order for the service to be able to persist in when the activities are destroyed:
        // Go to app settings -> battery -> allow background activity and also...
        // Remove the app from the "Optimized battery usage" apps list.
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Explorer::lock").apply {
                acquire()
            }
        }

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(
                NotificationChannel(
                    Code.CHANNEL.s, c.resources.getString(R.string.notif_channel),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = c.resources.getString(R.string.notif_channel_desc) })
        startForeground(CH_ID, NotificationCompat.Builder(c, Code.CHANNEL.s).apply {
            setSmallIcon(R.mipmap.launcher_round)
            setContentTitle(c.resources.getString(R.string.notif_title))
            setOngoing(true)
            priority = NotificationCompat.PRIORITY_DEFAULT
            setContentIntent(
                PendingIntent.getActivity(
                    c, 0, Intent(c, Panel::class.java), PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            addAction(0, c.resources.getString(R.string.notif_stop), control(c, Code.STOP))
        }.build())

        handler = object : Handler(Looper.myLooper()!!) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    HANDLE_STATUS -> status.value = msg.obj as String
                }
            }
        }
        analyzer = Analyzer(c)
        crawler = Crawler(this).also { it.start() }
        state.value = State.ACTIVE
        return true
    }

    override fun onStopJob(parameters: JobParameters): Boolean { // {"callback":{},"jobId":103}
        state.value = State.CHANGING
        wakeLock?.let { if (it.isHeld) it.release() }
        stopForeground(true)
        crawler.interrupt()
        System.gc()
        state.value = State.OFF
        return false
    }

    companion object {
        const val JOB_ID = 103
        const val CH_ID = 103
        const val HANDLE_STATUS = 0
        lateinit var handler: Handler
        val pack: String = Explorer::class.java.`package`!!.name
        val state = MutableLiveData(State.OFF)
        val status = MutableLiveData(Crawler.Signal.OFF.s)
        var shouldFollow = false
        var onlyPv = false

        fun control(c: Context, code: Code): PendingIntent = PendingIntent.getBroadcast(
            c, 0, Intent(c, Controller::class.java).apply { action = code.s },
            PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    enum class Code(val s: String) {
        CHANNEL("$pack.EXPLORING"),
        CRW_HANDLING("$pack.HANDLING"),
        STOP("$pack.ACTION_STOP"),
    }

    enum class State { OFF, ACTIVE, CHANGING }
}
