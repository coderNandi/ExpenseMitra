package com.example.expense.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.expense.ui.screens.AddExpenseScreen
import com.example.expense.ui.screens.HomeScreen
import com.example.expense.viewmodel.ExpenseViewModel

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object AddExpense : Screen("add_expense")
}

@Composable
fun ExpenseNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: ExpenseViewModel,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onAddExpenseClick = { navController.navigate(Screen.AddExpense.route) },
            )
        }
        composable(Screen.AddExpense.route) {
            AddExpenseScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
