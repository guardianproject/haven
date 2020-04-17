package org.havenapp.main.database.migration

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.Assert.assertEquals
import org.havenapp.main.database.HavenEventDB
import org.havenapp.main.database.converter.HavenEventDBConverters.Companion.dateToTimestamp
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Created by Arka Prava Basu <arkaprava94@gmail.com> on 27/10/18.
 */
class RoomMigrationTest {
    @get:Rule
    val migrationTestHelper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
            HavenEventDB::class.java.canonicalName,
            FrameworkSQLiteOpenHelperFactory())

    private var sugarDbOpenHelper: SugarDbOpenHelper? = null

    private val TEST_DB_NAME = "test.db"

    @Before
    fun setUpDb() {
        sugarDbOpenHelper =
                SugarDbOpenHelper(ApplicationProvider.getApplicationContext(), TEST_DB_NAME)
        SugarDbTestHelper.createTables(sugarDbOpenHelper!!)
    }

    @Test
    fun validateMigrationAndData() {
        SugarDbTestHelper.insertEvent(123, sugarDbOpenHelper!!)
        SugarDbTestHelper.insertEventTrigger(1, "abcabd", 124, 1, sugarDbOpenHelper!!)

        migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, 4,
                true, RoomMigration())

        val migratedDb = getMigratedRoomDb()

        val event = migratedDb.getEventDAO().getAllEvent()[0]
        val eventTrigger = migratedDb.getEventTriggerDAO().getAllEventTriggers()[0]

        assertEquals(dateToTimestamp(event.startTime)?.toInt(), 123)

        assertEquals(dateToTimestamp(eventTrigger.time)?.toInt(), 124)
        assertEquals(eventTrigger.path, "abcabd")
        assertEquals(eventTrigger.type, 1)
    }

    @After
    fun clearDb() {
        SugarDbTestHelper.clearDb(sugarDbOpenHelper!!)
    }

    private fun getMigratedRoomDb(): org.havenapp.main.database.HavenEventDB {
        val db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(),
                org.havenapp.main.database.HavenEventDB::class.java, TEST_DB_NAME)
                .addMigrations(RoomMigration())
                .build()

        migrationTestHelper.closeWhenFinished(db)

        return db
    }
}
