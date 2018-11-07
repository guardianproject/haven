package org.havenapp.main.database.converter

import androidx.room.TypeConverter
import java.util.*

/**
 * Created by Arka Prava Basu <arkaprava94@gmail.com> on 25/5/18.
 */
class HavenEventDBConverters {
    companion object {

        @JvmStatic
        @TypeConverter
        fun fromTimestamp(value: Long?): Date? {
            return if (value == null) null else Date(value)
        }

        @JvmStatic
        @TypeConverter
        fun dateToTimestamp(date: Date?): Long? {
            return date?.time
        }
    }
}