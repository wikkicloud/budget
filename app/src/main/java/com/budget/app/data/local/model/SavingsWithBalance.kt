package com.budget.app.data.local.model

/** Копилка вместе с посчитанным балансом: стартовая сумма + пополнения − выводы + проценты. */
data class SavingsWithBalance(
    val id: Long,
    val name: String,
    val createdAt: Long,
    /** Цель в копейках; null — копилка без цели. */
    val targetAmount: Long?,
    /** Стартовая сумма в копейках — деньги, уже лежавшие в копилке при создании. */
    val initialAmount: Long,
    /** Баланс в копейках. */
    val balance: Long
)