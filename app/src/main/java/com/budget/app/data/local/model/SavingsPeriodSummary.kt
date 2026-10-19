package com.budget.app.data.local.model

/**
 * Итоги по копилкам за период: сколько пополнено, снято и начислено процентов.
 * Суммы в копейках, всегда неотрицательные.
 */
data class SavingsPeriodSummary(
    val deposited: Long,
    val withdrawn: Long,
    val interest: Long
)