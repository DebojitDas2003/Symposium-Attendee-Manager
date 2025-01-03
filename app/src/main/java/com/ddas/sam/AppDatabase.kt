package com.ddas.sam

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Guest::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun guestDao(): GuestDao
}
