package org.havenapp.main.database.async

import android.os.AsyncTask
import org.havenapp.main.HavenApp
import org.havenapp.main.model.Event

/**
 * Created by Arka Prava Basu <arkaprava94@gmail.com> on 6/9/18.
 */
class EventInsertAsync(val listener: EventInsertListener): AsyncTask<Event, Unit, Long>() {
    override fun doInBackground(vararg params: Event): Long {
        val event = params.get(0)
        return HavenApp.getDataBaseInstance().getEventDAO().insert(event)
    }

    override fun onPostExecute(result: Long) {
        listener.onInsertionComplete(result)
    }

    interface EventInsertListener {
        fun onInsertionComplete(eventId: Long)
    }
}
