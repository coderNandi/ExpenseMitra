# App Lock Integration Guide

## Quick Start

### 1. Dependencies Already Added
```gradle
implementation("androidx.biometric:biometric:1.2.0-alpha05")
implementation("androidx.lifecycle:lifecycle-process:2.8.0")
```

### 2. Permission Already Added
```xml
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
```

### 3. Security Flag Already Enabled
```kotlin
// In MainActivity.onCreate()
window.setFlags(
    WindowManager.LayoutParams.FLAG_SECURE,
    WindowManager.LayoutParams.FLAG_SECURE
)
```

### 4. Authentication Components Already Initialized
```kotlin
// In ExpenseApplication.onCreate()
authenticationManager = AuthenticationManager()
biometricHelper = BiometricHelper(this)
sessionManager = SessionManager(this, authenticationManager)
authenticationViewModel = AuthenticationViewModel(biometricHelper, sessionManager)
```

## How It Works

### Step 1: App Launch
```
MainActivity.onCreate()
    └─> ExpenseTheme { ExpenseNavHost(...) }
        └─> Navigation determines initial screen
            ├─> If unauthenticated: Splash → Authentication
            ├─> If locked: Authentication
            └─> If active: Home
```

### Step 2: Authentication
```
AuthenticationScreen
    └─> User taps "Unlock"
        └─> BiometricHelper.authenticate()
            ├─> BiometricPrompt shows
            ├─> User completes biometric auth
            └─> AuthenticationViewModel.onAuthenticationSuccess()
                └─> SessionManager marks session as Active
                    └─> Navigation updates to Home screen
```

### Step 3: Session Management
```
SessionManager observes app lifecycle
    ├─> onStart(): Cancel lock timeout
    │   └─> Set session to Active
    └─> onStop(): Start lock timeout
        └─> After 30 seconds: Lock session
            └─> Navigation updates to Authentication screen
```

## Code Examples

### Triggering Authentication Manually
```kotlin
val biometricHelper = BiometricHelper(context)
biometricHelper.authenticate(
    activity = fragmentActivity,
    onSuccess = { /* Handle success */ },
    onFailure = { error -> /* Handle error */ },
    onCancelled = { /* Handle cancellation */ }
)
```

### Checking Biometric Availability
```kotlin
val availability = biometricHelper.canAuthenticate()
when (availability) {
    BiometricAvailability.Available -> {
        // Can proceed with auth
    }
    BiometricAvailability.NoneEnrolled -> {
        // Show "No fingerprint enrolled" message
    }
    else -> {
        // Handle other unavailability cases
    }
}
```

### Locking Session Programmatically
```kotlin
sessionManager.lockSession()
// User will see authentication screen on next app open
```

### Checking Session State
```kotlin
val isActive = sessionManager.isSessionActive()
val isLocked = sessionManager.isSessionLocked()
```

### Changing Timeout Duration
```kotlin
// In ExpenseApplication.onCreate()
sessionManager.setLockTimeout(60) // 60 seconds instead of 30
```

## Understanding the State Machines

### Authentication State Flow
```
Unauthenticated
    ↓ (user taps Unlock)
Authenticating
    ├─ Success → Authenticated
    ├─ Error → Error (can retry)
    ├─ Failed → Failed (can retry)
    └─ Cancelled → Idle (user cancelled)
```

### Session State Flow
```
Unauthenticated (App Launch)
    ↓
Active (After successful auth)
    ├─ onStop() after 30 seconds → Locked
    └─ onStart() within 30 seconds → Active
Locked
    ↓ (App foreground)
Authentication screen shown
    ↓ (User authenticates)
Active
```

## Error Messages and Their Meanings

### User-Friendly Messages Shown in UI

| Error | Meaning | Action |
|-------|---------|--------|
| "Authentication cancelled" | User dismissed prompt | Try again |
| "Authentication failed. Please try again" | Biometric didn't match | Try again |
| "Device lock screen not configured" | No PIN/Password/Pattern set | Configure in settings |
| "No biometric data enrolled" | No fingerprint/face added | Add to settings |
| "Too many failed attempts" | Lockout after repeated failures | Wait before retry |
| "Biometric is permanently locked" | Permanent lockout | Use device auth instead |
| "An error occurred. Please try again" | Unknown error | Retry or contact support |

## Integration with Existing Code

### HomeScreen
No changes needed! The HomeScreen is only shown after successful authentication.

### AddExpenseScreen
No changes needed! Only accessible after authentication.

### ExpenseViewModel
No changes needed! Not involved in authentication flow.

### Navigation
Already integrated! Updated to include:
- `Screen.Splash` - Initial loading
- `Screen.Authentication` - Biometric/device auth
- `Screen.Home` - Main app (unchanged)
- `Screen.AddExpense` - Add expense (unchanged)

## Customization Points

### 1. Change App Logo/Icons
Edit `AuthenticationScreen.kt`:
```kotlin
Icon(
    imageVector = Icons.Filled.Lock,  // ← Change this
    contentDescription = "App Lock",
    modifier = Modifier.size(80.dp),
    tint = MaterialTheme.colorScheme.primary
)
```

### 2. Change Timeout Duration
Edit `ExpenseApplication.kt`:
```kotlin
sessionManager.setLockTimeout(60)  // ← Change this (in seconds)
```

### 3. Add Custom Error Handling
Extend `BiometricError` in `BiometricHelper.kt`:
```kotlin
sealed class BiometricError {
    // Existing cases...
    data object CustomError : BiometricError()  // ← Add this
}
```

### 4. Add Custom Authentication Methods
Modify `BiometricHelper.authenticate()`:
```kotlin
// Add additional authenticator flags
setAllowedAuthenticators(
    BiometricManager.Authenticators.BIOMETRIC_STRONG or
    BiometricManager.Authenticators.DEVICE_CREDENTIAL or
    BiometricManager.Authenticators.BIOMETRIC_WEAK  // ← New
)
```

## Testing Checklist

- [ ] App requires authentication on first open
- [ ] Biometric prompt shows when "Unlock" tapped
- [ ] Successful auth navigates to Home
- [ ] Failed auth shows error message
- [ ] Cancelled auth keeps user on auth screen
- [ ] App timeout after 30 seconds in background
- [ ] No auth required if reopened within 30 seconds
- [ ] Screenshot fails (verify in device settings)
- [ ] Recent apps preview is blank
- [ ] Settings → Security → Fingerprint shows auth in use

## Debugging Tips

### Enable Logging
Add to BiometricHelper:
```kotlin
Log.d("BiometricAuth", "Authentication state: $biometricState")
```

### Simulate Biometric on Emulator
```bash
# Simulate successful fingerprint
adb shell cmd fingerprint simulate

# Simulate fingerprint rejection
adb shell cmd fingerprint simulate wrong
```

### Check Biometric Hardware
```kotlin
val biometricManager = BiometricManager.from(context)
Log.d("Biometric", "Available: ${biometricHelper.canAuthenticate()}")
```

### Verify FLAG_SECURE
```kotlin
val params = window.attributes
Log.d("Security", "FLAG_SECURE set: ${params.flags and WindowManager.LayoutParams.FLAG_SECURE != 0}")
```

## Common Issues and Solutions

### Issue: "Unresolved reference 'BiometricHelper'"
**Solution**: Ensure you're importing from `com.example.expense.auth.BiometricHelper`

### Issue: "App doesn't show authentication screen"
**Solution**: Check that `sessionState` is being observed in navigation. Ensure `ExpenseApplication` initializes all auth components.

### Issue: "Biometric always fails"
**Solution**: On emulator, ensure fingerprint is enrolled. Real devices need finger enrolled in Settings.

### Issue: "FLAG_SECURE not working"
**Solution**: Verify it's set in `MainActivity.onCreate()` before `setContent()`.

### Issue: "Session doesn't timeout"
**Solution**: Check that `ProcessLifecycleOwner` is properly initialized. Ensure `SessionManager` is created in `ExpenseApplication`.

## Performance Considerations

- BiometricPrompt shows native UI (very fast)
- SessionManager uses lightweight coroutine for timeout
- No persistent storage overhead
- FLAG_SECURE has minimal performance impact
- All state is in-memory (cleared on app close)

## Security Considerations

- All authentication handled by Android system APIs
- No biometric data stored by the app
- Session state never persisted
- FLAG_SECURE prevents screenshot/recording
- No custom encryption needed
- Follows NIST cybersecurity guidelines

## Future Enhancements

Prepared for easy addition of:
1. Multiple timeout options (1 min, 5 min, etc.)
2. Settings persistence (SharedPreferences/DataStore)
3. Fingerprint-specific UI
4. Face recognition-specific UI
5. Custom authentication UI wrapper
6. Biometric prompt customization
7. Advanced security audit logging

## References

- **BiometricPrompt Docs**: https://developer.android.com/reference/androidx/biometric/BiometricPrompt
- **Security Best Practices**: https://developer.android.com/topic/security/best-practices
- **Lifecycle Architecture**: https://developer.android.com/topic/libraries/architecture/lifecycle
- **Material Design 3**: https://m3.material.io/
