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
     * 准备 IntentSender 用于删除确认（API 30+）。
     * pre-30：直接 resolver.delete()（已弃用但可工作）。
     * VM 拿到 IntentSender 后用 ActivityResultLauncher 启动系统弹窗。
     */
    fun createDeleteRequest(item: MediaItem): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val uri = Uri.parse(item.uri)
        return MediaStore.createDeleteRequest(resolver, listOf(uri)).intentSender
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
