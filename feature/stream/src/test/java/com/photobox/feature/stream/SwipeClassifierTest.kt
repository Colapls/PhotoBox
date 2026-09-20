package com.photobox.feature.stream

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SwipeClassifierTest {

    private val slop = 10f

    @Test
    fun `classifySwipeAxis returns null when both axes within slop`() {
        assertNull(classifySwipeAxis(dx = 5f, dy = 5f, slop = slop))
        assertNull(classifySwipeAxis(dx = -5f, dy = -5f, slop = slop))
        assertNull(classifySwipeAxis(dx = 10f, dy = 10f, slop = slop))  // 边界仍算未跨过
    }

    @Test
    fun `classifySwipeAxis returns true when horizontal dominates`() {
        assertEquals(true, classifySwipeAxis(dx = 50f, dy = 5f, slop = slop))
        assertEquals(true, classifySwipeAxis(dx = -50f, dy = -5f, slop = slop))
    }

    @Test
    fun `classifySwipeAxis returns false when vertical dominates`() {
        assertEquals(false, classifySwipeAxis(dx = 5f, dy = 50f, slop = slop))
        assertEquals(false, classifySwipeAxis(dx = -5f, dy = -50f, slop = slop))
    }

    @Test
    fun `isLeftSwipe fires on negative X beyond threshold and dominating`() {
        assertTrue(isLeftSwipe(totalDx = -100f, totalDy = 5f, slop = slop))
    }

    @Test
    fun `isLeftSwipe rejects right swipe even if dominant`() {
        assertFalse(isLeftSwipe(totalDx = 100f, totalDy = 5f, slop = slop))
    }

    @Test
    fun `isLeftSwipe rejects near-stationary motion`() {
        assertFalse(isLeftSwipe(totalDx = -10f, totalDy = 0f, slop = slop))
    }

    @Test
    fun `isLeftSwipe rejects when vertical dominates by 2x ratio`() {
        // |dx|=50, |dy|=30 → 50 > 60? false → reject
        assertFalse(isLeftSwipe(totalDx = -50f, totalDy = 30f, slop = slop))
    }
}