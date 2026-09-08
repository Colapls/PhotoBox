package com.photobox.core.data.repository

import app.cash.turbine.test
import com.photobox.core.data.db.dao.LikeDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LikeRepositoryTest {
    private val likeDao: LikeDao = mockk(relaxed = true)
    private val repo = LikeRepository(likeDao)

    @Test fun `toggle adds like when not currently liked`() = runTest {
        every { likeDao.observeAllIds() } returns flowOf(listOf(99L))
        every { likeDao.observeIsLiked(42L) } returns flowOf(false)

        val result = repo.toggle(42L)

        assertTrue(result)
        coVerify { likeDao.upsert(match { it.mediaId == 42L }) }
    }

    @Test fun `toggle removes like when currently liked`() = runTest {
        every { likeDao.observeAllIds() } returns flowOf(listOf(42L))
        every { likeDao.observeIsLiked(42L) } returns flowOf(true)

        val result = repo.toggle(42L)

        assertFalse(result)
        coVerify { likeDao.delete(42L) }
    }

    @Test fun `cleanup deletes by id`() = runTest {
        repo.cleanup(42L)
        coVerify { likeDao.delete(42L) }
    }
}
