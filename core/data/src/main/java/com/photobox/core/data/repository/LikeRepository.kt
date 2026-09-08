package com.photobox.core.data.repository

import com.photobox.core.data.db.dao.LikeDao
import com.photobox.core.data.db.entity.LikeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LikeRepository @Inject constructor(
    private val likeDao: LikeDao,
) {
    fun observeIsLiked(mediaId: Long): Flow<Boolean> = likeDao.observeIsLiked(mediaId)

    suspend fun toggle(mediaId: Long): Boolean {
        val currentlyLiked = likeDao.observeAllIds()
            .map { mediaId in it }
            .first()
        return if (currentlyLiked) {
            likeDao.delete(mediaId)
            false
        } else {
            likeDao.upsert(LikeEntity(mediaId = mediaId, likedAt = System.currentTimeMillis()))
            true
        }
    }

    suspend fun cleanup(mediaId: Long) {
        likeDao.delete(mediaId)
    }
}
