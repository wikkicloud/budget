package com.budget.app.data.local.model

/** Расход вместе с названиями кошелька и категории — для списка операций. */
data class ExpenseWithDetails(
    val id: Long,
    val amount: Long,
    val description: String,
    val date: Long,
    val categoryId: Long?,
    val categoryName: String?,
    val categoryIcon: String?,
    val walletId: Long,
    val walletName: String,
    val walletType: String
)