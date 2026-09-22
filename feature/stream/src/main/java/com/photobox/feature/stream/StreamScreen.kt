package com.photobox.feature.stream

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Today
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import com.photobox.core.data.mediastore.MediaItem
import com.photobox.core.ui.BrandLogo

// 悬浮图标统一用灰白，避免纯白图片上白图标不可见。
private val OverlayIconTint = Color(0xFFE0E0E0)

@Composable
fun StreamScreen(
    modifier: Modifier = Modifier,
    viewModel: StreamViewModel = hiltViewModel(),
    onSwipeToDayAlbum: (dateMs: Long) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenOnThisDay: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.onLocationPermissionGranted()
    }

    // 每次进入 StreamScreen 都检查一次 ACCESS_MEDIA_LOCATION 授权状态：
    // 系统是事实来源（DataStore 不存，避免用户清数据后还自以为授权过）。
    // 之前用 in-memory flag 一次性弹出；用户若拒绝了，下次冷启动还会再弹一次。
    LaunchedEffect(state.items.isNotEmpty()) {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_MEDIA_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED
        if (state.items.isNotEmpty() && needsPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
        }
    }

    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator()
        }
        return
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

    // 双击点赞时触发：每次自增 → PhotoPage 内 LaunchedEffect 看到新值就播一次爱心爆发动画。
    var likeBurstTick by remember { mutableStateOf(0) }

    val shareVm: ShareHelperViewModel = hiltViewModel()
    val snackbar = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    val deleteLauncher = rememberDeleteRequestLauncher { success ->
        // Read from VM.state.value at the moment the result returns — NOT from a stale composition-captured `state`.
        val currentId = viewModel.state.value.currentItem?.mediaId ?: return@rememberDeleteRequestLauncher
        viewModel.onDeleteConfirmed(currentId, success)
    }

    var senderToLaunch by remember { mutableStateOf<IntentSender?>(null) }

    // 点击删除按钮直接启动系统 IntentSender 弹窗；系统弹窗本身就是确认，
    // 且会把照片送入系统相册的「最近删除」。这是 Android 11+ 唯一合规路径——
    // 不能调 resolver.delete()，否则对共享媒体会 SecurityException。
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
                // 左滑 → 跳当天相册。
                // 关键：完全被动观察，不调 change.consume()。
                // VerticalPager 自己会在 Main pass 拿走垂直翻页；我们在 Main pass 一起观察累计位移。
                // 阈值：|dx| > 80dp 且 |dx| > 2*|dy| 且向左。
                // 这是 Compose 文档推荐的"父 Box 加额外水平手势 + 子 VerticalPager"组合：
                // 父层完全不消费，子层（VerticalPager）拿到的还是 unconsumed 事件，照常翻页。
                val thresholdPx = 80.dp.toPx()
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var totalDx = 0f
                    var totalDy = 0f
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        totalDx += change.positionChange().x
                        totalDy += change.positionChange().y
                    }
                    if (abs(totalDx) > thresholdPx &&
                        abs(totalDx) > 2 * abs(totalDy) &&
                        totalDx < 0
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
                item = item,
                isActive = pageIndex == pagerState.currentPage,
                onDoubleTap = {
                    viewModel.onLikeToggle()
                    likeBurstTick += 1
                },
                burstTick = likeBurstTick,
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
                BrandLogo(modifier = Modifier.size(24.dp))
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

            Column(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalAlignment = Alignment.End,
            ) {
                Row(
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
                // 「那年今日」按钮：仅当往年今天有照片时才显示，悬浮在设置按钮下方。
                if (state.onThisDayAvailable) {
                    IconButton(
                        onClick = onOpenOnThisDay,
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "那年今日",
                            tint = OverlayIconTint,
                        )
                    }
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
                state.currentItem?.let { onSwipeToDayAlbum(it.dateTakenMs) }
            }) {
                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = "当天相册",
                    tint = OverlayIconTint,
                )
            }
            IconButton(onClick = {
                state.currentItem?.let { item ->
                    val activity = context as? Activity
                    if (activity == null) {
                        snackbarScope.launch {
                            snackbar.showSnackbar("无法打开分享面板")
                        }
                    } else {
                        shareVm.shareCurrent(activity, item.uri, snackbarScope, snackbar)
                    }
                }
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
                    // 点赞高亮用红色；未点赞保持灰白便于在深色背景上看见
                    tint = if (isLiked) Color(0xFFFF3344) else OverlayIconTint,
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
class ShareHelperViewModel @Inject constructor() : ViewModel() {
    /**
     * 把当前照片通过系统分享面板发出去。用户会看到 Android 自带的 Sharesheet，
     * 点微信 / QQ / 邮件等都走对应 App 处理。
     *
     * 实现要点：
     * - 用 ContentResolver 打开 MediaStore Uri 读出 Bitmap（不能直接传 in-memory bitmap，
     *   Intent.EXTRA_STREAM 必须是可序列化的 content:// Uri）。
     * - 临时 JPEG 落到 cacheDir，再用 FileProvider 包成 content:// Uri。
     * - 必须 FLAG_GRANT_READ_URI_PERMISSION，接收方才有读权限。
     */
    fun shareCurrent(
        activity: Activity,
        imageUri: String,
        scope: kotlinx.coroutines.CoroutineScope,
        snackbar: androidx.compose.material3.SnackbarHostState,
    ) {
        viewModelScope.launch {
            val bitmap: Bitmap? = withContext(Dispatchers.IO) {
                runCatching {
                    activity.contentResolver.openInputStream(Uri.parse(imageUri))?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }.getOrNull()
            }
            if (bitmap == null) {
                snackbar.showSnackbar("图片读取失败")
                return@launch
            }

            val cacheFile = withContext(Dispatchers.IO) {
                val outFile = File(activity.cacheDir, "share_${System.currentTimeMillis()}.jpg")
                FileOutputStream(outFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                bitmap.recycle()
                outFile
            }

            val uri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                cacheFile,
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "分享到").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching {
                activity.startActivity(chooser)
            }.onFailure {
                snackbar.showSnackbar("未找到可分享的应用")
            }
        }
    }
}
