package ir.mahdiparastesh.mobinaexplorer.misc

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock

open class Delayer(
    private val looper: Looper,
    private val timeout: Long = ir.mahdiparastesh.mobinaexplorer.Crawler.humanDelay(),
    private val listener: () -> Unit
) {
    private var mStopTimeInFuture = SystemClock.elapsedRealtime() + timeout
    private val mHandler = object : Handler(looper) {
        override fun handleMessage(msg: Message) {
            synchronized(this@Delayer) {
                if (mStopTimeInFuture - SystemClock.elapsedRealtime() <= 0)
                    listener()
                else sendMessageDelayed(obtainMessage(MSG), timeout)
            }
        }
    }

    init {
        mHandler.sendMessage(mHandler.obtainMessage(MSG))
    }

    companion object {
        private const val MSG = 1
    }
}
