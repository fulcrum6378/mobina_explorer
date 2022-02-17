package ir.mahdiparastesh.mobinaexplorer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.*
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModel
import ir.mahdiparastesh.mobinaexplorer.databinding.MainBinding
import ir.mahdiparastesh.mobinaexplorer.misc.Exporter
import ir.mahdiparastesh.mobinaexplorer.room.Candidate
import ir.mahdiparastesh.mobinaexplorer.view.ListUser
import ir.mahdiparastesh.mobinaexplorer.view.Momentum
import ir.mahdiparastesh.mobinaexplorer.view.UiTools
import ir.mahdiparastesh.mobinaexplorer.view.UiTools.Companion.color
import ir.mahdiparastesh.mobinaexplorer.view.UiTools.Companion.dm
import ir.mahdiparastesh.mobinaexplorer.view.UiTools.Companion.square
import ir.mahdiparastesh.mobinaexplorer.view.UiTools.Companion.vis
import ir.mahdiparastesh.mobinaexplorer.view.UiWork

// adb connect 192.168.1.20:

@SuppressLint("ClickableViewAccessibility", "NotifyDataSetChanged")
class Panel : ComponentActivity(), View.OnTouchListener {
    lateinit var c: Context
    private lateinit var b: MainBinding
    val m: Model by viewModels()
    private lateinit var exporter: Exporter
    private var anStatus: ObjectAnimator? = null
    private var canScroll = 0

    class Model : ViewModel() {
        var candidature: ArrayList<Candidate>? = null
        var listWhat = 0
        var sortBy = 0
    }

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
                        m.candidature = ArrayList(msg.obj as List<Candidate>)
                        arrangeList()
                    }
                    Action.REFRESH.ordinal, Action.REPAIR.ordinal -> candidature()
                    Action.UPDATE.ordinal -> if (m.candidature != null) (msg.obj as Candidate).apply {
                        val fnd = Candidate.findPosInList(id, m.candidature!!)
                        if (fnd != -1) m.candidature!![fnd] = this
                        b.candidature.adapter?.notifyDataSetChanged()
                        canSum()
                    }
                    Action.SUMMARY.ordinal -> AlertDialog.Builder(this@Panel)
                        .setTitle(R.string.app_name)
                        .setMessage(
                            c.getString(R.string.summary).format(*(msg.obj as Array<String>))
                        )
                        .setNeutralButton(R.string.ok, null).create().show()
                    Action.USER_LINK.ordinal -> (msg.obj as String?).apply {
                        if (this != null)
                            b.status.setOnClickListener { UiTools.openProfile(this@Panel, this) }
                        else b.status.setOnClickListener(null)
                    }
                    Action.NO_REM_PV.ordinal ->
                        Toast.makeText(c, R.string.noRemPv, Toast.LENGTH_LONG).show()
                    Action.HANDLE_TEST.ordinal -> (msg.obj as Analyzer.Transit)
                        .apply { listener.onFinished(results) }
                    Fetcher.HANDLE_VOLLEY -> (msg.obj as Fetcher.Listener.Transit)
                        .apply { Thread { listener.onFinished(response) }.start() }
                }
            }
        }

        // Robotic Start Button
        square(b.start)
        square(b.robot)
        Explorer.state.observe(this) { s -> exploring(s) }
        exploring(Explorer.state.value)
        b.start.setOnClickListener {
            PopupMenu(ContextThemeWrapper(this, R.style.Theme_MobinaExplorer), it).apply {
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.smStart -> toggle()
                        R.id.smFollow -> {
                            Explorer.shouldFollow = !item.isChecked; true; }
                        R.id.smOnlyPv -> {
                            Explorer.onlyPv = !item.isChecked; true; }
                        R.id.smSummary -> {
                            UiWork(this@Panel, Action.SUMMARY).start(); true; }
                        R.id.smExport -> {
                            exporter.launch(); true; }
                        R.id.smListCan -> listWhich(0)
                        R.id.smListRej -> listWhich(1)
                        R.id.smListObc -> listWhich(2)
                        R.id.smSortAlphabetically -> sortThemBy(0)
                        R.id.smSortByDate -> sortThemBy(1)
                        else -> false
                    }
                }
                inflate(R.menu.start)
                menu.findItem(R.id.smStart).title = getString(
                    if (Explorer.state.value == Explorer.State.ACTIVE) R.string.smStop
                    else R.string.smStart
                )
                menu.findItem(R.id.smFollow).isChecked = Explorer.shouldFollow
                menu.findItem(R.id.smOnlyPv).isChecked = Explorer.onlyPv
                menu.findItem(R.id.smListCan).isChecked = m.listWhat == 0
                menu.findItem(R.id.smListRej).isChecked = m.listWhat == 1
                menu.findItem(R.id.smListObc).isChecked = m.listWhat == 2
                menu.findItem(R.id.smSortAlphabetically).isChecked = m.sortBy == 0
                menu.findItem(R.id.smSortByDate).isChecked = m.sortBy == 1
                show()
            }
        }
        b.start.setOnLongClickListener { toggle() }

        // Status
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

        // Candidates
        b.root.setOnTouchListener(this)
        b.candidature.viewTreeObserver.addOnScrollChangedListener {
            canScroll = b.candidature.computeVerticalScrollOffset()
        }
        if (m.candidature == null) candidature()
        else arrangeList()

        // UiWork(c, Action.CUSTOM_WORK, UiWork.CustomWork { dao -> }).start()
        // Thread { TfUtils.preTrain(c) }.start()
        // TfUtils.test(c, b.face, b.bytes, Mobina(c).seventh)
        handler?.obtainMessage(Action.WAVE_DOWN.ordinal)?.sendToTarget()
    }

    override fun onDestroy() {
        handler = null
        super.onDestroy()
    }

    private fun toggle(): Boolean {
        if (Explorer.state.value == Explorer.State.CHANGING) return false
        UiTools.shake(c)
        startService(Intent(this, Explorer::class.java).apply {
            if (Explorer.state.value == Explorer.State.ACTIVE) action = Explorer.Code.STOP.s
        })
        return true
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

    private fun listWhich(i: Int): Boolean {
        if (i == m.listWhat) return false
        m.listWhat = i
        candidature()
        return true
    }

    private fun sortThemBy(i: Int): Boolean {
        if (i == m.sortBy) return false
        m.sortBy = i
        arrangeList()
        return true
    }

    private fun candidature() {
        UiWork(this@Panel, Action.CANDIDATES).start()
    }

    private fun arrangeList() {
        canSum()
        when (m.sortBy) {
            1 -> m.candidature?.sortBy { it.added }
            else -> m.candidature?.sortBy { it.nominee?.user }
        }
        m.candidature?.sortByDescending { it.score }
        if (b.candidature.adapter == null) {
            val scr = canScroll
            b.candidature.adapter = ListUser(this@Panel)
            if (!m.candidature.isNullOrEmpty()) b.candidature.scrollBy(0, scr)
        } else b.candidature.adapter?.notifyDataSetChanged()

        vis(b.noCan, m.candidature!!.isEmpty())
    }

    private fun canSum() {
        b.canSum.text = getString(
            when (m.listWhat) {
                1 -> R.string.canSumRej
                2 -> R.string.canSumObc
                else -> R.string.canSumNrm
            }, m.candidature!!.size
        )
    }

    enum class Action {
        CANDIDATES, UPDATE, CUSTOM_WORK, SUMMARY, REPAIR,
        WAVE_UP, WAVE_DOWN, REFRESH, USER_LINK, NO_REM_PV, HANDLE_TEST
    }
}
