package ir.mahdiparastesh.mobinaexplorer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
import ir.mahdiparastesh.mobinaexplorer.room.Candidate
import ir.mahdiparastesh.mobinaexplorer.room.Database
import ir.mahdiparastesh.mobinaexplorer.view.ListUser
import ir.mahdiparastesh.mobinaexplorer.view.Momentum
import ir.mahdiparastesh.mobinaexplorer.view.UiTools
import ir.mahdiparastesh.mobinaexplorer.view.UiTools.Companion.color
import ir.mahdiparastesh.mobinaexplorer.view.UiTools.Companion.vis

// adb connect 192.168.1.20:

@SuppressLint("ClickableViewAccessibility")
class Panel : AppCompatActivity(), View.OnTouchListener {
    private lateinit var c: Context
    private lateinit var b: MainBinding
    private var anStatus: ObjectAnimator? = null
    private lateinit var db: Database
    private lateinit var dao: Database.DAO
    private lateinit var dm: DisplayMetrics

    companion object {
        var handler: Handler? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = MainBinding.inflate(layoutInflater)
        setContentView(b.root)
        c = applicationContext
        db = Database.DbFile.build(c).also { dao = it.dao() }
        dm = resources.displayMetrics

        // Handler
        handler = object : Handler(Looper.getMainLooper()) {
            @Suppress("UNCHECKED_CAST")
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    Action.BYTES.ordinal ->
                        b.bytes.text = UiTools.bytes(c, Crawler.bytesSinceBoot())
                    Action.SIGNAL.ordinal -> {
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
                    Action.CANDIDATES.ordinal -> {
                        val canNom = msg.obj as List<Candidate>
                        b.candidature.adapter = ListUser(canNom, this@Panel)
                        vis(b.noCan, canNom.isEmpty())
                    }
                    Action.REFRESH.ordinal -> candidature()
                }
            }
        }
        handler?.obtainMessage(Action.BYTES.ordinal)?.sendToTarget()

        // Control the Foreground Service
        Explorer.state.observe(this) { s -> exploring(s) }
        exploring(Explorer.state.value)
        b.start.setOnClickListener {
            if (Explorer.state.value == Explorer.State.CHANGING) return@setOnClickListener
            UiTools.shake(c)
            startService(Intent(this, Explorer::class.java).apply {
                if (Explorer.state.value == Explorer.State.ACTIVE) action = Explorer.code(Explorer.Code.STOP)
            })
        }
        b.start.layoutParams = (b.start.layoutParams as ConstraintLayout.LayoutParams).apply {
            matchConstraintPercentHeight =
                (dm.widthPixels.toFloat() / dm.heightPixels) * matchConstraintPercentWidth
        }

        // Candidates
        b.root.setOnTouchListener(this)

        // Thread { TfUtils.preTrain(c) }.start()
        // TfUtils.test(c, b.face, b.bytes)
    }

    override fun onResume() {
        super.onResume()
        candidature()
    }

    override fun onDestroy() {
        handler = null
        super.onDestroy()
    }

    private var y = 0f
    private var lastMove: Long? = null
    override fun onTouch(v: View, ev: MotionEvent): Boolean = when (ev.action) {
        MotionEvent.ACTION_DOWN -> {
            momenta.stop()
            y = ev.y
            lastMove = SystemClock.elapsedRealtime()
            true
        }
        MotionEvent.ACTION_MOVE -> {
            speed = 1000f / (SystemClock.elapsedRealtime() - lastMove!!)
            (b.start.layoutParams as ConstraintLayout.LayoutParams).verticalBias.apply {
                val dist = y - ev.y
                if (dist > 0f && this > minBias)
                    momenta.move(true, this)
                if (dist <= 0f && this <= maxBias)
                    momenta.move(false, this)
            }
            y = ev.y
            lastMove = SystemClock.elapsedRealtime()
            true
        }
        MotionEvent.ACTION_UP -> if (lastMove != null) {
            lastMove = null
            momenta.fling(speed)
            speed = 0f
            (y - ev.y) < 50f
        } else false
        else -> false
    }

    private val maxBias = 0.5f
    private val minBias = 0f
    private val movePerMove = 0.018f
    private var speed = 0f // moves per second
    private val momenta = object : Momentum() {
        override fun onMove(increment: Boolean, scalar: Float): Float {
            var robotBias = scalar
            if (increment) robotBias -= movePerMove
            else robotBias += movePerMove

            if (robotBias < minBias) robotBias = minBias
            if (robotBias > maxBias) robotBias = maxBias
            b.start.layoutParams = (b.start.layoutParams as ConstraintLayout.LayoutParams)
                .apply { verticalBias = robotBias }
            ((robotBias * 1.75f) + 0.25f).apply {
                b.start.scaleX = this
                b.start.scaleY = this
            }
            b.start.translationY = ((-b.start.height * 0.25f) * (1f - (robotBias * 2f)))

            b.candidature.layoutParams =
                (b.candidature.layoutParams as ConstraintLayout.LayoutParams)
                    .apply { matchConstraintPercentHeight = ((maxBias - robotBias) * 1.66f) }
            ((0.5f - robotBias) * 2f).apply {
                b.noCan.scaleX = this
                b.noCan.scaleY = this
            }
            vis(b.shadow, robotBias < maxBias)
            vis(b.bytes, robotBias >= maxBias)
            b.status.alpha = robotBias * 2f

            return robotBias
        }
    }

    private fun exploring(state: Explorer.State?) {
        b.robot.alpha = if (state == Explorer.State.ACTIVE) 1f else .36f
        vis(b.status, state == Explorer.State.ACTIVE)
    }

    private fun candidature() {
        Thread {
            val canNom = arrayListOf<Candidate>()
            dao.candidates().forEach {
                canNom.add(it.apply { nominee = dao.nomineeById(it.id) })
            }
            handler?.obtainMessage(Action.CANDIDATES.ordinal, canNom)?.sendToTarget()
        }.start()
    }

    enum class Action { BYTES, SIGNAL, CANDIDATES, REFRESH }
}
