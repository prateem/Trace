package com.meetarp.trace

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.animation.addListener
import androidx.core.content.ContextCompat
import androidx.core.view.children
import kotlin.math.max

class TraceContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    /**
     * The color of the traced silhouette.
     * Default: android.R.color.darker_grey
     */
    @ColorInt
    var traceSilhouetteColor: Int = ContextCompat.getColor(context, android.R.color.darker_gray)

    /**
     * The color of the shimmer that runs across the traced silhouette.
     * Default: android.R.color.white
     */
    @ColorInt
    var traceShimmerColor: Int = ContextCompat.getColor(context, android.R.color.white)

    /**
     * Toggle for enabling or disabling cross-fade animation when starting or stopping shimmer.
     * Default: true
     */
    var crossFadeEnabled: Boolean = true

    /**
     * If [crossFadeEnabled] is true, this value controls the duration of the cross-fade animation.
     * Default: 750 ms
     */
    var crossFadeDuration: Long = DEFAULT_CROSS_FADE_DURATION

    private var allowSecondChild = false
    private val enabledStateMap: MutableMap<Int, Boolean> = mutableMapOf()
    private var crossFadeAnim: ValueAnimator? = null

    init {
        val componentTypedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.TraceContainer,
            defStyleAttr,
            defStyleRes
        )

        try {
            traceSilhouetteColor = componentTypedArray.getColor(
                R.styleable.TraceContainer_trace_silhouetteColor,
                traceSilhouetteColor
            )
            traceShimmerColor = componentTypedArray.getColor(
                R.styleable.TraceContainer_trace_shimmerColor,
                traceShimmerColor
            )
            crossFadeEnabled = componentTypedArray.getBoolean(
                R.styleable.TraceContainer_trace_crossFadeEnabled,
                true
            )
            crossFadeDuration = componentTypedArray.getInt(
                R.styleable.TraceContainer_trace_crossFadeDuration,
                DEFAULT_CROSS_FADE_DURATION.toInt()
            ).toLong()
        } finally {
            componentTypedArray.recycle()
        }
    }

    fun startShimmer(
        shimmerSpeed: Long = 1200,
        delegate: TraceDelegate? = null,
        shouldExcludeView: ((View) -> Boolean)? = null,
        crossFade: Boolean = true,
        synchronizer: ShimmerSynchronizer? = null
    ) {
        if (childCount == 0) {
            return
        }

        val target = getChildAt(0)
        val tracedView = Trace(context)
            .of(target, delegate, shouldExcludeView)
            .colored(traceSilhouetteColor)
            .shimmerColored(traceShimmerColor)
            .syncWith(synchronizer)
            .also {
                if (crossFade) {
                    it.alpha = 0f
                }
                it.startShimmer(shimmerSpeed)
            }

        children.filterIsInstance<Trace>()
            .forEach { removeView(it) }

        allowSecondChild = true
        if (tracedView.parent != this)
            addView(tracedView)
        allowSecondChild = false

        // Hide target and prevent click events from reaching it or any of its children
        synchronized(enabledStateMap) {
            if (enabledStateMap.isNotEmpty()) {
                resetTargetViewEnabledStates(target)
            }
            disableTargetViews(target)
        }

        crossFadeAnim?.cancel()
        if (crossFade && crossFadeEnabled && crossFadeDuration > 0) {
            crossFadeAnim = ValueAnimator.ofFloat(0f, 1f)
                .also {
                    it.duration = crossFadeDuration
                    it.addUpdateListener { anim ->
                        val progress = anim.animatedValue as Float
                        target.alpha = max(0f, target.alpha - progress)
                        tracedView.alpha = progress
                        invalidate()
                    }
                    it.addListener(
                        onEnd = {
                            // Just to be extra-sure
                            target.alpha = 0f
                            tracedView.alpha = 1f
                            crossFadeAnim = null
                            invalidate()
                        }
                    )
                    it.start()
                }
        } else {
            target.alpha = 0f
            tracedView.alpha = 1f
            crossFadeAnim = null
        }

        invalidate()
    }

    fun stopShimmer(crossFade: Boolean = true) {
        val tracedView = findTracedView()
        if (childCount == 0 || tracedView == null) {
            return
        }

        val target = getChildAt(0)
        synchronized(enabledStateMap) {
            resetTargetViewEnabledStates(target)
            enabledStateMap.clear()
        }

        crossFadeAnim?.cancel()
        if (crossFade && crossFadeEnabled && crossFadeDuration > 0) {
            crossFadeAnim = ValueAnimator.ofFloat(0f, 1f)
                .also {
                    it.duration = crossFadeDuration
                    it.addUpdateListener { anim ->
                        val progress = anim.animatedValue as Float
                        target.alpha = progress
                        tracedView.alpha = 1f - progress
                        invalidate()
                    }
                    it.addListener(
                        onEnd = {
                            // Just to be extra-sure
                            target.alpha = 1f
                            tracedView.alpha = 0f
                            removeView(tracedView)
                            crossFadeAnim = null
                            invalidate()
                        }
                    )
                    it.start()
                }
        } else {
            target.alpha = 1f
            tracedView.alpha = 0f
            removeView(tracedView)
            crossFadeAnim = null
        }
        invalidate()
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if ((childCount < 1 || allowSecondChild)) {
            return super.addView(child, index, params)
        }

        throw IllegalStateException("TraceContainer may only have one child.")
    }

    private fun findTracedView(): Trace? {
        return children.filterIsInstance<Trace>().firstOrNull()
    }

    private fun disableTargetViews(target: View) {
        when (target) {
            is ViewGroup -> target.children.forEach { disableTargetViews(it) }
            else -> {
                if (target.id == View.NO_ID) {
                    target.id = View.generateViewId()
                }
                enabledStateMap[target.id] = target.isEnabled
                target.isEnabled = false
            }
        }
    }

    private fun resetTargetViewEnabledStates(target: View) {
        when (target) {
            is ViewGroup -> target.children.forEach { resetTargetViewEnabledStates(it) }
            else -> {
                target.isEnabled = enabledStateMap[target.id] ?: true
            }
        }
    }

    companion object {
        private const val DEFAULT_CROSS_FADE_DURATION = 750L
    }

}