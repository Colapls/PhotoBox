package com.photobox.feature.day

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photobox.core.common.AppDispatchers
import com.photobox.core.data.mediastore.MediaItem
import com.photobox.core.data.mediastore.MediaStoreDataSource
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
            _state.value = _state.value.copy(items = items, dateMs = dateMs, isLoading = false)
        }
    }
}