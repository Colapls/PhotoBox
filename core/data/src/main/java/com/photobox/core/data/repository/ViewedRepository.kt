package com.photobox.core.data.repository

import com.photobox.core.data.db.dao.ViewedDao
import com.photobox.core.data.db.entity.ViewedEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViewedRepository @Inject constructor(
    private val viewedDao: ViewedDao,
) {
    fun observeAllIds(): Flow<List<Long>> = viewedDao.observeAllIds()

    fun observeCount(): Flow<Int> = viewedDao.observeCount()

    /** 记录已浏览（去重）：已存在则忽略。 */
    suspend fun markViewed(mediaId: Long) {
        viewedDao.upsert(ViewedEntity(mediaId = mediaId, viewedAt = System.currentTimeMillis()))
    }
}
