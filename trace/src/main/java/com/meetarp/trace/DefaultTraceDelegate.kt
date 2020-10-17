package com.meetarp.trace

import android.annotation.SuppressLint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.LayoutDirection
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.CompoundButtonCompat
import kotlin.math.floor
import kotlin.math.min

internal object DefaultTraceDelegate : TraceDelegate {

    // This implementation will always return true.
    override fun handle(view: View, path: Path, exclusion: List<Int>, offset: PointF): Boolean {
        if (!view.isVisible) {
            return true
        }

        val viewLeft = offset.x
        val viewTop = offset.y

        // Elegantly handle some specific view types...
        // ... otherwise make a simple rounded rect based on bounds
        when (view) {
            is CheckBox -> {
                val button = CompoundButtonCompat.getButtonDrawable(view)
                if (button != null) {
                    val buttonWidth = button.intrinsicWidth
                    val buttonHeight = button.intrinsicHeight

                    val checkboxOffsetX = buttonWidth / 4
                    val checkboxSizeX = buttonWidth / 2
                    val checkboxSizeY = buttonHeight / 2

                    val checkboxBounds = RectF()
                    if (view.layoutDirection == LayoutDirection.RTL) {
                        checkboxBounds.set(
                            viewLeft + view.width - checkboxOffsetX - checkboxSizeX,
                            viewTop + (view.height / 2) - (checkboxSizeY / 2),
                            viewLeft + view.width - checkboxOffsetX,
                            viewTop + (view.height / 2) + (checkboxSizeY / 2)
                        )
                    } else {
                        checkboxBounds.set(
                            viewLeft + checkboxOffsetX,
                            viewTop + (view.height / 2) - (checkboxSizeY / 2),
                            viewLeft + checkboxOffsetX + checkboxSizeX,
                            viewTop + (view.height / 2) + (checkboxSizeY / 2)
                        )
                    }
                    path.addRoundRect(
                        checkboxBounds,
                        R_RECT_RADIUS,
                        R_RECT_RADIUS,
                        Path.Direction.CW
                    )
                }

                createMultilineTextPath(view, path, offset)
            }
            is RadioButton -> {
                val button = CompoundButtonCompat.getButtonDrawable(view)
                if (button != null) {
                    val buttonWidth = button.intrinsicWidth

                    val radioCenter =
                        if (view.layoutDirection == LayoutDirection.RTL)
                            PointF(
                                viewLeft + view.width - buttonWidth / 2,
                                viewTop + view.height / 2
                            )
                        else
                            PointF(viewLeft + buttonWidth / 2, viewTop + view.height / 2)

                    val radius = 0.33f * buttonWidth
                    path.addCircle(radioCenter.x, radioCenter.y, radius, Path.Direction.CW)
                }

                createMultilineTextPath(view, path, offset)
            }
            is Button -> createSimplePath(view, path, offset)
            is TextView -> createMultilineTextPath(view, path, offset)
            else -> createSimplePath(view, path, offset)
        }

        return true
    }

    @SuppressLint("RtlHardcoded")
    private fun createMultilineTextPath(
        view: TextView,
        path: Path,
        offset: PointF
    ) {
        val button = (view as? CompoundButton)
            ?.let { CompoundButtonCompat.getButtonDrawable(view) }

        // Determine the bounds of the text portion of the view.
        val textBounds = RectF()
        if (button == null) {
            textBounds.set(
                offset.x,
                offset.y,
                offset.x + view.width,
                offset.y + view.height
            )
        } else {
            if (view.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                textBounds.set(
                    offset.x,
                    offset.y,
                    offset.x + view.width - button.intrinsicWidth,
                    offset.y + view.height
                )
            } else {
                textBounds.set(
                    offset.x + button.intrinsicWidth,
                    offset.y,
                    offset.x + view.width,
                    offset.y + view.height
                )
            }
        }

        val textContent = view.text.toString()
        val lineHeight = view.paint.fontMetrics.run { descent - ascent }
        val visibleLines = minOf(
            view.maxLines,
            view.lineCount,
            floor(textBounds.height() / lineHeight).toInt()
        )

        // Based on how FrameLayout reads gravity properties for its children
        val absoluteGravity = Gravity.getAbsoluteGravity(view.gravity, view.layoutDirection)
        val verticalGravity = view.gravity and Gravity.VERTICAL_GRAVITY_MASK

        var xOffset = 0
        var yOffset = 0

        // Since yOffset doesn't depend on a value that changes per-line, calculate before the loop
        when (verticalGravity) {
            Gravity.TOP ->
                yOffset = 0
            Gravity.CENTER_VERTICAL ->
                yOffset = (view.measuredHeight - (lineHeight.toInt() * visibleLines)) / 2
            Gravity.BOTTOM ->
                yOffset = view.measuredHeight - (lineHeight.toInt() * visibleLines)
        }

        val lineBounds = Rect()
        for (line in 0 until visibleLines) {
            val textForLine = textContent.substring(
                view.layout.getLineStart(line),
                view.layout.getLineEnd(line)
            )
            view.paint.getTextBounds(textForLine, 0, textForLine.length, lineBounds)

            when (absoluteGravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
                Gravity.LEFT ->
                    xOffset = 0
                Gravity.CENTER_HORIZONTAL -> {
                    xOffset = (textBounds.width().toInt() - lineBounds.width()) / 2
                }
                Gravity.RIGHT ->
                    xOffset = textBounds.width().toInt() - lineBounds.width()
            }

            val lineOffset = (line * lineHeight)
            val lineBottom = textBounds.top + yOffset + lineHeight - SPACE + lineOffset

            // Don't keep showing text lines if the view does not actually show them
            // E.g. View has maxLines set but there's enough text for maxLines++
            val remainingHeight = (textBounds.top + view.height) - lineBottom
            if (remainingHeight < 0) {
                if (-1 * remainingHeight > lineHeight / 2) {
                    break
                }
            }

            val rect = RectF(
                textBounds.left + xOffset + SPACE,
                textBounds.top + yOffset + lineOffset + SPACE,
                textBounds.left + xOffset + lineBounds.width() - SPACE,
                lineBottom
            )
            path.addRoundRect(rect, R_RECT_RADIUS, R_RECT_RADIUS, Path.Direction.CW)
        }
    }

    private fun createSimplePath(view: View, path: Path, offset: PointF) {
        val viewLeft = offset.x
        val viewTop = offset.y

        val rect = RectF(
            viewLeft + SPACE,
            viewTop + SPACE,
            viewLeft + view.measuredWidth - SPACE,
            viewTop + view.measuredHeight - SPACE
        )

        val rectRadius = min(view.measuredHeight, view.measuredWidth) * 0.075f
        path.addRoundRect(rect, rectRadius, rectRadius, Path.Direction.CW)
    }

    private const val SPACE = 2.5f
    private const val R_RECT_RADIUS = 10f
}