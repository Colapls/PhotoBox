package com.photobox.feature.day

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photobox.core.common.AppDispatchers
import com.photobox.core.data.mediastore.MediaItem
import com.photobox.core.data.mediastore.MediaStoreDataSource
import com.photobox.core.data.repository.DeleteRepository
import com.photobox.core.data.repository.FavoriteRepository
import com.photobox.core.data.repository.LikeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DayAlbumViewModel @Inject constructor(
    private val mediaStore: MediaStoreDataSource,
    private val likeRepo: LikeRepository,
    private val favoriteRepo: FavoriteRepository,
    private val deleteRepo: DeleteRepository,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _state = MutableStateFlow(DayAlbumUiState())
    val state: StateFlow<DayAlbumUiState> = _state.asStateFlow()

    init {
        // 监听全局 like / favorite ids（保持与随机流一致）
        combine(likeRepo.observeAllIds(), favoriteRepo.observeAllIds()) { likes, favorites ->
            likes.toSet() to favorites.toSet()
        }.onEach { (liked, favorites) ->
            _state.value = _state.value.copy(likedIds = liked, favoriteIds = favorites)
        }.launchIn(viewModelScope)
    }

    fun load(dateMs: Long) {
        viewModelScope.launch {
            val items = withContext(dispatchers.io) { mediaStore.queryByDay(dateMs) }
            _state.value = _state.value.copy(
                items = items,
                dateMs = dateMs,
                isLoading = false,
                isSelectionMode = false,
                selectedIds = emptySet(),
            )
        }
    }

    /**
     * 长按一张缩略图 → 进入选择模式，并把这一张作为初始选中。
     */
    fun enterSelectionMode(initialId: Long) {
        _state.value = _state.value.copy(
            isSelectionMode = true,
            selectedIds = setOf(initialId),
        )
    }

    fun toggleSelection(mediaId: Long) {
        val current = _state.value.selectedIds
        val newSet = if (mediaId in current) current - mediaId else current + mediaId
        if (newSet.isEmpty()) {
            // 全取消时直接退出选择模式
            _state.value = _state.value.copy(
                isSelectionMode = false,
                selectedIds = emptySet(),
            )
        } else {
            _state.value = _state.value.copy(selectedIds = newSet)
        }
    }

    fun exitSelectionMode() {
        _state.value = _state.value.copy(
            isSelectionMode = false,
            selectedIds = emptySet(),
        )
    }

    /**
     * 批量收藏 / 取消收藏：对当前选中集合中的每一项 toggle。
     * 不在选中态时调用是 no-op。
     */
    fun batchFavorite() {
        val s = _state.value
        if (!s.isSelectionMode || s.selectedIds.isEmpty()) return
        viewModelScope.launch {
            withContext(dispatchers.io) {
                s.items.filter { it.mediaId in s.selectedIds }.forEach { item ->
                    favoriteRepo.toggle(item)
                }
            }
            exitSelectionMode()
        }
    }

    /**
     * 准备批量删除 IntentSender：UI 拿到后启动系统弹窗。
     * 返回 null 表示无选中 / API < 30 不支持批量。
     */
    fun createBatchDeleteIntent(): IntentSender? {
        val s = _state.value
        if (!s.isSelectionMode || s.selectedIds.isEmpty()) return null
        val items = s.items.filter { it.mediaId in s.selectedIds }
        return deleteRepo.createBatchDeleteRequest(items)
    }

    /**
     * 用户在系统弹窗点了确认（success=true）。清理 Room 引用 + 刷新 + 退出选择模式。
     */
    fun onBatchDeleteConfirmed(success: Boolean) {
        if (!success) return
        val ids = _state.value.selectedIds
        viewModelScope.launch {
            withContext(dispatchers.io) { deleteRepo.cleanupAfterDeleteBatch(ids) }
            // 重新加载当天的照片（被删的会自然消失）
            val dateMs = _state.value.dateMs
            val items = withContext(dispatchers.io) { mediaStore.queryByDay(dateMs) }
            _state.value = _state.value.copy(
                items = items,
                isSelectionMode = false,
                selectedIds = emptySet(),
            )
        }
    }
}