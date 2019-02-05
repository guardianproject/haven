package org.havenapp.main.database.async

import android.os.AsyncTask
import org.havenapp.main.HavenApp
import org.havenapp.main.model.Event

/**
 * Created by Arka Prava Basu <arkaprava94@gmail.com> on 6/9/18.
 */
class EventDeleteAllAsync(private val listener: EventDeleteAllListener)
    : AsyncTask<List<Event>, Unit, Unit>() {
    override fun doInBackground(vararg params: List<Event>) {
        HavenApp.getDataBaseInstance().getEventDAO().deleteAll(params.get(0))
    }

    override fun onPostExecute(result: Unit?) {
        listener.onEventsDeleted()
    }

    interface EventDeleteAllListener {
        fun onEventsDeleted()
    }
}
