package com.photobox.core.data.repository

import com.photobox.core.data.db.dao.PendingShuffleDao
import com.photobox.core.data.db.dao.ShuffleDao
import com.photobox.core.data.db.entity.ShuffleStateEntity
import com.photobox.core.data.mediastore.MediaStoreDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RandomStreamRepository @Inject constructor(
    private val mediaStore: MediaStoreDataSource,
    private val shuffleDao: ShuffleDao,
    private val pendingShuffleDao: PendingShuffleDao,
) {
    /**
     * 当前洗牌序列。Flow：每次持久化状态变更（reshuffle、popCurrent）后 emit。
     * 列表可能为空（相册为空 / 全部已看）。
     */
    val queue: Flow<List<Long>> = shuffleDao.observeState()
        .map { it?.queueMediaIds.orEmpty() }
        .distinctUntilChanged()

    /**
     * 重新洗牌：从 MediaStore 全量 + pending_shuffle 合并，去重，Fisher-Yates。
     * 持久化新的 ShuffleStateEntity，重置 currentIndex = 0。
     */
    suspend fun reshuffle() {
        val all = mediaStore.queryAll().map { it.mediaId }
        val pending = pendingShuffleDao.readAllIds()
        val merged = (all + pending).distinct()
        val shuffled = fisherYates(merged)
        shuffleDao.upsert(
            ShuffleStateEntity(
                id = 1,
                queueMediaIds = shuffled,
                currentIndex = 0,
                roundStartedAt = System.currentTimeMillis(),
                lastShuffledMediaIds = emptyList(),
            )
        )
        pendingShuffleDao.clear()
    }

    /**
     * 标记当前 mediaId 为已浏览。
     * 不弹队列；用 lastShuffledMediaIds 记录已看。配合 popCurrent 决定何时结束一轮。
     */
    suspend fun markViewed(mediaId: Long) {
        val state = shuffleDao.readState() ?: return
        if (state.queueMediaIds.isEmpty()) return
        val updatedViewed = (state.lastShuffledMediaIds + mediaId).distinct()
        shuffleDao.upsert(state.copy(lastShuffledMediaIds = updatedViewed))
    }

    /**
     * 弹出队列头：返回该 id 并把 currentIndex + 1。
     * 队列头存在 → 返回 id；空 → 返回 null（VM 据此判断空状态）。
     */
    suspend fun popCurrent(): Long? {
        val state = shuffleDao.readState() ?: return null
        val queue = state.queueMediaIds
        if (state.currentIndex >= queue.size) return null
        val id = queue[state.currentIndex]
        shuffleDao.upsert(state.copy(currentIndex = state.currentIndex + 1))
        return id
    }

    /**
     * 当前轮是否结束：currentIndex >= queue.size。
     */
    suspend fun isRoundFinished(): Boolean {
        val state = shuffleDao.readState() ?: return true
        return state.currentIndex >= state.queueMediaIds.size
    }

    /**
     * Fisher-Yates 洗牌。纯函数，可独立测试。
     */
    private fun fisherYates(input: List<Long>): List<Long> {
        if (input.size <= 1) return input
        val arr = input.toMutableList()
        val rnd = java.util.Random()
        for (i in arr.size - 1 downTo 1) {
            val j = rnd.nextInt(i + 1)
            val tmp = arr[i]
            arr[i] = arr[j]
            arr[j] = tmp
        }
        return arr
    }
}
