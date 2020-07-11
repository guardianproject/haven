package org.havenapp.main.util

import android.graphics.ImageFormat
import android.media.Image

/**
 * Conversions for [android.media.Image]s into byte arrays.
 *
 * Thanks to com.otaliastudios:cameraview:2.4.0
 */
object ImageHelper {
    /**
     * From https://stackoverflow.com/a/52740776/4288782 .
     * The result array should have a size that is at least 3/2 * w * h.
     * This is correctly computed by [com.otaliastudios.cameraview.frame.FrameManager].
     *
     * @param image input image
     * @param result output array
     */
    fun convertToNV21(image: Image, result: ByteArray) {
        check(image.format == ImageFormat.YUV_420_888) { "CAn only convert from YUV_420_888." }
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 4
        val yBuffer = image.planes[0].buffer // Y
        val uBuffer = image.planes[1].buffer // U
        val vBuffer = image.planes[2].buffer // V
        var rowStride = image.planes[0].rowStride
        if (image.planes[0].pixelStride != 1) {
            throw AssertionError("Something wrong in convertToNV21")
        }
        var pos = 0
        if (rowStride == width) { // likely
            yBuffer[result, 0, ySize]
            pos += ySize
        } else {
            var yBufferPos = width - rowStride // not an actual position
            while (pos < ySize) {
                yBufferPos += rowStride - width
                yBuffer.position(yBufferPos)
                yBuffer[result, pos, width]
                pos += width
            }
        }
        rowStride = image.planes[2].rowStride
        val pixelStride = image.planes[2].pixelStride
        if (rowStride != image.planes[1].rowStride) {
            throw AssertionError("Something wrong in convertToNV21")
        }
        if (pixelStride != image.planes[1].pixelStride) {
            throw AssertionError("Something wrong in convertToNV21")
        }
        if (pixelStride == 2 && rowStride == width && uBuffer[0] == vBuffer[1]) {
            // maybe V an U planes overlap as per NV21, which means vBuffer[1]
            // is alias of uBuffer[0]
            val savePixel = vBuffer[1]
            vBuffer.put(1, 0.toByte())
            if (uBuffer[0].toInt() == 0) {
                vBuffer.put(1, 255.toByte())
                if (uBuffer[0].toInt() == 255) {
                    vBuffer.put(1, savePixel)
                    vBuffer[result, ySize, uvSize]
                    return  // shortcut
                }
            }

            // unfortunately, the check failed. We must save U and V pixel by pixel
            vBuffer.put(1, savePixel)
        }

        // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
        // but performance gain would be less significant
        for (row in 0 until height / 2) {
            for (col in 0 until width / 2) {
                val vuPos = col * pixelStride + row * rowStride
                result[pos++] = vBuffer[vuPos]
                result[pos++] = uBuffer[vuPos]
            }
        }
    }
}
