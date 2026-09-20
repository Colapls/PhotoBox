package com.photobox.feature.stream

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.core.content.ContextCompat
import com.photobox.core.data.mediastore.MediaItem
import com.photobox.core.ui.BrandLogo
import com.photobox.feature.share.ShareDispatcher
import com.photobox.feature.share.ShareResult
import com.photobox.feature.share.ShareResultBus
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Size
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

// 悬浮图标统一用灰白，避免纯白图片上白图标不可见。
private val OverlayIconTint = Color(0xFFE0E0E0)

@Composable
fun StreamScreen(
    modifier: Modifier = Modifier,
    viewModel: StreamViewModel = hiltViewModel(),
    onSwipeToDayAlbum: (dateMs: Long) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.onLocationPermissionGranted()
    }

    LaunchedEffect(state.locationPermissionPrompted, state.items.isNotEmpty()) {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_MEDIA_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED
        if (state.items.isNotEmpty() && needsPermission && !state.locationPermissionPrompted) {
            viewModel.onLocationPermissionPrompted()
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
        }
    }

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

    val shareVm: ShareHelperViewModel = hiltViewModel()
    val snackbar = remember { SnackbarHostState() }
    val shareBus = shareVm.shareResultBus
    LaunchedEffect(Unit) {
        shareBus.results.collect { result ->
            val msg = when (result) {
                is ShareResult.Success -> "已分享到微信"
                is ShareResult.Failure -> result.message
                ShareResult.WeChatNotInstalled -> "请先安装微信"
            }
            snackbar.showSnackbar(msg)
        }
    }

    val deleteLauncher = rememberDeleteRequestLauncher { success ->
        // Read from VM.state.value at the moment the result returns — NOT from a stale composition-captured `state`.
        val currentId = viewModel.state.value.currentItem?.mediaId ?: return@rememberDeleteRequestLauncher
        viewModel.onDeleteConfirmed(currentId, success)
    }

    var senderToLaunch by remember { mutableStateOf<IntentSender?>(null) }

    // 点击删除按钮直接启动系统 IntentSender 弹窗；系统弹窗本身就是确认，
    // 且会把照片送入系统相册的「最近删除」。
    if (senderToLaunch != null) {
        LaunchedEffect(senderToLaunch) {
            deleteLauncher.launch(IntentSenderRequest.Builder(senderToLaunch!!).build())
            senderToLaunch = null  // single-shot
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(state.currentItem?.mediaId) {
                // 用 Compose 自带的 detectHorizontalDragGestures 替代手写 awaitEachGesture：
                // 它内部用 awaitTouchSlopOrCancellation 处理「水平 vs 垂直」判定，
                // 不会和 VerticalPager 的 pointerInput 抢状态机。
                // 水平拖到阈值（4×touchSlop）→ 当天相册；垂直动作它自动放手。
                val touchSlop = viewConfiguration.touchSlop
                var accumulated = 0f
                detectHorizontalDragGestures(
                    onDragStart = { accumulated = 0f },
                    onDragEnd = {
                        if (accumulated < -touchSlop * 4) {
                            state.currentItem?.let { onSwipeToDayAlbum(it.dateTakenMs) }
                        }
                    },
                    onDragCancel = { accumulated = 0f },
                    onHorizontalDrag = { _, dragAmount -> accumulated += dragAmount },
                )
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
                item = item,
                onDoubleTap = { viewModel.onLikeToggle() },
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                BrandLogo(modifier = Modifier.size(32.dp))
                Column {
                    Text(
                        text = "PhotoBox",
                        color = OverlayIconTint,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.8f),
                                offset = Offset(0f, 1f),
                                blurRadius = 4f,
                            ),
                        ),
                    )
                    Text(
                        text = "照片盲盒",
                        color = OverlayIconTint.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = onOpenProfile) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "个人中心",
                        tint = OverlayIconTint,
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "设置",
                        tint = OverlayIconTint,
                    )
                }
            }
        }

        // 底部操作栏：固定显示，不随图片滑动
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End),
        ) {
            IconButton(onClick = {
                state.currentItem?.let { shareVm.shareCurrent(context, it.uri) }
            }) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "分享",
                    tint = OverlayIconTint,
                )
            }
            IconButton(onClick = { viewModel.onLikeToggle() }) {
                val currentId = state.currentItem?.mediaId
                val isLiked = currentId != null && currentId in state.likedIds
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isLiked) "取消点赞" else "点赞",
                    tint = OverlayIconTint,
                )
            }
            IconButton(onClick = { viewModel.onFavoriteToggle() }) {
                val currentId = state.currentItem?.mediaId
                val isFavorite = currentId != null && currentId in state.favoriteIds
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = if (isFavorite) "取消收藏" else "收藏",
                    tint = OverlayIconTint,
                )
            }
            IconButton(onClick = {
                state.currentItem?.let { senderToLaunch = viewModel.createDeleteIntent(it) }
            }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = OverlayIconTint,
                )
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@HiltViewModel
class ShareHelperViewModel @Inject constructor(
    private val dispatcher: ShareDispatcher,
    val shareResultBus: ShareResultBus,
) : ViewModel() {
    fun shareCurrent(context: android.content.Context, imageUri: String) {
        viewModelScope.launch {
            val request = ImageRequest.Builder(context)
                .data(imageUri)
                .size(Size(2048, 2048))
                .build()
            val result = context.imageLoader.execute(request)
            val drawable = (result as SuccessResult)
                .image
                .asDrawable(context.resources)
            val bitmap = (drawable as android.graphics.drawable.BitmapDrawable).bitmap
            dispatcher.shareImages(listOf(bitmap))
        }
    }
}
