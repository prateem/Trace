package com.meetarp.trace

import android.graphics.Path
import android.graphics.PointF
import android.view.View

interface TraceDelegate {

    /**
     * If desired, handle the drawing of the silhouette of the given [view].
     * Silhouette drawing should involve manipulation of [path].
     *
     * @param [view] A view whose silhouette has not yet been created yet.
     * @param [path] The [Path] object that hosts silhouettes for views being traced.
     * @param [exclusions] List of View IDs to exclude from being traced.
     * @param [offset] The left and top offsets of [view] in relation to the
     *      original trace target's left and top position.
     *
     * @return true if the [view] has had its silhouette drawn by this method; false otherwise
     *
     * @see [DefaultTraceDelegate] for example implementation
     */
    fun handle(
        view: View,
        path: Path,
        exclusions: List<Int>,
        offset: PointF
    ): Boolean

}