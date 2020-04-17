package org.havenapp.main.model

import androidx.annotation.WorkerThread
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
    @WorkerThread
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Event

        if (id != other.id) return false
        if (startTime != other.startTime) return false
        if (eventTriggers != other.eventTriggers) return false
        if (eventTriggerCountLD != other.eventTriggerCountLD) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (startTime?.hashCode() ?: 0)
        result = 31 * result + eventTriggers.hashCode()
        result = 31 * result + (eventTriggerCountLD?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Event(id=$id, startTime=$startTime, eventTriggers=$eventTriggers, eventTriggerCountLD=$eventTriggerCountLD)"
    }
}
