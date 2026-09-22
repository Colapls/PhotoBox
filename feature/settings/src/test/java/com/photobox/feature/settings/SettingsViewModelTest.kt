package com.photobox.feature.settings

import app.cash.turbine.test
import com.photobox.core.common.TestAppDispatchers
import com.photobox.core.data.datastore.FilterMode
import com.photobox.core.data.datastore.TimeRange
import com.photobox.core.data.datastore.UserPreferences
import com.photobox.core.data.datastore.UserPreferencesDataSource
import com.photobox.core.data.mediastore.MediaStoreDataSource
import com.photobox.core.data.repository.RandomStreamRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val prefs: UserPreferencesDataSource = mockk(relaxed = true)
    private val randomRepo: RandomStreamRepository = mockk(relaxed = true)
    private val mediaStore: MediaStoreDataSource = mockk(relaxed = true)
    private val dispatchers = TestAppDispatchers(
        io = UnconfinedTestDispatcher(),
        default = UnconfinedTestDispatcher(),
        main = UnconfinedTestDispatcher(),
    )

    @Test fun `setFilterMode delegates to prefs`() = runTest {
        every { prefs.userPreferences } returns MutableStateFlow(UserPreferences())
        every { mediaStore.queryAll() } returns emptyList()
        val vm = SettingsViewModel(mockk(relaxed = true), prefs, randomRepo, mediaStore, dispatchers)

        vm.setFilterMode(FilterMode.PHOTOS_ONLY)
        coVerify { prefs.setFilterMode(FilterMode.PHOTOS_ONLY) }
    }

    @Test fun `setTimeRange delegates to prefs`() = runTest {
        every { prefs.userPreferences } returns MutableStateFlow(UserPreferences())
        every { mediaStore.queryAll() } returns emptyList()
        val vm = SettingsViewModel(mockk(relaxed = true), prefs, randomRepo, mediaStore, dispatchers)

        vm.setTimeRange(TimeRange.THREE_YEARS_AGO)
        coVerify { prefs.setTimeRange(TimeRange.THREE_YEARS_AGO) }
    }

    @Test fun `reshuffle delegates to randomRepo and emits toast`() = runTest {
        every { prefs.userPreferences } returns MutableStateFlow(UserPreferences())
        every { mediaStore.queryAll() } returns emptyList()
        coEvery { randomRepo.reshuffle(any()) } returns Unit
        val vm = SettingsViewModel(mockk(relaxed = true), prefs, randomRepo, mediaStore, dispatchers)

        vm.state.test {
            skipItems(1)  // 初始 isLoading=true
            vm.reshuffle()
            coVerify { randomRepo.reshuffle(any()) }
            val s = awaitItem()
            assertEquals("已重新洗牌", s.toast)
            cancelAndIgnoreRemainingEvents()
        }
    }
}