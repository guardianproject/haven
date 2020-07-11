package org.havenapp.main.usecase

import android.graphics.ImageFormat
import android.os.Looper
import android.util.Log
import android.util.Size
import androidx.annotation.WorkerThread
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.havenapp.main.sensors.motion.MotionDetector
import org.havenapp.main.util.ImageHelper
import kotlin.math.ceil

private const val DETECTION_INTERVAL_MS = 200L

class MotionAnalyser(
        frameFormat: Int,
        private val frameSize: Size,
        private val motionDetector: MotionDetector
) : ImageAnalysis.Analyzer {

    private var lastFrame: ByteArray? = null
    private val buffer: ByteArray

    @Volatile
    private var shouldAnalyse = false

    private var analysisTimestamp = 0L

    init {
        assert(frameFormat == ImageFormat.YUV_420_888)
        val bitsPerPixel = ImageFormat.getBitsPerPixel(frameFormat)
        val sizeInBits: Long = frameSize.height * frameSize.width * bitsPerPixel.toLong()
        val bufferSize = ceil(sizeInBits / 8.0).toInt()
        buffer = ByteArray(bufferSize)
    }

    @WorkerThread
    override fun analyze(imageProxy: ImageProxy) {
        assert(!isMainThread())

        if (!shouldAnalyse) {
            imageProxy.close()
            return
        }

        val now = System.currentTimeMillis()
        if (analysisTimestamp + DETECTION_INTERVAL_MS > now) {
            imageProxy.close()
            Log.i(TAG, "Ignoring event due to detection interval policy")
            return
        }

        val image = imageProxy.image ?: kotlin.run {
            imageProxy.close()
            return
        }

        analysisTimestamp = now
        ImageHelper.convertToNV21(image, buffer)
        lastFrame?.let { prevFrame ->
            motionDetector.detect(prevFrame, buffer, frameSize.width, frameSize.height)
            buffer.copyInto(prevFrame)
        } ?: kotlin.run {
            lastFrame = ByteArray(buffer.size)
            buffer.copyInto(lastFrame!!)
        }

        imageProxy.close()
    }

    fun setAnalyze(analyze: Boolean) {
        shouldAnalyse = analyze
    }

    private fun isMainThread(): Boolean {
        return Looper.getMainLooper().thread === Thread.currentThread()
    }

    companion object {
        private val TAG = MotionAnalyser::class.java.simpleName
    }
}
