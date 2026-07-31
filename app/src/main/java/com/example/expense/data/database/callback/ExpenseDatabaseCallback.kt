package com.example.expense.data.database.callback

import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.expense.utils.VersionManager

private const val TAG = "DatabaseCallback"

/**
 * Callback for database lifecycle events.
 *
 * This is useful for:
 * - Logging database events
 * - Performing data validation after migrations
 * - Running integrity checks
 * - Cleaning up temporary data
 *
 * In the future, this could be extended to:
 * - Trigger cloud sync after successful migration
 * - Update analytics with migration events
 * - Perform automated backups
 */
class ExpenseDatabaseCallback(private val context: Context) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        Log.d(TAG, "Database created for first time")
        // Perform any initialization tasks for fresh installations
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        Log.d(TAG, "Database opened")

        // Verify database integrity after migrations
        verifyDatabaseIntegrity(db)

        // Log version information for debugging
        val versionManager = VersionManager(context)
        Log.i(TAG, "Database open. ${versionManager.getVersionInfo()}")
    }

    /**
     * Performs basic integrity checks to ensure migration was successful.
     */
    private fun verifyDatabaseIntegrity(db: SupportSQLiteDatabase) {
        try {
            // Check that the expenses table exists
            val cursor = db.query("SELECT COUNT(*) FROM expenses")
            if (cursor.moveToFirst()) {
                val count = cursor.getInt(0)
                Log.d(TAG, "Database integrity check passed. Expense records: $count")
            }
            cursor.close()
        } catch (e: Exception) {
            Log.w(TAG, "Database integrity check warning: ${e.message}")
            // Don't throw - just log. The database might be in a recoverable state.
        }
    }
}
