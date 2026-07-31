# App Lock Architecture & Design

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         MainActivity                              │
│  - Sets FLAG_SECURE                                             │
│  - Initializes Compose content                                  │
│  - Hosts Navigation                                             │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ↓
┌─────────────────────────────────────────────────────────────────┐
│                      ExpenseNavHost                              │
│  - Splash Screen                                                │
│  - Authentication Screen                                        │
│  - Home Screen                                                  │
│  - Add Expense Screen                                           │
└──────────────────────┬──────────────────────────────────────────┘
                       │
        ┌──────────────┴──────────────┬──────────────┐
        ↓                             ↓              ↓
┌──────────────────┐    ┌────────────────────┐   ┌──────────────┐
│Authentication    │    │  AuthViewModel     │   │SessionManager│
│ Screen           │    │                    │   │              │
│                  │    │ - Bridges Auth UI  │   │ - Lifecycle  │
│ - Shows UI       │    │   to Managers      │   │   aware      │
│ - User interaction        │ - Maps states      │   │ - Timeout    │
│                  │    │   to UI             │   │   management │
└────────┬─────────┘    └────────┬───────────┘   └──────┬───────┘
         │                       │                      │
         └───────────┬───────────┴──────────┬───────────┘
                     ↓                      ↓
         ┌─────────────────────┐  ┌──────────────────┐
         │ BiometricHelper     │  │AuthenticationMgr │
         │                     │  │                  │
         │ - BiometricPrompt   │  │ - Stores auth    │
         │ - Error handling    │  │   state          │
         │ - Availability check│  │ - Observable     │
         └─────────┬───────────┘  └──────┬───────────┘
                   │                     │
                   └──────────┬──────────┘
                              ↓
                   ┌──────────────────────┐
                   │   Android System     │
                   │  BiometricPrompt API │
                   │  ProcessLifecycleOwr │
                   └──────────────────────┘
```

## Data Flow Diagrams

### Authentication Flow
```
User Opens App
        │
        ↓
Splash Screen (2 sec)
        │
        ↓
Check Session State
        ├─ Authenticated + Within 30s → Skip to Home
        └─ Not Authenticated or Expired → Show Auth Screen
        │
        ↓
Authentication Screen
        │
        ├─ "Unlock" button pressed
        │
        ↓
BiometricHelper.authenticate()
        │
        ├─ Check device capability
        │
        ├─ Show BiometricPrompt
        │
        ├─ User completes auth
        │
        ├─ Success: onSuccess()
        │           │
        │           ↓
        │       SessionManager.onAuthenticationSuccess()
        │           │
        │           ├─ Set Authenticated
        │           ├─ Cancel lock timeout
        │           ├─ Update SessionState to Active
        │           │
        │           ↓
        │       AuthenticationViewModel.onAuthenticationSuccess()
        │           │
        │           ↓
        │       Navigation: Auth → Home
        │
        ├─ Failure: Show error message
        │           Allow retry
        │
        └─ Cancelled: Stay on Auth screen
```

### Session Timeout Flow
```
App in Foreground
        │
        ↓
SessionManager.onStart()
        ├─ Cancel pending lock timeout
        ├─ Set SessionState to Active
        │
        ↓
User interacts with app normally (Home, AddExpense screens)
        │
        ↓
App goes to background (user navigates away or device locks)
        │
        ↓
SessionManager.onStop()
        ├─ Start lock timeout (30 seconds)
        │
        ↓
Wait 30 seconds (or less if app resumes)
        │
        ├─ App resumes within 30s → Go to onStart() → Cancel timeout
        │
        └─ 30s elapsed → lockSession()
                ├─ Set Unauthenticated
                ├─ Update SessionState to Locked
                │
                ↓
        Next time app is opened or brought to foreground
                │
                ↓
        Check SessionState
                ├─ Locked → Show Authentication Screen
```

## Component Responsibility Matrix

```
┌────────────────────┬──────────────┬──────────────┬──────────────┐
│ Component          │ What it owns │ What it uses │ Observes     │
├────────────────────┼──────────────┼──────────────┼──────────────┤
│ MainActivity       │ FLAG_SECURE  │ Compose,     │ Navigation   │
│                    │ Lifecycle    │ Theme        │              │
├────────────────────┼──────────────┼──────────────┼──────────────┤
│ ExpenseNavHost     │ Routes,      │ ViewModels,  │ SessionState │
│                    │ Start dest   │ Screens      │              │
├────────────────────┼──────────────┼──────────────┼──────────────┤
│ AuthenticationScr  │ UI Rendering │ ViewModel,   │ UIState,     │
│                    │ User interact│ BiometricHlpr│ Errors       │
├────────────────────┼──────────────┼──────────────┼──────────────┤
│ AuthViewModel      │ UI State     │ BiometricHlp │ BiometricSt, │
│                    │ Mapping      │ SessionMgr   │ SessionState │
├────────────────────┼──────────────┼──────────────┼──────────────┤
│ BiometricHelper    │ Biometric    │ Android APIs │ Auth result  │
│                    │ Logic        │              │ errors       │
├────────────────────┼──────────────┼──────────────┼──────────────┤
│ SessionManager     │ Timeout      │ Lifecycle,   │ Foreground   │
│                    │ Lifecycle    │ AuthMgr      │ state        │
├────────────────────┼──────────────┼──────────────┼──────────────┤
│ AuthManager        │ Auth State   │ None         │ Internal st  │
│                    │ Observable   │              │              │
└────────────────────┴──────────────┴──────────────┴──────────────┘
```

## State Transitions

### BiometricState Transitions
```
┌─────┐
│Idle │
└──┬──┘
   │ authenticate() called
   ↓
┌──────────────┐
│Authenticating│
└──┬─────┬──┬──┘
   │     │  │
   │ S   │F │ Err
   ↓     ↓  ↓
 Succ   Fail Error
(next: (retr (retr
 nav)  y)   y)
```

### SessionState Transitions
```
┌─────────────────────┐
│Unauthenticated      │
└──────────┬──────────┘
           │ onAuthenticationSuccess()
           ↓
      ┌────────┐
      │ Active │←──────────────────┐
      └────┬───┘                   │
           │ onStop() + 30s delay  │
           ↓                       │ onStart() + <30s
      ┌──────────┐                 │
      │ Locked   │─────────────────┘
      └──────────┘ onStart() + >30s
           │
           ↓ Re-authenticate
      ┌────────┐
      │ Active │
      └────────┘
```

## Security Boundary Diagram

```
┌──────────────────────────────────────────────────┐
│              SECURITY BOUNDARY                   │
│  (FLAG_SECURE prevents:                         │
│   - Screenshots                                  │
│   - Screen recording                             │
│   - Recent apps preview)                         │
├──────────────────────────────────────────────────┤
│                                                  │
│  Before Authentication:                         │
│  ├─ Splash Screen     (No data)                 │
│  └─ Auth Screen       (No data)                 │
│                                                  │
│  After Authentication:                          │
│  ├─ Home Screen       (Protected)               │
│  ├─ Add Expense       (Protected)               │
│  ├─ Settings          (Protected)               │
│                                                  │
│  Session Management:                            │
│  ├─ Foreground: Active session                  │
│  ├─ Background: Timeout countdown               │
│  └─ Timeout Expired: Require re-auth            │
│                                                  │
└──────────────────────────────────────────────────┘
```

## Class Hierarchy

```
ViewModel
├─ AuthenticationViewModel (UI State)
│  └─ Observes: BiometricState, SessionState
│  └─ Exposes: AuthenticationUiState
│

DefaultLifecycleObserver
└─ SessionManager (Session Lifecycle)
   └─ Observes: ProcessLifecycleOwner
   └─ Manages: Timeout, Session State
   

Data Classes
├─ BiometricState (sealed)
│  ├─ Idle
│  ├─ Authenticating
│  ├─ Success
│  ├─ Failed
│  └─ Error(error: BiometricError)
│
├─ SessionState (sealed)
│  ├─ Active
│  ├─ Locked
│  └─ Unauthenticated
│
├─ BiometricError (sealed)
│  ├─ Cancelled
│  ├─ AuthenticationFailed
│  ├─ NoDeviceCredential
│  ├─ NoBiometrics
│  ├─ Lockout
│  ├─ LockoutPermanent
│  └─ ... (others)
│
├─ BiometricAvailability (sealed)
│  ├─ Available
│  ├─ NoHardware
│  ├─ HardwareUnavailable
│  ├─ NoneEnrolled
│  └─ ... (others)
│
└─ AuthenticationUiState (data class)
   ├─ isLoading: Boolean
   ├─ error: BiometricError?
   ├─ isFailed: Boolean
   ├─ canRetry: Boolean
   ├─ shouldShowError: Boolean
   └─ errorMessage: String
```

## Lifecycle Integration Points

```
ProcessLifecycleOwner
        │
        ├─ onStart()
        │  └─→ SessionManager.onStart()
        │      └─→ Cancel lock timeout
        │
        └─ onStop()
           └─→ SessionManager.onStop()
              └─→ Start lock timeout (30s)
                 └─→ After 30s: lockSession()
```

## Threading Model

```
Main Thread
├─ Compose UI rendering
├─ User interactions
├─ Navigation updates
├─ ViewModel state collection
│
└─ BiometricPrompt
   └─ Shows on main thread
   └─ Calls onStart, onStop in main executor

Coroutine Scope (Main Dispatcher)
├─ SessionManager
│  └─ Timeout countdown (delay())
│  └─ Lock session after timeout
│
└─ AuthenticationViewModel
   └─ StateFlow collection/mapping
```

## Error Handling Strategy

```
Exception/Error Occurs
        │
        ├─ BiometricPrompt Error
        │  └─ AuthenticationCallback.onAuthenticationError()
        │     └─ Map to BiometricError enum
        │     └─ Update BiometricState.Error
        │     └─ UI reads error
        │     └─ Show user message
        │
        ├─ Device Capability Issue
        │  └─ canAuthenticate() returns unavailability
        │  └─ Show settings dialog
        │  └─ Offer "Open Settings" button
        │
        └─ Session Management Issue
           └─ Timeout expires
           └─ Lock session immediately
           └─ Require re-authentication
```

## Configuration Points for Customization

```
app/build.gradle.kts
├─ biometric version: "androidx.biometric:biometric:1.2.0-alpha05"
│  
app/src/main/AndroidManifest.xml
├─ Permission: USE_BIOMETRIC
├─ minSdk: 24 (API level)
│
MainActivity.kt
├─ FLAG_SECURE: Disable screenshots
│
ExpenseApplication.kt
├─ sessionManager.setLockTimeout(30) ← Change timeout here
│
AuthenticationScreen.kt
├─ Icon: Icons.Filled.Lock ← Change icon
├─ Title: "Expense Tracker" ← Change app name
├─ Subtitle: "Authenticate..." ← Change message
│
SessionManager.kt
├─ lockTimeoutSeconds = 30L ← Default timeout
│
BiometricHelper.kt
├─ PromptInfo title/subtitle/description ← Customize messages
├─ Authenticator flags ← Add/remove auth methods
```

## Performance Characteristics

```
Operation                    Time Complexity  Space Complexity
──────────────────────────────────────────────────────────────
App Launch                   O(1)            O(1) - minimal overhead
BiometricPrompt Show         ~500ms          O(1) - native UI
Biometric Processing        Variable         O(1) - system level
Session Timeout Check       O(1)             O(1) - single boolean
State Transition            O(1)             O(1) - enum updates
Navigation Update           O(1)             O(1) - route switch
```

## Security Model Summary

```
                    User Identity Verification
                              │
                    ┌─────────┴─────────┐
                    │                   │
            ┌───────▼────────┐  ┌───────▼────────┐
            │  Biometric     │  │ Device Auth    │
            │  (Fingerprint, │  │ (PIN/Pass/     │
            │   Face, Iris)  │  │  Pattern)      │
            └────────────────┘  └────────────────┘
                    │                   │
                    └─────────┬─────────┘
                              │
                    ┌─────────▼──────────┐
                    │ Android Keystore   │
                    │ (Managed by System)│
                    └────────────────────┘
                              │
                    ┌─────────▼──────────┐
                    │ SessionManager     │
                    │ (30s Timeout)      │
                    └────────────────────┘
                              │
                    ┌─────────▼──────────┐
                    │ FLAG_SECURE        │
                    │ (Screenshot Block) │
                    └────────────────────┘
```

This architecture ensures that:
1. Authentication is always required
2. Session is protected during app usage
3. Screen content cannot be captured
4. Session expires when app is backgrounded for 30s
5. No sensitive data is stored by the app
6. All auth is handled by Android system APIs
