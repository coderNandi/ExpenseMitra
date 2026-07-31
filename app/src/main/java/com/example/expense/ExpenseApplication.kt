package com.example.expense

import android.app.Application
import com.example.expense.auth.AuthenticationManager
import com.example.expense.auth.AuthenticationViewModel
import com.example.expense.auth.BiometricHelper
import com.example.expense.auth.SessionManager
import com.example.expense.data.database.ExpenseDatabase
import com.example.expense.data.repository.ExpenseRepository
import com.example.expense.data.repository.ExpenseRepositoryImpl
import com.example.expense.viewmodel.ExpenseViewModel

class ExpenseApplication : Application() {
    lateinit var repository: ExpenseRepository
        private set

    lateinit var viewModel: ExpenseViewModel
        private set

    // Authentication components
    lateinit var authenticationManager: AuthenticationManager
        private set

    lateinit var biometricHelper: BiometricHelper
        private set

    lateinit var sessionManager: SessionManager
        private set

    lateinit var authenticationViewModel: AuthenticationViewModel
        private set

    override fun onCreate() {
        super.onCreate()
        val database = ExpenseDatabase.getInstance(this)
        repository = ExpenseRepositoryImpl(database.expenseDao())
        viewModel = ExpenseViewModel(repository)

        // Initialize authentication components
        authenticationManager = AuthenticationManager()
        biometricHelper = BiometricHelper(this)
        sessionManager = SessionManager(this, authenticationManager)
        authenticationViewModel = AuthenticationViewModel(biometricHelper, sessionManager)
    }
}

