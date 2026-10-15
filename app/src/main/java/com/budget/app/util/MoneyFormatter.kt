package com.budget.app.util

import kotlin.math.abs

object MoneyFormatter {

    /** Символ валюты, дописывается к сумме в [format]. */
    private const val CURRENCY = "₽"

    /** Пробелы и неразрывные пробелы, которые могут попасть в ввод вместе с числом. */
    private val SPACES = Regex("""[\s ]+""")

    /** Целая часть и не более двух знаков после разделителя. */
    private val AMOUNT_PATTERN = Regex("""\d+(\.\d{0,2})?""")

    /**
     * Форматирует сумму из копеек в строку вида "1 234,50 ₽"
     * @param amountInKopecks сумма в копейках
     * @return отформатированная строка
     */
    fun format(amountInKopecks: Long): String = "${formatPlain(amountInKopecks)} $CURRENCY"

    /**
     * Форматирует сумму из копеек без символа валюты — для полей ввода.
     * @param amountInKopecks сумма в копейках
     * @return строка вида "1 234,50"
     */
    fun formatPlain(amountInKopecks: Long): String {
        // Делим с сохранением знака: abs(Long.MIN_VALUE) переполнился бы, а рублёвая часть после / 100 — нет
        val rubles = amountInKopecks / 100
        val kopecks = abs(amountInKopecks % 100)

        return "${signOf(amountInKopecks)}${group(abs(rubles))},${kopecks.toString().padStart(2, '0')}"
    }

    /**
     * Форматирует сумму из копеек без копеек — для подписей осей и компактных мест.
     * @param amountInKopecks сумма в копейках
     * @return строка вида "1 235 ₽"
     */
    fun formatRubles(amountInKopecks: Long): String {
        // Округление до целых рублей, половина — от нуля
        val rubles = amountInKopecks / 100
        val kopecks = amountInKopecks % 100
        val rounded = if (abs(kopecks) >= 50) {
            rubles + if (amountInKopecks >= 0) 1 else -1
        } else {
            rubles
        }
        return "${signOf(amountInKopecks)}${group(abs(rounded))} $CURRENCY"
    }

    /** Знак минуса, если сумма отрицательная. */
    private fun signOf(amountInKopecks: Long): String =
        if (amountInKopecks < 0) "-" else ""

    /** Разбивает неотрицательное число на разряды по три: 1234567 -> "1 234 567". */
    private fun group(value: Long): String = value.toString()
        .reversed()
        .chunked(3)
        .joinToString(" ")
        .reversed()

    /**
     * Разбирает введённую сумму в копейки. Без Double/Float — только целые.
     * Принимает "1234", "1 234,50", "1234.5".
     * @return сумма в копейках или null, если ввод не является числом
     */
    fun parseToKopecks(input: String): Long? {
        val cleaned = SPACES.replace(input.trim(), "").replace(',', '.')

        if (!AMOUNT_PATTERN.matches(cleaned)) return null

        val parts = cleaned.split('.')
        val rubles = parts[0].toLongOrNull() ?: return null

        val kopecks = if (parts.size > 1) {
            parts[1].padEnd(2, '0').toLongOrNull() ?: return null
        } else {
            0L
        }

        // Проверка на переполнение при переводе рублей в копейки
        if (rubles > (Long.MAX_VALUE - kopecks) / 100) return null

        return rubles * 100 + kopecks
    }
}