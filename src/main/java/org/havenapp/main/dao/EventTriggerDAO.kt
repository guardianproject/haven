package org.havenapp.main.dao

import android.arch.persistence.room.*
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

    @Update
    fun update(eventTrigger: EventTrigger)

    @Query("SELECT * FROM event_trigger_table WHERE id = :id")
    fun findById(id : Long) : EventTrigger

    @Query("SELECT * FROM event_trigger_table WHERE mEventId = :eventId")
    fun getEventTriggerList(eventId: Long) : MutableList<EventTrigger>

    @Query("SELECT * FROM event_trigger_table")
    fun getAllEventTriggers() : MutableList<EventTrigger>
}