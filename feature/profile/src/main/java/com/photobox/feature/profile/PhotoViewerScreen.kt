package com.photobox.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.photobox.core.common.AppDispatchers
import com.photobox.core.data.mediastore.MediaItem
import com.photobox.core.data.mediastore.MediaStoreDataSource
import com.photobox.core.data.repository.FavoriteRepository
import com.photobox.core.data.repository.LikeRepository
import com.photobox.core.data.repository.ViewedRepository
import com.photobox.core.media.VideoPlayer
import com.photobox.core.media.ZoomablePhotoViewer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** 列表来源枚举（决定 viewer 从哪个 repository 拉 ids 列表）。 */
enum class PhotoListSource { LIKES, FAVORITES, VIEWED }

data class PhotoViewerUiState(
    val items: List<MediaItem> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * 全屏照片查看器：从指定源（likes / favorites / viewed）拉全部 ids，
 * 按 queryAll().mapNotNull 顺序排列，跳过已被用户删除的。
 *
 * 支持上下滑切照片、双指缩放（5x）、缩回 1x 自动归位、单指拖动。
 */
@HiltViewModel
class PhotoViewerViewModel @Inject constructor(
    private val likeRepo: LikeRepository,
    private val favoriteRepo: FavoriteRepository,
    private val viewedRepo: ViewedRepository,
    private val mediaStore: MediaStoreDataSource,
    private val dispatchers: AppDispatchers,
) : ViewModel() {
    private val _state = MutableStateFlow(PhotoViewerUiState())
    val state: StateFlow<PhotoViewerUiState> = _state.asStateFlow()

    fun load(source: PhotoListSource, initialMediaId: Long) {
        viewModelScope.launch {
            val ids: List<Long> = when (source) {
                PhotoListSource.LIKES -> likeRepo.observeAllIds().first()
                PhotoListSource.FAVORITES -> favoriteRepo.observeAllIds().first()
                PhotoListSource.VIEWED -> viewedRepo.observeAllIds().first()
            }
            val all = withContext(dispatchers.io) { mediaStore.queryAll() }
            val byId = all.associateBy { it.mediaId }
            // 保留来源列表的原始顺序（likes/favorites/viewed 通常按时间倒序，更直观）。
            _state.value = PhotoViewerUiState(
                items = ids.mapNotNull { byId[it] },
                isLoading = false,
            )
        }
    }
}

@Composable
fun PhotoViewerScreen(
    source: PhotoListSource,
    initialMediaId: Long,
    onBack: () -> Unit,
    viewModel: PhotoViewerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(source, initialMediaId) {
        viewModel.load(source, initialMediaId)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (state.items.isNotEmpty()) {
            val initialIndex = state.items.indexOfFirst { it.mediaId == initialMediaId }.coerceAtLeast(0)
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

        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = Color.White,
            )
        }
    }
}
