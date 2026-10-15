package com.budget.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyFormatterTest {

    // --- format ---

    @Test
    fun `format adds currency and two decimals`() {
        assertEquals("1 234,50 ₽", MoneyFormatter.format(123_450))
    }

    @Test
    fun `format keeps small amounts without grouping`() {
        assertEquals("0,00 ₽", MoneyFormatter.format(0))
        assertEquals("0,99 ₽", MoneyFormatter.format(99))
        assertEquals("1,00 ₽", MoneyFormatter.format(100))
        assertEquals("999,99 ₽", MoneyFormatter.format(99_999))
    }

    @Test
    fun `format groups thousands`() {
        assertEquals("1 000,00 ₽", MoneyFormatter.format(100_000))
        assertEquals("1 234 567,89 ₽", MoneyFormatter.format(123_456_789))
    }

    @Test
    fun `format keeps the sign for negative amounts`() {
        assertEquals("-1 234,50 ₽", MoneyFormatter.format(-123_450))
        assertEquals("-0,01 ₽", MoneyFormatter.format(-1))
    }

    @Test
    fun `format does not overflow on extreme amounts`() {
        assertEquals("92 233 720 368 547 758,07 ₽", MoneyFormatter.format(Long.MAX_VALUE))
        // abs(Long.MIN_VALUE) переполнился бы — проверяем, что деление со знаком работает
        assertEquals("-92 233 720 368 547 758,08 ₽", MoneyFormatter.format(Long.MIN_VALUE))
    }

    // --- formatPlain ---

    @Test
    fun `formatPlain omits the currency`() {
        assertEquals("1 234,50", MoneyFormatter.formatPlain(123_450))
        assertEquals("0,00", MoneyFormatter.formatPlain(0))
    }

    // --- formatRubles ---

    @Test
    fun `formatRubles rounds to whole rubles`() {
        assertEquals("1 234 ₽", MoneyFormatter.formatRubles(123_449))
        assertEquals("1 235 ₽", MoneyFormatter.formatRubles(123_450))
        assertEquals("1 235 ₽", MoneyFormatter.formatRubles(123_451))
    }

    @Test
    fun `formatRubles rounds negative amounts away from zero`() {
        assertEquals("-1 234 ₽", MoneyFormatter.formatRubles(-123_449))
        assertEquals("-1 235 ₽", MoneyFormatter.formatRubles(-123_450))
    }

    // --- parseToKopecks ---

    @Test
    fun `parseToKopecks reads whole rubles`() {
        assertEquals(123_400L, MoneyFormatter.parseToKopecks("1234"))
    }

    @Test
    fun `parseToKopecks accepts comma and dot as separator`() {
        assertEquals(123_450L, MoneyFormatter.parseToKopecks("1234,50"))
        assertEquals(123_450L, MoneyFormatter.parseToKopecks("1234.50"))
    }

    @Test
    fun `parseToKopecks pads a single decimal digit`() {
        assertEquals(123_450L, MoneyFormatter.parseToKopecks("1234,5"))
    }

    @Test
    fun `parseToKopecks ignores spaces and non-breaking spaces`() {
        assertEquals(123_450L, MoneyFormatter.parseToKopecks("1 234,50"))
        assertEquals(123_450L, MoneyFormatter.parseToKopecks("  1 234,50  "))
        assertEquals(123_450L, MoneyFormatter.parseToKopecks("1 234,50"))
    }

    @Test
    fun `parseToKopecks rejects non-numeric input`() {
        assertNull(MoneyFormatter.parseToKopecks(""))
        assertNull(MoneyFormatter.parseToKopecks("   "))
        assertNull(MoneyFormatter.parseToKopecks("abc"))
        assertNull(MoneyFormatter.parseToKopecks("1 234,50 ₽"))
        assertNull(MoneyFormatter.parseToKopecks("12,345"))
    }

    @Test
    fun `parseToKopecks rejects negative input`() {
        assertNull(MoneyFormatter.parseToKopecks("-1234"))
    }

    @Test
    fun `parseToKopecks rejects values that overflow kopecks`() {
        // Максимум, который ещё влезает в Long после умножения на 100
        assertEquals(9_223_372_036_854_775_800L, MoneyFormatter.parseToKopecks("92233720368547758"))
        assertNull(MoneyFormatter.parseToKopecks("92233720368547759"))
    }

    @Test
    fun `parseToKopecks and formatPlain make a round trip`() {
        val amounts = listOf(0L, 1L, 99L, 100L, 123_450L, 123_456_789L)
        amounts.forEach { kopecks ->
            assertEquals(kopecks, MoneyFormatter.parseToKopecks(MoneyFormatter.formatPlain(kopecks)))
        }
    }
}