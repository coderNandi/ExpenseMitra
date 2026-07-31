package com.example.expense.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.expense.data.model.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE date = :date ORDER BY timestamp DESC")
    fun observeExpensesByDate(date: String): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE date LIKE :monthQuery || '%'")
    fun observeMonthlyTotal(monthQuery: String): Flow<Double?>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun observeAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses")
    suspend fun clearAll()
}
