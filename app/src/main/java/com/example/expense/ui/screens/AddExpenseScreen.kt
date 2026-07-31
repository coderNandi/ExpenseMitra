package com.example.expense.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expense.utils.ExpenseInputValidator
import com.example.expense.viewmodel.ExpenseViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: ExpenseViewModel,
    onBack: () -> Unit,
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Add Expense", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it; descriptionError = null },
                label = { Text("Description") },
                isError = descriptionError != null,
                modifier = Modifier.fillMaxWidth(),
            )
            if (descriptionError != null) {
                Text(descriptionError.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it; amountError = null },
                label = { Text("Amount") },
                isError = amountError != null,
                modifier = Modifier.fillMaxWidth(),
            )
            if (amountError != null) {
                Text(amountError.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { showDatePicker = true }) {
                Text("Date: ${selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val validation = ExpenseInputValidator.validate(description, amount)
                    descriptionError = validation.descriptionError
                    amountError = validation.amountError
                    if (validation.isValid) {
                        viewModel.addExpense(description, amount, selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save Expense")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
