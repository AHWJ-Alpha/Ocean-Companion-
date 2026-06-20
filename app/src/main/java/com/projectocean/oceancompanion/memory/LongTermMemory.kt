package com.projectocean.oceancompanion.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "long_term_memory")
data class LongTermMemory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val summary: String,
    val sourceApp: String = "",
    val importance: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
