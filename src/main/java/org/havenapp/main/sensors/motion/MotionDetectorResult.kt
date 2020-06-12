package org.havenapp.main.sensors.motion

import android.graphics.Bitmap

data class MotionDetectorResult(
        val pixelsChanged: Int,
        val motionDetected: Boolean,
        val rawBitmap: Bitmap?
)
