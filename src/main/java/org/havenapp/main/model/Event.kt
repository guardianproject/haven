package org.havenapp.main.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
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
        set(value) {
            if (value == null) return
            field = value
            eventTriggerCountLD = Transformations.map(HavenApp.getDataBaseInstance().getEventTriggerDAO()
                    .getEventTriggerListCountAsync(field)) {
                Pair(field!!, it)
            }
        }

    @ColumnInfo(name = "M_START_TIME")
    var startTime : Date? = Date()

    @Ignore
    private var eventTriggers : MutableList<EventTrigger> = mutableListOf()

    @Ignore
    private var eventTriggerCountLD: LiveData<Pair<Long, Int>>? = null

    fun addEventTrigger(eventTrigger: EventTrigger) {
        eventTriggers.add(eventTrigger)
        eventTrigger.eventId = id
    }

    /**
     * Get the list of event triggers associated with this event.
     * <p>
     * When [eventTriggers] is empty this method performs a blocking db lookup.
     */
    fun getEventTriggers() : MutableList<EventTrigger> {

        if (eventTriggers.size == 0) {
            val eventTriggers = HavenApp.getDataBaseInstance().getEventTriggerDAO().getEventTriggerList(id)
            this.eventTriggers.addAll(eventTriggers)
        }

        return eventTriggers
    }

    fun getEventTriggersCountLD(): LiveData<Pair<Long, Int>>? {
        return eventTriggerCountLD
    }

    fun getEventTriggerCount(): Int {
        return eventTriggerCountLD?.value?.second ?: 0
    }
}
