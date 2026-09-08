package com.photobox.feature.stream

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photobox.core.common.AppDispatchers
import com.photobox.core.data.mediastore.MediaItem
import com.photobox.core.data.mediastore.MediaStoreDataSource
import com.photobox.core.data.repository.DeleteRepository
import com.photobox.core.data.repository.FavoriteRepository
import com.photobox.core.data.repository.LikeRepository
import com.photobox.core.data.repository.RandomStreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class StreamViewModel @Inject constructor(
    private val randomStreamRepo: RandomStreamRepository,
    private val likeRepo: LikeRepository,
    private val favoriteRepo: FavoriteRepository,
    private val deleteRepo: DeleteRepository,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _state = MutableStateFlow(StreamUiState(isEmpty = true))
    val state: StateFlow<StreamUiState> = _state

    init {
        viewModelScope.launch { reshuffleIfEmpty() }
        observeChanges()
    }

    private suspend fun reshuffleIfEmpty() {
        val queue = randomStreamRepo.queue.first()
        if (queue.isEmpty()) {
            withContext(dispatchers.io) { randomStreamRepo.reshuffle() }
        }
    }

    private fun observeChanges() {
        viewModelScope.launch {
            combine(
                randomStreamRepo.queue,
                likeRepo.observeAllIds(),
                favoriteRepo.observeAllIds(),
            ) { ids, liked, favorite ->
                Triple(ids, liked.toSet(), favorite.toSet())
            }.flowOn(dispatchers.io).collect { (ids, liked, favorite) ->
                val mediaList = ids.mapNotNull { id ->
                    mediaStoreDataSource.queryAll().firstOrNull { it.mediaId == id }
                }
                _state.value = _state.value.copy(
                    items = mediaList,
                    likedIds = liked,
                    favoriteIds = favorite,
                    isEmpty = mediaList.isEmpty(),
                    roundFinished = _state.value.roundFinished && mediaList.isNotEmpty(),
                )
            }
        }
    }

    fun onPageChanged(index: Int) {
        _state.value = _state.value.copy(currentIndex = index)
        val item = _state.value.items.getOrNull(index) ?: return
        viewModelScope.launch {
            withContext(dispatchers.io) { randomStreamRepo.markViewed(item.mediaId) }
            val finished = withContext(dispatchers.io) { randomStreamRepo.isRoundFinished() }
            _state.value = _state.value.copy(roundFinished = finished)
        }
    }

    fun onLikeToggle() {
        val current = _state.value.currentItem ?: return
        viewModelScope.launch { withContext(dispatchers.io) { likeRepo.toggle(current.mediaId) } }
    }

    fun onFavoriteToggle() {
        val current = _state.value.currentItem ?: return
        viewModelScope.launch { withContext(dispatchers.io) { favoriteRepo.toggle(current) } }
    }

    fun onReshuffle() {
        viewModelScope.launch {
            withContext(dispatchers.io) { randomStreamRepo.reshuffle() }
            _state.value = _state.value.copy(currentIndex = 0, roundFinished = false)
        }
    }

    fun createDeleteIntent(item: MediaItem) = deleteRepo.createDeleteRequest(item)

    fun onDeleteConfirmed(mediaId: Long, success: Boolean) {
        if (!success) return
        viewModelScope.launch {
            withContext(dispatchers.io) { deleteRepo.cleanupAfterDelete(mediaId) }
        }
    }
}
