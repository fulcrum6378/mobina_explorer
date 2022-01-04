package ir.mahdiparastesh.mobinaexplorer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import ir.mahdiparastesh.mobinaexplorer.databinding.MainBinding
import ir.mahdiparastesh.mobinaexplorer.view.UiTools
import ir.mahdiparastesh.mobinaexplorer.view.UiTools.Companion.color

// adb connect 192.168.1.20:

@SuppressLint("ClickableViewAccessibility")
class Panel : AppCompatActivity(), View.OnTouchListener {
    private lateinit var c: Context
    private lateinit var b: MainBinding
    private var anStatus: ObjectAnimator? = null
    private lateinit var dm: DisplayMetrics
    private val maxBias = 0.5f
    private val minBias = 0f
    private val movePerMove = 0.03f
    private var y = 0f
    private var lastMove: Long? = null
    private var speed = 0f // moves per second
    private var overdrive: AnimatorSet? = null // TODO: IMPLEMENT IT

    companion object {
        var handler: Handler? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = MainBinding.inflate(layoutInflater)
        setContentView(b.root)
        c = applicationContext
        dm = resources.displayMetrics

        // Handler
        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    Action.BYTES.ordinal ->
                        b.bytes.text = UiTools.bytes(c, Crawler.bytesSinceBoot())
                    Action.STATUS.ordinal -> {
                        b.status.text = msg.obj as String
                        b.status.setTextColor(color(c, R.color.alarm))
                        anStatus = ObjectAnimator.ofArgb(
                            b.status, "textColor",
                            color(c, R.color.alarm), color(c, R.color.CPO)
                        ).apply {
                            duration = 1000L
                            addListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator?) {
                                    anStatus = null
                                }
                            })
                            start()
                        }
                    }
                }
            }
        }
        handler?.obtainMessage(Action.BYTES.ordinal)?.sendToTarget()

        // Control the Foreground Service
        Explorer.active.observe(this) { b -> exploring(b) }
        exploring(Explorer.active.value == true)
        b.start.setOnClickListener {
            UiTools.shake(c)
            startService(Intent(this, Explorer::class.java).apply {
                if (Explorer.active.value == true) action = Explorer.code(Explorer.Code.STOP)
            })
        }
        b.start.layoutParams = (b.start.layoutParams as ConstraintLayout.LayoutParams).apply {
            matchConstraintPercentHeight =
                (dm.widthPixels.toFloat() / dm.heightPixels) * matchConstraintPercentWidth
        }

        // Candidates
        b.root.setOnTouchListener(this)
        // b.users.adapter = ListUser(data, this@Panel)

        // Thread { TfUtils.preTrain(c) }.start()
        // TfUtils.test(c, b.fd, b.bytes)
    }

    override fun onDestroy() {
        handler = null
        super.onDestroy()
    }

    override fun onTouch(v: View, ev: MotionEvent): Boolean = when (ev.action) {
        MotionEvent.ACTION_DOWN -> {
            y = ev.y
            lastMove = SystemClock.elapsedRealtime()
            overdrive?.cancel()
            true
        }
        MotionEvent.ACTION_MOVE -> {
            speed = 1000f / (SystemClock.elapsedRealtime() - lastMove!!)
            (b.start.layoutParams as ConstraintLayout.LayoutParams).verticalBias.apply {
                val dist = y - ev.y
                if (dist > 0f && this > minBias)
                    move(this, true)
                if (dist <= 0f && this <= maxBias)
                    move(this, false)
            }
            y = ev.y
            lastMove = SystemClock.elapsedRealtime()
            true
        }
        MotionEvent.ACTION_UP -> {
            lastMove = null
            speed = 0f
            b.bytes.text = speed.toString()
            y < 50f
        }
        else -> false
    }

    private fun move(bias: Float, up: Boolean): Float {
        var robotBias = bias
        if (up) robotBias -= movePerMove
        else robotBias += movePerMove

        if (robotBias < 0f) robotBias = 0f
        if (robotBias > 0.5f) robotBias = 0.5f
        b.start.layoutParams = (b.start.layoutParams as ConstraintLayout.LayoutParams)
            .apply { verticalBias = robotBias }
        ((robotBias * 1.75f) + 0.25f).apply {
            b.start.scaleX = this
            b.start.scaleY = this
        }
        b.start.translationY = ((-b.start.height * 0.25f) * (1f - (robotBias * 2f)))

        return robotBias
    }

    private fun exploring(bb: Boolean) {
        b.robot.alpha = if (bb) 1f else .36f
        UiTools.vis(b.status, bb)
    }

    enum class Action { BYTES, STATUS }
}
