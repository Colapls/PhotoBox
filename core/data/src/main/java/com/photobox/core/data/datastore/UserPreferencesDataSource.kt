package com.photobox.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val PRIVACY_ACK = booleanPreferencesKey("privacy_acknowledged")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_LOCK_LAST_UNLOCK_AT = longPreferencesKey("app_lock_last_unlock_at")
        val FILTER_MODE = stringPreferencesKey("filter_mode")
        val TIME_RANGE = stringPreferencesKey("time_range")
        val ALBUM_FILTER = stringPreferencesKey("album_filter")
        val VIEWED_COUNT = intPreferencesKey("viewed_count")
    }

    val userPreferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            onboardingDone = prefs[Keys.ONBOARDING_DONE] ?: false,
            privacyAcknowledged = prefs[Keys.PRIVACY_ACK] ?: false,
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            appLockLastUnlockAt = prefs[Keys.APP_LOCK_LAST_UNLOCK_AT] ?: 0L,
            filterMode = prefs[Keys.FILTER_MODE]?.let { runCatching { FilterMode.valueOf(it) }.getOrNull() }
                ?: FilterMode.MIXED,
            timeRange = prefs[Keys.TIME_RANGE]?.let { runCatching { TimeRange.valueOf(it) }.getOrNull() }
                ?: TimeRange.ALL,
            albumFilter = prefs[Keys.ALBUM_FILTER],
            viewedCount = prefs[Keys.VIEWED_COUNT] ?: 0,
        )
    }

    suspend fun setOnboardingDone(value: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_DONE] = value }
    }

    suspend fun setPrivacyAcknowledged(value: Boolean) {
        dataStore.edit { it[Keys.PRIVACY_ACK] = value }
    }

    suspend fun setAppLockEnabled(value: Boolean) {
        dataStore.edit { it[Keys.APP_LOCK_ENABLED] = value }
    }

    suspend fun setAppLockLastUnlockAt(value: Long) {
        dataStore.edit { it[Keys.APP_LOCK_LAST_UNLOCK_AT] = value }
    }

    suspend fun setFilterMode(value: FilterMode) {
        dataStore.edit { it[Keys.FILTER_MODE] = value.name }
    }

    suspend fun setTimeRange(value: TimeRange) {
        dataStore.edit { it[Keys.TIME_RANGE] = value.name }
    }

    suspend fun setAlbumFilter(value: String?) {
        dataStore.edit {
            if (value == null) it.remove(Keys.ALBUM_FILTER)
            else it[Keys.ALBUM_FILTER] = value
        }
    }

    suspend fun incrementViewedCount() {
        dataStore.edit { prefs ->
            prefs[Keys.VIEWED_COUNT] = (prefs[Keys.VIEWED_COUNT] ?: 0) + 1
        }
    }

    suspend fun setViewedCount(value: Int) {
        dataStore.edit { it[Keys.VIEWED_COUNT] = value }
    }

    suspend fun resetViewedCount() {
        dataStore.edit { it.remove(Keys.VIEWED_COUNT) }
    }
}
