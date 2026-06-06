package io.particle.commonui

import android.os.SystemClock
import android.view.View
import kotlin.math.cos


/**
 * A "breathing" alpha glow on [view] — mirroring a Particle device's cyan LED — but updated at
 * ~15fps instead of on every display frame.
 *
 * A continuously-running View animation invalidates the view every vsync (60–120fps), forcing the
 * renderer to redraw indefinitely. That's invisible on real hardware but pegs the emulator's
 * software renderer (badly stuttering host audio) and wastes power. Driving the alpha from a ~66ms
 * tick keeps the same effect while cutting the redraw rate ~4–8x.
 *
 * Call [start] when the indicator should glow (device online) and [stop] when it shouldn't, or when
 * the hosting view is recycled/detached, so the posted callbacks don't leak.
 */
class BreathingGlow(
    private val view: View,
    private val periodMs: Long = 2000L,
    private val minAlpha: Float = 0.2f,
    private val maxAlpha: Float = 1.0f,
    private val frameMs: Long = 66L  // ~15 fps
) {

    private var running = false
    private var startTime = 0L

    private val tick: Runnable = object : Runnable {
        override fun run() {
            if (!running) return
            val phase = ((SystemClock.uptimeMillis() - startTime) % periodMs).toFloat() / periodMs
            // cosine ease: maxAlpha -> minAlpha -> maxAlpha over one period
            val eased = (cos(phase.toDouble() * 2.0 * Math.PI).toFloat() + 1f) / 2f
            view.alpha = minAlpha + (maxAlpha - minAlpha) * eased
            view.postDelayed(this, frameMs)
        }
    }

    fun start() {
        if (running) return
        running = true
        startTime = SystemClock.uptimeMillis()
        view.removeCallbacks(tick)
        view.postOnAnimation(tick)
    }

    fun stop() {
        running = false
        view.removeCallbacks(tick)
        view.alpha = maxAlpha
    }
}
