package com.photobox.core.data.mediastore

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.photobox.core.data.datastore.FilterMode
import com.photobox.core.data.datastore.MediaFilter
import com.photobox.core.data.datastore.TimeRange
import com.photobox.core.data.db.dao.PendingShuffleDao
import com.photobox.core.data.db.entity.PendingShuffleEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pendingShuffleDao: PendingShuffleDao,
) {
    private val resolver: ContentResolver get() = context.contentResolver

    /**
     * 全量查询：图片 + 视频，按 DATE_TAKEN 倒序。
     * 调用方负责在 IO 调度器中执行。
     */
    fun queryAll(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        items += queryImages()
        items += queryVideos()
        return items.sortedByDescending { it.dateTakenMs }
    }

    private fun queryImages(): List<MediaItem> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.LATITUDE,
            MediaStore.Images.Media.LONGITUDE,
        )
        // 排除「最近删除」中的项目，避免刚被移入回收站的照片又出现在流里。
        // IS_TRASHED 列是 API 30+ 才有；pre-R 设备的选择器会被默默忽略（cursor 不报错），安全无副作用。
        val selection = "${MediaStore.MediaColumns.IS_TRASHED} = 0"
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        val out = mutableListOf<MediaItem>()
        resolver.query(collection, projection, selection, null, sortOrder)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val latCol = c.getColumnIndex(MediaStore.Images.Media.LATITUDE)
            val lonCol = c.getColumnIndex(MediaStore.Images.Media.LONGITUDE)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                // MediaStore 用 0.0 表示无 GPS（无 EXIF / Q+ 未授予 ACCESS_MEDIA_LOCATION）。
                // 下标 < 0 表示当前 provider 不存在该列（旧版/特定 ROM），同样视作无。
                val lat = if (latCol >= 0) c.getDouble(latCol) else 0.0
                val lon = if (lonCol >= 0) c.getDouble(lonCol) else 0.0
                val (latitude, longitude) = if (lat == 0.0 && lon == 0.0) null to null else lat to lon
                out += MediaItem(
                    mediaId = id,
                    uri = uri.toString(),
                    isVideo = false,
                    dateTakenMs = c.getLong(dateCol),
                    mimeType = c.getString(mimeCol) ?: "image/*",
                    latitude = latitude,
                    longitude = longitude,
                )
            }
        }
        return out
    }

    private fun queryVideos(): List<MediaItem> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
        )
        val sortOrder = "${MediaStore.Video.Media.DATE_TAKEN} DESC"
        val out = mutableListOf<MediaItem>()
        resolver.query(collection, projection, null, null, sortOrder)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                out += MediaItem(
                    mediaId = id,
                    uri = uri.toString(),
                    isVideo = true,
                    dateTakenMs = c.getLong(dateCol),
                    durationMs = c.getLong(durCol),
                    mimeType = c.getString(mimeCol) ?: "video/*",
                )
            }
        }
        return out
    }

    /**
     * 监听新增媒体（图片 + 视频），写入 pending_shuffle 表。
     * Flow 触发：MediaStore 内容变化 → 取最新 _ID（不在 queryAll 中的）。
     * 实现：observer 把变更投递到 Channel；flow 收集器负责查询 + diff + DAO 写入。
     * 这样 suspend 函数只在协程体内调用，避开了 ContentObserver 非挂起回调的限制。
     */
    fun observeNewMedia(knownMediaIds: Set<Long>): Flow<Unit> = callbackFlow {
        val channel = kotlinx.coroutines.channels.Channel<Unit>(
            capacity = kotlinx.coroutines.channels.Channel.CONFLATED,
        )
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                channel.trySend(Unit)
            }
        }
        val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val videoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        resolver.registerContentObserver(imageUri, true, observer)
        resolver.registerContentObserver(videoUri, true, observer)

        // 在 flow 内部做实际工作：每次 channel 收到信号 → 查询 + diff + DAO 写入。
        // 收集方所在的调度器决定 I/O 是否在 IO 线程；典型用法 viewModelScope + Dispatchers.IO。
        launch {
            for (signal in channel) {
                val current = queryAll().map { it.mediaId }.toSet()
                val fresh = current - knownMediaIds
                fresh.forEach { id ->
                    pendingShuffleDao.insertIfAbsent(
                        PendingShuffleEntity(mediaId = id, discoveredAt = System.currentTimeMillis())
                    )
                }
                trySend(Unit)
            }
        }

        awaitClose {
            resolver.unregisterContentObserver(observer)
            channel.close()
        }
    }

    /**
     * 查询指定日期当天（00:00:00 - 23:59:59.999）的所有媒体。
     * 实现：用 DATE_TAKEN 区间 [dayStartMs, dayEndMs) 过滤 images + videos。
     * 调用方负责 IO 调度。
     */
    fun queryByDay(dateTakenMs: Long): List<MediaItem> {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = dateTakenMs
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val dayStartMs = cal.timeInMillis
        val dayEndMs = dayStartMs + 24L * 60 * 60 * 1000

        return (queryImagesInRange(dayStartMs, dayEndMs) +
                queryVideosInRange(dayStartMs, dayEndMs))
            .sortedByDescending { it.dateTakenMs }
    }

    private fun queryImagesInRange(startMs: Long, endMs: Long): List<MediaItem> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val selection = "${MediaStore.Images.Media.DATE_TAKEN} >= ? AND ${MediaStore.Images.Media.DATE_TAKEN} < ? AND ${MediaStore.MediaColumns.IS_TRASHED} = 0"
        val args = arrayOf(startMs.toString(), endMs.toString())
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.LATITUDE,
            MediaStore.Images.Media.LONGITUDE,
        )
        val out = mutableListOf<MediaItem>()
        resolver.query(collection, projection, selection, args, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val latCol = c.getColumnIndex(MediaStore.Images.Media.LATITUDE)
            val lonCol = c.getColumnIndex(MediaStore.Images.Media.LONGITUDE)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                val lat = if (latCol >= 0) c.getDouble(latCol) else 0.0
                val lon = if (lonCol >= 0) c.getDouble(lonCol) else 0.0
                val (latitude, longitude) = if (lat == 0.0 && lon == 0.0) null to null else lat to lon
                out += MediaItem(
                    mediaId = id,
                    uri = uri.toString(),
                    isVideo = false,
                    dateTakenMs = c.getLong(dateCol),
                    mimeType = c.getString(mimeCol) ?: "image/*",
                    latitude = latitude,
                    longitude = longitude,
                )
            }
        }
        return out
    }

    private fun queryVideosInRange(startMs: Long, endMs: Long): List<MediaItem> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val selection = "${MediaStore.Video.Media.DATE_TAKEN} >= ? AND ${MediaStore.Video.Media.DATE_TAKEN} < ?"
        val args = arrayOf(startMs.toString(), endMs.toString())
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
        )
        val out = mutableListOf<MediaItem>()
        resolver.query(collection, projection, selection, args, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                out += MediaItem(
                    mediaId = id,
                    uri = uri.toString(),
                    isVideo = true,
                    dateTakenMs = c.getLong(dateCol),
                    durationMs = c.getLong(durCol),
                    mimeType = c.getString(mimeCol) ?: "video/*",
                )
            }
        }
        return out
    }

    /**
     * 「那年今日」轻量检查：只数当年今天之外有多少张照片，避免进入「那年今日」页前加载全量。
     * 复用 [queryOnThisDay] 的 SQL 条件，但只 SELECT _ID。
     */
    fun countOnThisDay(referenceDateTakenMs: Long): Int {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = referenceDateTakenMs
        }
        val month = cal.get(java.util.Calendar.MONTH) + 1
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val monthDay = "%02d-%02d".format(month, day)

        // IS_TRASHED 列是 API 30+ 才有；pre-R 的 selection 里加这串会被 cursor 默默忽略，安全。
        val trashedFilter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            " AND ${MediaStore.MediaColumns.IS_TRASHED} = 0"
        } else ""
        val imageSel = "strftime('%m-%d', ${MediaStore.Images.Media.DATE_TAKEN}/1000, 'unixepoch', 'localtime') = ?" +
            trashedFilter
        val videoSel = "strftime('%m-%d', ${MediaStore.Video.Media.DATE_TAKEN}/1000, 'unixepoch', 'localtime') = ?"
        val args = arrayOf(monthDay)
        var count = 0

        listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        ).forEachIndexed { idx, uri ->
            val sel = if (idx == 0) imageSel else videoSel
            runCatching {
                resolver.query(uri, arrayOf("_id"), sel, args, null)?.use { c ->
                    count += c.count
                }
            }  // 任何 cursor 异常都不要崩 —— countOnThisDay 是 UI 优化，不影响主流程
        }
        return count
    }

    /**
     * 「那年今日」：查询 month-day 与参考日期相同、但 year 不同的所有媒体。
     * - 用 SQL strftime('%m-%d', DATE_TAKEN/1000, 'unixepoch', 'localtime') 匹配；
     *   DATE_TAKEN 是 UTC ms，所以本地时区校正通过 'localtime'。
     * - 含视频；按 DATE_TAKEN 倒序（最新年份在前）。
     */
    fun queryOnThisDay(referenceDateTakenMs: Long): List<MediaItem> {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = referenceDateTakenMs
        }
        val month = cal.get(java.util.Calendar.MONTH) + 1  // 1-based
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        // SQLite strftime 接受 %m (01-12) / %d (01-31) → 拼成 'MM-DD' 与 DATE_TAKEN 比较。
        val monthDay = "%02d-%02d".format(month, day)

        val imageSel = "strftime('%m-%d', ${MediaStore.Images.Media.DATE_TAKEN}/1000, 'unixepoch', 'localtime') = ? " +
            "AND ${MediaStore.MediaColumns.IS_TRASHED} = 0"
        val videoSel = "strftime('%m-%d', ${MediaStore.Video.Media.DATE_TAKEN}/1000, 'unixepoch', 'localtime') = ?"
        val args = arrayOf(monthDay)

        val out = mutableListOf<MediaItem>()

        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val imageProj = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.MIME_TYPE,
        )
        resolver.query(imageCollection, imageProj, imageSel, args,
            "${MediaStore.Images.Media.DATE_TAKEN} DESC")?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(imageCollection, id)
                out += MediaItem(
                    mediaId = id,
                    uri = uri.toString(),
                    isVideo = false,
                    dateTakenMs = c.getLong(dateCol),
                    mimeType = c.getString(mimeCol) ?: "image/*",
                )
            }
        }

        val videoCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val videoProj = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
        )
        resolver.query(videoCollection, videoProj, videoSel, args,
            "${MediaStore.Video.Media.DATE_TAKEN} DESC")?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(videoCollection, id)
                out += MediaItem(
                    mediaId = id,
                    uri = uri.toString(),
                    isVideo = true,
                    dateTakenMs = c.getLong(dateCol),
                    durationMs = c.getLong(durCol),
                    mimeType = c.getString(mimeCol) ?: "video/*",
                )
            }
        }

        return out
    }

    /**
     * 按筛选条件查询。
     * filterMode: PHOTOS_ONLY / VIDEOS_ONLY / MIXED
     * timeRange: ALL / LAST_MONTH / LAST_THREE_MONTHS / LAST_YEAR /
     *            THREE_YEARS_AGO / FIVE_YEARS_AGO / CUSTOM
     *   - 前 6 种基于 nowMs 过滤 DATE_TAKEN >= cutoff（CUSTOM 用 customStartMs/customEndMs 区间）
     * albumFilter: null（不限）或具体相册名（占位，详见注释）
     */
    fun queryWithFilter(
        filter: MediaFilter,
        nowMs: Long = System.currentTimeMillis(),
    ): List<MediaItem> {
        val raw = queryAll()
        return raw.filter { item ->
            val modeOk = when (filter.filterMode) {
                FilterMode.PHOTOS_ONLY -> !item.isVideo
                FilterMode.VIDEOS_ONLY -> item.isVideo
                FilterMode.GIFS_ONLY -> !item.isVideo && item.mimeType.equals("image/gif", ignoreCase = true)
                FilterMode.MIXED -> true
            }
            val timeOk = when (filter.timeRange) {
                TimeRange.ALL -> true
                TimeRange.LAST_MONTH -> item.dateTakenMs >= nowMs - 30L * 24 * 60 * 60 * 1000
                TimeRange.LAST_THREE_MONTHS -> item.dateTakenMs >= nowMs - 90L * 24 * 60 * 60 * 1000
                TimeRange.LAST_YEAR -> item.dateTakenMs >= nowMs - 365L * 24 * 60 * 60 * 1000
                TimeRange.THREE_YEARS_AGO -> item.dateTakenMs >= nowMs - 3L * 365 * 24 * 60 * 60 * 1000
                TimeRange.FIVE_YEARS_AGO -> item.dateTakenMs >= nowMs - 5L * 365 * 24 * 60 * 60 * 1000
                TimeRange.CUSTOM -> {
                    val startOk = filter.customStartMs == null || item.dateTakenMs >= filter.customStartMs
                    val endOk = filter.customEndMs == null || item.dateTakenMs <= filter.customEndMs
                    startOk && endOk
                }
            }
            // albumFilter: 暂不实现相册级别过滤（MediaItem 没有 bucket 字段），
            // 仅在 albumFilter == null 时全部通过；非 null 时当前实现返回空集（UI 已提示）。
            val albumOk = filter.albumFilter == null
            modeOk && timeOk && albumOk
        }
    }

    /**
     * MediaStore 内的总媒体数（图片 + 视频），用于个人中心 dashboard。
     * 用 `_COUNT` 直接查询避免拉全量元数据。
     */
    fun totalCount(): Int {
        var count = 0
        listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        ).forEach { uri ->
            resolver.query(uri, arrayOf("_id"), null, null, null)?.use { c ->
                count += c.count
            }
        }
        return count
    }
}
