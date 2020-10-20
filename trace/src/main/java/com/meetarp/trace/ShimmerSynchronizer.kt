package com.meetarp.trace

import android.animation.ValueAnimator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Used to synchronize animation progress across multiple Trace instances.
 * Can be used as an argument for [TraceContainer.startShimmer] or
 * [Trace.syncWith].
 */
class ShimmerSynchronizer(private val shimmerSpeed: Long = 1200) {

    var shimmerProgress: Int = 0
        private set

    val isStarted: Boolean
        get() = animator.isStarted

    // Use WeakReferences so we never prevent View cleanup
    // CopyOnWriteArray so that we can modify while reading if we absolutely need to
    private val observers = CopyOnWriteArrayList<WeakReference<Trace>>()

    private val animator = ValueAnimator.ofInt(0, 100)
        .also { anim ->
            anim.duration = shimmerSpeed
            anim.interpolator = FastOutSlowInInterpolator()
            anim.addUpdateListener {
                shimmerProgress = anim.animatedValue as Int

                observers.toList().forEach {
                    it.get()?.invalidate()
                        ?: observers.remove(it)
                }
            }
            anim.repeatCount = ValueAnimator.INFINITE
            anim.repeatMode = ValueAnimator.RESTART
            anim.start()
        }

    fun register(observer: Trace) {
        observers.removeAll { it.get() == observer || it.get() == null }
        observers.add(WeakReference(observer))
        start()
    }

    fun unregister(observer: Trace) {
        observers.removeAll { it.get() == observer || it.get() == null }
        if (observers.isEmpty()) {
            stop()
        }
    }

    private fun start() {
        if (!isStarted) {
            animator.start()
        }
    }

    private fun stop() {
        animator.cancel()
    }

}