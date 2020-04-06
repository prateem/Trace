package com.meetarp.trace

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * Trace will iterate through the views in a given [ViewGroup] hierarchy and create
 * silhouettes based on whether or not the [View] implements the interface [Traceable]:
 *
 * If the View does implement [Traceable], Trace will create a silhouette
 * from the result of [Path] object returned by the [Traceable.trace] call.
 *
 * If the View does not implement the interface, Trace will use rounded
 * rectangles to create a silhouette based on the boundaries of the view.
 *
 * Important: Views whose visibilities are set to either [View.INVISIBLE]
 * or [View.GONE] will be ignored, as well as any views whose IDs are included
 * in the `exclusions` list specified when identifying the target for `Trace`
 * via [Trace.of].
 */
class Trace @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {

    private var tracedPath: Path? = null
    private val tracePaint: Paint = Paint()
        .also { it.color = ContextCompat.getColor(context, android.R.color.darker_gray) }
    private val boundsRect = RectF()

    private var shimmerWidth = 0.33f
    private val shimmerPaint = Paint()
        .also { it.alpha = 0x40 } // Hex for 25% alpha

    private var shimmerColor = ContextCompat.getColor(context, android.R.color.white)
    private val shimmerShape = Path()
    private val shimmerShapeOffset = Path()
    private val shimmerPath = Path()

    private var shimmerAnimator: Animator? = null
    private var shimmerProgress = 0

    private val transparent = ContextCompat.getColor(context, android.R.color.transparent)

    /**
     * Perform a trace on all the views in the hierarchy of the given [root].
     * The trace will cause this view to draw a silhouette of items in the given hierarchy.
     * Views whose visibilities are not [View.VISIBLE] will be ignored.
     *
     * Any views in the hierarchy that implement [Traceable] will defer to its implementation of
     * [Traceable.trace] to determine the path(s) to add to this Trace.
     *
     * If a view does not implement [Traceable] then a silhouette is drawn based on logical bounds.
     *
     * @param root The root view of the desired trace
     * @param exclusions IDs to exclude in the trace
     * @see Traceable
     */
    fun of(root: ViewGroup, exclusions: List<Int> = emptyList()): Trace {
        root.post {
            tracedPath = traceInternal(root, Path(), exclusions)
                .also { path -> path.computeBounds(boundsRect, true) }

            boundsRect.set(
                0f,
                0f,
                boundsRect.width() ,
                boundsRect.height()
            )

            shimmerShape.reset()
            shimmerShape.lineTo(boundsRect.width() * shimmerWidth, 0f)
            shimmerShape.lineTo(boundsRect.width() * shimmerWidth, boundsRect.height())
            shimmerShape.lineTo(0f, boundsRect.height())
            shimmerShape.close()

            updateShimmerShader()

            Log.d(LOG_TAG, "BoundsRect: $boundsRect")
            requestLayout()
        }
        return this
    }

    /**
     * Set the [color] for the traced silhouette segments.
     * Must be a color resource integer.
     * If not set specifically, the default is [android.R.color.darker_gray]
     */
    fun colored(@ColorRes color: Int): Trace {
        tracePaint.color = ContextCompat.getColor(context, color)
        invalidate()
        return this
    }

    /**
     * Set the [color] for the shimmer that animates when [startShimmer] is running.
     * Must be a color resource integer.
     * If not set specifically, the default is [android.R.color.white]
     */
    fun shimmerColored(@ColorRes color: Int): Trace {
        shimmerColor = ContextCompat.getColor(context, color)
        updateShimmerShader()
        invalidate()
        return this
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return
        val traced = tracedPath ?: return

        canvas.drawPath(traced, tracePaint)

        if (shimmerAnimator != null) {
            shimmerPath.reset()

            // Copy the shape and offset it. Easier to copy+offset than to reset previous offsets.
            shimmerShapeOffset.reset()
            shimmerShapeOffset.set(shimmerShape)
            shimmerShapeOffset.offset(boundsRect.right * (shimmerProgress / 100f), 0f)

            // Draw only the intersection of the (offset) shape and traced paths for shimmering
            shimmerPath.op(shimmerShapeOffset, traced, Path.Op.INTERSECT)
            canvas.drawPath(shimmerPath, shimmerPaint)
        }
    }

    private fun traceInternal(
        root: ViewGroup,
        path: Path,
        exclusions: List<Int>,
        offset: PointF = PointF(0f, 0f)
    ): Path {
        val children = (0 until root.childCount).map { i -> root.getChildAt(i) }

        for (child in children) {
            if (child.id in exclusions || child.visibility != VISIBLE)
                continue

            val childLeft = child.left.toFloat() + offset.x
            val childTop = child.top.toFloat() + offset.y
            path.moveTo(childLeft, childTop)

            when (child) {
                is Traceable -> path.addPath(child.trace(), childLeft, childTop)
                is ViewGroup -> traceInternal(child, path, exclusions, PointF(childLeft, childTop))
                is TextView -> {
                    val textContent = child.text.toString()
                    val lineCount = child.lineCount
                    val lineHeight = child.paint.fontMetrics.run { bottom - top }

                    val textBounds = Rect()
                    for (line in 0 until lineCount) {
                        val textForLine = textContent.substring(
                            child.layout.getLineStart(line),
                            child.layout.getLineEnd(line)
                        )

                        child.paint.getTextBounds(textForLine, 0, textForLine.length, textBounds)

                        val lineOffset = (line * lineHeight)
                        val rect = RectF(
                            childLeft + SPACE,
                            childTop + SPACE + lineOffset,
                            childLeft + textBounds.width() - SPACE,
                            childTop + lineHeight - SPACE + lineOffset
                        )
                        path.addRoundRect(rect, R_RECT_RADIUS, R_RECT_RADIUS, Path.Direction.CW)
                    }
                }
                else -> {
                    val rect = RectF(
                        childLeft + SPACE,
                        childTop + SPACE,
                        childLeft + child.measuredWidth - SPACE,
                        childTop + child.measuredHeight - SPACE
                    )
                    path.addRoundRect(rect, R_RECT_RADIUS, R_RECT_RADIUS, Path.Direction.CW)
                }
            }
        }

        return path
    }

    /**
     * Start the shimmer animation over the traced silhouette.
     * @param shimmerSpeed The period of the shimmer in milliseconds. Default 1000.
     */
    fun startShimmer(shimmerSpeed: Long = 1000) {
        shimmerProgress = 0
        shimmerAnimator = ValueAnimator.ofInt(0, 100)
            .also { anim ->
                anim.duration = shimmerSpeed
                anim.interpolator = FastOutSlowInInterpolator()
                anim.addUpdateListener {
                    shimmerProgress = anim.animatedValue as Int
                    invalidate()
                }
                anim.repeatCount = ValueAnimator.INFINITE
                anim.repeatMode = ValueAnimator.RESTART
                anim.start()
            }
    }

    /**
     * Stop the shimmer animation over the traced silhouette.
     */
    fun stopShimmer() {
        shimmerAnimator?.cancel()

        shimmerAnimator = null
        shimmerProgress = 0
        invalidate()
    }

    private fun updateShimmerShader() {
        shimmerPaint.shader = LinearGradient(
            0f,
            0f,
            boundsRect.width(),
            0f,
            intArrayOf(
                transparent,
                shimmerColor,
                transparent,
                shimmerColor,
                transparent
            ),
            floatArrayOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f),
            Shader.TileMode.REPEAT
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (tracedPath != null) {
            setMeasuredDimension(
                boundsRect.width().toInt(),
                boundsRect.height().toInt()
            )
        }
    }

    companion object {
        private const val LOG_TAG = "Trace"
        private const val SPACE = 2.5f
        private const val R_RECT_RADIUS = 10f
    }

}