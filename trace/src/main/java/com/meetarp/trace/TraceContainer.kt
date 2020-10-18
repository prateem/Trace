package com.meetarp.trace

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.view.children

class TraceContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    @ColorInt
    private var traceSilhouetteColor: Int = 0

    @ColorInt
    private var traceShimmerColor: Int = 0

    private var trace: Trace? = null
    private var allowSecondChild = false
    private val enabledStateMap: MutableMap<Int, Boolean> = mutableMapOf()

    init {
        val componentTypedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.TraceContainer,
            defStyleAttr,
            defStyleRes
        )

        try {
            traceSilhouetteColor = componentTypedArray.getColor(
                R.styleable.TraceContainer_trace_silhouetteColor,
                ContextCompat.getColor(context, android.R.color.darker_gray)
            )
            traceShimmerColor = componentTypedArray.getColor(
                R.styleable.TraceContainer_trace_shimmerColor,
                ContextCompat.getColor(context, android.R.color.white)
            )
        } finally {
            componentTypedArray.recycle()
        }
    }

    fun startShimmer(
        shimmerSpeed: Long = 1200,
        delegate: TraceDelegate? = null,
        shouldExcludeView: ((View) -> Boolean)? = null
    ) {
        if (childCount == 0) {
            return
        }

        if (trace != null) {
            trace?.startShimmer(shimmerSpeed)
            return
        }

        val target = getChildAt(0)
        allowSecondChild = true
        trace = Trace(context)
            .of(target, delegate, shouldExcludeView)
            .colored(traceSilhouetteColor)
            .shimmerColored(traceShimmerColor)
            .also { it.startShimmer(shimmerSpeed) }

        addView(trace)
        allowSecondChild = false

        // Hide target and prevent click events from reaching it or any of its children
        target.alpha = 0f
        disableTargetViews(target)

        invalidate()
    }

    fun stopShimmer() {
        if (childCount == 0 || trace == null) {
            return
        }

        val target = getChildAt(0)
        target.alpha = 1f
        resetTargetViewEnabledStates(target)
        enabledStateMap.clear()

        removeView(trace)
        trace = null
        invalidate()
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (childCount < 1 || allowSecondChild) {
            return super.addView(child, index, params)
        }

        throw IllegalStateException("TraceContainer may only have one child.")
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

}