package ir.mahdiparastesh.mobinaexplorer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import ir.mahdiparastesh.mobinaexplorer.databinding.MainBinding
import java.lang.StringBuilder

// adb connect 192.168.1.20:

class Panel : AppCompatActivity() {
    private lateinit var c: Context
    private lateinit var b: MainBinding

    companion object {
        var handler: Handler? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = MainBinding.inflate(layoutInflater)
        setContentView(b.root)
        c = applicationContext

        // Handler
        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    Action.BYTES.ordinal -> b.bytes.text = bytes(Crawler.bytesSinceBoot())
                    Action.STATUS.ordinal -> b.status.text = msg.obj as String
                } // GsonBuilder().setPrettyPrinting().create()
            }
        }
        handler?.obtainMessage(Action.BYTES.ordinal)?.sendToTarget()

        // Foreground Service
        Explorer.active.observe(this) { b -> exploring(b) }
        exploring(Explorer.active.value == true)
        b.root.setOnClickListener {
            startService(Intent(this, Explorer::class.java).apply {
                if (Explorer.active.value == true) action = Explorer.code(Explorer.Code.STOP)
            })
        }

        // b.users.adapter = ListUser(data, this@Panel)
    }

    override fun onDestroy() {
        handler = null
        super.onDestroy()
    }

    private fun exploring(bb: Boolean) {
        b.explore.alpha = if (bb) 1f else .36f
        vis(b.status, bb)
    }

    @Suppress("unused")
    private fun vis(v: View, bb: Boolean = true) {
        v.visibility = if (bb) View.VISIBLE else View.GONE
    }

    private fun bytes(l: Long): String {
        val units = resources.getStringArray(R.array.bytes)
        var gig = 0L
        var meg = 0L
        var kil = 0L
        var ll = l
        if (ll >= 1073741824L) {
            gig = ll / 1073741824L
            ll %= 1073741824L
        }
        if (ll >= 1048576L) {
            meg = ll / 1048576L
            ll %= 1048576L
        }
        if (ll >= 1024L) {
            kil = ll / 1024L
            ll %= 1024L
        }
        return StringBuilder().apply {
            if (gig > 0) append("$gig ${units[3]}, ")
            if (meg > 0) append("$meg ${units[2]}, ")
            if (kil > 0) append("$kil ${units[1]}, ")
            append("$ll ${units[0]}")
        }.toString()
    }

    enum class Action { BYTES, STATUS }
}
