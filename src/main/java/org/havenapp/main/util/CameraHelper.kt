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
