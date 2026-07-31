#!/bin/bash

# App Lock Implementation - Complete Guide

## 📋 Project Structure

The app lock implementation is organized into the following components:

### 1. Authentication Package (`auth/`)
```
com.example.expense.auth/
├── AuthenticationManager.kt    - Core authentication state
├── BiometricHelper.kt          - Biometric prompt and device auth
├── SessionManager.kt           - Session lifecycle and timeouts
└── AuthenticationViewModel.kt  - UI state management
```

### 2. UI Screens (`ui/screens/`)
```
├── SplashScreen.kt             - Initial loading screen
├── AuthenticationScreen.kt      - Main auth UI
└── SettingsScreen.kt          - Settings (future-ready)
```

### 3. Settings (`settings/`)
```
AppLockSettings.kt             - Configuration model for app lock
```

## 🔐 Security Measures Implemented

### FLAG_SECURE Implementation
- Prevents screenshots on all screens
- Blocks display in recent apps preview
- Configured in `MainActivity.onCreate()`

### Session Management
- Tracks app foreground/background state using ProcessLifecycleOwner
- Automatically locks app after 30 seconds in background
- Requires re-authentication when app is reopened after timeout

### Authentication Flow
- Biometric (Fingerprint/Face) as primary method
- Device PIN/Password as fallback
- Device Pattern as additional fallback
- No custom PIN storage - uses system authentication

## 🏗️ Architecture

### MVVM Pattern
```
UI Layer (Compose)
    ↓
ViewModel (AuthenticationViewModel)
    ↓
Business Logic (BiometricHelper, SessionManager)
    ↓
System APIs (BiometricPrompt, ProcessLifecycleOwner)
```

### State Management
- `BiometricState` - Authentication attempt state
- `SessionState` - App session state (Active/Locked/Unauthenticated)
- `BiometricError` - Detailed error information
- `AuthenticationUiState` - UI-specific state

## 🔄 App Launch Flow

1. **App Start**
   - Splash Screen shows briefly
   - Automatic navigation to Authentication Screen

2. **Authentication Screen**
   - Displays app logo and lock icon
   - User taps "Unlock" button
   - Biometric prompt launches

3. **Authentication**
   - Device strongest auth method is used
   - On success: navigates to Home Screen
   - On failure: stays on auth screen for retry
   - On cancel: closes app

4. **Session Management**
   - User interacts with app normally
   - When app goes to background, 30-second countdown starts
   - If app reopens within 30 seconds: no auth required
   - If app reopens after 30 seconds: authentication required again

## 📱 Supported Authentication Methods

The app uses `BiometricManager.Authenticators` flags:
- **BIOMETRIC_STRONG** - Fingerprint/Face with strongest security
- **DEVICE_CREDENTIAL** - PIN/Password/Pattern as fallback

The BiometricPrompt API automatically selects the strongest available method.

## ⚙️ Configuration

### Timeout Duration
To change the lock timeout, modify in `ExpenseApplication.kt`:
```kotlin
sessionManager.setLockTimeout(60) // Change from 30 to 60 seconds
```

### Future Settings Options
All timeout options are already configured in `LockTimeout` enum:
- `IMMEDIATE` (0 seconds)
- `THIRTY_SECONDS` (30 seconds) - Currently active
- `ONE_MINUTE` (60 seconds)
- `FIVE_MINUTES` (300 seconds)

To enable other timeouts:
1. Add UI to Settings Screen
2. Store selection in SharedPreferences/DataStore
3. Pass to `sessionManager.setLockTimeout()`

## 🐛 Error Handling

All biometric errors are handled with user-friendly messages:

| Error | User Message | Action |
|-------|--------------|--------|
| Cancelled | "Authentication cancelled" | Stay on auth screen |
| AuthenticationFailed | "Please try again" | Allow retry |
| NoDeviceCredential | "Device lock not configured" | Show settings button |
| NoBiometrics | "No biometric enrolled" | Show settings button |
| Lockout | "Too many failed attempts" | Wait before retry |
| LockoutPermanent | "Use device PIN/Password" | Fallback to device auth |
| Timeout | "Try again" | Allow retry |

## 🧪 Testing the Implementation

### Test Case 1: Normal Authentication Flow
1. Open app - should show Splash then Authentication screen
2. Tap "Unlock"
3. Complete biometric authentication
4. Should navigate to Home Screen

### Test Case 2: Session Timeout
1. Open app and authenticate
2. Close app (or go to home screen)
3. Wait 30+ seconds
4. Open app again
5. Should show Authentication screen

### Test Case 3: Session Within Timeout
1. Open app and authenticate
2. Close app
3. Reopen within 30 seconds
4. Should skip authentication and show home screen

### Test Case 4: No Biometric Capability
1. On device without fingerprint enrolled
2. Open app
3. Should show security settings dialog
4. User can tap "Open Settings" to configure device lock

### Test Case 5: Error Handling
1. Attempt authentication with wrong biometric multiple times
2. Should show appropriate error message
3. Allow user to retry

## 🔒 Security Checklist

- [x] Screenshots disabled via FLAG_SECURE
- [x] Recent apps preview blocked
- [x] Authentication required on each app open
- [x] Session timeout implemented (30 seconds)
- [x] No passwords/biometrics stored
- [x] Uses system BiometricPrompt API
- [x] Lifecycle-aware session management
- [x] Error handling for all auth scenarios
- [x] Fallback to device PIN/Password/Pattern
- [x] No backstack manipulation after auth

## 📦 Dependencies Added

```gradle
// Biometric Authentication
implementation("androidx.biometric:biometric:1.2.0-alpha05")

// Already in project:
// androidx.lifecycle:lifecycle-runtime-compose:2.8.0
// androidx.lifecycle:lifecycle-process:2.8.0
// androidx.navigation:navigation-compose:2.8.0
```

## 🚀 Building and Running

```bash
# Build the project
./gradlew assembleDebug

# Run on emulator/device
./gradlew installDebug
adb shell am start -n com.example.expense/.MainActivity

# Run with lint checks
./gradlew build
```

## 📝 File Structure

```
app/src/main/
├── AndroidManifest.xml                          # Added BIOMETRIC permission
├── java/com/example/expense/
│   ├── MainActivity.kt                          # Updated with FLAG_SECURE
│   ├── ExpenseApplication.kt                    # Initialize auth components
│   ├── auth/
│   │   ├── AuthenticationManager.kt             # State management
│   │   ├── BiometricHelper.kt                   # BiometricPrompt wrapper
│   │   ├── SessionManager.kt                    # Lifecycle management
│   │   └── AuthenticationViewModel.kt           # UI ViewModel
│   ├── settings/
│   │   └── AppLockSettings.kt                   # Settings configuration
│   └── ui/
│       ├── screens/
│       │   ├── SplashScreen.kt                  # Splash UI
│       │   ├── AuthenticationScreen.kt          # Auth UI
│       │   └── SettingsScreen.kt                # Settings UI (future)
│       └── navigation/
│           └── ExpenseNavigation.kt             # Updated navigation flow
└── AndroidManifest.xml                          # Updated permissions
```

## 🎯 Implementation Highlights

### Single Responsibility Principle
- `BiometricHelper` - Only handles biometric operations
- `SessionManager` - Only manages session lifecycle
- `AuthenticationManager` - Only manages auth state
- `AuthenticationViewModel` - Only manages UI state

### Lifecycle Integration
- Uses `DefaultLifecycleObserver` for lifecycle events
- Integrated with `ProcessLifecycleOwner` for app-wide lifecycle
- Automatic lock timeout on background
- Graceful resume on foreground

### Error Recovery
- User-friendly error messages
- Graceful fallback to device PIN/Password
- Clear action items for each error
- Ability to open security settings for configuration

### Future Extensibility
- Settings screen ready for more options
- All timeout values pre-configured
- Easy to add more biometric scenarios
- Can integrate with fingerprint-specific APIs in future

## ❓ FAQs

**Q: What happens if the user denies biometric permission?**
A: The app shows a settings dialog allowing user to configure device lock in security settings.

**Q: Can users bypass authentication?**
A: No. Exiting the app cancels authentication, and re-opening requires authentication again.

**Q: What if device has no biometric hardware?**
A: App automatically falls back to device PIN/Password/Pattern via DEVICE_CREDENTIAL flag.

**Q: Is the 30-second timeout configurable?**
A: Yes, via `sessionManager.setLockTimeout(seconds)`. Settings UI is ready for future expansion.

**Q: What happens to data if app is killed while running?**
A: Authentication state is cleared immediately on next app open, requiring re-authentication.

**Q: Can I test this on an Android emulator?**
A: Yes! Set up fingerprint on emulator via Settings > Security > Fingerprint, then use adb to simulate touches.

## 📚 References

- [AndroidX Biometric Library](https://developer.android.com/jetpack/androidx/releases/biometric)
- [BiometricPrompt API](https://developer.android.com/reference/androidx/biometric/BiometricPrompt)
- [Process Lifecycle Owner](https://developer.android.com/reference/androidx/lifecycle/ProcessLifecycleOwner)
- [Android Security Best Practices](https://developer.android.com/topic/security)
