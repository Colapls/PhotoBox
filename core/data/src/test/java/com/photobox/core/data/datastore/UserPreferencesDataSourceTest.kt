package com.photobox.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.mutablePreferencesOf
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserPreferencesDataSourceTest {

    @Test
    fun `setOnboardingDone persists true`() = runTest {
        val ds = UserPreferencesDataSource(fakeDataStore())
        ds.setOnboardingDone(true)
        ds.userPreferences.test {
            val pref = awaitItem()
            assertTrue(pref.onboardingDone)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `default values when empty`() = runTest {
        val ds = UserPreferencesDataSource(fakeDataStore(initial = mutablePreferencesOf()))
        ds.userPreferences.test {
            val pref = awaitItem()
            assertEquals(false, pref.onboardingDone)
            assertEquals(false, pref.privacyAcknowledged)
            assertEquals(false, pref.appLockEnabled)
            assertEquals(FilterMode.MIXED, pref.filterMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun fakeDataStore(
        initial: Preferences = mutablePreferencesOf()
    ): DataStore<Preferences> {
        val flow = MutableStateFlow(initial)
        val ds = mockk<DataStore<Preferences>>(relaxed = true)
        every { ds.data } returns flow
        coEvery { ds.edit(any()) } coAnswers {
            val transform = firstArg<suspend (MutablePreferences) -> Unit>()
            val current = flow.value
            val mutable = current.toMutablePreferences()
            transform(mutable)
            flow.value = mutable.toPreferences()
            mutable.toPreferences()
        }
        return ds
    }
}
