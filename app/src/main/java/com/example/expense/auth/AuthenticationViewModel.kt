package com.example.expense.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for authentication UI.
 * Manages authentication UI state and coordinates with authentication managers.
 */
class AuthenticationViewModel(
    private val biometricHelper: BiometricHelper,
    private val sessionManager: SessionManager
) : ViewModel() {

    val authenticationUiState: StateFlow<AuthenticationUiState> = biometricHelper.biometricState
        .map { biometricState ->
            AuthenticationUiState(
                isLoading = biometricState is BiometricState.Authenticating,
                error = (biometricState as? BiometricState.Error)?.error,
                isFailed = biometricState is BiometricState.Failed,
                canRetry = biometricState !is BiometricState.Authenticating
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthenticationUiState()
        )

    // Expose session state for navigation
    val sessionState: StateFlow<SessionState> = sessionManager.sessionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SessionState.Unauthenticated
        )

    /**
     * Check if device has biometric capability.
     */
    fun checkBiometricAvailability(): BiometricAvailability = biometricHelper.canAuthenticate()

    /**
     * Open device security settings.
     */
    fun openSecuritySettings(activity: android.app.Activity) {
        biometricHelper.openSecuritySettings(activity)
    }

    /**
     * Mark user as successfully authenticated.
     */
    fun onAuthenticationSuccess() {
        sessionManager.onAuthenticationSuccess()
        biometricHelper.resetState()
    }

    /**
     * Reset biometric state for retry.
     */
    fun resetAuthenticationState() {
        biometricHelper.resetState()
    }
}

/**
 * UI state for authentication screen.
 */
data class AuthenticationUiState(
    val isLoading: Boolean = false,
    val error: BiometricError? = null,
    val isFailed: Boolean = false,
    val canRetry: Boolean = true
) {
    val shouldShowError: Boolean = error != null && error !is BiometricError.Cancelled
    val errorMessage: String = error?.getUserMessage() ?: ""
}

