package org.havenapp.main.usecase

import android.graphics.ImageFormat
import android.util.Size
import androidx.annotation.WorkerThread
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.otaliastudios.cameraview.internal.utils.ImageHelper
import org.havenapp.main.sensors.motion.MotionDetector
import java.nio.ByteBuffer
import kotlin.math.ceil

class MotionAnalyser(
        frameFormat: Int,
        private val frameSize: Size,
        private val motionDetector: MotionDetector
) : ImageAnalysis.Analyzer {

    private var lastFrame: ByteArray? = null
    private val buffer: ByteArray

    @Volatile
    private var shouldAnalyse = false

    init {
        assert(frameFormat == ImageFormat.YUV_420_888)
        val bitsPerPixel = ImageFormat.getBitsPerPixel(frameFormat)
        val sizeInBits: Long = frameSize.height * frameSize.width * bitsPerPixel.toLong()
        val bufferSize = ceil(sizeInBits / 8.0).toInt()
        buffer = ByteArray(bufferSize)
    }

    @WorkerThread
    override fun analyze(imageProxy: ImageProxy) {
        if (!shouldAnalyse) {
            imageProxy.close()
            return
        }

        val image = imageProxy.image ?: kotlin.run {
            imageProxy.close()
            return
        }

        ImageHelper.convertToNV21(image, buffer)
        lastFrame?.let { prevFrame ->
            motionDetector.detect(prevFrame, buffer, frameSize.width, frameSize.height)
            buffer.copyInto(lastFrame!!)
        } ?: kotlin.run {
            lastFrame = ByteArray(buffer.size)
            buffer.copyInto(lastFrame!!)
        }

        imageProxy.close()
    }

    fun setAnalyze(analyze: Boolean) {
        shouldAnalyse = analyze
    }
}
