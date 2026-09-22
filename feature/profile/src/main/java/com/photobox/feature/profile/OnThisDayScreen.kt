package com.photobox.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.photobox.core.common.AppDispatchers
import com.photobox.core.data.mediastore.MediaItem
import com.photobox.core.data.mediastore.MediaStoreDataSource
import com.photobox.core.media.VideoPlayer
import com.photobox.core.media.ZoomablePhotoViewer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 「那年今日」：列出过去若干年中、与今天同月日的所有照片。
 * 每张照片角标显示它拍摄的年份 + 月日，方便回忆"那年这天发生了什么"。
 * 点击照片 → 进入 [OnThisDayPhotoViewer] 浏览完整列表。
 */
@HiltViewModel
class OnThisDayViewModel @Inject constructor(
    private val mediaStore: MediaStoreDataSource,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _state = MutableStateFlow(OnThisDayUiState())
    val state: StateFlow<OnThisDayUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val items = withContext(dispatchers.io) { mediaStore.queryOnThisDay(now) }
            _state.value = OnThisDayUiState(items = items, isLoading = false)
        }
    }
}

data class OnThisDayUiState(
    val items: List<MediaItem> = emptyList(),
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnThisDayScreen(
    onBack: () -> Unit,
    onPhotoClick: (mediaId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnThisDayViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    val today = remember { Calendar.getInstance() }
    val titleFmt = remember { SimpleDateFormat("MM 月 dd 日", Locale.CHINA) }
    val todayLabel = titleFmt.format(today.time)

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.on_this_day_title, todayLabel)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.on_this_day_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
            state.items.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.on_this_day_empty, todayLabel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) {
                    items(items = state.items, key = { it.mediaId }) { item ->
                        OnThisDayThumbnail(
                            item = item,
                            onClick = { onPhotoClick(item.mediaId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnThisDayThumbnail(
    item: MediaItem,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
    ) {
        coil3.compose.AsyncImage(
            model = item.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // 左上角：年份标签
        val yearLabel = remember(item.dateTakenMs) {
            val cal = Calendar.getInstance().apply { timeInMillis = item.dateTakenMs }
            "${cal.get(Calendar.YEAR)} 年"
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(4.dp),
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                text = yearLabel,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            )
        }
        if (item.isVideo) {
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

/**
 * 「那年今日」的全屏查看器：复用 [OnThisDayViewModel] 的 state，所以照片列表和 grid 严格一致。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnThisDayPhotoViewer(
    initialMediaId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnThisDayViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // 从 grid 跳进来时这里会拿到一个全新的 VM（hiltViewModel 默认绑当前 nav entry），
    // 所以 state.items 还是空的。立即 load 一次，等数据回来再渲染 pager。
    LaunchedEffect(Unit) { viewModel.load() }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (state.items.isNotEmpty()) {
            // 用 items.size 作为 key：列表变化（重新 query）时重新建 pager state，
            // 避免 pageCount 与 items.size 不一致导致 IllegalArgumentException。
            key(state.items.size, initialMediaId) {
                val initialIndex = state.items
                    .indexOfFirst { it.mediaId == initialMediaId }
                    .coerceAtLeast(0)
                val pagerState = rememberPagerState(initialPage = initialIndex) { state.items.size }
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { state.items[it].mediaId },
                ) { pageIndex ->
                    val item = state.items[pageIndex]
                    if (item.isVideo) {
                        VideoPlayer(
                            uri = item.uri,
                            isActive = pageIndex == pagerState.currentPage,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        ZoomablePhotoViewer(
                            uri = item.uri,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        } else if (!state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.on_this_day_empty, ""),
                    color = Color.White,
                )
            }
        }
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.on_this_day_back),
                tint = Color.White,
            )
        }
    }
}