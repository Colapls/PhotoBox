package com.photobox.feature.applock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photobox.core.common.AppLockConstants
import com.photobox.core.data.datastore.UserPreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val prefs: UserPreferencesDataSource,
    val biometricAuthenticator: BiometricAuthenticator,
) : ViewModel() {

    private val _state = MutableStateFlow(AppLockUiState(isBiometricAvailable = biometricAuthenticator.isAvailable()))
    val state: StateFlow<AppLockUiState> = _state.asStateFlow()

    init {
        prefs.userPreferences
            .onEach { p ->
                if (!p.appLockEnabled) {
                    _state.value = _state.value.copy(isLocked = false, errorMessage = null)
                } else if (isUnlockStillValid(p.appLockLastUnlockAt)) {
                    _state.value = _state.value.copy(isLocked = false, errorMessage = null)
                } else {
                    _state.value = _state.value.copy(isLocked = true, errorMessage = null)
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * spec 4.5：解锁后 30 秒内回到 App 不重新弹框。
     */
    private fun isUnlockStillValid(lastUnlockAt: Long): Boolean {
        if (lastUnlockAt == 0L) return false
        val now = System.currentTimeMillis()
        return (now - lastUnlockAt) < AppLockConstants.UNLOCK_TTL_MS
    }

    fun onUnlockSucceeded() {
        viewModelScope.launch {
            prefs.setAppLockLastUnlockAt(System.currentTimeMillis())
            _state.value = _state.value.copy(isLocked = false, errorMessage = null)
        }
    }

    fun onUnlockFailed(message: String) {
        _state.value = _state.value.copy(errorMessage = message)
    }
}
