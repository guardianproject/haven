package org.havenapp.main.dao

import android.arch.persistence.room.*
import org.havenapp.main.model.EventRoom

/**
 * Created by Arka Prava Basu <arkaprava94@gmail.com> on 23/5/18.
 */
@Dao
interface EventDAO {

    @Insert
    fun insert(eventRoom: EventRoom)

    @Delete
    fun delete(eventRoom: EventRoom)

    @Update
    fun update(eventRoom: EventRoom)

    @Query("SELECT * FROM event_table WHERE id = :id")
    fun findById(id: Long) : EventRoom
}