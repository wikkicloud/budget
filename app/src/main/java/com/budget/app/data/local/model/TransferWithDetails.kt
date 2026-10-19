package com.budget.app.data.local.model

/** Перевод вместе с названиями кошельков — для списка на экране «Кошельки». */
data class TransferWithDetails(
    val id: Long,
    val fromWalletId: Long,
    val fromWalletName: String,
    val toWalletId: Long,
    val toWalletName: String,
    val amount: Long,
    val date: Long,
    val note: String?
)