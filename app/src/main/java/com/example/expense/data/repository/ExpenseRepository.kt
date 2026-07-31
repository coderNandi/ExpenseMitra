package com.example.expense.data.repository

import com.example.expense.data.database.ExpenseDao
import com.example.expense.data.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeExpensesByDate(date: String): Flow<List<Expense>>
    fun observeAllExpenses(): Flow<List<Expense>>
    suspend fun addExpense(expense: Expense): Long
    suspend fun clearAll()
}

class ExpenseRepositoryImpl(
    private val expenseDao: ExpenseDao
) : ExpenseRepository {
    override fun observeExpensesByDate(date: String): Flow<List<Expense>> = expenseDao.observeExpensesByDate(date)

    override fun observeAllExpenses(): Flow<List<Expense>> = expenseDao.observeAllExpenses()

    override suspend fun addExpense(expense: Expense): Long = expenseDao.insertExpense(expense)

    override suspend fun clearAll() = expenseDao.clearAll()
}
