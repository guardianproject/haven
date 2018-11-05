package org.havenapp.main.database.async

import android.os.AsyncTask
import org.havenapp.main.HavenApp
import org.havenapp.main.model.Event

/**
 * Created by Arka Prava Basu <arkaprava94@gmail.com> on 6/9/18.
 */
class EventDeleteAsync(private val listener: EventDeleteListener)
    : AsyncTask<Event, Unit, Unit>() {
    override fun doInBackground(vararg params: Event) {
        HavenApp.getDataBaseInstance().getEventDAO().delete(params.get(0))
    }

    override fun onPostExecute(result: Unit?) {
        listener.onDeleteEvent()
    }

    interface EventDeleteListener {
        fun onDeleteEvent()
    }
}
