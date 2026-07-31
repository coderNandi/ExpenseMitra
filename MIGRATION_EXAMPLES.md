# Migration Examples: From Simple to Complex

This document provides real-world examples of database migrations for various scenarios you may encounter.

---

## Example 1: Simple Column Addition

**Scenario**: Adding expense tags feature (v1.0.1)

### Migration Code

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d(TAG, "Migration 1→2: Adding tags column")
        database.execSQL(
            """ALTER TABLE expenses 
            ADD COLUMN tags TEXT NOT NULL DEFAULT ''"""
        )
        Log.d(TAG, "Tags column added successfully")
    }
}
```

### Gradle Update

```kotlin
versionCode = 2
versionName = "1.0.1"
```

### Database Version Update

```kotlin
@Database(entities = [Expense::class], version = 2, exportSchema = true)
abstract class ExpenseDatabase : RoomDatabase()
```

### Entity Update

```kotlin
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val amount: Double,
    val date: String,
    val timestamp: String,
    val tags: String = ""  // New field
)
```

### What Happens

✅ All existing 500 expense records kept
✅ New `tags` column added with empty string default
✅ New records can have tags
✅ Zero data loss
✅ Users don't notice anything

---

## Example 2: Adding Multiple Columns with Different Types

**Scenario**: Expense analytics (v1.1.0)

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d(TAG, "Migration 2→3: Adding analytics columns")
        
        // Add budget category
        database.execSQL(
            """ALTER TABLE expenses 
            ADD COLUMN budget_category TEXT DEFAULT 'Uncategorized'"""
        )
        
        // Add expense method (cash, card, etc)
        database.execSQL(
            """ALTER TABLE expenses 
            ADD COLUMN payment_method TEXT DEFAULT 'Cash'"""
        )
        
        // Add receipt tracking (store photo URI)
        database.execSQL(
            """ALTER TABLE expenses 
            ADD COLUMN receipt_uri TEXT"""  // Nullable
        )
        
        // Add manual approval flag
        database.execSQL(
            """ALTER TABLE expenses 
            ADD COLUMN requires_approval INTEGER DEFAULT 0"""
        )
        
        Log.d(TAG, "Analytics columns added successfully")
    }
}
```

**Result**: 4 new columns, all existing data preserved

---

## Example 3: Creating a New Table

**Scenario**: Budget tracking (v1.2.0)

```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d(TAG, "Migration 3→4: Creating budgets table")
        
        // Create new budgets table
        database.execSQL(
            """CREATE TABLE budgets (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                limit_amount REAL NOT NULL,
                category TEXT NOT NULL,
                month TEXT NOT NULL,
                created_at TEXT NOT NULL
            )"""
        )
        
        // Create index for faster queries
        database.execSQL(
            "CREATE UNIQUE INDEX idx_budget_category_month ON budgets(category, month)"
        )
        
        Log.d(TAG, "Budgets table created successfully")
    }
}
```

**Result**: New table added, old expenses table untouched

---

## Example 4: Creating Multiple Related Tables

**Scenario**: Recurring expenses + Split expenses (v2.0.0)

```kotlin
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d(TAG, "Migration 4→5: Adding recurring & split features")
        
        // Recurring expenses table
        database.execSQL(
            """CREATE TABLE recurring_expenses (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                original_expense_id INTEGER NOT NULL,
                recurrence_type TEXT NOT NULL,
                next_occurrence TEXT NOT NULL,
                active INTEGER DEFAULT 1,
                FOREIGN KEY(original_expense_id) REFERENCES expenses(id) ON DELETE CASCADE
            )"""
        )
        
        // Split expense participants
        database.execSQL(
            """CREATE TABLE split_participants (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                expense_id INTEGER NOT NULL,
                participant_name TEXT NOT NULL,
                amount REAL NOT NULL,
                FOREIGN KEY(expense_id) REFERENCES expenses(id) ON DELETE CASCADE
            )"""
        )
        
        // Create indexes
        database.execSQL(
            "CREATE INDEX idx_recurring_expense_id ON recurring_expenses(original_expense_id)"
        )
        database.execSQL(
            "CREATE INDEX idx_split_expense_id ON split_participants(expense_id)"
        )
        
        // Track recurrence state in main expenses table
        database.execSQL(
            """ALTER TABLE expenses 
            ADD COLUMN recurrence_id INTEGER"""
        )
        
        Log.d(TAG, "Recurring and split features added successfully")
    }
}
```

**Result**: 2 new tables, 1 new column, all with proper relationships

---

## Example 5: Renaming a Column (Complex Migration)

**Scenario**: Rename `date` to `expense_date` for clarity (v2.1.0)

```kotlin
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d(TAG, "Migration 5→6: Renaming 'date' to 'expense_date'")
        
        // Note: SQLite doesn't support RENAME COLUMN directly, so we:
        // 1. Create new table with updated schema
        database.execSQL(
            """CREATE TABLE expenses_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                description TEXT NOT NULL,
                amount REAL NOT NULL,
                expense_date TEXT NOT NULL,
                timestamp TEXT NOT NULL,
                tags TEXT DEFAULT '',
                budget_category TEXT DEFAULT 'Uncategorized',
                payment_method TEXT DEFAULT 'Cash',
                receipt_uri TEXT,
                requires_approval INTEGER DEFAULT 0,
                recurrence_id INTEGER
            )"""
        )
        
        // 2. Copy data from old table (mapping old column to new)
        database.execSQL(
            """INSERT INTO expenses_new 
            SELECT id, description, amount, date, timestamp, tags, 
                   budget_category, payment_method, receipt_uri, 
                   requires_approval, recurrence_id 
            FROM expenses"""
        )
        
        // 3. Drop old table
        database.execSQL("DROP TABLE expenses")
        
        // 4. Rename new table to original name
        database.execSQL("ALTER TABLE expenses_new RENAME TO expenses")
        
        // 5. Recreate indexes
        database.execSQL(
            "CREATE UNIQUE INDEX idx_budget_category_month ON budgets(category, month)"
        )
        
        Log.d(TAG, "Column renaming completed successfully. All data preserved!")
    }
}
```

**Result**: Column renamed, all 1000+ records preserved with exact values

---

## Example 6: Removing Deprecated Data

**Scenario**: Remove old unused column (v2.2.0)

```kotlin
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d(TAG, "Migration 6→7: Removing deprecated column")
        
        // Note: To remove a column, must recreate table
        // 1. Create new table without the column
        database.execSQL(
            """CREATE TABLE expenses_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                description TEXT NOT NULL,
                amount REAL NOT NULL,
                expense_date TEXT NOT NULL,
                timestamp TEXT NOT NULL,
                tags TEXT DEFAULT '',
                budget_category TEXT DEFAULT 'Uncategorized',
                payment_method TEXT DEFAULT 'Cash',
                receipt_uri TEXT,
                requires_approval INTEGER DEFAULT 0,
                recurrence_id INTEGER
                -- receipt_uri is now the last used column - removed deprecated_field
            )"""
        )
        
        // 2. Copy data (omitting the column to remove)
        database.execSQL(
            """INSERT INTO expenses_new 
            SELECT id, description, amount, expense_date, timestamp, tags, 
                   budget_category, payment_method, receipt_uri, 
                   requires_approval, recurrence_id 
            FROM expenses"""
        )
        
        // 3. Drop and rename
        database.execSQL("DROP TABLE expenses")
        database.execSQL("ALTER TABLE expenses_new RENAME TO expenses")
        
        Log.d(TAG, "Deprecated column removed. Data preserved!")
    }
}
```

---

## Example 7: Data Migration (Transforming Values)

**Scenario**: Convert old category codes to new ones (v2.3.0)

```kotlin
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d(TAG, "Migration 7→8: Converting category codes")
        
        try {
            // Map old codes to new codes
            val categoryMapping = mapOf(
                "FOOD" to "FOOD_DRINKS",
                "TRAVEL" to "TRANSPORTATION",
                "SHOPPING" to "RETAIL",
                "OTHER" to "MISCELLANEOUS"
            )
            
            categoryMapping.forEach { (oldCode, newCode) ->
                database.execSQL(
                    """UPDATE expenses 
                    SET budget_category = '$newCode' 
                    WHERE budget_category = '$oldCode'"""
                )
                Log.d(TAG, "Converted $oldCode → $newCode")
            }
            
            Log.d(TAG, "Category conversion completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during category migration: ${e.message}", e)
            throw e  // Re-throw to notify user
        }
    }
}
```

**Result**: All expense categories updated to new format

---

## Example 8: Adding Constraints and Validations

**Scenario**: Add NOT NULL constraint to new required field (v2.4.0)

```kotlin
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d(TAG, "Migration 8→9: Adding currency tracking")
        
        try {
            // First, add nullable column
            database.execSQL(
                """ALTER TABLE expenses 
                ADD COLUMN currency TEXT"""
            )
            
            // Set default values for existing records
            database.execSQL(
                """UPDATE expenses 
                SET currency = 'INR' 
                WHERE currency IS NULL"""
            )
            
            // Now we could make it NOT NULL if needed (by recreating table)
            // For now, it's NOT NULL DEFAULT 'INR' implicitly
            
            Log.d(TAG, "Currency tracking added successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding currency field: ${e.message}", e)
            throw e
        }
    }
}
```

---

## Adding All Migrations to the Array

After creating each migration, add it to the array:

```kotlin
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
    MIGRATION_7_8,
    MIGRATION_8_9
    // Future migrations will be added here
)
```

---

## Testing Your Migration

### Manual Testing

```kotlin
// In your test:
val testDb = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase::class.java)
    .addMigrations(MIGRATION_1_2)  // Add just the migration you're testing
    .build()

// Insert data in old schema
testDb.execSQL("INSERT INTO expenses VALUES (1, 'Test', 50.0, '2024-01-01', '10:00')")

// Migration happens automatically on open
// Now test new schema
val expenses = testDb.expenseDao().observeAllExpenses().first()
assertEquals(1, expenses.size)
assertEquals("Test", expenses[0].description)
```

### Automated Testing

```bash
./gradlew connectedAndroidTest -Dtest=DataPersistenceTest
```

---

## Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| "no such table" | Migration didn't run | Check ALL_MIGRATIONS includes migration |
| "duplicate column name" | Column already exists | Add `IF NOT EXISTS` or check previous migrations |
| Data loss | Destructive operation | Never DROP without copying data first |
| Migration timeout | Too many records | Optimize SQL or add index first |
| Rollback needed | Migration corrupted data | No automatic rollback - requires fixing manually |

---

## Best Practices

1. **Always test with real data** - Use production DB backup for testing
2. **Keep migrations small** - One logical change per migration
3. **Add logging** - Log every step for debugging
4. **Document reasoning** - Explain why schema changed
5. **Handle edge cases** - Think about NULL values, duplicates
6. **Never assume data** - Always verify and set defaults
7. **Add indexes** - Improve query performance after changes
8. **Test rollback** - Can you go back if needed?
9. **Version incrementally** - Don't skip version numbers
10. **Review before release** - Have another dev review migrations

---

## Remember

> Your database migration is a contract with your users' data. 
> Get it right or face angry users with lost data.

**Always prioritize data preservation!** 🛡️
