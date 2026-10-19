package com.budget.app.domain.model

/**
 * Прогресс копилки по цели — общий расчёт для экрана «Копилки» и блока «Копилки» в аналитике.
 * Считается только для копилок с положительной целью: без цели прогресса нет.
 */
object SavingsProgress {

    /**
     * Доля накопленного от цели, 0..1; null — цели нет.
     * Больше цели — 1: копилка считается выполненной, а не заполненной на 130 %.
     */
    fun fraction(balance: Long, targetAmount: Long?): Float? {
        val target = targetAmount?.takeIf { it > 0 } ?: return null
        return (balance.toFloat() / target).coerceIn(0f, 1f)
    }

    /** Целые проценты от цели; null — цели нет. */
    fun percent(balance: Long, targetAmount: Long?): Int? =
        fraction(balance, targetAmount)?.let { (it * 100f).toInt() }
}