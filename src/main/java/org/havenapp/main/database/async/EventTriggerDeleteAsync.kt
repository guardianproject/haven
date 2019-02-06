package org.havenapp.main.database.async

import android.os.AsyncTask
import org.havenapp.main.HavenApp
import org.havenapp.main.model.EventTrigger

/**
 * Created by Arka Prava Basu <arkaprava94@gmail.com> on 8/9/18.
 */
class EventTriggerDeleteAsync(private val callback: DeleteCallback)
    : AsyncTask<EventTrigger, Unit, Unit>() {
    override fun doInBackground(vararg params: EventTrigger) {
        HavenApp.getDataBaseInstance().getEventTriggerDAO().delete(params.get(0))
        // todo delete file here?
    }

    override fun onPostExecute(result: Unit?) {
        callback.onEventTriggerDeleted()
    }

    interface DeleteCallback {
        fun onEventTriggerDeleted()
    }
}
