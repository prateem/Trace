package com.meetarp.trace

import android.graphics.Path
import android.graphics.PointF
import android.view.View

interface TraceDelegate {

    fun handle(
        view: View,
        path: Path,
        exclusion: List<Int>,
        offset: PointF
    ): Boolean

}