package com.photobox.core.data.repository

import com.photobox.core.data.db.dao.FavoriteDao
import com.photobox.core.data.db.entity.FavoriteEntity
import com.photobox.core.data.mediastore.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
) {
    fun observeIsFavorite(mediaId: Long): Flow<Boolean> = favoriteDao.observeIsFavorite(mediaId)

    fun observeAllIds(): Flow<List<Long>> = favoriteDao.observeAllIds()

    suspend fun toggle(item: MediaItem): Boolean {
        val currentlyFav = favoriteDao.observeAllIds()
            .map { item.mediaId in it }
            .first()
        return if (currentlyFav) {
            favoriteDao.delete(item.mediaId)
            false
        } else {
            favoriteDao.upsert(
                FavoriteEntity(
                    mediaId = item.mediaId,
                    addedAt = System.currentTimeMillis(),
                    mediaUri = item.uri,
                    dateTaken = item.dateTakenMs,
                )
            )
            true
        }
    }

    suspend fun cleanup(mediaId: Long) {
        favoriteDao.delete(mediaId)
    }
}
