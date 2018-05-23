package org.havenapp.main.dao

import android.arch.persistence.room.*
import org.havenapp.main.model.EventTriggerRoom

/**
 * Created by Arka Prava Basu <arkaprava94@gmail.com> on 23/5/18.
 */
@Dao
interface EventTriggerDAO {

    @Insert
    fun insert(eventTriggerRoom: EventTriggerRoom)

    @Delete
    fun delete(eventTriggerRoom: EventTriggerRoom)

    @Update
    fun update(eventTriggerRoom: EventTriggerRoom)

    @Query("SELECT * FROM event_trigger_table WHERE id = :id")
    fun findById(id : Long) : EventTriggerRoom

    @Query("SELECT * FROM event_trigger_table WHERE mEventId = :eventId")
    fun getEventTriggerList(eventId: Long) : MutableList<EventTriggerRoom>
}