package com.photobox.feature.stream

import app.cash.turbine.test
import com.photobox.core.common.AppDispatchers
import com.photobox.core.data.mediastore.MediaItem
import com.photobox.core.data.mediastore.MediaStoreDataSource
import com.photobox.core.data.repository.DeleteRepository
import com.photobox.core.data.repository.FavoriteRepository
import com.photobox.core.data.repository.LikeRepository
import com.photobox.core.data.repository.RandomStreamRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class StreamViewModelTest {
    private val dispatchers = AppDispatchers(
        io = UnconfinedTestDispatcher(),
        default = UnconfinedTestDispatcher(),
        main = UnconfinedTestDispatcher(),
    )
    private val randomStreamRepo: RandomStreamRepository = mockk(relaxed = true)
    private val likeRepo: LikeRepository = mockk(relaxed = true)
    private val favoriteRepo: FavoriteRepository = mockk(relaxed = true)
    private val deleteRepo: DeleteRepository = mockk(relaxed = true)
    private val mediaStore: MediaStoreDataSource = mockk()

    private val item = MediaItem(1L, "content://x", false, 100L, mimeType = "image/jpeg")

    @Before fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { mediaStore.queryAll() } returns listOf(item)
        coEvery { randomStreamRepo.queue } returns flowOf(listOf(1L))
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `init reshuffles when queue empty`() = runTest {
        val queueFlow = MutableStateFlow<List<Long>>(emptyList())
        every { randomStreamRepo.queue } returns queueFlow
        every { likeRepo.observeAllIds() } returns flowOf(emptyList())
        every { favoriteRepo.observeAllIds() } returns flowOf(emptyList())

        StreamViewModel(randomStreamRepo, likeRepo, favoriteRepo, deleteRepo, mediaStore, dispatchers)

        coVerify { randomStreamRepo.reshuffle() }
    }

    @Test fun `onLikeToggle delegates to LikeRepository`() = runTest {
        val queueFlow = MutableStateFlow(listOf(1L))
        every { randomStreamRepo.queue } returns queueFlow
        every { likeRepo.observeAllIds() } returns flowOf(emptyList())
        every { favoriteRepo.observeAllIds() } returns flowOf(emptyList())
        val vm = StreamViewModel(randomStreamRepo, likeRepo, favoriteRepo, deleteRepo, mediaStore, dispatchers)

        vm.state.test {
            // 跳过初始 isEmpty=true
            skipItems(1)
            // 等待 items 被填充
            var s = awaitItem()
            while (s.items.isEmpty()) s = awaitItem()
            assertEquals(item, s.currentItem)

            vm.onLikeToggle()
            coVerify { likeRepo.toggle(1L) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `onReshuffle resets currentIndex`() = runTest {
        every { randomStreamRepo.queue } returns flowOf(listOf(1L))
        every { likeRepo.observeAllIds() } returns flowOf(emptyList())
        every { favoriteRepo.observeAllIds() } returns flowOf(emptyList())
        val vm = StreamViewModel(randomStreamRepo, likeRepo, favoriteRepo, deleteRepo, mediaStore, dispatchers)

        vm.onReshuffle()

        coVerify { randomStreamRepo.reshuffle() }
        assertEquals(0, vm.state.value.currentIndex)
    }
}
