package com.example.expense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.expense.ui.navigation.ExpenseNavHost
import com.example.expense.ui.theme.ExpenseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = applicationContext as ExpenseApplication
        setContent {
            ExpenseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ExpenseNavHost(viewModel = app.viewModel)
                }
            }
        }
    }
}