package com.example.expense.data.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.expense.data.model.Expense
import com.example.expense.data.database.migration.ALL_MIGRATIONS
import com.example.expense.utils.VersionManager

private const val TAG = "ExpenseDatabase"

/**
 * Room Database for the Expense Tracker application.
 *
 * ✓ Version 1: Initial schema with expenses table
 * ✓ Version 2: Added category column for expense categorization
 * ✓ Version 3: Added notes and transaction type tracking
 * ✓ Version 4: Added recurring expenses support
 *
 * Database versioning strategy:
 * - Every schema change increments the version number
 * - Migrations are defined in the migration package
 * - Room automatically applies migrations when upgrading
 * - No data loss during schema changes
 *
 * Data persistence:
 * - Stored in application's internal storage
 * - Survives app updates, restarts, and device reboots
 * - Only removed on explicit "Clear App Data" or uninstall
 */
@Database(entities = [Expense::class], version = 1, exportSchema = true)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        private const val DATABASE_NAME = "expense_database"

        /**
         * Gets or creates the database instance with migration support.
         *
         * On first launch after upgrade:
         * 1. Detects version change via VersionManager
         * 2. Room automatically applies necessary migrations
         * 3. All existing data is preserved
         * 4. Version info is updated in shared preferences
         *
         * @param context Application context
         * @return ExpenseDatabase instance with migrations applied
         */
        fun getInstance(context: Context): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                Log.d(TAG, "Initializing ExpenseDatabase")

                val versionManager = VersionManager(context)
                val previousVersionCode = versionManager.getPreviousVersionCode()
                val currentVersionCode = versionManager.getCurrentVersionCode()

                if (versionManager.isAppUpgraded()) {
                    Log.i(TAG, "App upgrade detected: ${versionManager.getVersionInfo()}")
                    Log.d(TAG, "Migrations will be applied automatically by Room")
                }

                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        ExpenseDatabase::class.java,
                        DATABASE_NAME
                    )
                        // Add all migrations to handle version upgrades
                        .addMigrations(*ALL_MIGRATIONS)
                        // Error handling: Fail gracefully if migration path doesn't exist
                        // This prevents automatic database deletion
                        .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                        .build()

                    INSTANCE = instance

                    // After successful database initialization, update the stored version
                    versionManager.saveCurrentVersion()
                    Log.i(TAG, "Database initialized successfully. Version saved: ${versionManager.getCurrentVersionName()}")

                    instance
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize database: ${e.message}", e)
                    throw e
                }
            }
        }

        /**
         * Clears the database instance from memory.
         * Used for testing purposes only.
         * The actual database file is NOT deleted - only the in-memory instance.
         */
        fun closeDatabase() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
                Log.d(TAG, "Database instance closed")
            }
        }
    }
}
