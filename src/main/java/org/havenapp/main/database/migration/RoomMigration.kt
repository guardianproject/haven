package org.havenapp.main.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * [Migration] for the transition from Sugar ORM (database version = 3)
 * to Room database (database version = 4).
 * <p>
 * Created by Arka Prava Basu <arkaprava94@gmail.com> on 28/8/18.
 */
class RoomMigration: Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Since we did not alter table, nothing to do here.
    }
}
