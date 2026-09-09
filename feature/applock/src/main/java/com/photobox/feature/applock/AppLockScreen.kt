package com.photobox.feature.applock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AppLockScreen(
    viewModel: AppLockViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    LaunchedEffect(state.isLocked, activity) {
        if (state.isLocked && state.isBiometricAvailable && activity != null) {
            // 自动弹出 BiometricPrompt（仅在 isLocked 翻转 + activity 就绪时触发）
            viewModel.biometricAuthenticator.showPrompt(
                activity = activity,
                title = activity.getString(R.string.applock_biometric_prompt_title),
                subtitle = activity.getString(R.string.applock_biometric_prompt_subtitle),
                onSuccess = { viewModel.onUnlockSucceeded() },
                onError = { _, msg -> viewModel.onUnlockFailed(msg) },
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xCC0A0A0A)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp),
                )
            }
            Text(
                text = stringResource(R.string.applock_title),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )
            Text(
                text = if (state.isBiometricAvailable) {
                    stringResource(R.string.applock_subtitle)
                } else {
                    stringResource(R.string.applock_unavailable)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
            if (state.isBiometricAvailable && activity != null) {
                Button(onClick = {
                    viewModel.biometricAuthenticator.showPrompt(
                        activity = activity,
                        title = activity.getString(R.string.applock_biometric_prompt_title),
                        subtitle = activity.getString(R.string.applock_biometric_prompt_subtitle),
                        onSuccess = { viewModel.onUnlockSucceeded() },
                        onError = { _, msg -> viewModel.onUnlockFailed(msg) },
                    )
                }) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                    Text(
                        text = stringResource(R.string.applock_unlock_button),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}
