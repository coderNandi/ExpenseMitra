package com.example.expense.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

private const val TAG = "RoomMigration"

/**
 * Migration from database version 1 to version 2.
 * Scenario: Adding a new column "category" to expenses table.
 *
 * ✓ Preserves all existing expense records
 * ✓ Sets default value for new column in existing records
 * ✓ Maintains referential integrity
 * ✓ Backward compatible
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        try {
            Log.d(TAG, "Running migration 1→2: Adding category column to expenses table")

            // Add new column with a default value
            // Using DEFAULT ensures existing records have a valid value
            database.execSQL(
                """
                ALTER TABLE expenses 
                ADD COLUMN category TEXT NOT NULL DEFAULT 'General'
                """.trimIndent()
            )

            Log.d(TAG, "Migration 1→2 completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Migration 1→2 failed: ${e.message}", e)
            throw e
        }
    }
}

/**
 * Migration from database version 2 to version 3.
 * Scenario: Adding expense notes and transaction type tracking.
 *
 * ✓ Preserves all existing expense records
 * ✓ Maintains table relationships
 * ✓ Adds optional fields for new features
 * ✓ Backward compatible
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        try {
            Log.d(TAG, "Running migration 2→3: Adding notes and type columns to expenses table")

            // Add notes column (optional)
            database.execSQL(
                """
                ALTER TABLE expenses 
                ADD COLUMN notes TEXT DEFAULT ''
                """.trimIndent()
            )

            // Add type column (EXPENSE or INCOME)
            database.execSQL(
                """
                ALTER TABLE expenses 
                ADD COLUMN type TEXT NOT NULL DEFAULT 'EXPENSE'
                """.trimIndent()
            )

            Log.d(TAG, "Migration 2→3 completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Migration 2→3 failed: ${e.message}", e)
            throw e
        }
    }
}

/**
 * Migration from database version 3 to version 4.
 * Scenario: Adding recurring expense support with new table.
 *
 * ✓ Creates new table for recurring expense metadata
 * ✓ Preserves all existing one-time expense records
 * ✓ Maintains data integrity
 * ✓ Backward compatible with one-time expenses
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        try {
            Log.d(TAG, "Running migration 3→4: Creating recurring_expenses table")

            // Add recurrence tracking column to expenses
            database.execSQL(
                """
                ALTER TABLE expenses 
                ADD COLUMN recurrence_type TEXT DEFAULT 'NONE'
                """.trimIndent()
            )

            // Create new table for recurring expense metadata
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS recurring_expenses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    expense_id INTEGER NOT NULL,
                    recurrence_type TEXT NOT NULL,
                    recurrence_end_date TEXT,
                    FOREIGN KEY(expense_id) REFERENCES expenses(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            // Create index for faster queries
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS idx_recurring_expense_id 
                ON recurring_expenses(expense_id)
                """.trimIndent()
            )

            Log.d(TAG, "Migration 3→4 completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Migration 3→4 failed: ${e.message}", e)
            throw e
        }
    }
}

/**
 * All migration paths for the ExpenseDatabase.
 * Room will automatically apply the required migrations when upgrading versions.
 *
 * To add a new migration:
 * 1. Create a new migration object (e.g., MIGRATION_4_5)
 * 2. Add it to the migrations array in ExpenseDatabase
 * 3. Increment the database version in @Database annotation
 * 4. Increment versionCode and versionName in build.gradle.kts
 *
 * Example for future migration (version 4 → 5):
 * val MIGRATION_4_5 = object : Migration(4, 5) {
 *     override fun migrate(database: SupportSQLiteDatabase) {
 *         // Add budget_limit column
 *         database.execSQL("ALTER TABLE expenses ADD COLUMN budget_limit REAL DEFAULT 0.0")
 *     }
 * }
 */
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4
    // Future migrations will be added here
)
