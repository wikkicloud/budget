package com.budget.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import com.budget.app.R

sealed class Screen(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    data object Expenses : Screen("expenses", R.string.screen_expenses, Icons.Filled.ShoppingCart)
    data object Income : Screen("income", R.string.screen_income, Icons.Filled.Payments)
    data object Wallets : Screen("wallets", R.string.screen_wallets, Icons.Filled.Home)
    data object Savings : Screen("savings", R.string.screen_savings, Icons.Filled.Savings)
    data object Analytics : Screen("analytics", R.string.screen_analytics, Icons.AutoMirrored.Filled.List)

    companion object {
        val screens = listOf(Expenses, Income, Wallets, Savings, Analytics)
    }
}