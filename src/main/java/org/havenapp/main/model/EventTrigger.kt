package org.havenapp.main.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import org.havenapp.main.R
import org.havenapp.main.resources.IResourceManager
import java.util.*

/**
 * Created by Arka Prava Basu <arkaprava94@gmail.com> on 22/5/18.
 */
@Entity(tableName = "EVENT_TRIGGER")
class EventTrigger {
    companion object {
        /**
         * Acceleration detected message
         */
        const val ACCELEROMETER = 0

        /**
         * Camera motion detected message
         */
        const val CAMERA = 1

        /**
         * Mic noise detected message
         */
        const val MICROPHONE = 2

        /**
         * Pressure change detected message
         */
        const val PRESSURE = 3

        /**
         * Light change detected message
         */
        const val LIGHT = 4

        /**
         * Power change detected message
         */
        const val POWER = 5

        /**
         * Significant motion detected message
         */
        const val BUMP = 6

        /**
         * Significant motion detected message
         */
        const val CAMERA_VIDEO = 7

        /**
         * Heartbeat notification message
         */
        const val HEART = 8
    }

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ID")
    var id : Long? = null
        get() = field

    @ColumnInfo(name = "M_TYPE")
    var mType: Int? = 0

    // Trigger time
    @ColumnInfo(name  = "M_TIME")
    var mTime: Date? = Date()

    @ColumnInfo(name = "M_EVENT_ID")
    var mEventId: Long? = 0

    @ColumnInfo(name = "M_PATH")
    var mPath: String? = null

    fun getStringType(resourceManager: IResourceManager): String {
        var sType = ""

        sType = when (mType) {
            ACCELEROMETER -> resourceManager.getString(R.string.sensor_accel)
            LIGHT -> resourceManager.getString(R.string.sensor_light)
            CAMERA -> resourceManager.getString(R.string.sensor_camera)
            MICROPHONE -> resourceManager.getString(R.string.sensor_sound)
            POWER -> resourceManager.getString(R.string.sensor_power)
            BUMP -> resourceManager.getString(R.string.sensor_bump)
            CAMERA_VIDEO -> resourceManager.getString(R.string.sensor_camera_video)
            HEART -> resourceManager.getString(R.string.sensor_heartbeat)
            else -> resourceManager.getString(R.string.sensor_unknown)
        }

        return sType
    }

    fun getMimeType(): String? {
        var mimeType: String? = ""

        mimeType = when (mType) {
            CAMERA -> "image/*"
            MICROPHONE -> "audio/*"
            CAMERA_VIDEO -> "video/*"
            else -> null
        }

        return mimeType
    }
}