package com.photobox.core.data.repository

import app.cash.turbine.test
import com.photobox.core.data.datastore.UserPreferences
import com.photobox.core.data.datastore.UserPreferencesDataSource
import com.photobox.core.data.mediastore.MediaStoreDataSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class StatsRepositoryTest {
    private val likeRepo: LikeRepository = mockk(relaxed = true)
    private val favoriteRepo: FavoriteRepository = mockk(relaxed = true)
    private val mediaStore: MediaStoreDataSource = mockk(relaxed = true)
    private val prefs: UserPreferencesDataSource = mockk(relaxed = true)

    private val repo = StatsRepository(likeRepo, favoriteRepo, mediaStore, prefs)

    @Test fun `observeStats combines 4 sources`() = runTest {
        every { likeRepo.observeAllIds() } returns flowOf(listOf(1L, 2L, 3L))
        every { favoriteRepo.observeAllIds() } returns flowOf(listOf(10L, 20L))
        every { prefs.userPreferences } returns flowOf(UserPreferences(viewedCount = 42))
        every { mediaStore.totalCount() } returns 1000

        repo.observeStats(UnconfinedTestDispatcher()).test {
            val stats = awaitItem()
            assertEquals(3, stats.likeCount)
            assertEquals(2, stats.favoriteCount)
            assertEquals(1000, stats.totalMediaCount)
            assertEquals(42, stats.viewedCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `observeStats re-emits when like count changes`() = runTest {
        every { likeRepo.observeAllIds() } returns flowOf(listOf(1L), listOf(1L, 2L))
        every { favoriteRepo.observeAllIds() } returns flowOf(emptyList())
        every { prefs.userPreferences } returns flowOf(UserPreferences())
        every { mediaStore.totalCount() } returns 500

        repo.observeStats(UnconfinedTestDispatcher()).test {
            assertEquals(1, awaitItem().likeCount)
            assertEquals(2, awaitItem().likeCount)
            cancelAndIgnoreRemainingEvents()
        }
    }
}