package com.budget.app.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateFormatter {

    private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    /** Форматирует timestamp в дату вида "14.10.2026". */
    fun formatDate(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .format(DATE_FORMAT)
}