package com.budget.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object DateFormatter {

    private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val ZONE: ZoneId get() = ZoneId.systemDefault()

    /** Форматирует timestamp в дату вида "14.10.2026". */
    fun formatDate(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(ZONE).format(DATE_FORMAT)

    /** Дата (в системной зоне) из timestamp операции. */
    fun toLocalDate(timestamp: Long): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(ZONE).toLocalDate()

    /** Начало дня в системной зоне — так хранится дата операции. */
    fun toTimestamp(date: LocalDate): Long =
        date.atStartOfDay(ZONE).toInstant().toEpochMilli()

    /** UTC-полночь даты — формат, в котором DatePicker хранит и отдаёт выбранную дату. */
    fun toPickerMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}