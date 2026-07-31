package com.example.expense

import com.example.expense.utils.ExpenseInputValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseInputValidatorTest {
    @Test
    fun invalidWhenDescriptionIsBlank() {
        val result = ExpenseInputValidator.validate("   ", "12.50")

        assertFalse(result.isValid)
        assertTrue(result.descriptionError != null)
    }

    @Test
    fun invalidWhenAmountIsZeroOrNegative() {
        val result = ExpenseInputValidator.validate("Lunch", "0")

        assertFalse(result.isValid)
        assertTrue(result.amountError != null)
    }

    @Test
    fun validWhenDescriptionAndAmountAreProvided() {
        val result = ExpenseInputValidator.validate("Lunch", "12.50")

        assertTrue(result.isValid)
        assertTrue(result.descriptionError == null)
        assertTrue(result.amountError == null)
    }
}
