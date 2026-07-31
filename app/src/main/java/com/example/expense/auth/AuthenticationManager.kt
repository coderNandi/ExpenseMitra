package com.example.expense.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Core authentication state management.
 * Handles overall authentication state without UI concerns.
 */
class AuthenticationManager {
    private val _authenticationState = MutableStateFlow<AuthenticationState>(AuthenticationState.Unauthenticated)
    val authenticationState: StateFlow<AuthenticationState> = _authenticationState.asStateFlow()

    fun setAuthenticated() {
        _authenticationState.value = AuthenticationState.Authenticated
    }

    fun setUnauthenticated() {
        _authenticationState.value = AuthenticationState.Unauthenticated
    }

    fun isAuthenticated(): Boolean = _authenticationState.value is AuthenticationState.Authenticated
}

/**
 * Represents the overall authentication state of the application.
 */
sealed class AuthenticationState {
    data object Unauthenticated : AuthenticationState()
    data object Authenticated : AuthenticationState()
}
