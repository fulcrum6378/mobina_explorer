package ir.mahdiparastesh.mobinaexplorer

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData

@SuppressLint("UnspecifiedImmutableFlag")
class Explorer : Service() {
    lateinit var c: Context
    private var wakeLock: PowerManager.WakeLock? = null
    lateinit var analyzer: Analyzer
    lateinit var crawler: Crawler

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action != null && state.value != State.CHANGING) when (intent.action) {
            Code.STOP.s -> if (state.value == State.ACTIVE) destroy()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("WakelockTimeout")
    override fun onCreate() {
        state.value = State.CHANGING
        super.onCreate()
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
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = c.resources.getString(R.string.notif_channel_desc) })
        startForeground(CH_ID, NotificationCompat.Builder(c, Code.CHANNEL.s).apply {
            setSmallIcon(R.mipmap.launcher_round)
            setContentTitle(c.resources.getString(R.string.notif_title))
            setOngoing(true)
            setProgress(0, 0, true)
            priority = NotificationCompat.PRIORITY_LOW
            setContentIntent(
                PendingIntent.getActivity(
                    c, 0, Intent(c, Panel::class.java), PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            addAction(0, c.resources.getString(R.string.notif_stop), pi(c, Code.STOP))
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
    }

    fun destroy() {
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        state.value = State.CHANGING
        wakeLock?.let { if (it.isHeld) it.release() }
        crawler.interrupt()
        super.onDestroy()
        System.gc()
        state.value = State.OFF
    }

    override fun onBind(intent: Intent): IBinder? = null

    companion object {
        const val STRATEGY_ANALYSE = 0
        const val STRATEGY_COLLECT = 1
        const val STRATEGY_SEARCH = 2
        const val CH_ID = 103
        const val HANDLE_STATUS = 0
        lateinit var handler: Handler
        var strategy = 0 // 0 => Analyse, 1 => Collect, 2 => Search
        val pack: String = Explorer::class.java.`package`!!.name
        val state = MutableLiveData(State.OFF)
        val status = MutableLiveData(Crawler.Signal.OFF.s)
        var onlyPv = false

        fun pi(c: Context, code: Code): PendingIntent = PendingIntent.getService(
            c, 0, Intent(c, Explorer::class.java).apply { action = code.s },
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
