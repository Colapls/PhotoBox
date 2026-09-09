package com.photobox.feature.stream

import android.content.IntentSender
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun StreamScreen(
    modifier: Modifier = Modifier,
    viewModel: StreamViewModel = hiltViewModel(),
    onSwipeToDayAlbum: (dateMs: Long) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isEmpty) {
        EmptyState(
            title = "相册里一张照片都没有",
            description = "去系统相机拍几张，回来看这里能不能刷出来",
            modifier = modifier,
        )
        return
    }

    if (state.roundFinished) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "本轮已浏览 ${state.viewedCount} / ${state.items.size}",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Button(
                    onClick = { viewModel.onReshuffle() },
                    modifier = Modifier.padding(top = 16.dp),
                ) { Text("再来一轮") }
            }
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = state.currentIndex,
        pageCount = { state.items.size },
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .collect { index -> viewModel.onPageChanged(index) }
    }

    val deleteLauncher = rememberDeleteRequestLauncher { success ->
        // Read from VM.state.value at the moment the result returns — NOT from a stale composition-captured `state`.
        val currentId = viewModel.state.value.currentItem?.mediaId ?: return@rememberDeleteRequestLauncher
        viewModel.onDeleteConfirmed(currentId, success)
    }

    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }
    var senderToLaunch by remember { mutableStateOf<IntentSender?>(null) }
    var showDeleteSheet by remember { mutableStateOf(false) }

    if (senderToLaunch != null && pendingDeleteId != null) {
        LaunchedEffect(senderToLaunch) {
            deleteLauncher.launch(senderToLaunch!!)
            senderToLaunch = null  // single-shot
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(state.currentItem?.mediaId) {
                val thresholdPx = 80.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var totalX = 0f
                    var totalY = 0f
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        totalX += change.positionChange().x
                        totalY += change.positionChange().y
                    }
                    if (kotlin.math.abs(totalX) > thresholdPx &&
                        kotlin.math.abs(totalX) > 2 * kotlin.math.abs(totalY) &&
                        totalX < 0
                    ) {
                        state.currentItem?.let { onSwipeToDayAlbum(it.dateTakenMs) }
                    }
                }
            },
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            key = { state.items[it].mediaId },
        ) { pageIndex ->
            val item = state.items[pageIndex]
            PhotoPage(
                imageUri = item.uri,
                isFavorite = item.mediaId in state.favoriteIds,
                onFavoriteToggle = { viewModel.onFavoriteToggle() },
                onRequestDelete = {
                    pendingDeleteId = state.currentItem?.mediaId
                    senderToLaunch = state.currentItem?.let { viewModel.createDeleteIntent(it) }
                    showDeleteSheet = true
                },
                onDoubleTap = { viewModel.onLikeToggle() },
            )
        }

        if (showDeleteSheet) {
            DeleteConfirmationSheet(
                onConfirm = {
                    showDeleteSheet = false
                    // 系统会通过 IntentSender 弹窗继续 — LaunchedEffect 在 senderToLaunch 设置时已触发
                },
                onDismiss = {
                    showDeleteSheet = false
                    pendingDeleteId = null
                    senderToLaunch = null
                },
            )
        }
    }
}
