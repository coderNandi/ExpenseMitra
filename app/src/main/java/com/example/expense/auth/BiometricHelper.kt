package com.example.expense.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Handles biometric authentication using AndroidX BiometricPrompt API.
 * Manages BiometricPrompt lifecycle and provides secure authentication.
 */
class BiometricHelper(private val context: Context) {
    private val _biometricState = MutableStateFlow<BiometricState>(BiometricState.Idle)
    val biometricState: StateFlow<BiometricState> = _biometricState.asStateFlow()

    /**
     * Checks device biometric capabilities and system authentication availability.
     */
    fun canAuthenticate(): BiometricAvailability {
        val biometricManager = BiometricManager.from(context)
        return when (val result = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.Available
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NoHardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HardwareUnavailable
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NoneEnrolled
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAvailability.SecurityUpdateRequired
            else -> BiometricAvailability.Unknown
        }
    }

    /**
     * Initiates biometric authentication prompt.
     * Uses the strongest available authentication method on the device.
     */
    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFailure: (BiometricError) -> Unit,
        onCancelled: () -> Unit
    ) {
        if (canAuthenticate() == BiometricAvailability.Available) {
            val executor = activity.mainExecutor
            val biometricPrompt = BiometricPrompt(activity, executor, AuthenticationCallback(
                onSuccess = onSuccess,
                onFailure = onFailure,
                onCancelled = onCancelled,
                stateUpdater = { newState -> _biometricState.value = newState }
            ))

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Expense Tracker")
                .setSubtitle("Authenticate to access your expenses")
                .setDescription("Use your biometric credential or device lock screen authentication")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()

            _biometricState.value = BiometricState.Authenticating
            biometricPrompt.authenticate(promptInfo)
        } else {
            // Device doesn't have biometric or system authentication
            _biometricState.value = BiometricState.Error(BiometricError.DeviceNotSupported)
        }
    }

    /**
     * Opens device security settings for user to configure authentication method.
     */
    fun openSecuritySettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
        activity.startActivity(intent)
    }

    /**
     * Resets biometric state to idle.
     */
    fun resetState() {
        _biometricState.value = BiometricState.Idle
    }

    /**
     * Internal callback handling biometric authentication results.
     */
    private inner class AuthenticationCallback(
        private val onSuccess: () -> Unit,
        private val onFailure: (BiometricError) -> Unit,
        private val onCancelled: () -> Unit,
        private val stateUpdater: (BiometricState) -> Unit
    ) : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            stateUpdater(BiometricState.Success)
            onSuccess()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            val error = when (errorCode) {
                BiometricPrompt.ERROR_CANCELED -> BiometricError.Cancelled
                BiometricPrompt.ERROR_USER_CANCELED -> BiometricError.Cancelled
                BiometricPrompt.ERROR_NEGATIVE_BUTTON -> BiometricError.Cancelled
                BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> BiometricError.NoDeviceCredential
                BiometricPrompt.ERROR_NO_BIOMETRICS -> BiometricError.NoBiometrics
                BiometricPrompt.ERROR_NO_SPACE -> BiometricError.StorageUnavailable
                BiometricPrompt.ERROR_TIMEOUT -> BiometricError.Timeout
                BiometricPrompt.ERROR_UNABLE_TO_PROCESS -> BiometricError.UnableToProcess
                BiometricPrompt.ERROR_LOCKOUT -> BiometricError.Lockout
                BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> BiometricError.LockoutPermanent
                BiometricPrompt.ERROR_HW_NOT_PRESENT -> BiometricError.HardwareNotPresent
                BiometricPrompt.ERROR_HW_UNAVAILABLE -> BiometricError.HardwareUnavailable
                else -> BiometricError.Unknown
            }
            stateUpdater(BiometricState.Error(error))
            onFailure(error)
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            stateUpdater(BiometricState.Failed)
            onFailure(BiometricError.AuthenticationFailed)
        }
    }
}

/**
 * Represents availability of biometric authentication on the device.
 */
sealed class BiometricAvailability {
    data object Available : BiometricAvailability()
    data object NoHardware : BiometricAvailability()
    data object HardwareUnavailable : BiometricAvailability()
    data object NoneEnrolled : BiometricAvailability()
    data object SecurityUpdateRequired : BiometricAvailability()
    data object Unknown : BiometricAvailability()

    fun isAvailable(): Boolean = this == Available
}

/**
 * Represents the state of an ongoing biometric authentication attempt.
 */
sealed class BiometricState {
    data object Idle : BiometricState()
    data object Authenticating : BiometricState()
    data object Success : BiometricState()
    data object Failed : BiometricState()
    data class Error(val error: BiometricError) : BiometricState()
}

/**
 * Represents specific biometric authentication errors with user-friendly messages.
 */
sealed class BiometricError {
    data object Cancelled : BiometricError()
    data object AuthenticationFailed : BiometricError()
    data object NoDeviceCredential : BiometricError()
    data object NoBiometrics : BiometricError()
    data object DeviceNotSupported : BiometricError()
    data object StorageUnavailable : BiometricError()
    data object Timeout : BiometricError()
    data object UnableToProcess : BiometricError()
    data object Lockout : BiometricError()
    data object LockoutPermanent : BiometricError()
    data object HardwareNotPresent : BiometricError()
    data object HardwareUnavailable : BiometricError()
    data object Unknown : BiometricError()

    /**
     * Returns user-friendly error message.
     */
    fun getUserMessage(): String = when (this) {
        is Cancelled -> "Authentication cancelled"
        is AuthenticationFailed -> "Authentication failed. Please try again"
        is NoDeviceCredential -> "Device lock screen not configured. Please enable it in settings"
        is NoBiometrics -> "No biometric data enrolled. Please add a fingerprint or face"
        is DeviceNotSupported -> "Your device does not support biometric authentication"
        is StorageUnavailable -> "Device storage is not available"
        is Timeout -> "Authentication timed out. Please try again"
        is UnableToProcess -> "Unable to process your request. Please try again"
        is Lockout -> "Too many failed attempts. Please try again later"
        is LockoutPermanent -> "Biometric is permanently locked. Use device PIN/Password"
        is HardwareNotPresent -> "Biometric hardware not found"
        is HardwareUnavailable -> "Biometric hardware is not available"
        is Unknown -> "An error occurred. Please try again"
    }
}
