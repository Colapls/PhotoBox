package com.photobox.core.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.photobox.core.data.db.AppDatabase
import com.photobox.core.data.db.entity.LikeEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class LikeDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: LikeDao

    @Before fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.likeDao()
    }

    @After fun tearDown() { db.close() }

    @Test fun upsert_then_observeIsLiked_emits_true() = runTest {
        dao.upsert(LikeEntity(42L, likedAt = 100L))
        dao.observeIsLiked(42L).test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun delete_clears_like() = runTest {
        dao.upsert(LikeEntity(42L, 100L))
        dao.delete(42L)
        dao.observeIsLiked(42L).test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}