package com.example.expense.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expense.data.model.Expense
import com.example.expense.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class ExpenseUiState(
    val selectedDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val expenses: List<Expense> = emptyList(),
    val totalAmount: Double = 0.0,
    val isLoading: Boolean = false,
)

class ExpenseViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    init {
        observeSelectedDateExpenses()
    }

    private fun observeSelectedDateExpenses() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            _uiState.collectLatest { currentState ->
                repository.observeExpensesByDate(currentState.selectedDate).collect { expenses ->
                    val total = expenses.sumOf { it.amount }
                    _uiState.value = currentState.copy(
                        expenses = expenses,
                        totalAmount = total,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun selectDate(date: LocalDate) {
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        _uiState.value = _uiState.value.copy(selectedDate = dateString)
        viewModelScope.launch {
            repository.observeExpensesByDate(dateString).collect { expenses ->
                val total = expenses.sumOf { it.amount }
                _uiState.value = _uiState.value.copy(
                    expenses = expenses,
                    totalAmount = total,
                )
            }
        }
    }

    fun addExpense(description: String, amount: String, date: String) {
        val parsedAmount = amount.toDoubleOrNull() ?: return
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val expense = Expense(
            description = description.trim(),
            amount = parsedAmount,
            date = date,
            timestamp = timestamp,
        )
        viewModelScope.launch {
            repository.addExpense(expense)
            repository.observeExpensesByDate(date).collect { expenses ->
                val total = expenses.sumOf { it.amount }
                _uiState.value = _uiState.value.copy(
                    expenses = expenses,
                    totalAmount = total,
                )
            }
        }
    }
}
