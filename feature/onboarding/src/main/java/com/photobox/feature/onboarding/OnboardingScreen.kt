package com.photobox.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OnboardingScreen(
    onCompleted: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 3 })

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageChanged(pagerState.currentPage)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,   // 强制流程顺序：隐私 → 权限 → 引导
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> PrivacyPromiseScreen(onAcknowledge = {
                    viewModel.onPrivacyAcknowledged()
                    advance(pagerState, 1)
                })
                1 -> PermissionPromptScreen(onGranted = {
                    viewModel.onPermissionGranted()
                    advance(pagerState, 2)
                })
                2 -> GestureIntroScreen(onContinue = {
                    viewModel.complete(onCompleted)
                })
            }
        }

        // 进度点指示器
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        ) {
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
            ) {
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == pagerState.currentPage) 18.dp else 6.dp, 6.dp)
                            .background(
                                if (i == pagerState.currentPage)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

private suspend fun advance(pagerState: androidx.compose.foundation.pager.PagerState, page: Int) {
    pagerState.animateScrollToPage(page)
}
