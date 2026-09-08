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
}
