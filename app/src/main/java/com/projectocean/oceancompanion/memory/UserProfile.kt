package com.projectocean.oceancompanion.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Long = 1,
    val persona: String = "OceanNative",
    val preferredProvider: String = "openai",
    val proactiveReminders: Boolean = true
)
