package org.havenapp.main.util

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.os.Build
import androidx.annotation.RequiresApi

fun CameraCharacteristics.isLimitedLevelDevice(): Boolean =
        (this[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL] ==
                CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)

fun CameraCharacteristics.isFullLevel(): Boolean =
        (this[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL] ==
                CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)

fun CameraCharacteristics.isLegacyDevice(): Boolean =
        (this[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL] ==
                CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)

@RequiresApi(Build.VERSION_CODES.N)
fun CameraCharacteristics.isLevel3(): Boolean =
        (this[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL] ==
                CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3)

@RequiresApi(Build.VERSION_CODES.P)
fun CameraCharacteristics.isLevelExternal(): Boolean =
        (this[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL] ==
                CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL)

fun CameraCharacteristics.checkGuarantee(outputList: List<OutputCharacteristics>): Boolean {
    when {
        outputList.isEmpty() -> {
            return false
        }
        (Build.VERSION.SDK_INT >= 24) && isLevel3() -> {
            return checkGuaranteeForFull(outputList)
        }
        (Build.VERSION.SDK_INT >= 28) && isLevelExternal() -> {
            return checkGuaranteeForLimited(outputList)
        }
        isLimitedLevelDevice() -> {
            return checkGuaranteeForLimited(outputList)
        }
        isLegacyDevice() -> {
            return checkGuaranteeForLegacy(outputList)
        }
        isFullLevel() -> {
            return checkGuaranteeForFull(outputList)
        }
    }
    return false
}

private fun CameraCharacteristics.checkGuaranteeForLegacy(outputList: List<OutputCharacteristics>): Boolean {
    return when (outputList.size) {
        1 -> true
        2, 3 -> {
            var b = true
            outputList.forEach {
                b = b && ((it.size <= OutputSize.PREVIEW && it.type <= OutputType.YUV) ||
                        (it.size <= OutputSize.MAXIMUM && it.type == OutputType.JPEG))
            }
            b
        }
        else -> false
    }
}

private fun CameraCharacteristics.checkGuaranteeForLimited(outputList: List<OutputCharacteristics>): Boolean {
    return when (outputList.size) {
        1 -> true
        2 -> {
            outputList.sortedBy { it.size }
            (outputList[0].size <= OutputSize.PREVIEW && outputList[0].type <= OutputType.YUV) &&
                    (outputList[1].size <= OutputSize.RECORD && outputList[1].type <= OutputType.YUV)
        }
        3 -> {
            outputList.sortedBy { it.size }
            when {
                outputList[2].size <= OutputSize.RECORD -> {
                    outputList[2].type == OutputType.JPEG &&
                            (outputList[0].size <= OutputSize.PREVIEW && outputList[0].type == OutputType.PRIV) &&
                            (outputList[1].size <= OutputSize.RECORD && outputList[1].type <= OutputType.YUV)
                }
                outputList[2].size == OutputSize.MAXIMUM -> {
                    outputList[2].type == OutputType.JPEG &&
                            (outputList[0].size <= OutputSize.PREVIEW && outputList[0].type == OutputType.YUV) &&
                            (outputList[1].size <= OutputSize.PREVIEW && outputList[1].type == OutputType.YUV)
                }
                else -> false
            }
        }
        else -> false
    }
}

private fun CameraCharacteristics.checkGuaranteeForFull(outputList: List<OutputCharacteristics>): Boolean {
    return when (outputList.size) {
        1 -> true
        2 -> {
            outputList.sortedBy { it.size }
            (outputList[0].size <= OutputSize.PREVIEW && outputList[0].type <= OutputType.YUV) &&
                    (outputList[1].size <= OutputSize.MAXIMUM && outputList[1].type <= OutputType.YUV)
        }
        3 -> {
            outputList.sortedBy { it.size }
            when (outputList[2].type) {
                OutputType.JPEG -> {
                    (outputList[0].size <= OutputSize.PREVIEW && outputList[0].type == OutputType.PRIV) &&
                            (outputList[1].size == OutputSize.PREVIEW && outputList[1].type == OutputType.PRIV)
                }
                OutputType.YUV -> {
                    (outputList[0].size == OutputSize.S640x480 && outputList[0].type == OutputType.YUV) &&
                            (outputList[1].size <= OutputSize.PREVIEW && outputList[1].type <= OutputType.YUV)
                }
                else -> {
                    false
                }
            }
        }
        else -> false
    }
}

data class OutputCharacteristics(
        val type: OutputType,
        val size: OutputSize
)

enum class OutputType {
    /**
     * any target whose available sizes are found using
     * [android.hardware.camera2.params.StreamConfigurationMap.getOutputSizes]
     * with no direct application-visible format
     */
    PRIV,

    /**
     * target Surface using the ImageFormat.YUV_420_888 format
     */
    YUV,

    /**
     * refers to the ImageFormat.JPEG format
     */
    JPEG,

    /**
     * refers to the ImageFormat.RAW_SENSOR format
     */
    RAW
}

enum class OutputSize {
    S640x480,

    /**
     * refers to the best size match to the device's screen resolution,
     * or to 1080p (1920x1080), whichever is smaller.
     */
    PREVIEW,

    /**
     * refers to the camera device's maximum supported recording resolution,
     * as determined by [android.media.CamcorderProfile]
     */
    RECORD,

    /**
     * refers to the camera device's maximum output resolution for that format or target
     * from [android.hardware.camera2.params.StreamConfigurationMap.getOutputSizes]
     */
    MAXIMUM
}
