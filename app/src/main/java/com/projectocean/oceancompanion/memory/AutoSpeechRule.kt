package com.projectocean.oceancompanion.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "auto_speech_rule")
data class AutoSpeechRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appNameKeyword: String,
    val prompt: String,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
