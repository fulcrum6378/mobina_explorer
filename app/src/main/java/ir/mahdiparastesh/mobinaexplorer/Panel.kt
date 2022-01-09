package ir.mahdiparastesh.mobinaexplorer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.DisplayMetrics
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import ir.mahdiparastesh.mobinaexplorer.databinding.MainBinding
import ir.mahdiparastesh.mobinaexplorer.misc.Exporter
import ir.mahdiparastesh.mobinaexplorer.room.Candidate
import ir.mahdiparastesh.mobinaexplorer.view.ListUser
import ir.mahdiparastesh.mobinaexplorer.view.Momentum
import ir.mahdiparastesh.mobinaexplorer.view.UiTools
import ir.mahdiparastesh.mobinaexplorer.view.UiTools.Companion.color
import ir.mahdiparastesh.mobinaexplorer.view.UiTools.Companion.vis
import ir.mahdiparastesh.mobinaexplorer.view.UiWork

// adb connect 192.168.1.20:

@SuppressLint("ClickableViewAccessibility")
class Panel : AppCompatActivity(), View.OnTouchListener {
    private lateinit var c: Context
    private lateinit var b: MainBinding
    private lateinit var exporter: Exporter
    private lateinit var dm: DisplayMetrics
    private var anStatus: ObjectAnimator? = null
    private var candidature: ArrayList<Candidate>? = null
    private var canScroll = 0

    companion object {
        const val DISABLED_ALPHA = .4f
        var handler: Handler? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = MainBinding.inflate(layoutInflater)
        setContentView(b.root)
        c = applicationContext
        exporter = Exporter(this@Panel)
        dm = resources.displayMetrics

        // Handler
        handler = object : Handler(Looper.getMainLooper()) {
            @Suppress("UNCHECKED_CAST")
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    Action.WAVE_UP.ordinal ->
                        UiTools.wave(this@Panel, b.start, R.drawable.explosive)
                    Action.WAVE_DOWN.ordinal ->
                        b.bytes.text = UiTools.bytes(c, Crawler.bytesSinceBoot())
                    Action.CANDIDATES.ordinal -> {
                        val scr = canScroll
                        candidature = ArrayList(msg.obj as List<Candidate>)
                        arrangeList()
                        vis(b.noCan, candidature!!.isEmpty())
                        if (candidature!!.isNotEmpty()) b.candidature.scrollBy(0, scr)
                    }
                    Action.REFRESH.ordinal -> candidature()
                    Action.REJECT.ordinal, Action.ACCEPT.ordinal -> (msg.obj as Candidate).apply {
                        candidature?.let { it[it.indexOf(this)] = this }
                        arrangeList()
                    }
                    Action.SUMMARY.ordinal -> AlertDialog.Builder(this@Panel)
                        .setTitle(R.string.app_name)
                        .setMessage(
                            c.getString(R.string.summary).format(*(msg.obj as Array<String>))
                        )
                        .setNeutralButton(R.string.ok, null).create().show()
                }
            }
        }
        handler?.obtainMessage(Action.WAVE_DOWN.ordinal)?.sendToTarget()

        // Control the Foreground Service
        Explorer.state.observe(this) { s -> exploring(s) }
        exploring(Explorer.state.value)
        b.start.setOnClickListener {
            if (Explorer.state.value == Explorer.State.CHANGING) return@setOnClickListener
            UiTools.shake(c)
            startService(Intent(this, Explorer::class.java).apply {
                if (Explorer.state.value == Explorer.State.ACTIVE) action = Explorer.Code.STOP.s
            })
        }
        square(b.start)
        square(b.robot)
        Explorer.status.observe(this) { s ->
            b.status.text = s
            b.status.setTextColor(color(c, R.color.alarm))
            anStatus = ObjectAnimator.ofArgb(
                b.status, "textColor", color(c, R.color.alarm), color(c, R.color.CPO)
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
        b.start.setOnLongClickListener {
            PopupMenu(ContextThemeWrapper(this, R.style.Theme_MobinaExplorer), it).apply {
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.smFollow -> {
                            Explorer.shouldFollow = !item.isChecked
                            true
                        }
                        R.id.smSummary -> {
                            UiWork(c, Action.SUMMARY).start()
                            true
                        }
                        R.id.smExport -> {
                            exporter.launch()
                            true
                        }
                        else -> false
                    }
                }
                inflate(R.menu.start)
                menu.findItem(R.id.smFollow).isChecked = Explorer.shouldFollow
                show()
            }
            true
        }

        // Candidates
        b.root.setOnTouchListener(this)
        b.candidature.viewTreeObserver.addOnScrollChangedListener {
            canScroll = b.candidature.computeVerticalScrollOffset()
        }

        /*UiWork(c, Action.CUSTOM_WORK, UiWork.CustomWork { dao ->
            dao.nominees().forEach { dao.updateNominee(it.apply { anal = false }) }
        }).start()*/
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
    private val movePerMove = 0.021f
    private var speed = 0f // moves per second
    private val momenta = object : Momentum() {
        override fun onMove(increment: Boolean, scalar: Float): Float {
            var robotBias = scalar
            if (increment) robotBias -= movePerMove
            else robotBias += movePerMove

            if (robotBias < minBias) robotBias = minBias
            if (robotBias > maxBias) robotBias = maxBias
            resizeStart(b.start, robotBias)
            resizeStart(b.robot, robotBias)
            b.start.translationY = ((-b.start.height * 0.25f) * (1f - (robotBias * 2f)))
            b.robot.translationY = ((-b.robot.height * 0.1f) * (1f - (robotBias * 2f)))

            b.candidature.layoutParams =
                (b.candidature.layoutParams as ConstraintLayout.LayoutParams)
                    .apply { matchConstraintPercentHeight = ((maxBias - robotBias) * 1.66f) }
            ((0.5f - robotBias) * 2f).apply {
                b.noCan.scaleX = this
                b.noCan.scaleY = this
            }
            vis(b.shadow, robotBias < maxBias)
            vis(b.bytes, robotBias >= maxBias)
            vis(b.canSum, robotBias >= maxBias)
            b.status.alpha = robotBias * 2f

            return robotBias
        }

        fun resizeStart(v: View, robotBias: Float) {
            v.layoutParams = (v.layoutParams as ConstraintLayout.LayoutParams)
                .apply { verticalBias = robotBias }
            ((robotBias * 1.5f) + 0.25f).apply {
                v.scaleX = this
                v.scaleY = this
            }
        }
    }

    private fun exploring(state: Explorer.State?) {
        b.robot.alpha = if (state == Explorer.State.ACTIVE) 1f else DISABLED_ALPHA
        vis(b.status, state == Explorer.State.ACTIVE)
    }

    private fun candidature() {
        UiWork(c, Action.CANDIDATES).start()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun arrangeList() {
        b.canSum.text = getString(
            R.string.canSum, candidature!!.size,
            candidature!!.filter { !it.rejected }.size,
            candidature!!.filter { it.rejected }.size
        )
        candidature?.sortWith(Candidate.Sort(Candidate.Sort.BY_NOM_USER))
        candidature?.sortWith(Candidate.Sort(Candidate.Sort.BY_SCORE))
        candidature?.sortWith(Candidate.Sort(Candidate.Sort.BY_REJECTED))
        if (b.candidature.adapter == null)
            b.candidature.adapter = ListUser(candidature!!, this@Panel)
        else b.candidature.adapter?.notifyDataSetChanged()
    }

    private fun square(v: View) {
        v.layoutParams = (v.layoutParams as ConstraintLayout.LayoutParams).apply {
            matchConstraintPercentHeight =
                (dm.widthPixels.toFloat() / dm.heightPixels) * matchConstraintPercentWidth
        }
    }

    enum class Action {
        CANDIDATES, REJECT, ACCEPT, CUSTOM_WORK, SUMMARY,
        WAVE_UP, WAVE_DOWN, REFRESH
    }
}
