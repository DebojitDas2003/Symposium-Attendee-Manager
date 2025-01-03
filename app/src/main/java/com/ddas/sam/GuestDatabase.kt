package com.ddas.sam

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Guest::class], version = 2) // Incremented version number to 2
abstract class GuestDatabase : RoomDatabase() {
    abstract fun guestDao(): GuestDao

    companion object {
        @Volatile
        private var INSTANCE: GuestDatabase? = null

        // Migration for version 1 to version 2 (adjust as per your schema changes)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Check if the "deleted" column already exists before adding it
                val cursor = database.query("PRAGMA table_info(guest)")
                var columnExists = false
                while (cursor.moveToNext()) {
                    val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    if (columnName == "deleted") {
                        columnExists = true
                        break
                    }
                }
                cursor.close()

                if (!columnExists) {
                    database.execSQL("ALTER TABLE guest ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
                }
            }
        }



        fun getDatabase(context: Context): GuestDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GuestDatabase::class.java,
                    "guest_database"
                )
                    .addMigrations(MIGRATION_1_2) // Add the migration
                    .fallbackToDestructiveMigration() // Will erase old data in case migration is not defined
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
