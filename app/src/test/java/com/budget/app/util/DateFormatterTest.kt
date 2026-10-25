package com.budget.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class DateFormatterTest {

    @Test
    fun `formatDate renders dd MM yyyy`() {
        val stamp = DateFormatter.toTimestamp(LocalDate.of(2026, 10, 14))
        assertEquals("14.10.2026", DateFormatter.formatDate(stamp))
    }

    @Test
    fun `toTimestamp is start of day in system zone`() {
        val date = LocalDate.of(2026, 10, 14)
        val zoned = Instant.ofEpochMilli(DateFormatter.toTimestamp(date))
            .atZone(ZoneId.systemDefault())
        assertEquals(date, zoned.toLocalDate())
        assertEquals(0, zoned.hour)
        assertEquals(0, zoned.minute)
    }

    @Test
    fun `toLocalDate returns the same calendar day as start of day in system zone`() {
        val date = LocalDate.of(2026, 10, 14)
        assertEquals(date, DateFormatter.toLocalDate(DateFormatter.toTimestamp(date)))
    }

    @Test
    fun `round trip keeps the calendar day`() {
        val date = LocalDate.of(2026, 2, 28)
        assertEquals(date, DateFormatter.toLocalDate(DateFormatter.toTimestamp(date)))
    }
}