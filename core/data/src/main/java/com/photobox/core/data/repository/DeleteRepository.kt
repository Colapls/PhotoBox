package com.photobox.core.data.repository

import android.content.ContentResolver
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.photobox.core.data.db.dao.FavoriteDao
import com.photobox.core.data.db.dao.LikeDao
import com.photobox.core.data.db.dao.ShuffleDao
import com.photobox.core.data.mediastore.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val likeDao: LikeDao,
    private val favoriteDao: FavoriteDao,
    private val shuffleDao: ShuffleDao,
) {
    private val resolver: ContentResolver get() = context.contentResolver

    /**
     * 直接删除（用户要求无二次确认）：
     * - API 30+：MediaStore.delete 走的是「移到最近删除」，照片进入系统相册「最近删除」30 天。
     *   **不需要弹系统确认**——这是 resolver.delete 的副作用，不是 createDeleteRequest。
     * - API < 30：直接物理删除。
     *
     * 返回删除的行数；UI 据此判断成功失败。
     */
    suspend fun deleteNow(item: MediaItem): Int {
        val uri = Uri.parse(item.uri)
        val deleted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 在 API 30+ 上，ContentResolver.delete() 也会把照片移入「最近删除」，
            // 行为等同于用户手动在相册里删除。
            resolver.delete(uri, null, null)
        } else {
            @Suppress("DEPRECATION")
            resolver.delete(uri, null, null)
        }
        if (deleted > 0) cleanupAfterDelete(item.mediaId)
        return deleted
    }

    /**
     * 准备 IntentSender 用于系统弹窗确认删除（API 30+）。
     * 现在项目里没人调用此方法（走 [deleteNow] 无确认路径），保留以备以后需要。
     */
    fun createDeleteRequest(item: MediaItem): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val uri = Uri.parse(item.uri)
        return MediaStore.createDeleteRequest(resolver, listOf(uri)).intentSender
    }

    /**
     * 批量删除：把若干个 media 的 URI 一次性传给 MediaStore.createDeleteRequest。
     * 系统弹窗会一次性展示全部 URI 并要求一次确认，确认后全部移入「最近删除」。
     * API < 30 暂不支持批量删除。
     */
    fun createBatchDeleteRequest(items: List<MediaItem>): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || items.isEmpty()) return null
        val uris = items.map { Uri.parse(it.uri) }
        return MediaStore.createDeleteRequest(resolver, uris).intentSender
    }

    /**
     * 批量删除成功后清理 Room 中的引用。
     */
    suspend fun cleanupAfterDeleteBatch(mediaIds: Set<Long>) {
        if (mediaIds.isEmpty()) return
        mediaIds.forEach { mediaId ->
            likeDao.delete(mediaId)
            favoriteDao.delete(mediaId)
        }
        val state = shuffleDao.readState() ?: return
        val newQueue = state.queueMediaIds.filter { it !in mediaIds }
        val newViewed = state.lastShuffledMediaIds.filter { it !in mediaIds }
        if (newQueue != state.queueMediaIds || newViewed != state.lastShuffledMediaIds) {
            shuffleDao.upsert(
                state.copy(
                    queueMediaIds = newQueue,
                    lastShuffledMediaIds = newViewed,
                )
            )
        }
    }

    /**
     * 在系统确认删除成功后清理 Room 中的引用。
     * 调用方：ActivityResultLauncher 的 onResult 回调（onResult: Boolean）。
     */
    suspend fun cleanupAfterDelete(mediaId: Long) {
        likeDao.delete(mediaId)
        favoriteDao.delete(mediaId)
        // shuffleState 中移除该 mediaId
        val state = shuffleDao.readState() ?: return
        val newQueue = state.queueMediaIds.filter { it != mediaId }
        val newViewed = state.lastShuffledMediaIds.filter { it != mediaId }
        if (newQueue != state.queueMediaIds || newViewed != state.lastShuffledMediaIds) {
            shuffleDao.upsert(
                state.copy(
                    queueMediaIds = newQueue,
                    lastShuffledMediaIds = newViewed,
                )
            )
        }
    }
}
