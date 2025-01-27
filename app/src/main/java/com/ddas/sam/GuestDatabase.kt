package com.ddas.sam

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Guest::class], version = 2) // Increment version number to 2
abstract class GuestDatabase : RoomDatabase() {
    abstract fun guestDao(): GuestDao

    companion object {
        @Volatile
        private var INSTANCE: GuestDatabase? = null

        fun getDatabase(context: Context): GuestDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GuestDatabase::class.java,
                    "guest_database"
                )
                    .fallbackToDestructiveMigration() // This will delete the old database and create a new one
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}