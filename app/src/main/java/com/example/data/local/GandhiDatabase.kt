package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TranscriptEntity::class, TermuxScriptEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GandhiDatabase : RoomDatabase() {
    abstract fun gandhiDao(): GandhiDao

    companion object {
        @Volatile
        private var INSTANCE: GandhiDatabase? = null

        fun getInstance(context: Context): GandhiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GandhiDatabase::class.java,
                    "hey_gandhi_vault.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
