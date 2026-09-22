package com.photobox.feature.stream

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photobox.core.common.AppDispatchers
import com.photobox.core.data.datastore.MediaFilter
import com.photobox.core.data.datastore.UserPreferencesDataSource
import com.photobox.core.data.mediastore.MediaItem
import com.photobox.core.data.mediastore.MediaStoreDataSource
import com.photobox.core.data.repository.DeleteRepository
import com.photobox.core.data.repository.FavoriteRepository
import com.photobox.core.data.repository.LikeRepository
import com.photobox.core.data.repository.RandomStreamRepository
import com.photobox.core.data.repository.ViewedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class StreamViewModel @Inject constructor(
    private val randomStreamRepo: RandomStreamRepository,
    private val likeRepo: LikeRepository,
    private val favoriteRepo: FavoriteRepository,
    private val deleteRepo: DeleteRepository,
    private val viewedRepo: ViewedRepository,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val userPrefs: UserPreferencesDataSource,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _state = MutableStateFlow(StreamUiState(isLoading = true))
    val state: StateFlow<StreamUiState> = _state

    // 每次 ACCESS_MEDIA_LOCATION 获批时 +1，使 observeChanges() 重跑一次 queryAll()，
    // 把新查到的经纬度带回 MediaItem → 触发 UI 重新渲染位置行。
    private val refreshTick = MutableStateFlow(0)

    private var lastViewedIndex: Int = -1

    init {
        viewModelScope.launch { reshuffleIfEmpty() }
        observeChanges()
        checkOnThisDay()
        // 订阅用户偏好（筛选 / 时间范围）。变化时强制 reshuffle 重新套用 filter。
        userPrefs.userPreferences
            .onEach { prefs ->
                // queue 仍然存在（DB 里持久化）但可能已经不匹配新 filter，
                // 比如「只看照片」后用户清空时间范围 → 老 queue 里可能还有视频。
                // 直接 reshuffle 保证队列始终符合当前 filter。
                val currentFilter = MediaFilter.from(prefs)
                withContext(dispatchers.io) { randomStreamRepo.reshuffle(currentFilter) }
                // 关键：设置变更后立即把 currentIndex / lastViewedIndex 清零，
                // 否则新 queue 的 size 可能比 currentIndex 小 → pager 启动崩溃；
                // 或者用户停留在过期的位置上看到旧的图片。
                lastViewedIndex = -1
                _state.value = _state.value.copy(currentIndex = 0, roundFinished = false)
            }
            .launchIn(viewModelScope)
        // 订阅 MediaStore 新增媒体。observeNewMedia 内部通过 ContentObserver 触发，
        // 并把新发现的 ID 写入 pending_shuffle_dao 作为副作用（订阅本身即激活）。
        // 现有 observeChanges() 流程消费的是 randomStreamRepo.queue（已 shuffle 的批次），
        // 新 ID 需用户手动触发 reshuffle 才进入流式浏览（spec §5.3 允许）。
        mediaStoreDataSource.observeNewMedia(knownMediaIds = currentShuffleIds())
            .onEach { /* subscription itself activates the ContentObserver pipeline */ }
            .launchIn(viewModelScope)
    }

    private suspend fun reshuffleIfEmpty() {
        val queue = randomStreamRepo.queue.first()
        if (queue.isEmpty()) {
            val prefs = userPrefs.userPreferences.first()
            withContext(dispatchers.io) { randomStreamRepo.reshuffle(MediaFilter.from(prefs)) }
        }
    }

    /**
     * 检查往年今天是否有照片，结果写到 state.onThisDayAvailable。
     * 用 [MediaStoreDataSource.countOnThisDay] 的轻量 SQL，只数 _ID 不拉元数据。
     * 全部用 runCatching 包住：这是 UI 优化，不是主流程，任何 IO 异常都不应让 app 崩溃。
     */
    private fun checkOnThisDay() {
        viewModelScope.launch {
            runCatching {
                val count = withContext(dispatchers.io) {
                    mediaStoreDataSource.countOnThisDay(System.currentTimeMillis())
                }
                _state.value = _state.value.copy(onThisDayAvailable = count > 0)
            }
        }
        // 监听 MediaStore 变化时再复查一次（refreshTick 已经在 combine 里）
        refreshTick.onEach {
            runCatching {
                val count = withContext(dispatchers.io) {
                    mediaStoreDataSource.countOnThisDay(System.currentTimeMillis())
                }
                _state.value = _state.value.copy(onThisDayAvailable = count > 0)
            }
        }.launchIn(viewModelScope)
    }

    private fun currentShuffleIds(): Set<Long> = state.value.items.map { it.mediaId }.toSet()

    private fun observeChanges() {
        viewModelScope.launch {
            combine(
                randomStreamRepo.queue,
                likeRepo.observeAllIds(),
                favoriteRepo.observeAllIds(),
                refreshTick,
            ) { ids, liked, favorite, _ ->
                Triple(ids, liked.toSet(), favorite.toSet())
            }.flowOn(dispatchers.io).collect { (ids, liked, favorite) ->
                // queue 自身已经过 filter；这里只把 id → MediaItem 元信息（uri / dateTakenMs / isVideo 等）
                // 拉出来给 UI 用。注意：queryAll() 返回全量，mapNotNull 后只会保留在 queue 里的项，
                // 等价于二次过滤，保留以防 queue 与 MediaStore 之间出现不一致（被删的 / 新增的）。
                val allMedia = mediaStoreDataSource.queryAll()
                val byRealId = allMedia.associateBy { it.mediaId }
                val mediaList = ids.mapNotNull { byRealId[it] }
                val wasEmpty = _state.value.isEmpty
                val nowEmpty = mediaList.isEmpty()
                _state.value = _state.value.copy(
                    items = mediaList,
                    likedIds = liked,
                    favoriteIds = favorite,
                    isLoading = false,
                    isEmpty = nowEmpty,
                    // 仅在「之前有，现在没有」的边界弹出提示，避免 reshuffle/刷新时反复弹
                    filterEmptyPromptVisible = !wasEmpty && nowEmpty,
                    roundFinished = _state.value.roundFinished && mediaList.isNotEmpty(),
                )
            }
        }
    }

    fun onPageChanged(index: Int) {
        if (index == lastViewedIndex) return
        lastViewedIndex = index
        _state.value = _state.value.copy(
            currentIndex = index,
            viewedCount = _state.value.viewedCount + 1,
        )
        val item = _state.value.items.getOrNull(index) ?: return
        viewModelScope.launch {
            // 同时写两份：RandomStreamRepository 维护本轮已看（决定 roundFinished），
            // ViewedRepository 维护全期已看 ID（供个人中心「已浏览」列表展示）。
            withContext(dispatchers.io) {
                randomStreamRepo.markViewed(item.mediaId)
                viewedRepo.markViewed(item.mediaId)
            }
            val finished = withContext(dispatchers.io) { randomStreamRepo.isRoundFinished() }
            _state.value = _state.value.copy(roundFinished = finished)
        }
    }

    fun onLikeToggle() {
        val current = _state.value.currentItem ?: return
        viewModelScope.launch { withContext(dispatchers.io) { likeRepo.toggle(current.mediaId) } }
    }

    fun onFavoriteToggle() {
        val current = _state.value.currentItem ?: return
        viewModelScope.launch { withContext(dispatchers.io) { favoriteRepo.toggle(current) } }
    }

    fun onReshuffle() {
        lastViewedIndex = -1
        viewModelScope.launch {
            val prefs = userPrefs.userPreferences.first()
            withContext(dispatchers.io) { randomStreamRepo.reshuffle(MediaFilter.from(prefs)) }
            _state.value = _state.value.copy(
                currentIndex = 0,
                roundFinished = false,
                viewedCount = 0,
            )
        }
    }

    /**
     * 当前筛选条件下没有匹配项时，用户从空状态 AlertDialog 点确认。
     * 清掉时间范围和模式（保留 albumFilter 不动——用户主动选的相册不该自动改），
     * 然后 reshuffle。prefs.onEach 订阅会顺带把 currentIndex 重置并触发再次筛选。
     */
    fun onFilterEmptyFallback() {
        _state.value = _state.value.copy(filterEmptyPromptVisible = false)
        viewModelScope.launch {
            userPrefs.setFilterMode(com.photobox.core.data.datastore.FilterMode.MIXED)
            userPrefs.setTimeRange(com.photobox.core.data.datastore.TimeRange.ALL)
            userPrefs.setCustomRange(null, null)
            // 上面 setFilterMode/setTimeRange 已经触发 prefs.onEach → reshuffle，
            // 不需要再单独 onReshuffle。
        }
    }

    /** 用户主动关闭 AlertDialog（不走 fallback） */
    fun dismissFilterEmptyPrompt() {
        _state.value = _state.value.copy(filterEmptyPromptVisible = false)
    }

    fun createDeleteIntent(item: MediaItem) = deleteRepo.createDeleteRequest(item)

    fun onDeleteConfirmed(mediaId: Long, success: Boolean) {
        if (!success) return
        viewModelScope.launch {
            withContext(dispatchers.io) { deleteRepo.cleanupAfterDelete(mediaId) }
        }
    }

    /**
     * 无确认直接删除：API 30+ 走「最近删除」（30 天后系统自动清），API < 30 物理删除。
     * 直接调用 resolver.delete()，不需要 IntentSender 弹窗。
     */
    fun deleteNow(item: MediaItem) {
        viewModelScope.launch {
            val deleted = withContext(dispatchers.io) { deleteRepo.deleteNow(item) }
            _state.value = _state.value.copy(
                lastDeleteFailed = deleted <= 0,
            )
        }
    }

    fun consumeDeleteError() {
        _state.value = _state.value.copy(lastDeleteFailed = false)
    }

    /**
     * 用户授予了 ACCESS_MEDIA_LOCATION。触发一次 MediaStore 重新查询，让 MediaItem
     * 重新带回经纬度字段，UI 自动重新渲染位置行。
     */
    fun onLocationPermissionGranted() {
        refreshTick.value = refreshTick.value + 1
    }
}
