package org.havenapp.main.database.async

import android.os.AsyncTask
import org.havenapp.main.HavenApp
import org.havenapp.main.model.Event

/**
 * Created by Arka Prava Basu <arkaprava94@gmail.com> on 6/9/18.
 */
class EventInsertAllAsync(private val listener: EventInsertListener)
    : AsyncTask<List<Event>, Unit, List<Long>>() {
    override fun doInBackground(vararg params: List<Event>): List<Long> {
        return HavenApp.getDataBaseInstance().getEventDAO().insertAll(params.get(0))
    }

    override fun onPostExecute(result: List<Long>) {
        listener.onInsertionComplete(result)
    }

    interface EventInsertListener {
        fun onInsertionComplete(eventIdList: List<Long>)
    }
}
