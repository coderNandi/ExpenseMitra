package com.example.expense

import android.app.Application
import com.example.expense.data.database.ExpenseDatabase
import com.example.expense.data.repository.ExpenseRepository
import com.example.expense.data.repository.ExpenseRepositoryImpl
import com.example.expense.viewmodel.ExpenseViewModel

class ExpenseApplication : Application() {
    lateinit var repository: ExpenseRepository
        private set

    lateinit var viewModel: ExpenseViewModel
        private set

    override fun onCreate() {
        super.onCreate()
        val database = ExpenseDatabase.getInstance(this)
        repository = ExpenseRepositoryImpl(database.expenseDao())
        viewModel = ExpenseViewModel(repository)
    }
}
