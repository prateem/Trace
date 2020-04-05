package com.example.meetarp.trace

import android.content.Context
import android.graphics.Path
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

        path.addCircle(17f, 17f, 15f, Path.Direction.CW)
        path.addCircle(162f, 27f, 25f, Path.Direction.CW)

        return path
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(0, 0)
    }

}