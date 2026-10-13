package com.budget.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.budget.app.ui.analytics.AnalyticsScreen
import com.budget.app.ui.expenses.ExpensesScreen
import com.budget.app.ui.income.IncomeScreen
import com.budget.app.ui.wallets.WalletsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Expenses.route,
        modifier = modifier
    ) {
        composable(Screen.Expenses.route) {
            ExpensesScreen()
        }
        composable(Screen.Income.route) {
            IncomeScreen()
        }
        composable(Screen.Wallets.route) {
            WalletsScreen()
        }
        composable(Screen.Analytics.route) {
            AnalyticsScreen()
        }
    }
}
