package com.example.expense.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.expense.auth.AuthenticationViewModel
import com.example.expense.auth.SessionState
import com.example.expense.ui.screens.AddExpenseScreen
import com.example.expense.ui.screens.AuthenticationScreen
import com.example.expense.ui.screens.HomeScreen
import com.example.expense.ui.screens.SplashScreen
import com.example.expense.viewmodel.ExpenseViewModel

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Authentication : Screen("authentication")
    data object Home : Screen("home")
    data object AddExpense : Screen("add_expense")
}

@Composable
fun ExpenseNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    expenseViewModel: ExpenseViewModel,
    authenticationViewModel: AuthenticationViewModel,
) {
    // Observe session state to manage authentication flow
    val sessionState by authenticationViewModel.sessionState.collectAsState()
    
    // Determine start destination based on authentication state
    val startDestination = when (sessionState) {
        SessionState.Locked -> Screen.Authentication.route
        SessionState.Unauthenticated -> Screen.Splash.route
        SessionState.Active -> Screen.Home.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Screen.Splash.route) {
            SplashScreen()
            // Navigate to authentication after short delay
            androidx.compose.runtime.LaunchedEffect(Unit) {
                navController.navigate(Screen.Authentication.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        }

        composable(Screen.Authentication.route) {
            AuthenticationScreen(
                viewModel = authenticationViewModel,
                onAuthenticationSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Authentication.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = expenseViewModel,
                onAddExpenseClick = { navController.navigate(Screen.AddExpense.route) },
            )
        }

        composable(Screen.AddExpense.route) {
            AddExpenseScreen(
                viewModel = expenseViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }

    // Handle session state changes
    androidx.compose.runtime.LaunchedEffect(sessionState) {
        when (sessionState) {
            SessionState.Locked -> {
                navController.navigate(Screen.Authentication.route) {
                    // Clear backstack to prevent accidental access to home
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
            }
            else -> {} // Other states are handled by initial startDestination
        }
    }
}
