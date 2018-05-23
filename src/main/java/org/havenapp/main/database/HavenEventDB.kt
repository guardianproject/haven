package org.havenapp.main.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import org.havenapp.main.dao.EventDAO
import org.havenapp.main.dao.EventTriggerDAO
import org.havenapp.main.model.EventRoom
import org.havenapp.main.model.EventTriggerRoom

/**
 * Created by Arka Prava Basu <arkaprava94@gmail.com> on 23/5/18.
 */
@Database(entities = [(EventRoom::class), (EventTriggerRoom::class)], version = 1)
abstract class HavenEventDB: RoomDatabase() {

    abstract fun getEventDAO(): EventDAO

    abstract fun getEventTriggerDAO() : EventTriggerDAO

    companion object {
        private var INSTANCE : HavenEventDB? = null

        @JvmStatic
        fun getDatabase(context: Context) : HavenEventDB {

            if (INSTANCE == null) {
                synchronized(HavenEventDB::class) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(context.applicationContext,
                                HavenEventDB::class.java, "haven_database")
                                .fallbackToDestructiveMigration()
                                .build()
                    }
                }
            }

            return INSTANCE!!
        }
    }
}