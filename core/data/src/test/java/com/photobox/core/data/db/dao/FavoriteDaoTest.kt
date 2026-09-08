package com.photobox.core.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.photobox.core.data.db.AppDatabase
import com.photobox.core.data.db.entity.FavoriteEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class FavoriteDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: FavoriteDao

    @Before fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.favoriteDao()
    }

    @After fun tearDown() { db.close() }

    @Test fun upsert_then_observeIsFavorite_emits_true() = runTest {
        dao.upsert(FavoriteEntity(mediaId = 1L, addedAt = 100L, mediaUri = "u", dateTaken = 0L))
        dao.observeIsFavorite(1L).test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun delete_clears_favorite_state() = runTest {
        dao.upsert(FavoriteEntity(1L, 100L, "u", 0L))
        dao.delete(1L)
        dao.observeIsFavorite(1L).test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun observeAllIds_returns_inserted_ids() = runTest {
        dao.upsert(FavoriteEntity(1L, 1L, "u1", 0L))
        dao.upsert(FavoriteEntity(2L, 2L, "u2", 0L))
        dao.observeAllIds().test {
            assertEquals(listOf(1L, 2L), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}