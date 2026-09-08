package com.photobox.core.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.photobox.core.data.db.AppDatabase
import com.photobox.core.data.db.entity.ShuffleStateEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class ShuffleDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: ShuffleDao

    @Before fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.shuffleDao()
    }

    @After fun tearDown() { db.close() }

    @Test fun readState_returns_null_when_empty() = runTest {
        assertNull(dao.readState())
    }

    @Test fun upsert_then_readState_round_trips() = runTest {
        val s = ShuffleStateEntity(
            id = 1,
            queueMediaIds = listOf(10L, 20L, 30L),
            currentIndex = 1,
            roundStartedAt = 100L,
            lastShuffledMediaIds = listOf(10L),
        )
        dao.upsert(s)
        val read = dao.readState()
        assertEquals(s, read)
    }

    @Test fun clear_removes_state() = runTest {
        dao.upsert(ShuffleStateEntity(1, listOf(1L), 0, 0L, emptyList()))
        dao.clear()
        assertNull(dao.readState())
    }

    @Test fun observeState_emits_upserted_state() = runTest {
        dao.observeState().test {
            assertNull(awaitItem()) // initial null
            dao.upsert(ShuffleStateEntity(1, listOf(1L, 2L), 0, 0L, emptyList()))
            val next = awaitItem()
            assertEquals(listOf(1L, 2L), next?.queueMediaIds)
            cancelAndIgnoreRemainingEvents()
        }
    }
}