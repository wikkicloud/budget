package com.budget.app.data.local.model

/** Операция вместе с данными кошелька и категории — для аналитики. */
data class TransactionWithDetails(
    val id: Long,
    /** "EXPENSE" или "INCOME". */
    val type: String,
    /** Сумма в копейках. */
    val amount: Long,
    val date: Long,
    val categoryId: Long?,
    val categoryName: String?,
    val categoryIcon: String?,
    val categoryColor: String?,
    val walletId: Long,
    val walletName: String
)