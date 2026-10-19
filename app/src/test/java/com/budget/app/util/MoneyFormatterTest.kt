package com.budget.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

    // --- filterAmountInput ---

    @Test
    fun `filterAmountInput drops letters and other symbols`() {
        assertEquals("", MoneyFormatter.filterAmountInput("abc"))
        assertEquals("1234", MoneyFormatter.filterAmountInput("12a3b4"))
        assertEquals("1500", MoneyFormatter.filterAmountInput("1 500 ₽"))
        assertEquals("100", MoneyFormatter.filterAmountInput("-100"))
    }

    @Test
    fun `filterAmountInput keeps digits and one separator`() {
        assertEquals("1234,50", MoneyFormatter.filterAmountInput("1234,50"))
        assertEquals("0,99", MoneyFormatter.filterAmountInput("0,99"))
        // Копейки фильтр не дополняет нулём — это делает parseToKopecks
        assertEquals("1234,5", MoneyFormatter.filterAmountInput("1234,5"))
        assertEquals(123_450L, MoneyFormatter.parseToKopecks(MoneyFormatter.filterAmountInput("1234,5")))
    }

    @Test
    fun `filterAmountInput replaces a dot with a comma`() {
        assertEquals("1234,50", MoneyFormatter.filterAmountInput("1234.50"))
    }

    @Test
    fun `filterAmountInput keeps only the first separator`() {
        assertEquals("1234,50", MoneyFormatter.filterAmountInput("1234,5,0"))
        assertEquals("1234,50", MoneyFormatter.filterAmountInput("1234.5,0"))
    }

    @Test
    fun `filterAmountInput drops a separator without digits before it`() {
        assertEquals("", MoneyFormatter.filterAmountInput(","))
        assertEquals("5", MoneyFormatter.filterAmountInput(".5"))
        assertEquals("5", MoneyFormatter.filterAmountInput(",5"))
    }

    @Test
    fun `filterAmountInput limits the decimals to two`() {
        assertEquals("12,34", MoneyFormatter.filterAmountInput("12,345"))
        assertEquals("12,34", MoneyFormatter.filterAmountInput("12,3499"))
    }

    @Test
    fun `filterAmountInput result is always parseable`() {
        val inputs = listOf(
            "1234", "1 234,50", "12a3b4", "1 500 ₽", "12,345", ",5", ",", "abc", "", "0"
        )
        inputs.forEach { input ->
            val filtered = MoneyFormatter.filterAmountInput(input)
            // Пустой ввод — не число, любой непустой обязан разбираться
            if (filtered.isNotEmpty()) {
                assertNotNull("не разобралось: \"$input\" -> \"$filtered\"", MoneyFormatter.parseToKopecks(filtered))
            }
        }
    }

    // --- groupAmountInput ---

    @Test
    fun `groupAmountInput separates thousands while typing`() {
        assertEquals("1 000", MoneyFormatter.groupAmountInput("1000"))
        assertEquals("1 000 000", MoneyFormatter.groupAmountInput("1000000"))
        // Уже разделённый ввод не портится: пробелы отбрасываются, разряды расставляются заново
        assertEquals("1 000 000", MoneyFormatter.groupAmountInput("1 000 000"))
        assertEquals("12 345 678", MoneyFormatter.groupAmountInput("12345678"))
    }

    @Test
    fun `groupAmountInput keeps short numbers as they are`() {
        assertEquals("0", MoneyFormatter.groupAmountInput("0"))
        assertEquals("7", MoneyFormatter.groupAmountInput("7"))
        assertEquals("999", MoneyFormatter.groupAmountInput("999"))
    }

    @Test
    fun `groupAmountInput separates only the whole part`() {
        assertEquals("1 234,50", MoneyFormatter.groupAmountInput("1234,50"))
        assertEquals("1 234,5", MoneyFormatter.groupAmountInput("1234,5"))
        // Копейки без рублей фильтр отбрасывает вместе с разделителем: «,5» — это 5 рублей,
        // а «0,5» — уже готовый ввод и остаётся как есть
        assertEquals("5", MoneyFormatter.groupAmountInput(",5"))
        assertEquals("0,5", MoneyFormatter.groupAmountInput("0,5"))
    }

    @Test
    fun `groupAmountInput cleans the input like filterAmountInput`() {
        assertEquals("", MoneyFormatter.groupAmountInput(""))
        assertEquals("", MoneyFormatter.groupAmountInput("abc"))
        assertEquals("1 500", MoneyFormatter.groupAmountInput("1 500 ₽"))
        assertEquals("12,34", MoneyFormatter.groupAmountInput("12,345"))
        assertEquals("1 000,50", MoneyFormatter.groupAmountInput("1000.5,0"))
    }

    @Test
    fun `groupAmountInput result is always parseable`() {
        val inputs = listOf("1000", "1 000 000,50", "12a3b4", "1 500 ₽", ",5", "", "0", "999999999")
        inputs.forEach { input ->
            val grouped = MoneyFormatter.groupAmountInput(input)
            // Пустой ввод — не число, любой непустой обязан разбираться обратно в копейки
            if (grouped.isNotEmpty()) {
                assertNotNull(
                    "не разобралось: \"$input\" -> \"$grouped\"",
                    MoneyFormatter.parseToKopecks(grouped)
                )
            }
        }
    }

    @Test
    fun `groupAmountInput and parseToKopecks make a round trip`() {
        val amounts = listOf(0L, 700L, 100_000L, 123_450L, 100_000_000L)
        amounts.forEach { kopecks ->
            assertEquals(
                kopecks,
                MoneyFormatter.parseToKopecks(
                    MoneyFormatter.groupAmountInput(MoneyFormatter.formatPlain(kopecks))
                )
            )
        }
    }

    @Test
    fun `parseToKopecks and formatPlain make a round trip`() {
        val amounts = listOf(0L, 1L, 99L, 100L, 123_450L, 123_456_789L)
        amounts.forEach { kopecks ->
            assertEquals(kopecks, MoneyFormatter.parseToKopecks(MoneyFormatter.formatPlain(kopecks)))
        }
    }
}