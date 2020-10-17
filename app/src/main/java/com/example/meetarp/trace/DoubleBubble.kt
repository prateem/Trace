package com.example.meetarp.trace

import android.content.Context
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.meetarp.trace.Traceable

class DoubleBubble @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attributeSet, defStyleAttr, defStyleRes), Traceable {

    override fun trace(): Path {
        val path = Path()

        val rect = RectF()
        rect.set(0f, 0f, 230f, 190f)
        path.addRoundRect(rect, 5f, 5f, Path.Direction.CW)

        val insetRect = RectF(rect)
        insetRect.inset(2f, 2f)
        path.addRoundRect(insetRect, 5f, 5f, Path.Direction.CCW)

        path.addCircle(50f, 50f, 30f, Path.Direction.CW)
        path.addCircle(160f, 120f, 50f, Path.Direction.CW)

        return path
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(0, 0)
    }

}