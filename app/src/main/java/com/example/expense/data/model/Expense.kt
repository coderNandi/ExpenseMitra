package com.example.expense.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val amount: Double,
    val date: String,
    val timestamp: String,
) {
    val displayDate: String
        get() = date

    val displayTime: String
        get() = timestamp

    fun formattedAmount(currency: String = "₹", locale: java.util.Locale = java.util.Locale("en", "IN")): String {
        val formatter = java.text.NumberFormat.getCurrencyInstance(locale)
        formatter.currency = java.util.Currency.getInstance("INR")
        return formatter.format(amount)
    }
}
