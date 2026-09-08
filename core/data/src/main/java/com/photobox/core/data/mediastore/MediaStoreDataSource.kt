package com.photobox.core.data.mediastore

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.photobox.core.data.db.dao.PendingShuffleDao
import com.photobox.core.data.db.entity.PendingShuffleEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        val out = mutableListOf<MediaItem>()
        resolver.query(collection, projection, null, null, sortOrder)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                out += MediaItem(
                    mediaId = id,
                    uri = uri.toString(),
                    isVideo = false,
                    dateTakenMs = c.getLong(dateCol),
                    mimeType = c.getString(mimeCol) ?: "image/*",
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
     * 简化实现：每次 ContentObserver 触发时，调用 queryAll() 与 pending 集合求差。
     * 实际项目可优化为基于 notify_change 的 URI 携带 _ID，但 ContentObserver 回调不带具体 URI。
     */
    fun observeNewMedia(knownMediaIds: Set<Long>): Flow<Unit> = callbackFlow {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
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
        resolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer
        )
        resolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer
        )
        awaitClose {
            resolver.unregisterContentObserver(observer)
        }
    }
}
