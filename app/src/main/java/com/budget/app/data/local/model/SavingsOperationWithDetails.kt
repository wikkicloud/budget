package com.budget.app.data.local.model

/** Операция по копилке вместе с названиями копилки и кошелька. */
data class SavingsOperationWithDetails(
    val id: Long,
    val savingsId: Long,
    val savingsName: String,
    /** null — операция не связана с кошельком (проценты). */
    val walletId: Long?,
    val walletName: String?,
    val amount: Long,
    /** "DEPOSIT", "WITHDRAW" или "INTEREST". */
    val type: String,
    val date: Long
)