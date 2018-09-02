package org.havenapp.main.model

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import org.havenapp.main.HavenApp
import java.util.*

/**
 * Created by Arka Prava Basu <arkaprava94@gmail.com> on 20/5/18.
 */
@Entity(tableName = "EVENT")
class Event {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ID")
    var id : Long? = null
        get() = field

    @ColumnInfo(name = "M_START_TIME")
    var mStartTime : Date? = Date()

    @Ignore
    private var mEventTriggers : MutableList<EventTrigger> = mutableListOf()

    fun addEventTrigger(eventTrigger: EventTrigger) {
        mEventTriggers.add(eventTrigger)
        eventTrigger.mEventId = id
    }

    /**
     * Get the list of event triggers associated with this event.
     * <p>
     * When [mEventTriggers] is empty this method performs a blocking db lookup.
     */
    fun getEventTriggers() : MutableList<EventTrigger> {

        if (mEventTriggers.size == 0) {
            val eventTriggers = HavenApp.dataBaseInstance.getEventTriggerDAO().getEventTriggerList(id)
            mEventTriggers.addAll(eventTriggers)
        }

        return mEventTriggers
    }

    /**
     * Perform a db lookup for event triggers corresponding to this event.
     * Unlike [getEventTriggers] this method performs a non blocking db lookup and
     * returns a lifecycle aware data holder for list of [EventTrigger]s
     */
    fun getEventTriggersAsync(): LiveData<MutableList<EventTrigger>> {
        return Transformations.map(HavenApp.dataBaseInstance.getEventTriggerDAO().getEventTriggerListAsync(id)) {
            mEventTriggers = it
            it
        }
    }

    fun getEventTriggerCount(): Int {
        if (mEventTriggers.size == 0) {
            return getEventTriggers().size
        }

        return mEventTriggers.size
    }
}