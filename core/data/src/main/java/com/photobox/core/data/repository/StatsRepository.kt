package com.photobox.core.data.repository

import com.photobox.core.data.datastore.UserPreferencesDataSource
import com.photobox.core.data.mediastore.MediaStoreDataSource
import com.photobox.core.data.repository.ViewedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class ProfileStats(
    val likeCount: Int,
    val favoriteCount: Int,
    val totalMediaCount: Int,
    val viewedCount: Int,
)

@Singleton
class StatsRepository @Inject constructor(
    private val likeRepo: LikeRepository,
    private val favoriteRepo: FavoriteRepository,
    private val viewedRepo: ViewedRepository,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val prefs: UserPreferencesDataSource,
) {
    /**
     * 组合 4 个数据源：like 数 / favorite 数 / MediaStore 总数 / 已浏览数。
     * MediaStore 总数每次都重新查询（call site 必须在 IO 调度器）。
     * 「已浏览」以 ViewedRepository 的 count 为准（不依赖 DataStore 的计数键）。
     */
    fun observeStats(ioDispatcher: kotlinx.coroutines.CoroutineDispatcher): Flow<ProfileStats> =
        combine(
            likeRepo.observeAllIds(),
            favoriteRepo.observeAllIds(),
            viewedRepo.observeCount(),
        ) { likes, favorites, viewed ->
            Triple(likes.size, favorites.size, viewed)
        }.map { (likes, favorites, viewed) ->
            ProfileStats(
                likeCount = likes,
                favoriteCount = favorites,
                totalMediaCount = mediaStoreDataSource.totalCount(),
                viewedCount = viewed,
            )
        }.flowOn(ioDispatcher)
}