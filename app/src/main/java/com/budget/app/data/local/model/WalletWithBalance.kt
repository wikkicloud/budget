package com.budget.app.data.local.model

import com.budget.app.data.local.entity.WalletEntity

data class WalletWithBalance(
    val id: Long,
    val name: String,
    val type: String,
    val initialBalance: Long,
    val balance: Long
)
