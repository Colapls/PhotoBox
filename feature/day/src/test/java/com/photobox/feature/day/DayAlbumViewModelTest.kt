package com.photobox.feature.day

import app.cash.turbine.test
import com.photobox.core.common.TestAppDispatchers
import com.photobox.core.data.mediastore.MediaItem
import com.photobox.core.data.mediastore.MediaStoreDataSource
import com.photobox.core.data.repository.DeleteRepository
import com.photobox.core.data.repository.FavoriteRepository
import com.photobox.core.data.repository.LikeRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class DayAlbumViewModelTest {
    private val mediaStore: MediaStoreDataSource = mockk(relaxed = true)
    private val likeRepo: LikeRepository = mockk(relaxed = true)
    private val favoriteRepo: FavoriteRepository = mockk(relaxed = true)
    private val deleteRepo: DeleteRepository = mockk(relaxed = true)
    private val dispatchers = TestAppDispatchers(
        io = UnconfinedTestDispatcher(),
        default = UnconfinedTestDispatcher(),
        main = UnconfinedTestDispatcher(),
    )

    @Test fun `load populates items from queryByDay`() = runTest {
        val now = 1700000000000L
        val items = listOf(
            MediaItem(1L, "content://1", false, now, mimeType = "image/jpeg"),
            MediaItem(2L, "content://2", false, now - 1000L, mimeType = "image/jpeg"),
        )
        every { mediaStore.queryByDay(now) } returns items
        every { likeRepo.observeAllIds() } returns flowOf(emptyList())
        every { favoriteRepo.observeAllIds() } returns flowOf(emptyList())

        val vm = DayAlbumViewModel(mediaStore, likeRepo, favoriteRepo, deleteRepo, dispatchers)
        vm.load(now)

        vm.state.test {
            val s = awaitItem()
            assertEquals(2, s.items.size)
            assertFalse(s.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }
}