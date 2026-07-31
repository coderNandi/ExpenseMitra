package com.example.expense.data.repository

import com.example.expense.data.database.ExpenseDao
import com.example.expense.data.model.Expense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ExpenseRepository {
    fun observeExpensesByDate(date: String): Flow<List<Expense>>
    fun observeMonthlyTotal(monthQuery: String): Flow<Double>
    fun observeAllExpenses(): Flow<List<Expense>>
    suspend fun addExpense(expense: Expense): Long
    suspend fun deleteExpense(expense: Expense)
    suspend fun clearAll()
}

class ExpenseRepositoryImpl(
    private val expenseDao: ExpenseDao
) : ExpenseRepository {
    override fun observeExpensesByDate(date: String): Flow<List<Expense>> = expenseDao.observeExpensesByDate(date)

    override fun observeMonthlyTotal(monthQuery: String): Flow<Double> = 
        expenseDao.observeMonthlyTotal(monthQuery).map { it ?: 0.0 }

    override fun observeAllExpenses(): Flow<List<Expense>> = expenseDao.observeAllExpenses()

    override suspend fun addExpense(expense: Expense): Long = expenseDao.insertExpense(expense)

    override suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    override suspend fun clearAll() = expenseDao.clearAll()
}
