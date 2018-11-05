package org.havenapp.main.database.async

import android.os.AsyncTask
import org.havenapp.main.HavenApp
import org.havenapp.main.model.EventTrigger

/**
 * Created by Arka Prava Basu <arkaprava94@gmail.com> on 8/9/18.
 */
class EventTriggerInsertAsync(private val callback: InsertCallback)
    : AsyncTask<EventTrigger, Unit, Long>() {
    override fun doInBackground(vararg params: EventTrigger): Long {
        return HavenApp.getDataBaseInstance().getEventTriggerDAO().insert(params.get(0))
    }

    override fun onPostExecute(result: Long) {
        callback.onEventTriggerInserted(result)
    }

    interface InsertCallback {
        fun onEventTriggerInserted(eventTriggerId: Long)
    }
}
