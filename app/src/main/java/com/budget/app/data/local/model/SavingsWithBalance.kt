package com.budget.app.data.local.model

/** Копилка вместе с посчитанным балансом: пополнения − выводы + проценты. */
data class SavingsWithBalance(
    val id: Long,
    val name: String,
    val createdAt: Long,
    /** Цель в копейках; null — копилка без цели. */
    val targetAmount: Long?,
    /** Баланс в копейках. */
    val balance: Long
)