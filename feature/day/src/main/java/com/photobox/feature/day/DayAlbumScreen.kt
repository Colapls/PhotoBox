package com.photobox.feature.day

import android.content.IntentSender
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.photobox.core.data.mediastore.MediaItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 当天相册 grid：
 * - 默认模式：点击缩略图 = 进入全屏查看器（[DayAlbumPhotoViewer]）。
 * - 长按一张 = 进入选择模式（[DayAlbumViewModel.enterSelectionMode]）。
 * - 选择模式：点击 = 选中/取消选中；全取消自动退出。
 * - 选择模式顶栏显示"已选 N" + 收藏 / 删除 / 退出按钮。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayAlbumScreen(
    dateMs: Long,
    onBack: () -> Unit,
    onPhotoClick: (mediaId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DayAlbumViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val unsupportedMsg = stringResource(R.string.day_batch_delete_unsupported)

    // "批量删除不支持"snackbar 触发信号：true 时下一次 LaunchedEffect 弹出提示并清零。
    var unsupportedPending by remember { mutableStateOf(false) }
    LaunchedEffect(unsupportedPending) {
        if (unsupportedPending) {
            snackbar.showSnackbar(unsupportedMsg)
            unsupportedPending = false
        }
    }

    LaunchedEffect(dateMs) {
        viewModel.load(dateMs)
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onBatchDeleteConfirmed(result.resultCode == android.app.Activity.RESULT_OK)
    }

    var senderToLaunch by remember { mutableStateOf<IntentSender?>(null) }
    if (senderToLaunch != null) {
        LaunchedEffect(senderToLaunch) {
            deleteLauncher.launch(IntentSenderRequest.Builder(senderToLaunch!!).build())
            senderToLaunch = null
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (state.isSelectionMode) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.day_selected_count, state.selectedIds.size),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.day_exit_selection),
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.batchFavorite() },
                            enabled = state.selectedIds.isNotEmpty(),
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = stringResource(R.string.day_batch_favorite),
                            )
                        }
                        IconButton(
                            onClick = {
                                val sender = viewModel.createBatchDeleteIntent()
                                if (sender != null) {
                                    senderToLaunch = sender
                                } else {
                                    unsupportedPending = true
                                }
                            },
                            enabled = state.selectedIds.isNotEmpty(),
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.day_batch_delete),
                            )
                        }
                    },
                )
            } else {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = formatDate(dateMs),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.day_back),
                            )
                        }
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbar) },
    ) { padding ->
        if (state.items.isEmpty() && !state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.day_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            items(state.items, key = { it.mediaId }) { item ->
                val selected = item.mediaId in state.selectedIds
                DayAlbumThumbnail(
                    item = item,
                    isSelected = selected,
                    selectionEnabled = state.isSelectionMode,
                    onClick = {
                        if (state.isSelectionMode) {
                            viewModel.toggleSelection(item.mediaId)
                        } else {
                            onPhotoClick(item.mediaId)
                        }
                    },
                    onLongClick = {
                        if (!state.isSelectionMode) {
                            viewModel.enterSelectionMode(item.mediaId)
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayAlbumThumbnail(
    item: MediaItem,
    isSelected: Boolean,
    selectionEnabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (selectionEnabled) {
            // 选中态：覆盖一层半透明高亮 + 右上角 ✓ 图标
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        else Color.Black.copy(alpha = 0.15f),
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Black.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(50),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        } else if (item.isVideo) {
            // 非选中态：视频项右下角叠加 ▶ 图标，提示这是视频。
            // 缩略图本身由 AsyncImage + VideoFrameDecoder 抽首帧渲染，不再黑屏。
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(50),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "视频",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

private fun formatDate(ms: Long): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return fmt.format(Date(ms))
}