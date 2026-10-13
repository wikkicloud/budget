package com.budget.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Expenses : Screen("expenses", "Расходы", Icons.Filled.ShoppingCart)
    data object Income : Screen("income", "Приход", Icons.Filled.Add)
    data object Wallets : Screen("wallets", "Кошельки", Icons.Filled.Home)
    data object Analytics : Screen("analytics", "Аналитика", Icons.Filled.List)

    companion object {
        val screens = listOf(Expenses, Income, Wallets, Analytics)
    }
}
