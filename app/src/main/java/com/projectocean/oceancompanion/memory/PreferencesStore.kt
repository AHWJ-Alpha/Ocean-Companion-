package com.projectocean.oceancompanion.memory

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("ocean_preferences")

class PreferencesStore(private val context: Context) {
    val provider = context.dataStore.data.map { it[Keys.Provider] ?: "openai" }
    val apiBaseUrl = context.dataStore.data.map { it[Keys.ApiBaseUrl] ?: "https://api.openai.com/v1" }
    val apiKey = context.dataStore.data.map { it[Keys.ApiKey] ?: "" }
    val modelName = context.dataStore.data.map { it[Keys.ModelName] ?: "gpt-4o-mini" }
    val persona = context.dataStore.data.map { it[Keys.Persona] ?: "OceanNative" }
    val customPersonaPrompt = context.dataStore.data.map { it[Keys.CustomPersonaPrompt] ?: "" }
    val userName = context.dataStore.data.map { it[Keys.UserName] ?: "\u4f60" }
    val companionName = context.dataStore.data.map { it[Keys.CompanionName] ?: "Ocean" }
    val iconText = context.dataStore.data.map { it[Keys.IconText] ?: "Ocean" }
    val iconImageUri = context.dataStore.data.map { it[Keys.IconImageUri] ?: "" }
    val speechIntervalMinutes = context.dataStore.data.map { it[Keys.SpeechIntervalMinutes] ?: 15 }
    val triggerAppNames = context.dataStore.data.map { it[Keys.TriggerAppNames] ?: "" }
    val panelRatio = context.dataStore.data.map { it[Keys.PanelRatio] ?: 0.5f }
    val proactiveReminders = context.dataStore.data.map { it[Keys.ProactiveReminders] ?: true }

    suspend fun setProvider(value: String) {
        context.dataStore.edit { it[Keys.Provider] = value }
    }

    suspend fun setApiBaseUrl(value: String) = context.dataStore.edit { it[Keys.ApiBaseUrl] = value }
    suspend fun setApiKey(value: String) = context.dataStore.edit { it[Keys.ApiKey] = value }
    suspend fun setModelName(value: String) = context.dataStore.edit { it[Keys.ModelName] = value }
    suspend fun setCustomPersonaPrompt(value: String) = context.dataStore.edit { it[Keys.CustomPersonaPrompt] = value }
    suspend fun setUserName(value: String) = context.dataStore.edit { it[Keys.UserName] = value }
    suspend fun setCompanionName(value: String) = context.dataStore.edit { it[Keys.CompanionName] = value }
    suspend fun setIconText(value: String) = context.dataStore.edit { it[Keys.IconText] = value }
    suspend fun setIconImageUri(value: String) = context.dataStore.edit { it[Keys.IconImageUri] = value }
    suspend fun setSpeechIntervalMinutes(value: Int) = context.dataStore.edit { it[Keys.SpeechIntervalMinutes] = value }
    suspend fun setTriggerAppNames(value: String) = context.dataStore.edit { it[Keys.TriggerAppNames] = value }
    suspend fun setPanelRatio(value: Float) = context.dataStore.edit { it[Keys.PanelRatio] = value }
    suspend fun setProactiveReminders(value: Boolean) = context.dataStore.edit { it[Keys.ProactiveReminders] = value }

    private object Keys {
        val Provider = stringPreferencesKey("provider")
        val ApiBaseUrl = stringPreferencesKey("api_base_url")
        val ApiKey = stringPreferencesKey("api_key")
        val ModelName = stringPreferencesKey("model_name")
        val Persona = stringPreferencesKey("persona")
        val CustomPersonaPrompt = stringPreferencesKey("custom_persona_prompt")
        val UserName = stringPreferencesKey("user_name")
        val CompanionName = stringPreferencesKey("companion_name")
        val IconText = stringPreferencesKey("icon_text")
        val IconImageUri = stringPreferencesKey("icon_image_uri")
        val SpeechIntervalMinutes = intPreferencesKey("speech_interval_minutes")
        val TriggerAppNames = stringPreferencesKey("trigger_app_names")
        val PanelRatio = floatPreferencesKey("panel_ratio")
        val ProactiveReminders = booleanPreferencesKey("proactive_reminders")
    }
}
