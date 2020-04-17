package org.havenapp.main.database.migration

/**
 * Created by Arka Prava Basu <arkaprava94@gmail.com> on 28/10/18.
 */
class SugarDbTestHelper {

    companion object {
        fun createTables(helper: SugarDbOpenHelper) {
            val db = helper.writableDatabase

            val createEventTable =
                    "CREATE TABLE IF NOT EXISTS EVENT ( ID INTEGER PRIMARY KEY AUTOINCREMENT , M_START_TIME INTEGER )"
            val createEventTriggerTable =
                    "CREATE TABLE IF NOT EXISTS EVENT_TRIGGER ( ID INTEGER PRIMARY KEY AUTOINCREMENT , M_EVENT_ID INTEGER, M_PATH TEXT, M_TIME INTEGER , M_TYPE INTEGER )"

            db.execSQL(createEventTable)
            db.execSQL(createEventTriggerTable)

            db.close()
        }


        fun insertEvent(startTime: Int, helper: SugarDbOpenHelper) {
            val db = helper.writableDatabase

            db?.execSQL("INSERT INTO EVENT(M_START_TIME) VALUES ($startTime)")

            db.close()
        }

        fun insertEventTrigger(eventId: Int, path: String, startTime: Int,
                               type: Int, helper: SugarDbOpenHelper) {
            val db = helper.writableDatabase

            db?.execSQL("INSERT INTO EVENT_TRIGGER(M_EVENT_ID, M_PATH, M_TIME, M_TYPE) VALUES ($eventId, \"$path\", $startTime, $type)")

            db.close()
        }

        fun clearDb(helper: SugarDbOpenHelper) {
            val db = helper.writableDatabase

            db?.execSQL("DROP TABLE IF EXISTS EVENT")
            db?.execSQL("DROP TABLE IF EXISTS EVENT_TRIGGER")

            db.close()
        }
    }
}
