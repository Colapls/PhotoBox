package com.photobox.core.data.datastore

enum class FilterMode { PHOTOS_ONLY, VIDEOS_ONLY, MIXED }
enum class TimeRange { ALL, THREE_YEARS_AGO, FIVE_YEARS_AGO }

data class UserPreferences(
    val onboardingDone: Boolean = false,
    val privacyAcknowledged: Boolean = false,
    val appLockEnabled: Boolean = false,
    val appLockLastUnlockAt: Long = 0L,
    val filterMode: FilterMode = FilterMode.MIXED,
    val timeRange: TimeRange = TimeRange.ALL,
    val albumFilter: String? = null,
    val viewedCount: Int = 0,
)
