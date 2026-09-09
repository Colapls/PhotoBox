package com.photobox.feature.profile

import app.cash.turbine.test
import com.photobox.core.common.AppDispatchers
import com.photobox.core.data.repository.ProfileStats
import com.photobox.core.data.repository.StatsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val statsRepo: StatsRepository = mockk(relaxed = true)
    private val dispatchers = AppDispatchers(
        io = UnconfinedTestDispatcher(),
        default = UnconfinedTestDispatcher(),
        main = UnconfinedTestDispatcher(),
    )

    @Before fun setup() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `state emits loaded stats`() = runTest {
        val stats = ProfileStats(likeCount = 5, favoriteCount = 3, totalMediaCount = 100, viewedCount = 20)
        every { statsRepo.observeStats(any()) } returns flowOf(stats)

        val vm = ProfileViewModel(statsRepo, dispatchers)

        vm.state.test {
            // 跳过 initial isLoading=true
            val loaded = awaitItem()
            assertEquals(stats, loaded.stats)
            assertFalse(loaded.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }
}