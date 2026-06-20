package com.projectocean.oceancompanion.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OceanDao {
    @Query("SELECT * FROM conversation_history ORDER BY createdAt DESC LIMIT 50")
    fun recentConversations(): Flow<List<ConversationHistory>>

    @Insert
    suspend fun insertConversation(item: ConversationHistory)

    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun userProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: UserProfile)

    @Query("SELECT * FROM long_term_memory ORDER BY updatedAt DESC LIMIT 100")
    fun longTermMemories(): Flow<List<LongTermMemory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMemory(memory: LongTermMemory)

    @Query("SELECT * FROM auto_speech_rule WHERE enabled = 1 ORDER BY createdAt DESC")
    fun enabledRules(): Flow<List<AutoSpeechRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRule(rule: AutoSpeechRule)
}
