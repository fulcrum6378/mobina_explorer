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

class Explorer : Service() {
    private lateinit var c: Context
    lateinit var crawler: Crawler
    var handler: Handler? = null

    companion object {
        const val CH_ID = 103
        val CHANNEL = Explorer::class.java.`package`!!.name + ".EXPLORING"
        val ACTION_STOP = Explorer::class.java.`package`!!.name + ".ACTION_STOP"
        val active = MutableLiveData(false)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action != null && intent.action.equals(ACTION_STOP, ignoreCase = true)) {
            stopForeground(true)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onCreate() {
        super.onCreate()
        active.value = true
        c = applicationContext

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(
                NotificationChannel(
                    CHANNEL, c.resources.getString(R.string.notif_channel),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = c.resources.getString(R.string.notif_channel_desc) })
        startForeground(CH_ID, NotificationCompat.Builder(c, CHANNEL).apply {
            setSmallIcon(R.mipmap.launcher_round)
            setContentTitle(c.resources.getString(R.string.notif_title))
            setOngoing(true)
            priority = NotificationCompat.PRIORITY_DEFAULT
            setContentIntent(
                PendingIntent.getActivity(
                    c, 0, Intent(c, Panel::class.java), PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            addAction(
                0, c.resources.getString(R.string.notif_stop), PendingIntent.getService(
                    c, 0, Intent(c, Explorer::class.java).apply { action = ACTION_STOP },
                    PendingIntent.FLAG_CANCEL_CURRENT
                )
            )
        }.build())

        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
            }
        }
        crawler = Crawler(this).also { it.start() }
    }

    override fun onDestroy() {
        active.value = false
        handler = null
        crawler.interrupt()
        super.onDestroy()
        System.gc()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
