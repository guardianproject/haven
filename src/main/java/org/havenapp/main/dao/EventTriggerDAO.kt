package org.havenapp.main.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import org.havenapp.main.model.EventTrigger

/**
 * Created by Arka Prava Basu <arkaprava94@gmail.com> on 23/5/18.
 */
@Dao
interface EventTriggerDAO {

    @Insert
    fun insert(eventTrigger: EventTrigger) : Long

    @Delete
    fun delete(eventTrigger: EventTrigger)

    @Delete
    fun deleteAll(eventTriggerList: List<EventTrigger>)

    @Update
    fun update(eventTrigger: EventTrigger)

    @Query("SELECT * FROM EVENT_TRIGGER WHERE ID = :id")
    fun findById(id : Long?) : EventTrigger

    @Query("SELECT * FROM EVENT_TRIGGER WHERE M_EVENT_ID = :eventId")
    fun getEventTriggerList(eventId: Long?) : MutableList<EventTrigger>

    @Query("SELECT * FROM EVENT_TRIGGER WHERE M_EVENT_ID = :eventId")
    fun getEventTriggerListAsync(eventId: Long?) : LiveData<MutableList<EventTrigger>>

    @Query("SELECT * FROM EVENT_TRIGGER")
    fun getAllEventTriggers() : MutableList<EventTrigger>

    @Query("SELECT COUNT(*) FROM EVENT_TRIGGER WHERE M_EVENT_ID = :eventId")
    fun getEventTriggerListCountAsync(eventId: Long?) : LiveData<Int>
}
