package com.photobox.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.imageLoader
import com.photobox.core.common.AppDispatchers
import com.photobox.core.data.datastore.FilterMode
import com.photobox.core.data.datastore.TimeRange
import com.photobox.core.data.datastore.UserPreferencesDataSource
import com.photobox.core.data.mediastore.MediaStoreDataSource
import com.photobox.core.data.repository.RandomStreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferencesDataSource,
    private val randomStreamRepo: RandomStreamRepository,
    private val mediaStore: MediaStoreDataSource,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        prefs.userPreferences
            .onEach { p ->
                _state.value = _state.value.copy(prefs = p, isLoading = false)
                refreshCacheSize()
            }
            .launchIn(viewModelScope)
        loadAlbums()
    }

    private fun loadAlbums() {
        viewModelScope.launch {
            val albums = withContext(dispatchers.io) {
                mediaStore.queryAll().map { it.uri }.distinct()  // 占位：真实相册列表需要 BUCKET_DISPLAY_NAME；Plan 3 仅返回 distinct uri
            }
            _state.value = _state.value.copy(availableAlbums = albums)
        }
    }

    fun setFilterMode(mode: FilterMode) {
        viewModelScope.launch { prefs.setFilterMode(mode) }
    }

    fun setTimeRange(range: TimeRange) {
        viewModelScope.launch { prefs.setTimeRange(range) }
    }

    fun setAlbumFilter(album: String?) {
        viewModelScope.launch { prefs.setAlbumFilter(album) }
    }

    fun toggleAppLock(enabled: Boolean) {
        viewModelScope.launch { prefs.setAppLockEnabled(enabled) }
    }

    fun reshuffle() {
        viewModelScope.launch {
            withContext(dispatchers.io) { randomStreamRepo.reshuffle() }
            _state.value = _state.value.copy(toast = "已重新洗牌")
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            withContext(dispatchers.io) {
                context.imageLoader.diskCache?.clear()
                context.imageLoader.memoryCache?.clear()
            }
            _state.value = _state.value.copy(toast = "已清除缓存")
            refreshCacheSize()
        }
    }

    fun consumeToast() {
        _state.value = _state.value.copy(toast = null)
    }

    private fun refreshCacheSize() {
        viewModelScope.launch {
            val size = withContext(dispatchers.io) {
                context.imageLoader.diskCache?.size ?: 0L
            }
            _state.value = _state.value.copy(cacheSizeBytes = size)
        }
    }
}