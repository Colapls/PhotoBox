package com.photobox.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photobox.core.common.AppDispatchers
import com.photobox.core.data.mediastore.MediaStoreDataSource
import com.photobox.core.data.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val statsRepo: StatsRepository,
    private val mediaStore: MediaStoreDataSource,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        observeStats()
        checkOnThisDayAvailability()
    }

    private fun observeStats() {
        statsRepo.observeStats(dispatchers.io)
            .onEach { stats ->
                _state.value = _state.value.copy(stats = stats, isLoading = false)
            }
            .catch {
                _state.value = _state.value.copy(stats = null, isLoading = false)
            }
            .launchIn(viewModelScope)
    }

    /**
     * 检查今天是否有往年拍的照片。countOnThisDay 用同样的 SQL 条件但只取 _ID，避免
     * 一打开个人中心就拉全量 —— 只关心"有没有"。runCatching 包住：异常不能崩主流程。
     */
    private fun checkOnThisDayAvailability() {
        viewModelScope.launch {
            runCatching {
                val count = withContext(dispatchers.io) {
                    mediaStore.countOnThisDay(System.currentTimeMillis())
                }
                _state.value = _state.value.copy(onThisDayCount = count)
            }
        }
    }
}
