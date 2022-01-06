package ir.mahdiparastesh.mobinaexplorer

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData

@SuppressLint("UnspecifiedImmutableFlag")
class Explorer : Service() {
    private lateinit var c: Context
    lateinit var analyzer: Analyzer
    lateinit var crawler: Crawler

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action != null && state.value != State.CHANGING) when (intent.action) {
            Code.RESUME.s -> if (state.value == State.SLEPT) {
                state.value = State.ACTIVE
                crawler.running = true
            }
            Code.PAUSE.s -> if (state.value == State.ACTIVE) {
                state.value = State.SLEPT
                crawler.running = false // TODO: This is NOT real pausing
            }
            Code.STOP.s -> if (state.value == State.ACTIVE || state.value == State.SLEPT) {
                stopForeground(true)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onCreate() {
        state.value = State.CHANGING
        super.onCreate()
        c = applicationContext

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

    override fun onDestroy() {
        state.value = State.CHANGING
        crawler.interrupt()
        super.onDestroy()
        System.gc()
        state.value = State.OFF
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CH_ID = 103
        val state = MutableLiveData(State.OFF)
        val status = MutableLiveData(Crawler.Signal.OFF.s)
        lateinit var handler: Handler
        const val HANDLE_STATUS = 0

        fun packageName(): String = Explorer::class.java.`package`!!.name

        fun pi(c: Context, code: Code): PendingIntent = PendingIntent.getService(
            c, 0, Intent(c, Explorer::class.java).apply { action = code.s },
            PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    enum class Code(val s: String) {
        CHANNEL("${packageName()}.EXPLORING"),
        CRW_HANDLING("${packageName()}.HANDLING"),
        RESUME("${packageName()}.ACTION_RESUME"),
        PAUSE("${packageName()}.ACTION_PAUSE"),
        STOP("${packageName()}.ACTION_STOP"),
    }

    enum class State { OFF, ACTIVE, SLEPT, CHANGING }
}
