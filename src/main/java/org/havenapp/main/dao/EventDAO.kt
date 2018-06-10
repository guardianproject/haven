package org.havenapp.main.dao

import android.arch.persistence.room.*
import org.havenapp.main.model.Event

/**
 * Created by Arka Prava Basu <arkaprava94@gmail.com> on 23/5/18.
 */
@Dao
interface EventDAO {

    @Insert
    fun insert(event: Event): Long

    @Delete
    fun delete(event: Event)

    @Update
    fun update(event: Event)

    @Query("SELECT * FROM EVENT WHERE ID = :id")
    fun findById(id: Long?) : Event

    @Query("SELECT * FROM EVENT ORDER BY ID")
    fun getAllEvent() : List<Event>

    @Query("SELECT * FROM EVENT ORDER BY ID DESC")
    fun getAllEventDesc() : List<Event>

    @Query("SELECT COUNT(*) FROM EVENT")
    fun count() : Int
}