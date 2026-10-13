package com.budget.app.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MoneyFormatter {
    /**
     * Форматирует сумму из копеек в строку вида "1 234,50 ₽"
     * @param amountInKopecks сумма в копейках
     * @return отформатированная строка
     */
    fun format(amountInKopecks: Long): String {
        val rubles = amountInKopecks / 100.0

        val symbols = DecimalFormatSymbols(Locale("ru", "RU")).apply {
            groupingSeparator = ' '
            decimalSeparator = ','
        }

        val formatter = DecimalFormat("#,##0.00", symbols)
        return "${formatter.format(rubles)} ₽"
    }
}
