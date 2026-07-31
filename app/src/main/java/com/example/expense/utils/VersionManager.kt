package com.example.expense.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.pm.PackageInfoCompat

/**
 * Manages application version tracking and detection.
 * Used to identify when upgrades occur and trigger necessary migrations.
 *
 * Version tracking:
 * - Stores the previously installed version
 * - Compares it with the current version
 * - Allows migration logic to run based on version differences
 */
class VersionManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "app_version_manager",
        Context.MODE_PRIVATE
    )
    private val packageInfo = context.packageManager.getPackageInfo(
        context.packageName,
        0
    )

    companion object {
        private const val KEY_PREVIOUS_VERSION_CODE = "previous_version_code"
        private const val KEY_PREVIOUS_VERSION_NAME = "previous_version_name"
    }

    /**
     * Gets the current application version code.
     * This is used by Android system to determine if an update is available.
     */
    fun getCurrentVersionCode(): Long = PackageInfoCompat.getLongVersionCode(packageInfo)

    /**
     * Gets the current application version name.
     * This is the user-facing version string (e.g., "1.0.0").
     */
    fun getCurrentVersionName(): String = packageInfo.versionName ?: "1.0.0"

    /**
     * Gets the previously installed version code.
     * Returns 0 if this is a fresh installation.
     */
    fun getPreviousVersionCode(): Long = sharedPreferences.getLong(KEY_PREVIOUS_VERSION_CODE, 0)

    /**
     * Gets the previously installed version name.
     * Returns empty string if this is a fresh installation.
     */
    fun getPreviousVersionName(): String = sharedPreferences.getString(KEY_PREVIOUS_VERSION_NAME, "") ?: ""

    /**
     * Checks if the app has been upgraded.
     * Returns true if the current version code is different from the stored version code.
     */
    fun isAppUpgraded(): Boolean {
        val currentVersionCode = getCurrentVersionCode()
        val previousVersionCode = getPreviousVersionCode()
        return previousVersionCode > 0 && currentVersionCode > previousVersionCode
    }

    /**
     * Checks if this is a fresh installation (first launch).
     * Returns true if no previous version has been recorded.
     */
    fun isFreshInstall(): Boolean = getPreviousVersionCode() == 0L

    /**
     * Saves the current version as the "previous version" for future comparisons.
     * Call this after all migrations have completed successfully.
     */
    fun saveCurrentVersion() {
        sharedPreferences.edit().apply {
            putLong(KEY_PREVIOUS_VERSION_CODE, getCurrentVersionCode())
            putString(KEY_PREVIOUS_VERSION_NAME, getCurrentVersionName())
            apply()
        }
    }

    /**
     * Returns a human-readable version comparison string.
     * Useful for debugging and logging.
     */
    fun getVersionInfo(): String {
        val previousVersion = if (getPreviousVersionCode() == 0L) "fresh" else "${getPreviousVersionName()} (${getPreviousVersionCode()})"
        val currentVersion = "${getCurrentVersionName()} (${getCurrentVersionCode()})"
        return "Previous: $previousVersion → Current: $currentVersion"
    }
}
