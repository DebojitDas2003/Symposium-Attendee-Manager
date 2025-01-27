package com.ddas.sam

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GuestDao {
    @Query("SELECT * FROM guest WHERE deleted = 0")
    fun getAllGuests(): Flow<List<Guest>>


    @Query("SELECT * FROM guest")
    suspend fun getAllGuestsOnce(): List<Guest>

    @Query("SELECT * FROM guest WHERE name = :name LIMIT 1")
    suspend fun getGuestByName(name: String): Guest?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuest(guest: Guest)

    @Update
    suspend fun updateGuest(guest: Guest)

    @Delete
    suspend fun deleteGuest(guest: Guest)


    @Query("DELETE FROM guest")
    suspend fun clearAllGuests()



}
