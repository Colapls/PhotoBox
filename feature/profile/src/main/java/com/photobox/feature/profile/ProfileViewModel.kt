package com.photobox.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photobox.core.common.AppDispatchers
import com.photobox.core.data.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val statsRepo: StatsRepository,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        observeStats()
    }

    private fun observeStats() {
        statsRepo.observeStats(dispatchers.io)
            .onEach { stats ->
                _state.value = ProfileUiState(stats = stats, isLoading = false)
            }
            .catch { e ->
                // Plan 3: 仅 log，不弹错误 UI（spec 8 节"已知遗留"接受）
                _state.value = ProfileUiState(stats = null, isLoading = false)
            }
            .launchIn(viewModelScope)
    }
}