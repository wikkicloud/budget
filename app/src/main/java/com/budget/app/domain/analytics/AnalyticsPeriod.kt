package com.budget.app.domain.analytics

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Период, за который считается аналитика. */
enum class AnalyticsPeriod { WEEK, MONTH, YEAR, CUSTOM }

/** Шаг, с которым группируются операции на графике. */
enum class AnalyticsBucket { DAY, MONTH }

/**
 * Разобранный период: границы [from, to) и шаг группировки.
 * @param from начало периода, timestamp (включительно)
 * @param to конец периода, timestamp (не включительно)
 */
data class AnalyticsRange(
    val from: Long,
    val to: Long,
    val bucket: AnalyticsBucket
) {
    /** Дата начала периода. */
    val fromDate: LocalDate get() = toLocalDate(from)

    /** Последний день периода, входящий в него. */
    val toDate: LocalDate get() = toLocalDate(to - 1)

    companion object {
        /** Диапазон длиннее двух месяцев показываем по месяцам, иначе по дням. */
        private const val DAYS_PER_MONTH_BUCKET = 62L

        private val ZONE: ZoneId get() = ZoneId.systemDefault()

        fun toLocalDate(timestamp: Long): LocalDate =
            Instant.ofEpochMilli(timestamp).atZone(ZONE).toLocalDate()

        fun toTimestamp(date: LocalDate): Long =
            date.atStartOfDay(ZONE).toInstant().toEpochMilli()

        /** Границы периода [period] относительно даты [today]. */
        fun of(period: AnalyticsPeriod, today: LocalDate, custom: ClosedRange<LocalDate>?): AnalyticsRange =
            when (period) {
                AnalyticsPeriod.WEEK -> {
                    val from = today.minusDays(6)
                    AnalyticsRange(toTimestamp(from), toTimestamp(today.plusDays(1)), AnalyticsBucket.DAY)
                }

                AnalyticsPeriod.MONTH -> {
                    val from = today.withDayOfMonth(1)
                    AnalyticsRange(
                        toTimestamp(from),
                        toTimestamp(from.plusMonths(1)),
                        AnalyticsBucket.DAY
                    )
                }

                AnalyticsPeriod.YEAR -> {
                    val from = today.withDayOfYear(1)
                    AnalyticsRange(
                        toTimestamp(from),
                        toTimestamp(from.plusYears(1)),
                        AnalyticsBucket.MONTH
                    )
                }

                AnalyticsPeriod.CUSTOM -> {
                    // Диапазон не задан — считаем по текущему месяцу
                    if (custom == null) return of(AnalyticsPeriod.MONTH, today, null)

                    val from = custom.start
                    val to = custom.endInclusive
                    val days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1

                    AnalyticsRange(
                        toTimestamp(from),
                        toTimestamp(to.plusDays(1)),
                        if (days > DAYS_PER_MONTH_BUCKET) AnalyticsBucket.MONTH else AnalyticsBucket.DAY
                    )
                }
            }
    }
}