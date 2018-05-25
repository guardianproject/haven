package org.havenapp.main.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import org.havenapp.main.HavenApp
import java.util.*

/**
 * Created by Arka Prava Basu <arkaprava94@gmail.com> on 20/5/18.
 */
@Entity(tableName = "event_table")
class Event {

    @PrimaryKey(autoGenerate = true)
    var id : Long = 0
        get() = field

    var mStartTime : Date = Date()

    @Ignore
    private var mEventTriggers : MutableList<EventTrigger> = mutableListOf()

    fun addEventTrigger(eventTrigger: EventTrigger) {
        mEventTriggers.add(eventTrigger)
        eventTrigger.mEventId = id
    }

    /**
     * Get the list of event triggers associated with this event
     */
    fun getEventTriggers() : MutableList<EventTrigger> {

        if (mEventTriggers.size == 0) {
            val eventTriggers = HavenApp.dataBaseInstance.getEventTriggerDAO().getEventTriggerList(id)
            mEventTriggers.addAll(eventTriggers)
        }

        return mEventTriggers
    }
}