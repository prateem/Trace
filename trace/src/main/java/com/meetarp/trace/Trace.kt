package com.meetarp.trace

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.util.LayoutDirection
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.marginLeft
import androidx.core.view.marginTop
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import kotlin.math.max

/**
 * Trace will iterate through the views in a given [View] hierarchy and create
 * silhouettes based on whether or not the [View] implements the interface [Traceable]:
 *
 * If a View does implement [Traceable], Trace will create a silhouette
 * from the result of [Path] object returned by the [Traceable.trace] call.
 *
 * If a View does not implement the [Traceable] interface, Trace will try to use the user-provided
 * [TraceDelegate], if one was supplied. If a delegate isn't provided or if [TraceDelegate.handle]
 * does not return true, then Trace will hand-off to the [DefaultTraceDelegate]. The default
 * delegate will handle some basic views elegantly but otherwise utilizes rounded rectangles
 * to create a silhouette based on the boundaries of the view.
 *
 * Note: Views whose IDS are found in the `exclusions` list are ignored. The exclusions list can
 * be specified when calling [TraceContainer.startShimmer] or when identifying the target for
 * `Trace` via [Trace.of].
 *
 * Note: The default implementation of [TraceDelegate] will ignore views whose visibilities are
 * set to either [View.INVISIBLE] or [View.GONE].
 */
class Trace @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {

    private var tracedPath: Path? = null
    private val tracePaint: Paint = Paint()
        .also {
            it.color = ContextCompat.getColor(context, android.R.color.darker_gray)
            it.isAntiAlias = true
        }
    private val boundsRect = RectF()

    private var shimmerWidth = 0.33f
    private val shimmerPaint = Paint()
        .also {
            it.alpha = (255 * 0.15).toInt()
            it.isAntiAlias = true
        }

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
     * Views whose IDs can be found in [exclusions] will be ignored.
     *
     * Any views in the hierarchy that implement [Traceable] will defer to its implementation of
     * [Traceable.trace] to determine the path(s) to add to this Trace.
     *
     * If a view does not implement [Traceable] then the [delegate]'s [TraceDelegate.handle]
     * method will be called on it, if a delegate is provided. If not handled, then the
     * default delegate will create a silhouette drawn based on logical bounds.
     *
     * @param root The root view of the desired trace
     * @param delegate The user-defined [TraceDelegate] to handle drawing silhouettes
     *  for non-[Traceable] views.
     * @param exclusions IDs to exclude in the trace
     *
     * @see Traceable
     * @see TraceDelegate
     */
    fun of(
        root: View,
        delegate: TraceDelegate? = null,
        exclusions: List<Int> = emptyList()
    ): Trace {
        root.post {
            // We need to set the initial offset such that we negate the effect of
            // initial margins or padding or else everything will shifted inaccurately
            val rootParent = root.parent as View
            val initialOffset = PointF(
                -1f * (root.marginLeft + rootParent.paddingLeft),
                -1f * (root.marginTop + rootParent.paddingTop)
            )

            // Since Path is drawn on a canvas with relative offsets, we need to account
            // for the left offset of a View that may be WRAP_CONTENT in a RTL layout.
            if (rootParent.layoutDirection == LayoutDirection.RTL) {
                initialOffset.offset(-1f * root.left, 0f)
            }

            tracedPath = traceInternal(root, Path(), exclusions, initialOffset, delegate)
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

            Log.d(LOG_TAG, "BoundsRect: $boundsRect")
            requestLayout()
        }

        return this
    }

    private fun traceInternal(
        view: View,
        path: Path,
        exclusions: List<Int>,
        offset: PointF,
        delegate: TraceDelegate?
    ): Path {
        val viewLeft = max(view.left.toFloat() + offset.x, 0f)
        val viewTop = max(view.top.toFloat() + offset.y, 0f)
        val newOffset = PointF(viewLeft, viewTop)

        /**
         * Try to ensure that the trace looks about right in terms of sizing, including margins, etc.
         *
         * The reason addRect is called twice here is because due to a combination of the Path
         * fillType (WINDING - default) and the alternating Path directions of the calls,
         * they effectively negate each other and thus THESE particular two addRect calls do not
         * cause any pixels to be filled in.
         *
         * See: https://bit.ly/3lYzm9h for more info
         */
        RectF().let {
            it.set(viewLeft, viewTop, viewLeft + view.width, viewTop + view.height)
            path.addRect(it, Path.Direction.CW)
            path.addRect(it, Path.Direction.CCW)
        }

        return when (view) {
            is ViewGroup -> traceViewGroup(view, path, exclusions, newOffset, delegate)
            else -> traceView(view, path, exclusions, newOffset, delegate)
        }
    }

    private fun traceViewGroup(
        root: ViewGroup,
        path: Path,
        exclusions: List<Int>,
        offset: PointF,
        delegate: TraceDelegate?
    ): Path {
        val children = (0 until root.childCount).map { i -> root.getChildAt(i) }
        for (child in children) {
            traceInternal(child, path, exclusions, offset, delegate)
        }
        return path
    }

    private fun traceView(
        view: View,
        path: Path,
        exclusions: List<Int>,
        offset: PointF,
        delegate: TraceDelegate?
    ): Path {
        if (view.id in exclusions)
            return path

        val viewLeft = offset.x
        val viewTop = offset.y

        // Handling traceable is done by simply utilizing trace() and
        // ensuring it is drawn properly based on layout direction.
        if (view is Traceable) {
            if (view.isVisible) {
                val tracedPath = view.trace()
                val bounds = RectF().also { tracedPath.computeBounds(it, true) }
                val left =
                    if (view.layoutDirection == LayoutDirection.RTL)
                        viewLeft - bounds.width()
                    else
                        viewLeft

                path.addPath(view.trace(), left, viewTop)
            }

            return path
        }

        // If a user-defined delegate exists and handles the given view, then move on.
        // Otherwise, hand-off to the default delegate.
        if (delegate?.handle(view, path, exclusions, offset) == true) {
            return path
        }
        DefaultTraceDelegate.handle(view, path, exclusions, offset)

        return path
    }

    /**
     * Set the [color] for the traced silhouette segments.
     * If not set specifically, the default is [android.R.color.darker_gray]
     */
    fun setColorResource(@ColorRes color: Int): Trace {
        return colored(ContextCompat.getColor(context, color))
    }

    /**
     * Set the [color] for the traced silhouette segments.
     * If not set specifically, the default is [android.R.color.darker_gray]
     */
    fun colored(@ColorInt color: Int): Trace {
        tracePaint.color = color
        invalidate()
        return this
    }

    /**
     * Set the [color] for the shimmer that animates when [startShimmer] is running.
     * If not set specifically, the default is [android.R.color.white]
     */
    fun setShimmerColorResource(@ColorRes color: Int): Trace {
        return shimmerColored(ContextCompat.getColor(context, color))
    }

    /**
     * Set the [color] for the shimmer that animates when [startShimmer] is running.
     * If not set specifically, the default is [android.R.color.white]
     */
    fun shimmerColored(@ColorInt color: Int): Trace {
        shimmerColor = color
        invalidate()
        return this
    }

    /**
     * Start the shimmer animation over the traced silhouette.
     * @param shimmerSpeed The period of the shimmer in milliseconds. Default 1000.
     */
    fun startShimmer(shimmerSpeed: Long = 1000) {
        if (shimmerAnimator != null) {
            if (shimmerAnimator?.duration != shimmerSpeed) {
                shimmerAnimator?.duration = shimmerSpeed
            }
            return
        }

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

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return
        val traced = tracedPath ?: return

        canvas.drawPath(traced, tracePaint)

        if (shimmerAnimator != null) {
            updateShimmerShader()
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (tracedPath != null) {
            setMeasuredDimension(
                boundsRect.width().toInt(),
                boundsRect.height().toInt()
            )
        }
    }

    private fun updateShimmerShader() {
        val shimmerStartPos = boundsRect.right * (shimmerProgress / 100f)
        val shimmerShapeWidth = boundsRect.width() * shimmerWidth
        shimmerPaint.shader = LinearGradient(
            shimmerStartPos,
            0f,
            shimmerStartPos + shimmerShapeWidth,
            0f,
            intArrayOf(
                transparent,
                shimmerColor,
                shimmerColor,
                transparent
            ),
            floatArrayOf(0.0f, 0.25f, 0.75f, 1.0f),
            Shader.TileMode.REPEAT
        )
    }

    companion object {
        private const val LOG_TAG = "Trace"
    }

}