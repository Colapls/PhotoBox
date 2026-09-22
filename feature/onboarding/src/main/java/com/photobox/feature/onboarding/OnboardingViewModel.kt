package com.photobox.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photobox.core.data.datastore.UserPreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val currentPage: Int = 0,
    val privacyAcknowledged: Boolean = false,
    val permissionGranted: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: UserPreferencesDataSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        // 兜底：如果 DataStore 里已经 onboardingDone=true（说明之前写过盘但被某种原因又回到这个流程），
        // 立即把 currentPage 推到第 3 页，让用户一键「开始我的照片盲盒」即可退出；
        // 不会出现"第二次以后打开 App 还显示欢迎页"的死循环。
        prefs.userPreferences
            .onEach { userPrefs ->
                if (userPrefs.onboardingDone) {
                    _uiState.update {
                        it.copy(
                            currentPage = 2,
                            privacyAcknowledged = true,
                            permissionGranted = true,
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onPageChanged(page: Int) {
        _uiState.update { it.copy(currentPage = page) }
    }

    fun onPrivacyAcknowledged() {
        _uiState.update { it.copy(privacyAcknowledged = true) }
        viewModelScope.launch { prefs.setPrivacyAcknowledged(true) }
    }

    fun onPermissionGranted() {
        _uiState.update { it.copy(permissionGranted = true) }
    }

    fun complete(onDone: () -> Unit) {
        viewModelScope.launch {
            // 先 await 写盘成功再跳转 —— 避免 OnboardingScreen.onCompleted() 走完
            // navigate(STREAM) 之后写入失败的边角情况（虽然罕见）。
            prefs.setOnboardingDone(true)
            onDone()
        }
    }
}