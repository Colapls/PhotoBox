package com.photobox.core.data.repository

import com.photobox.core.data.db.dao.PendingShuffleDao
import com.photobox.core.data.db.dao.ShuffleDao
import com.photobox.core.data.db.entity.ShuffleStateEntity
import com.photobox.core.data.mediastore.MediaStoreDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RandomStreamRepositoryTest {
    private val mediaStore: MediaStoreDataSource = mockk()
    private val shuffleDao: ShuffleDao = mockk(relaxed = true)
    private val pendingShuffleDao: PendingShuffleDao = mockk(relaxed = true)
    private val repo = RandomStreamRepository(mediaStore, shuffleDao, pendingShuffleDao)

    @Test fun `reshuffle persists merged Fisher-Yates sequence`() = runTest {
        every { mediaStore.queryAll() } returns mockkList(1L..100L)
        coEvery { pendingShuffleDao.readAllIds() } returns listOf(50L, 200L)
        coEvery { shuffleDao.readState() } returns null

        repo.reshuffle()

        coVerify {
            shuffleDao.upsert(match { state ->
                state.id == 1 &&
                    state.currentIndex == 0 &&
                    state.queueMediaIds.toSet() == (1L..100L).toSet() + setOf(50L, 200L) &&
                    state.queueMediaIds.size == 101
            })
        }
        coVerify { pendingShuffleDao.clear() }
    }

    @Test fun `reshuffle dedupes overlapping ids`() = runTest {
        every { mediaStore.queryAll() } returns mockkList(1L, 2L, 3L)
        coEvery { pendingShuffleDao.readAllIds() } returns listOf(3L, 4L)
        coEvery { shuffleDao.readState() } returns null

        repo.reshuffle()

        coVerify {
            shuffleDao.upsert(match { it.queueMediaIds.toSet() == setOf(1L, 2L, 3L, 4L) })
        }
    }

    @Test fun `popCurrent returns queue head and advances index`() = runTest {
        coEvery { shuffleDao.readState() } returns ShuffleStateEntity(
            id = 1,
            queueMediaIds = listOf(10L, 20L, 30L),
            currentIndex = 1,
            roundStartedAt = 0L,
            lastShuffledMediaIds = emptyList(),
        )

        val popped = repo.popCurrent()
        assertEquals(20L, popped)
        coVerify { shuffleDao.upsert(match { it.currentIndex == 2 }) }
    }

    @Test fun `popCurrent returns null when index exhausted`() = runTest {
        coEvery { shuffleDao.readState() } returns ShuffleStateEntity(
            id = 1,
            queueMediaIds = listOf(10L),
            currentIndex = 1,
            roundStartedAt = 0L,
            lastShuffledMediaIds = emptyList(),
        )
        assertEquals(null, repo.popCurrent())
    }

    @Test fun `isRoundFinished true when index equals size`() = runTest {
        coEvery { shuffleDao.readState() } returns ShuffleStateEntity(
            id = 1, queueMediaIds = listOf(1L, 2L), currentIndex = 2,
            roundStartedAt = 0L, lastShuffledMediaIds = emptyList(),
        )
        assertTrue(repo.isRoundFinished())
    }

    @Test fun `markViewed appends to lastShuffledMediaIds without dupes`() = runTest {
        coEvery { shuffleDao.readState() } returns ShuffleStateEntity(
            id = 1, queueMediaIds = listOf(1L, 2L, 3L), currentIndex = 0,
            roundStartedAt = 0L, lastShuffledMediaIds = listOf(1L, 2L),
        )
        repo.markViewed(2L)
        coVerify {
            shuffleDao.upsert(match { it.lastShuffledMediaIds == listOf(1L, 2L) })
        }
    }

    // helper
    private fun mockkList(vararg ids: Long): List<com.photobox.core.data.mediastore.MediaItem> =
        ids.map { id ->
            mockk<com.photobox.core.data.mediastore.MediaItem> { every { mediaId } returns id }
        }
}
