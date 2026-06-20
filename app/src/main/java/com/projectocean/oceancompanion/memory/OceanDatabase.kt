package com.projectocean.oceancompanion.memory

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ConversationHistory::class, UserProfile::class, LongTermMemory::class, AutoSpeechRule::class],
    version = 2,
    exportSchema = false
)
abstract class OceanDatabase : RoomDatabase() {
    abstract fun dao(): OceanDao

    companion object {
        fun create(context: Context): OceanDatabase = Room.databaseBuilder(
            context,
            OceanDatabase::class.java,
            "ocean.db"
        ).fallbackToDestructiveMigration().build()
    }
}
