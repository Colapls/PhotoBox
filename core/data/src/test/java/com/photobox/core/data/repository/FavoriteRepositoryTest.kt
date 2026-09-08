package com.photobox.core.data.repository

import com.photobox.core.data.db.dao.FavoriteDao
import com.photobox.core.data.mediastore.MediaItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavoriteRepositoryTest {
    private val favoriteDao: FavoriteDao = mockk(relaxed = true)
    private val repo = FavoriteRepository(favoriteDao)

    private val item = MediaItem(
        mediaId = 1L, uri = "content://x", isVideo = false,
        dateTakenMs = 100L, mimeType = "image/jpeg",
    )

    @Test fun `toggle adds favorite when absent`() = runTest {
        every { favoriteDao.observeAllIds() } returns flowOf(emptyList())
        every { favoriteDao.observeIsFavorite(1L) } returns flowOf(false)

        val result = repo.toggle(item)

        assertTrue(result)
        coVerify {
            favoriteDao.upsert(match { it.mediaId == 1L && it.mediaUri == "content://x" })
        }
    }

    @Test fun `toggle removes favorite when present`() = runTest {
        every { favoriteDao.observeAllIds() } returns flowOf(listOf(1L))
        every { favoriteDao.observeIsFavorite(1L) } returns flowOf(true)

        val result = repo.toggle(item)

        assertFalse(result)
        coVerify { favoriteDao.delete(1L) }
    }

    @Test fun `cleanup deletes by id`() = runTest {
        repo.cleanup(1L)
        coVerify { favoriteDao.delete(1L) }
    }
}
