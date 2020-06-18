package org.havenapp.main.sensors.motion

import android.graphics.Bitmap

data class MotionDetectorResult(
        val pixelsChanged: Int,
        val motionDetected: Boolean,
        val rawBitmap: Bitmap?
)

/**
 * A data wrapper class to represent an event, ie, once consumed
 * will not be available again.
 */
class Event<T>(data: T) {
    private var dataInternal: T? = data
    fun consume(): T? {
        dataInternal ?: return null
        val copy = dataInternal
        dataInternal = null
        return copy
    }
}
