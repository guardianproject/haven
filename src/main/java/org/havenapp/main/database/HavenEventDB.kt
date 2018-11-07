package org.havenapp.main.database

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.havenapp.main.dao.EventDAO
import org.havenapp.main.dao.EventTriggerDAO
import org.havenapp.main.database.converter.HavenEventDBConverters
import org.havenapp.main.database.migration.RoomMigration
import org.havenapp.main.model.Event
import org.havenapp.main.model.EventTrigger

/**
 * Created by Arka Prava Basu <arkaprava94@gmail.com> on 23/5/18.
 */
@Database(entities = [(Event::class), (EventTrigger::class)], version = 4)
@TypeConverters(HavenEventDBConverters::class)
abstract class HavenEventDB : RoomDatabase() {

    abstract fun getEventDAO(): EventDAO

    abstract fun getEventTriggerDAO(): EventTriggerDAO

    companion object {
        private var INSTANCE: HavenEventDB? = null

        @JvmStatic
        fun getDatabase(context: Context): HavenEventDB {

            if (INSTANCE == null) {
                synchronized(HavenEventDB::class) {
                    if (INSTANCE == null) {

                        // notify interested components that db initialization is starting
                        var dbIntent = Intent()
                        dbIntent.putExtra(DB_INIT_STATUS, DB_INIT_START)
                        dbIntent.action = DB_INIT_STATUS
                        LocalBroadcastManager.getInstance(context).sendBroadcast(dbIntent)

                        INSTANCE = Room.databaseBuilder(context.applicationContext,
                                HavenEventDB::class.java, "haven.db")
                                .allowMainThreadQueries() // todo remove this
                                .addMigrations(RoomMigration())
                                .build()

                        // notify interested components that db initialization has succeeded
                        dbIntent = Intent()
                        dbIntent.putExtra(DB_INIT_STATUS, DB_INIT_END)
                        dbIntent.action = DB_INIT_STATUS
                        LocalBroadcastManager.getInstance(context).sendBroadcast(dbIntent)

                    }
                }
            }

            return INSTANCE!!
        }
    }
}