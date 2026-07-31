package com.example.expense.auth

import android.app.Activity
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Manages app session lifecycle and authentication timeout.
 * Tracks when app enters/leaves foreground and locks after configured timeout.
 */
class SessionManager(context: Context, private val authenticationManager: AuthenticationManager) :
    DefaultLifecycleObserver {

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    // Configuration: time to wait before locking (30 seconds by default)
    private var lockTimeoutSeconds = 30L

    private var lockJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    init {
        // Observe app lifecycle events
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /**
     * Sets the timeout duration before app is locked.
     * @param seconds Time in seconds before locking
     */
    fun setLockTimeout(seconds: Long) {
        lockTimeoutSeconds = seconds
    }

    /**
     * Called when app comes to foreground.
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // Cancel any pending lock
        cancelLockTimeout()

        // Check if session is still valid
        if (!authenticationManager.isAuthenticated()) {
            _sessionState.value = SessionState.Locked
        } else {
            _sessionState.value = SessionState.Active
        }
    }

    /**
     * Called when app goes to background.
     * Starts the lock timeout.
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (authenticationManager.isAuthenticated()) {
            startLockTimeout()
        }
    }

    /**
     * Marks user as authenticated and session active.
     */
    fun onAuthenticationSuccess() {
        authenticationManager.setAuthenticated()
        _sessionState.value = SessionState.Active
        cancelLockTimeout()
    }

    /**
     * Locks the session and requires re-authentication.
     */
    fun lockSession() {
        authenticationManager.setUnauthenticated()
        _sessionState.value = SessionState.Locked
    }

    /**
     * Checks if session is currently active.
     */
    fun isSessionActive(): Boolean = _sessionState.value is SessionState.Active

    /**
     * Checks if session is locked.
     */
    fun isSessionLocked(): Boolean = _sessionState.value is SessionState.Locked

    private fun startLockTimeout() {
        lockJob = coroutineScope.launch {
            delay(TimeUnit.SECONDS.toMillis(lockTimeoutSeconds))
            lockSession()
        }
    }

    private fun cancelLockTimeout() {
        lockJob?.cancel()
        lockJob = null
    }
}

/**
 * Represents the current state of user session.
 */
sealed class SessionState {
    data object Active : SessionState()
    data object Locked : SessionState()
    data object Unauthenticated : SessionState()
}
