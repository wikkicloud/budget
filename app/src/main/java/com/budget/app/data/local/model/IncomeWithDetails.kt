package com.budget.app.data.local.model

/** Приход вместе с названием кошелька — для списка операций. */
data class IncomeWithDetails(
    val id: Long,
    val amount: Long,
    val description: String,
    val date: Long,
    val walletId: Long,
    val walletName: String,
    val walletType: String
)