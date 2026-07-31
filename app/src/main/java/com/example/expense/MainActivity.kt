package com.example.expense

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.expense.ui.navigation.ExpenseNavHost
import com.example.expense.ui.theme.ExpenseTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge layout
        enableEdgeToEdge()
        
        // Security: Prevent screenshots and display in recent apps
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        
        val app = applicationContext as ExpenseApplication
        
        setContent {
            ExpenseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ExpenseNavHost(
                        expenseViewModel = app.viewModel,
                        authenticationViewModel = app.authenticationViewModel
                    )
                }
            }
        }
    }
}