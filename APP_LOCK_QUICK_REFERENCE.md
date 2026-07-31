# App Lock - Quick Reference & Code Snippets

## File Locations

```
Authentication Components:
├── app/src/main/java/com/example/expense/auth/
│   ├── AuthenticationManager.kt
│   ├── BiometricHelper.kt
│   ├── SessionManager.kt
│   └── AuthenticationViewModel.kt
│
Settings Configuration:
├── app/src/main/java/com/example/expense/settings/
│   └── AppLockSettings.kt
│
UI Screens:
├── app/src/main/java/com/example/expense/ui/screens/
│   ├── SplashScreen.kt
│   ├── AuthenticationScreen.kt
│   └── SettingsScreen.kt
│
Updated Files:
├── app/src/main/java/com/example/expense/
│   ├── MainActivity.kt
│   ├── ExpenseApplication.kt
│   └── ui/navigation/ExpenseNavigation.kt
│
Manifest:
└── app/src/main/AndroidManifest.xml (added permission)
```

## Quick Copy-Paste Code Samples

### 1. Access AuthenticationViewModel (from Composable)
```kotlin
val authViewModel: AuthenticationViewModel = LocalContext.current
    .let { it as Activity }
    .let { (it.application as ExpenseApplication).authenticationViewModel }
```

### 2. Check if User is Authenticated
```kotlin
val isAuthenticated = authViewModel.sessionState
    .collectAsState()
    .value == SessionState.Active
```

### 3. Manual Authentication Trigger
```kotlin
val biometricHelper = BiometricHelper(context)
biometricHelper.authenticate(
    activity = fragmentActivity,
    onSuccess = {
        // User authenticated successfully
        authViewModel.onAuthenticationSuccess()
    },
    onFailure = { error ->
        // Handle error
        Log.e("Auth", error.getUserMessage())
    },
    onCancelled = {
        // User cancelled authentication
    }
)
```

### 4. Programmatic Session Lock
```kotlin
val sessionManager = (context.applicationContext as ExpenseApplication).sessionManager
sessionManager.lockSession()
```

### 5. Change Session Timeout
```kotlin
val sessionManager = (context.applicationContext as ExpenseApplication).sessionManager
sessionManager.setLockTimeout(120) // 120 seconds
```

### 6. Access Biometric Availability
```kotlin
val biometricHelper = (context.applicationContext as ExpenseApplication).biometricHelper
val availability = biometricHelper.canAuthenticate()

when (availability) {
    BiometricAvailability.Available -> {
        // Can use biometric
    }
    BiometricAvailability.NoneEnrolled -> {
        // No fingerprint/face enrolled
    }
    else -> {
        // Not available
    }
}
```

### 7. Open Security Settings Programmatically
```kotlin
val authViewModel = (context.applicationContext as ExpenseApplication).authenticationViewModel
authViewModel.openSecuritySettings(activity)
```

### 8. Observe Authentication State Changes
```kotlin
@Composable
fun MyScreen() {
    val authState by authViewModel.authenticationUiState.collectAsState()
    
    when {
        authState.isLoading -> Text("Authenticating...")
        authState.shouldShowError -> Text(authState.errorMessage)
        authState.canRetry -> Button(onClick = { /* Retry */ })
    }
}
```

### 9. Observe Session State Changes
```kotlin
@Composable
fun MyScreen() {
    val sessionState by authViewModel.sessionState.collectAsState()
    
    when (sessionState) {
        SessionState.Active -> {
            // Show protected content
        }
        SessionState.Locked -> {
            // Show lock screen
        }
        SessionState.Unauthenticated -> {
            // Show auth screen
        }
    }
}
```

### 10. Custom Error Handling
```kotlin
fun displayError(error: BiometricError?) {
    error?.let {
        when (it) {
            BiometricError.Cancelled -> showToast("Authentication cancelled")
            BiometricError.AuthenticationFailed -> showToast("Please try again")
            BiometricError.Lockout -> showToast("Too many attempts. Try later")
            BiometricError.LockoutPermanent -> showToast("Use device PIN/Password")
            else -> showToast(it.getUserMessage())
        }
    }
}
```

## Navigation Reference

### Screen Routes
```kotlin
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Authentication : Screen("authentication")
    data object Home : Screen("home")
    data object AddExpense : Screen("add_expense")
}
```

### Navigate to Home After Auth
```kotlin
navController.navigate(Screen.Home.route) {
    popUpTo(Screen.Authentication.route) { inclusive = true }
}
```

### Navigate to Auth (Session Locked)
```kotlin
navController.navigate(Screen.Authentication.route) {
    popUpTo(Screen.Home.route) { inclusive = true }
}
```

## Biometric Error Messages

```kotlin
enum class BiometricError {
    Cancelled,                  // "Authentication cancelled"
    AuthenticationFailed,       // "Authentication failed. Please try again"
    NoDeviceCredential,        // "Device lock not configured..."
    NoBiometrics,              // "No biometric data enrolled..."
    DeviceNotSupported,        // "Your device does not support biometric auth"
    StorageUnavailable,        // "Device storage is not available"
    Timeout,                   // "Authentication timed out. Please try again"
    UnableToProcess,           // "Unable to process. Please try again"
    Lockout,                   // "Too many failed attempts. Try later"
    LockoutPermanent,          // "Biometric permanently locked. Use PIN/Password"
    HardwareNotPresent,        // "Biometric hardware not found"
    HardwareUnavailable,       // "Biometric hardware is unavailable"
    Unknown                    // "An error occurred. Please try again"
}
```

## Session States

```kotlin
sealed class SessionState {
    data object Active : SessionState()              // User authenticated, app in foreground
    data object Locked : SessionState()              // Session timed out, needs re-auth
    data object Unauthenticated : SessionState()    // Never authenticated or app just started
}
```

## Biometric States

```kotlin
sealed class BiometricState {
    data object Idle : BiometricState()                    // No auth in progress
    data object Authenticating : BiometricState()         // Prompt showing
    data object Success : BiometricState()                // Auth succeeded
    data object Failed : BiometricState()                 // Auth failed (can retry)
    data class Error(val error: BiometricError) : BiometricState()  // Error occurred
}
```

## Biometric Availability

```kotlin
sealed class BiometricAvailability {
    data object Available : BiometricAvailability()              // Can authenticate
    data object NoHardware : BiometricAvailability()            // No hardware
    data object HardwareUnavailable : BiometricAvailability()   // Hardware unavailable
    data object NoneEnrolled : BiometricAvailability()          // Not enrolled
    data object SecurityUpdateRequired : BiometricAvailability() // Update needed
    data object Unknown : BiometricAvailability()               // Unknown state
}
```

## Common Patterns

### Pattern: Wait for Authentication
```kotlin
suspend fun waitForAuthentication(viewModel: AuthenticationViewModel) {
    viewModel.authenticationUiState.collect { state ->
        if (!state.isLoading && state.error == null) {
            // User either authenticated or cancelled
            return@collect
        }
    }
}
```

### Pattern: Retry Logic
```kotlin
@Composable
fun RetryButton(viewModel: AuthenticationViewModel) {
    val uiState by viewModel.authenticationUiState.collectAsState()
    
    Button(
        enabled = uiState.canRetry && !uiState.isLoading,
        onClick = {
            viewModel.resetAuthenticationState()
            // Trigger auth again
        }
    ) {
        Text("Try Again")
    }
}
```

### Pattern: Timeout Listener
```kotlin
@Composable
fun TimeoutListener(viewModel: AuthenticationViewModel) {
    val sessionState by viewModel.sessionState.collectAsState()
    
    LaunchedEffect(sessionState) {
        if (sessionState == SessionState.Locked) {
            // Show notification or log
            Log.w("Session", "Session locked due to timeout")
        }
    }
}
```

### Pattern: Conditional Screen Display
```kotlin
@Composable
fun MainContent(viewModel: AuthenticationViewModel) {
    val sessionState by viewModel.sessionState.collectAsState()
    
    when (sessionState) {
        SessionState.Active -> ProtectedContent()
        SessionState.Locked -> LockedScreen()
        SessionState.Unauthenticated -> LoadingScreen()
    }
}
```

## Debugging Checklist

### Enable Verbose Logging
```kotlin
// In BiometricHelper.kt
private val LOG_TAG = "BiometricAuth"

Log.d(LOG_TAG, "canAuthenticate: ${canAuthenticate()}")
Log.d(LOG_TAG, "authenticate() called")
Log.d(LOG_TAG, "onAuthenticationSucceeded")
Log.w(LOG_TAG, "onAuthenticationError: $errorCode - $errString")
```

### Verify Biometric Configuration
```kotlin
// In debug activity
val biometricManager = BiometricManager.from(context)
val canAuthenticate = biometricManager.canAuthenticate(
    BiometricManager.Authenticators.BIOMETRIC_STRONG or
    BiometricManager.Authenticators.DEVICE_CREDENTIAL
)
Log.d("Biometric", "Can authenticate: $canAuthenticate")
```

### Check Session State
```kotlin
// In debug activity
val sessionState = sessionManager.isSessionActive()
Log.d("Session", "Active: $sessionState, Locked: ${sessionManager.isSessionLocked()}")
```

### Verify FLAG_SECURE
```kotlin
// In MainActivity.onCreate()
val flags = window.attributes.flags
val isSecure = (flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
Log.d("Security", "FLAG_SECURE set: $isSecure")
```

## Gradle Build Commands

### Build APK
```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

### Install and Run
```bash
./gradlew installDebug
adb shell am start -n com.example.expense/.MainActivity
```

### Run Specific Test
```bash
./gradlew testDebugUnitTest
```

### Clean Build
```bash
./gradlew clean assembleDebug
```

### Build with Verbose Output
```bash
./gradlew build -v
```

## Emulator Commands for Testing

### Simulate Fingerprint Authentication
```bash
adb shell cmd fingerprint simulate
```

### Simulate Fingerprint Failure
```bash
adb shell cmd fingerprint simulate --failure
```

### Check Enrolled Fingerprints
```bash
adb shell settings get secure fingerprint_state
```

### Set Device PIN (for testing)
```bash
# Use Android Settings UI in emulator to set PIN
# Or via adb: Requires device unlock
```

## Production Checklist

Before deploying to production:

- [ ] Test on at least 3 physical Android devices
- [ ] Verify biometric/fingerprint works on each
- [ ] Test session timeout manually
- [ ] Verify FLAG_SECURE prevents screenshots
- [ ] Check recent apps preview is blank
- [ ] Test all error scenarios
- [ ] Verify user-friendly error messages
- [ ] Test settings navigation
- [ ] Performance test under load
- [ ] Security audit complete
- [ ] Privacy policy updated
- [ ] Terms of service updated
- [ ] Build signed APK
- [ ] Upload to Google Play

## Performance Optimization Tips

### If App Load Time is High
- Lazy initialize biometric helper
- Use viewModelScope for coroutines
- Cache availability check result

### If Session Timeout is Inconsistent
- Verify ProcessLifecycleOwner integration
- Check for background services interfering
- Ensure no manual lifecycle manipulation

### If Battery Drain
- Session manager uses minimal resources
- Verify no excessive logging
- Check for leak in coroutine scope

### If UI Lag During Auth
- Auth UI is on main thread (intended)
- Don't add heavy operations in callbacks
- Use suspend functions properly

## Helpful References

### Official Android Documentation
- https://developer.android.com/jetpack/androidx/releases/biometric
- https://developer.android.com/reference/androidx/biometric/BiometricPrompt
- https://developer.android.com/topic/security

### Material Design 3
- https://m3.material.io/components/buttons
- https://m3.material.io/foundations/layout

### Kotlin & Coroutines
- https://kotlinlang.org/docs/coroutines-overview.html
- https://developer.android.com/jetpack/androidx/releases/lifecycle

## Support & Troubleshooting

For issues or questions:
1. Check APP_LOCK_DEVELOPER_GUIDE.md
2. Review APP_LOCK_ARCHITECTURE.md
3. Run `./gradlew compileDebugKotlin` to verify compilation
4. Check logs: `adb logcat | grep BiometricAuth`
5. Enable verbose mode in code
6. Test on actual device (not emulator)
