package com.example.expense.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.expense.auth.AuthenticationViewModel
import com.example.expense.auth.BiometricAvailability
import com.example.expense.auth.BiometricError

/**
 * Authentication screen with biometric prompt.
 * Displays lock icon, app name, and unlock button.
 * Handles all authentication scenarios including errors and fallbacks.
 */
@Composable
fun AuthenticationScreen(
    viewModel: AuthenticationViewModel,
    onAuthenticationSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.authenticationUiState.collectAsState()
    val context = LocalContext.current as FragmentActivity
    var showDeviceSettingsDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }

    // Auto-trigger authentication on screen display
    LaunchedEffect(Unit) {
        val availability = viewModel.checkBiometricAvailability()
        when (availability) {
            BiometricAvailability.Available -> {
                // Will trigger biometric prompt when user taps unlock
            }
            BiometricAvailability.NoHardware,
            BiometricAvailability.NoneEnrolled,
            BiometricAvailability.HardwareUnavailable -> {
                showSecurityDialog = true
            }
            else -> {
                showDeviceSettingsDialog = true
            }
        }
    }

    // Listen for successful authentication
    LaunchedEffect(uiState) {
        // Success state is handled by checking if error is cleared and callback triggered
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Lock Icon
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = "App Lock",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // App Name
        Text(
            text = "Expense Tracker",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Authentication Message
        Text(
            text = "Authenticate to access your Expense Tracker",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Error Message
        if (uiState.shouldShowError) {
            Text(
                text = uiState.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Unlock Button
        Button(
            onClick = {
                viewModel.resetAuthenticationState()
                triggerAuthentication(
                    viewModel = viewModel,
                    activity = context,
                    onSuccess = onAuthenticationSuccess,
                    onSecuritySettingsNeeded = { showSecurityDialog = true }
                )
            },
            enabled = uiState.canRetry && !uiState.isLoading,
            modifier = Modifier
                .height(48.dp)
                .padding(horizontal = 32.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Unlock")
            }
        }
    }

    // Dialog: Security settings required
    if (showSecurityDialog) {
        AlertDialog(
            onDismissRequest = { showSecurityDialog = false },
            title = { Text("Security Configuration Required") },
            text = {
                Text(
                    "Please enable Fingerprint or Device Lock in your phone settings " +
                    "to use the app."
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.openSecuritySettings(context as Activity)
                    showSecurityDialog = false
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSecurityDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Device settings error
    if (showDeviceSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showDeviceSettingsDialog = false },
            title = { Text("Device Not Supported") },
            text = {
                Text(uiState.error?.getUserMessage() ?: "Your device cannot be authenticated")
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeviceSettingsDialog = false
                    // In production, might exit app or show fallback
                }) {
                    Text("OK")
                }
            }
        )
    }
}

/**
 * Triggers the biometric authentication prompt.
 */
private fun triggerAuthentication(
    viewModel: AuthenticationViewModel,
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onSecuritySettingsNeeded: () -> Unit
) {
    val biometricHelper = BiometricHelperInstance.get(activity)
    
    biometricHelper.authenticate(
        activity = activity,
        onSuccess = {
            viewModel.onAuthenticationSuccess()
            onSuccess()
        },
        onFailure = { error ->
            if (error is BiometricError.NoDeviceCredential || 
                error is BiometricError.DeviceNotSupported) {
                onSecuritySettingsNeeded()
            }
        },
        onCancelled = {
            // User cancelled authentication, stay on screen and let them retry
        }
    )
}

/**
 * Singleton for BiometricHelper to maintain single instance.
 */
object BiometricHelperInstance {
    private var instance: com.example.expense.auth.BiometricHelper? = null

    fun get(context: android.content.Context): com.example.expense.auth.BiometricHelper {
        if (instance == null) {
            instance = com.example.expense.auth.BiometricHelper(context.applicationContext)
        }
        return instance!!
    }
}
