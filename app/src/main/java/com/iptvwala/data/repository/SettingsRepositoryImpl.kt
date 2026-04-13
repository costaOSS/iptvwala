package com.iptvwala.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.iptvwala.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {
    
    private object Keys {
        val SERVER_ENABLED = booleanPreferencesKey("server_enabled")
        val SERVER_PORT = intPreferencesKey("server_port")
        val SERVER_PIN = stringPreferencesKey("server_pin")
        val DEVICE_NAME = stringPreferencesKey("device_name")
        
        val VIDEO_QUALITY = stringPreferencesKey("video_quality")
        val BUFFER_SIZE = stringPreferencesKey("buffer_size")
        val DECODER = stringPreferencesKey("decoder")
        val BACKGROUND_PLAYBACK = booleanPreferencesKey("background_playback")
        val AUTO_RETRY = booleanPreferencesKey("auto_retry")
        
        val THEME = stringPreferencesKey("theme")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val CHANNEL_LAYOUT = stringPreferencesKey("channel_layout")
        val SHOW_NUMBERS = booleanPreferencesKey("show_numbers")
        
        val AUTO_REFRESH_INTERVAL = intPreferencesKey("auto_refresh_interval")
        val LAST_SOURCE_REFRESH = longPreferencesKey("last_source_refresh")
    }
    
    override fun getString(key: String, default: String): Flow<String> {
        return context.dataStore.data.map { prefs ->
            when (key) {
                "server_pin" -> prefs[Keys.SERVER_PIN] ?: ""
                "device_name" -> prefs[Keys.DEVICE_NAME] ?: "IPTVwala"
                "video_quality" -> prefs[Keys.VIDEO_QUALITY] ?: "auto"
                "buffer_size" -> prefs[Keys.BUFFER_SIZE] ?: "medium"
                "decoder" -> prefs[Keys.DECODER] ?: "auto"
                "theme" -> prefs[Keys.THEME] ?: "system"
                "accent_color" -> prefs[Keys.ACCENT_COLOR] ?: "purple"
                "channel_layout" -> prefs[Keys.CHANNEL_LAYOUT] ?: "grid"
                else -> default
            }
        }
    }
    
    override fun getInt(key: String, default: Int): Flow<Int> {
        return context.dataStore.data.map { prefs ->
            when (key) {
                "server_port" -> prefs[Keys.SERVER_PORT] ?: 8080
                "auto_refresh_interval" -> prefs[Keys.AUTO_REFRESH_INTERVAL] ?: 12
                else -> prefs[intPreferencesKey(key)] ?: default
            }
        }
    }
    
    override fun getBoolean(key: String, default: Boolean): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            when (key) {
                "server_enabled" -> prefs[Keys.SERVER_ENABLED] ?: false
                "background_playback" -> prefs[Keys.BACKGROUND_PLAYBACK] ?: true
                "auto_retry" -> prefs[Keys.AUTO_RETRY] ?: true
                "show_numbers" -> prefs[Keys.SHOW_NUMBERS] ?: false
                else -> prefs[booleanPreferencesKey(key)] ?: default
            }
        }
    }
    
    override fun getLong(key: String, default: Long): Flow<Long> {
        return context.dataStore.data.map { prefs ->
            when (key) {
                "last_source_refresh" -> prefs[Keys.LAST_SOURCE_REFRESH] ?: 0L
                else -> prefs[longPreferencesKey(key)] ?: default
            }
        }
    }
    
    override suspend fun putString(key: String, value: String) {
        context.dataStore.edit { prefs ->
            when (key) {
                "server_pin" -> prefs[Keys.SERVER_PIN] = value
                "device_name" -> prefs[Keys.DEVICE_NAME] = value
                "video_quality" -> prefs[Keys.VIDEO_QUALITY] = value
                "buffer_size" -> prefs[Keys.BUFFER_SIZE] = value
                "decoder" -> prefs[Keys.DECODER] = value
                "theme" -> prefs[Keys.THEME] = value
                "accent_color" -> prefs[Keys.ACCENT_COLOR] = value
                "channel_layout" -> prefs[Keys.CHANNEL_LAYOUT] = value
            }
        }
    }
    
    override suspend fun putInt(key: String, value: Int) {
        context.dataStore.edit { prefs ->
            when (key) {
                "server_port" -> prefs[Keys.SERVER_PORT] = value
                "auto_refresh_interval" -> prefs[Keys.AUTO_REFRESH_INTERVAL] = value
                else -> prefs[intPreferencesKey(key)] = value
            }
        }
    }
    
    override suspend fun putBoolean(key: String, value: Boolean) {
        context.dataStore.edit { prefs ->
            when (key) {
                "server_enabled" -> prefs[Keys.SERVER_ENABLED] = value
                "background_playback" -> prefs[Keys.BACKGROUND_PLAYBACK] = value
                "auto_retry" -> prefs[Keys.AUTO_RETRY] = value
                "show_numbers" -> prefs[Keys.SHOW_NUMBERS] = value
                else -> prefs[booleanPreferencesKey(key)] = value
            }
        }
    }
    
    override suspend fun putLong(key: String, value: Long) {
        context.dataStore.edit { prefs ->
            when (key) {
                "last_source_refresh" -> prefs[Keys.LAST_SOURCE_REFRESH] = value
                else -> prefs[longPreferencesKey(key)] = value
            }
        }
    }
    
    override suspend fun clear(key: String) {
        context.dataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey(key))
            prefs.remove(intPreferencesKey(key))
            prefs.remove(booleanPreferencesKey(key))
            prefs.remove(longPreferencesKey(key))
        }
    }
    
    override suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
