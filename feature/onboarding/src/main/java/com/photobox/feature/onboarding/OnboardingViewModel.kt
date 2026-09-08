package com.photobox.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photobox.core.data.datastore.UserPreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
            prefs.setOnboardingDone(true)
            onDone()
        }
    }
}
