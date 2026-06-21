package com.projectocean.oceancompanion.memory

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ConversationHistory::class, UserProfile::class, LongTermMemory::class, AutoSpeechRule::class],
    version = 3,
    exportSchema = false
)
abstract class OceanDatabase : RoomDatabase() {
    abstract fun dao(): OceanDao

    companion object {
        fun create(context: Context): OceanDatabase = Room.databaseBuilder(
            context,
            OceanDatabase::class.java,
            "ocean.db"
        ).addMigrations(MIGRATION_2_3).build()

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE conversation_history ADD COLUMN sessionId TEXT NOT NULL DEFAULT 'default'")
                db.execSQL("ALTER TABLE conversation_history ADD COLUMN topic TEXT NOT NULL DEFAULT 'Ocean Companion'")
                db.execSQL("ALTER TABLE conversation_history ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE conversation_history SET updatedAt = createdAt WHERE updatedAt = 0")
            }
        }
    }
}
