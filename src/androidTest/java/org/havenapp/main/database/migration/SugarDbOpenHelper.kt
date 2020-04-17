package org.havenapp.main.database.migration

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Created by Arka Prava Basu <arkaprava94@gmail.com> on 27/10/18.
 */
class SugarDbOpenHelper(context: Context, dbName: String)
    : SQLiteOpenHelper(context, dbName, null, 3) {


    private val createEventTable =
            "CREATE TABLE IF NOT EXISTS EVENT ( ID INTEGER PRIMARY KEY AUTOINCREMENT , M_START_TIME INTEGER )"
    private val createEventTriggerTable =
            "CREATE TABLE IF NOT EXISTS EVENT_TRIGGER ( ID INTEGER PRIMARY KEY AUTOINCREMENT , M_EVENT_ID INTEGER, M_PATH TEXT, M_TIME INTEGER , M_TYPE INTEGER )"

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(createEventTable)
        db?.execSQL(createEventTriggerTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

    }

    override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

    }
}
