package org.havenapp.main.util

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata

fun CameraCharacteristics.isLegacyDevice(): Boolean =
        (this[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL] ==
                CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)

fun CameraCharacteristics.isLimitedLevelDevice(): Boolean =
        (this[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL] ==
                CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)

fun CameraCharacteristics.isFullLevel(): Boolean =
        (this[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL] ==
                CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)

