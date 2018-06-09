package org.havenapp.main.dao

import android.arch.persistence.room.*
import org.havenapp.main.model.Event

/**
 * Created by Arka Prava Basu <arkaprava94@gmail.com> on 23/5/18.
 */
@Dao
interface EventDAO {

    @Insert
    fun insert(event: Event)

    @Delete
    fun delete(event: Event)

    @Update
    fun update(event: Event)

    @Query("SELECT * FROM event_table WHERE id = :id")
    fun findById(id: Long) : Event

    @Query("SELECT * FROM event_table ORDER BY id")
    fun getAllEvent() : List<Event>

    @Query("SELECT * FROM event_table ORDER BY id DESC")
    fun getAllEventDesc() : List<Event>

    @Query("SELECT COUNT(*) FROM event_table")
    fun count() : Int
}