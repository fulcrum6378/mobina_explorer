package ir.mahdiparastesh.mobinaexplorer.view

import android.os.CountDownTimer

abstract class Momentum {
    private var increment: Boolean? = null
    private var scalar: Float? = null
    private var continuum = false

    fun move(increment: Boolean, scalar: Float) {
        this.increment = increment
        val moved = onMove(increment, scalar)
        if (moved == this.scalar) stop()
        this.scalar = moved
    }

    abstract fun onMove(increment: Boolean, scalar: Float): Float

    fun fling(speed: Float, user: Boolean = true) {
        if (user) continuum = true
        if (increment == null || scalar == null || speed <= 0f || !continuum) {
            continuum = false
            return
        }

        move(increment!!, scalar!!)
        val dur = (1000f / speed).toLong()
        object : CountDownTimer(dur, dur) {
            override fun onTick(until: Long) {}
            override fun onFinish() {
                if (continuum) fling((speed * 0.999f) - 2.5f, false)
            }
        }.start()
    }

    fun stop() {
        continuum = false
        increment = null
        scalar = null
    }
}
