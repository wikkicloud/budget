package com.budget.app.util

import kotlin.math.abs

object MoneyFormatter {

    /** Символ валюты, дописывается к сумме в [format]. Он же нужен полям ввода сумм. */
    const val CURRENCY = "₽"

    /** Пробелы и неразрывные пробелы, которые могут попасть в ввод вместе с числом. */
    private val SPACES = Regex("""[\s ]+""")

    /** Целая часть и не более двух знаков после разделителя. */
    private val AMOUNT_PATTERN = Regex("""\d+(\.\d{0,2})?""")

    /** Копейки — два знака после разделителя, больше в поле суммы не вводится. */
    private const val MAX_DECIMALS = 2

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
    private fun group(value: Long): String = groupDigits(value.toString())

    /** Разбивает строку из цифр на разряды по три: "1234567" -> "1 234 567". */
    private fun groupDigits(digits: String): String = digits
        .reversed()
        .chunked(3)
        .joinToString(" ")
        .reversed()

    /**
     * Очищает ввод суммы: в поле остаются только цифры и один разделитель
     * с не более чем двумя знаками после него. Буквы и прочие символы отбрасываются,
     * разделитель без цифр перед ним игнорируется, точка приводится к запятой —
     * как в [formatPlain]. Годится для `onValueChange` полей суммы.
     * Пробелы — разделители разрядов — тоже отбрасываются, как и в [parseToKopecks].
     * @param input то, что ввёл пользователь
     * @return ввод, пригодный для [parseToKopecks]
     */
    fun filterAmountInput(input: String): String {
        val builder = StringBuilder()
        var separatorSeen = false
        var decimals = 0

        for (char in input) {
            if (char in '0'..'9') {
                // Больше двух знаков после разделителя не бывает — это копейки
                if (separatorSeen && decimals == MAX_DECIMALS) continue
                if (separatorSeen) decimals++
                builder.append(char)
            } else if (char == ',' || char == '.') {
                // Второй разделитель и разделитель без цифр перед ним не пропускаем
                if (separatorSeen || builder.isEmpty()) continue
                separatorSeen = true
                builder.append(',')
            }
            // Прочие символы (буквы, минус, знак валюты) отбрасываются
        }

        return builder.toString()
    }

    /**
     * Готовит ввод для отображения в поле суммы: чистит его как [filterAmountInput]
     * и сразу разделяет целую часть на разряды — «1000000» превращается в «1 000 000»
     * прямо во время набора, как в [formatPlain]. Копейки не дополняются нулём:
     * «12,5» так и остаётся «12,5», пока пользователь не введёт второй знак.
     * Годится для `onValueChange` полей суммы, где нужна сумма с разрядами.
     * @param input то, что ввёл пользователь
     * @return ввод, пригодный для [parseToKopecks]
     */
    fun groupAmountInput(input: String): String {
        val filtered = filterAmountInput(input)
        // Пустой ввод остаётся пустым: поле для суммы необязательно и должно очищаться до конца
        if (filtered.isEmpty()) return filtered

        // Разделитель разбираем до группировки: отделять разряды нужно только в целой части
        val separatorIndex = filtered.indexOf(',')
        val integerDigits = if (separatorIndex == -1) filtered else filtered.substring(0, separatorIndex)
        val kopecks = if (separatorIndex == -1) "" else filtered.substring(separatorIndex)

        return groupDigits(integerDigits) + kopecks
    }

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