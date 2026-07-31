package com.example.expense.utils

data class ValidationResult(
    val isValid: Boolean,
    val descriptionError: String? = null,
    val amountError: String? = null,
)

object ExpenseInputValidator {
    fun validate(description: String, amount: String): ValidationResult {
        val trimmedDescription = description.trim()
        val parsedAmount = amount.toDoubleOrNull()

        val descriptionError = when {
            trimmedDescription.isEmpty() -> "Description is required"
            else -> null
        }

        val amountError = when {
            parsedAmount == null -> "Enter a valid amount"
            parsedAmount <= 0.0 -> "Amount must be greater than zero"
            else -> null
        }

        return ValidationResult(
            isValid = descriptionError == null && amountError == null,
            descriptionError = descriptionError,
            amountError = amountError,
        )
    }
}
