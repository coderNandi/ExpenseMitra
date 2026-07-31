package com.example.expense.settings

/**
 * Configuration for app lock settings.
 * Future-proof design for easy addition of more timeout options.
 */
enum class LockTimeout(val displayName: String, val seconds: Long) {
    IMMEDIATE("Lock Immediately", 0),
    THIRTY_SECONDS("Lock After 30 Seconds", 30),
    ONE_MINUTE("Lock After 1 Minute", 60),
    FIVE_MINUTES("Lock After 5 Minutes", 300);

    companion object {
        val DEFAULT = THIRTY_SECONDS
    }
}

/**
 * App Lock settings model.
 * Currently only Lock After 30 Seconds is implemented.
 * Other options are ready for future implementation.
 */
data class AppLockSettings(
    val isEnabled: Boolean = true,
    val lockTimeout: LockTimeout = LockTimeout.DEFAULT
)
