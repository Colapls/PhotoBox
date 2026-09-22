package com.photobox.core.data.datastore

enum class FilterMode { PHOTOS_ONLY, VIDEOS_ONLY, GIFS_ONLY, MIXED }

enum class TimeRange {
    ALL,
    LAST_MONTH,
    LAST_THREE_MONTHS,
    LAST_YEAR,
    THREE_YEARS_AGO,
    FIVE_YEARS_AGO,
    CUSTOM,
}

data class UserPreferences(
    val onboardingDone: Boolean = false,
    val privacyAcknowledged: Boolean = false,
    val filterMode: FilterMode = FilterMode.MIXED,
    val timeRange: TimeRange = TimeRange.ALL,
    /** [TimeRange.CUSTOM] 模式下生效，null 表示该端点不限。 */
    val customStartMs: Long? = null,
    val customEndMs: Long? = null,
    val albumFilter: String? = null,
    val viewedCount: Int = 0,
)

/**
 * 把 [UserPreferences] 中跟查询相关的字段打包，方便传给 [MediaStoreDataSource.queryWithFilter]。
 */
data class MediaFilter(
    val filterMode: FilterMode = FilterMode.MIXED,
    val timeRange: TimeRange = TimeRange.ALL,
    val customStartMs: Long? = null,
    val customEndMs: Long? = null,
    val albumFilter: String? = null,
) {
    companion object {
        fun from(prefs: UserPreferences): MediaFilter = MediaFilter(
            filterMode = prefs.filterMode,
            timeRange = prefs.timeRange,
            customStartMs = prefs.customStartMs,
            customEndMs = prefs.customEndMs,
            albumFilter = prefs.albumFilter,
        )
    }
}