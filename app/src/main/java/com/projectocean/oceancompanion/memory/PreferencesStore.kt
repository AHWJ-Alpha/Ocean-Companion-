package com.projectocean.oceancompanion.memory

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.projectocean.oceancompanion.ai.ApiProfile
import kotlinx.coroutines.flow.first
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
    val proactiveBannerMaxChars = context.dataStore.data.map { it[Keys.ProactiveBannerMaxChars] ?: 60 }
    val companionReplyMaxChars = context.dataStore.data.map { it[Keys.CompanionReplyMaxChars] ?: 0 }
    val proactiveBannerOffsetDp = context.dataStore.data.map { it[Keys.ProactiveBannerOffsetDp] ?: 12 }
    val proactiveMuteMinutes = context.dataStore.data.map { it[Keys.ProactiveMuteMinutes] ?: 30 }
    val proactiveMutedUntil = context.dataStore.data.map { it[Keys.ProactiveMutedUntil] ?: 0L }
    val companionOpenGesture = context.dataStore.data.map { it[Keys.CompanionOpenGesture] ?: "long_press" }
    val lastUpdatePromptDay = context.dataStore.data.map { it[Keys.LastUpdatePromptDay] ?: "" }
    val lastWhatsNewVersion = context.dataStore.data.map { it[Keys.LastWhatsNewVersion] ?: "" }
    val themeMode = context.dataStore.data.map { it[Keys.ThemeMode] ?: "system" }
    val animePrimaryColor = context.dataStore.data.map { it[Keys.AnimePrimaryColor] ?: "#39C5BB" }
    val animeSecondaryColor = context.dataStore.data.map { it[Keys.AnimeSecondaryColor] ?: "#00AEEF" }
    val apiProfilesJson = context.dataStore.data.map { it[Keys.ApiProfilesJson] ?: "" }
    val apiProfiles = context.dataStore.data.map { prefs ->
        val saved = ApiProfile.decode(prefs[Keys.ApiProfilesJson].orEmpty())
        saved.ifEmpty {
            listOf(
                ApiProfile(
                    label = prefs[Keys.Provider]?.ifBlank { "OpenAI" } ?: "OpenAI",
                    provider = prefs[Keys.Provider] ?: "openai",
                    baseUrl = prefs[Keys.ApiBaseUrl] ?: "https://api.openai.com/v1",
                    apiKey = prefs[Keys.ApiKey] ?: "",
                    model = prefs[Keys.ModelName] ?: "gpt-4o-mini"
                )
            )
        }
    }

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
    suspend fun setProactiveBannerMaxChars(value: Int) = context.dataStore.edit { it[Keys.ProactiveBannerMaxChars] = value }
    suspend fun setCompanionReplyMaxChars(value: Int) = context.dataStore.edit { it[Keys.CompanionReplyMaxChars] = value }
    suspend fun setProactiveBannerOffsetDp(value: Int) = context.dataStore.edit { it[Keys.ProactiveBannerOffsetDp] = value }
    suspend fun setProactiveMutedUntil(value: Long) = context.dataStore.edit { it[Keys.ProactiveMutedUntil] = value }
    suspend fun setCompanionOpenGesture(value: String) = context.dataStore.edit { it[Keys.CompanionOpenGesture] = value }
    suspend fun setLastUpdatePromptDay(value: String) = context.dataStore.edit { it[Keys.LastUpdatePromptDay] = value }
    suspend fun setLastWhatsNewVersion(value: String) = context.dataStore.edit { it[Keys.LastWhatsNewVersion] = value }
    suspend fun setApiProfiles(value: List<ApiProfile>) = context.dataStore.edit { it[Keys.ApiProfilesJson] = ApiProfile.encode(value) }
    suspend fun setThemeMode(value: String) = context.dataStore.edit { it[Keys.ThemeMode] = value }
    suspend fun setAnimePrimaryColor(value: String) = context.dataStore.edit { it[Keys.AnimePrimaryColor] = value }
    suspend fun setAnimeSecondaryColor(value: String) = context.dataStore.edit { it[Keys.AnimeSecondaryColor] = value }

    suspend fun resolvedApiProfiles(): List<ApiProfile> = apiProfiles.first()

    suspend fun saveSettings(
        provider: String,
        apiBaseUrl: String,
        apiKey: String,
        modelName: String,
        userName: String,
        companionName: String,
        iconText: String,
        customPersonaPrompt: String,
        triggerAppNames: String,
        speechIntervalMinutes: Int,
        panelRatio: Float,
        proactiveReminders: Boolean,
        proactiveBannerMaxChars: Int = 60,
        companionReplyMaxChars: Int = 0,
        proactiveBannerOffsetDp: Int = 12,
        proactiveMuteMinutes: Int = 30,
        companionOpenGesture: String = "long_press",
        themeMode: String = "system",
        animePrimaryColor: String = "#39C5BB",
        animeSecondaryColor: String = "#00AEEF"
    ) = saveSettings(
        provider = provider,
        apiBaseUrl = apiBaseUrl,
        apiKey = apiKey,
        modelName = modelName,
        userName = userName,
        companionName = companionName,
        iconText = iconText,
        customPersonaPrompt = customPersonaPrompt,
        triggerAppNames = triggerAppNames,
        speechIntervalMinutes = speechIntervalMinutes,
        panelRatio = panelRatio,
        proactiveReminders = proactiveReminders,
        proactiveBannerMaxChars = proactiveBannerMaxChars,
        companionReplyMaxChars = companionReplyMaxChars,
        proactiveBannerOffsetDp = proactiveBannerOffsetDp,
        proactiveMuteMinutes = proactiveMuteMinutes,
        companionOpenGesture = companionOpenGesture,
        themeMode = themeMode,
        animePrimaryColor = animePrimaryColor,
        animeSecondaryColor = animeSecondaryColor,
        apiProfiles = listOf(
            ApiProfile(
                label = provider.ifBlank { "OpenAI" },
                provider = provider,
                baseUrl = apiBaseUrl,
                apiKey = apiKey,
                model = modelName
            )
        )
    )

    suspend fun saveSettings(
        provider: String,
        apiBaseUrl: String,
        apiKey: String,
        modelName: String,
        userName: String,
        companionName: String,
        iconText: String,
        customPersonaPrompt: String,
        triggerAppNames: String,
        speechIntervalMinutes: Int,
        panelRatio: Float,
        proactiveReminders: Boolean,
        proactiveBannerMaxChars: Int = 60,
        companionReplyMaxChars: Int = 0,
        proactiveBannerOffsetDp: Int = 12,
        proactiveMuteMinutes: Int = 30,
        companionOpenGesture: String = "long_press",
        themeMode: String = "system",
        animePrimaryColor: String = "#39C5BB",
        animeSecondaryColor: String = "#00AEEF",
        apiProfiles: List<ApiProfile>
    ) = context.dataStore.edit {
        it[Keys.Provider] = provider
        it[Keys.ApiBaseUrl] = apiBaseUrl
        it[Keys.ApiKey] = apiKey
        it[Keys.ModelName] = modelName
        it[Keys.ApiProfilesJson] = ApiProfile.encode(apiProfiles)
        it[Keys.UserName] = userName
        it[Keys.CompanionName] = companionName
        it[Keys.IconText] = iconText
        it[Keys.CustomPersonaPrompt] = customPersonaPrompt
        it[Keys.TriggerAppNames] = triggerAppNames
        it[Keys.SpeechIntervalMinutes] = speechIntervalMinutes
        it[Keys.PanelRatio] = panelRatio
        it[Keys.ProactiveReminders] = proactiveReminders
        it[Keys.ProactiveBannerMaxChars] = proactiveBannerMaxChars
        it[Keys.CompanionReplyMaxChars] = companionReplyMaxChars
        it[Keys.ProactiveBannerOffsetDp] = proactiveBannerOffsetDp
        it[Keys.ProactiveMuteMinutes] = proactiveMuteMinutes
        it[Keys.CompanionOpenGesture] = companionOpenGesture
        it[Keys.ThemeMode] = themeMode
        it[Keys.AnimePrimaryColor] = animePrimaryColor
        it[Keys.AnimeSecondaryColor] = animeSecondaryColor
    }

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
        val ProactiveBannerMaxChars = intPreferencesKey("proactive_banner_max_chars")
        val CompanionReplyMaxChars = intPreferencesKey("companion_reply_max_chars")
        val ProactiveBannerOffsetDp = intPreferencesKey("proactive_banner_offset_dp")
        val ProactiveMuteMinutes = intPreferencesKey("proactive_mute_minutes")
        val ProactiveMutedUntil = androidx.datastore.preferences.core.longPreferencesKey("proactive_muted_until")
        val CompanionOpenGesture = stringPreferencesKey("companion_open_gesture")
        val LastUpdatePromptDay = stringPreferencesKey("last_update_prompt_day")
        val LastWhatsNewVersion = stringPreferencesKey("last_whats_new_version")
        val ThemeMode = stringPreferencesKey("theme_mode")
        val AnimePrimaryColor = stringPreferencesKey("anime_primary_color")
        val AnimeSecondaryColor = stringPreferencesKey("anime_secondary_color")
        val ApiProfilesJson = stringPreferencesKey("api_profiles_json")
    }
}
