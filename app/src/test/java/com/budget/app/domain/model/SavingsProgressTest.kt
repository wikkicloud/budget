package com.budget.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Прогресс копилки по цели: доля и проценты. */
class SavingsProgressTest {

    @Test
    fun `half of the goal is fifty percent`() {
        assertEquals(0.5f, SavingsProgress.fraction(50_000, 100_000)!!, 0.0001f)
        assertEquals(50, SavingsProgress.percent(50_000, 100_000))
    }

    @Test
    fun `empty savings has zero progress`() {
        assertEquals(0f, SavingsProgress.fraction(0, 100_000)!!, 0.0001f)
        assertEquals(0, SavingsProgress.percent(0, 100_000))
    }

    @Test
    fun `reached goal is one hundred percent`() {
        assertEquals(1f, SavingsProgress.fraction(100_000, 100_000)!!, 0.0001f)
        assertEquals(100, SavingsProgress.percent(100_000, 100_000))
    }

    @Test
    fun `savings above the goal do not exceed one hundred percent`() {
        assertEquals(1f, SavingsProgress.fraction(250_000, 100_000)!!, 0.0001f)
        assertEquals(100, SavingsProgress.percent(250_000, 100_000))
    }

    @Test
    fun `negative balance does not go below zero`() {
        assertEquals(0f, SavingsProgress.fraction(-5_000, 100_000)!!, 0.0001f)
        assertEquals(0, SavingsProgress.percent(-5_000, 100_000))
    }

    @Test
    fun `savings without a goal has no progress`() {
        assertNull(SavingsProgress.fraction(50_000, null))
        assertNull(SavingsProgress.percent(50_000, null))
    }

    @Test
    fun `zero or negative goal is treated as no goal`() {
        assertNull(SavingsProgress.fraction(50_000, 0))
        assertNull(SavingsProgress.fraction(50_000, -1_000))
    }

    @Test
    fun `percent is rounded down to whole percent`() {
        assertEquals(33, SavingsProgress.percent(33_333, 100_000))
        assertEquals(66, SavingsProgress.percent(66_666, 100_000))
    }

    @Test
    fun `kopecks do not break the calculation`() {
        assertEquals(1f, SavingsProgress.fraction(1, 1)!!, 0.0001f)
        assertEquals(100, SavingsProgress.percent(1, 1))
    }
}