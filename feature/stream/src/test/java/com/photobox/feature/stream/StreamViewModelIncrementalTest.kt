package com.photobox.feature.stream

import com.photobox.core.common.TestAppDispatchers
import com.photobox.core.data.mediastore.MediaStoreDataSource
import com.photobox.core.data.repository.DeleteRepository
import com.photobox.core.data.repository.FavoriteRepository
import com.photobox.core.data.repository.LikeRepository
import com.photobox.core.data.repository.RandomStreamRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class StreamViewModelIncrementalTest {
    @Test fun `init subscribes to observeNewMedia`() = runTest {
        val mediaStore: MediaStoreDataSource = mockk(relaxed = true)
        every { mediaStore.observeNewMedia(any()) } returns flowOf(Unit)
        val vm = StreamViewModel(
            randomStreamRepo = mockk<RandomStreamRepository>(relaxed = true),
            likeRepo = mockk<LikeRepository>(relaxed = true),
            favoriteRepo = mockk<FavoriteRepository>(relaxed = true),
            deleteRepo = mockk<DeleteRepository>(relaxed = true),
            mediaStoreDataSource = mediaStore,
            dispatchers = TestAppDispatchers(
                io = UnconfinedTestDispatcher(),
                default = UnconfinedTestDispatcher(),
                main = UnconfinedTestDispatcher(),
            ),
        )
        assertNotNull(vm.state)
    }
}
