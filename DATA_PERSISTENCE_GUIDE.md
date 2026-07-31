# Data Persistence & APK Upgrade Strategy

## Overview

This document outlines the complete data persistence and upgrade strategy for the Expense Tracker application. It ensures that user data is never lost during app updates and provides a framework for future feature development.

## Key Principles

1. **Data Preservation First**: User data is the most valuable asset. All upgrade mechanisms prioritize data safety over convenience.
2. **Automatic Migrations**: Room Database handles schema changes automatically without user intervention.
3. **Version Tracking**: The app tracks version history to detect and handle upgrades appropriately.
4. **Graceful Error Handling**: Failures never result in automatic data deletion.

---

## Version Management Strategy

### How to Increment Versions

Every time you release a new version:

1. **Update `build.gradle.kts`**:
   ```kotlin
   versionCode = 2              // Always increment by 1
   versionName = "1.0.1"        // Use semantic versioning
   ```

2. **Semantic Versioning Format**: `MAJOR.MINOR.PATCH`
   - `MAJOR`: Breaking changes (completely new features)
   - `MINOR`: New features (backward compatible)
   - `PATCH`: Bug fixes only

### Version Progression Example

```
Version 1.0.0 (versionCode: 1)
    ↓ (patch release - bug fix)
Version 1.0.1 (versionCode: 2)
    ↓ (minor release - new feature)
Version 1.1.0 (versionCode: 3)
    ↓ (minor release - new feature)
Version 1.1.1 (versionCode: 4)
    ↓ (major release - breaking changes)
Version 2.0.0 (versionCode: 5)
```

---

## Database Schema Evolution

### Current Schema (Version 1)

```sql
CREATE TABLE expenses (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    description TEXT NOT NULL,
    amount REAL NOT NULL,
    date TEXT NOT NULL,
    timestamp TEXT NOT NULL
);
```

### Adding Features with Safe Migrations

#### Example 1: Adding a New Column

When adding a new feature like expense categories:

1. **In Migrations.kt**:
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // ALTER TABLE is safe - all existing records keep their values
        // New records must provide this field
        database.execSQL(
            """ALTER TABLE expenses 
            ADD COLUMN category TEXT NOT NULL DEFAULT 'General'"""
        )
    }
}
```

2. **Update @Database annotation**:
```kotlin
@Database(entities = [Expense::class], version = 2, exportSchema = true)
abstract class ExpenseDatabase : RoomDatabase()
```

3. **Update build.gradle.kts**:
```kotlin
versionCode = 2
versionName = "1.0.1"
```

4. **Update Expense entity**:
```kotlin
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val amount: Double,
    val date: String,
    val timestamp: String,
    val category: String = "General"  // New field with default
)
```

#### Example 2: Creating a Related Table

When adding recurring expense support:

1. **Create migration**:
```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // New table doesn't affect existing data
        database.execSQL(
            """CREATE TABLE recurring_expenses (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                expense_id INTEGER NOT NULL,
                recurrence_type TEXT NOT NULL,
                FOREIGN KEY(expense_id) REFERENCES expenses(id) ON DELETE CASCADE
            )"""
        )
    }
}
```

#### Example 3: Renaming a Column

For structural changes with data preservation:

```kotlin
val MIGRATION_N_N1 = object : Migration(N, N+1) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // SQLite doesn't have direct RENAME COLUMN, so:
        // 1. Create new table with updated schema
        database.execSQL(
            """CREATE TABLE expenses_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                description TEXT NOT NULL,
                amount REAL NOT NULL,
                date TEXT NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            )"""
        )
        
        // 2. Copy data from old table
        database.execSQL(
            """INSERT INTO expenses_new 
            SELECT id, description, amount, date, timestamp as created_at, timestamp as updated_at 
            FROM expenses"""
        )
        
        // 3. Drop old table
        database.execSQL("DROP TABLE expenses")
        
        // 4. Rename new table
        database.execSQL("ALTER TABLE expenses_new RENAME TO expenses")
        
        // 5. Recreate indexes if needed
        database.execSQL("CREATE INDEX idx_date ON expenses(date)")
    }
}
```

---

## How Upgrades Work (Step-by-Step)

### Flow Diagram

```
User installs updated APK
    ↓
ExpenseDatabase.getInstance() called
    ↓
VersionManager detects version change
    ↓
Room checks current DB version vs @Database version
    ↓
Missing migrations applied automatically ← Key: All migrations in ALL_MIGRATIONS array
    ↓
All existing data preserved (no records deleted)
    ↓
New version info saved to SharedPreferences
    ↓
App launches with all data intact
```

### Example Scenario: Upgrading from 1.0.0 to 1.1.0

**Before Upgrade**:
- versionCode: 1, versionName: "1.0.0"
- Database version: 1
- Data: 200 expense records in database

**Upgrade Process**:

1. User installs APK with:
   - versionCode: 3, versionName: "1.1.0"
   - Database version: 3

2. On first launch:
   - `ExpenseDatabase.getInstance()` is called
   - VersionManager detects: previousVersionCode (1) < currentVersionCode (3)
   - Room finds local database is version 1, but @Database expects version 3
   - Room automatically applies: MIGRATION_1_2, then MIGRATION_2_3
   - Both migrations execute successfully
   - All 200 expense records remain intact

3. After upgrade:
   - App opens normally
   - User sees all 200 expenses
   - New features (from 1.1.0) are available
   - Version info is saved for future comparisons

---

## File Structure

```
com/example/expense/
├── data/
│   ├── database/
│   │   ├── ExpenseDatabase.kt              # Main database with versioning
│   │   ├── ExpenseDao.kt                   # Database access operations
│   │   ├── migration/
│   │   │   └── Migrations.kt              # All migration definitions
│   │   └── callback/
│   │       └── ExpenseDatabaseCallback.kt # Lifecycle callbacks
│   ├── model/
│   │   └── Expense.kt                      # Entity with version tracking
│   └── repository/
│       └── ExpenseRepository.kt            # Repository pattern
├── utils/
│   └── VersionManager.kt                   # Version detection & tracking
└── ExpenseApplication.kt                   # App initialization
```

---

## Testing Data Persistence

### Running Tests

```bash
# Run all instrumented tests
./gradlew connectedAndroidTest

# Run only data persistence tests
./gradlew connectedAndroidTest -Dtest=DataPersistenceTest
```

### Test Scenarios Included

1. **Fresh Installation**: Verify data creation on first install
2. **Version Upgrade**: Verify data survives same-schema upgrades
3. **Filtered Queries**: Verify queries work after upgrade
4. **Large Dataset**: Verify 1000+ records persist
5. **Data Type Consistency**: Verify field types maintained
6. **Concurrent Access**: Verify thread-safe operations

---

## Error Handling & Recovery

### What Happens If Migration Fails

1. **Database Lock**: Migration runs in exclusive transaction
2. **No Partial Writes**: Either fully succeeds or fully reverts
3. **Exception Propagation**: Error logs and throws Exception
4. **Graceful Failure**: App doesn't auto-delete database
5. **Recovery Option**: User can clear app data manually if needed

### Preventing Migration Failures

**DO's**:
- ✅ Use `ALTER TABLE ... ADD COLUMN` for new fields
- ✅ Always provide `DEFAULT` values for new columns
- ✅ Create new tables instead of modifying existing ones
- ✅ Use `IF NOT EXISTS` for idempotent operations
- ✅ Test migrations in development before releasing

**DON'Ts**:
- ❌ Never use `fallbackToDestructiveMigration()` in production
- ❌ Never delete tables to "reset" schema
- ❌ Never drop columns without migration (breaking change)
- ❌ Never change column types without explicit migration
- ❌ Never assume migration won't fail - always plan for errors

---

## Future Extensions

The current architecture supports adding:

### 1. Bluetooth Synchronization
```kotlin
// In repository layer
suspend fun syncWithBluetooth(device: BluetoothDevice) {
    val remoteExpenses = device.fetchExpenses()
    // Merge with local expenses
    // Handle conflicts gracefully
}
```

### 2. Cloud Backup
```kotlin
// In repository layer
suspend fun backupToCloud() {
    val expenses = repository.getAllExpenses()
    // Upload to Firebase/Cloud Storage
}
```

### 3. Automatic Export
```kotlin
// In repository layer
suspend fun exportToCSV(filename: String) {
    val expenses = repository.getAllExpenses()
    // Create CSV file
}
```

### 4. Multi-Device Sync
```kotlin
// New table for sync state
@Entity(tableName = "sync_metadata")
data class SyncMetadata(
    @PrimaryKey val id: Int = 1,
    val lastSyncTime: Long,
    val lastSyncStatus: String
)
```

The database versioning system handles all schema changes needed for these features without data loss.

---

## Best Practices Checklist

Before releasing a new version:

- [ ] Increment `versionCode` by exactly 1
- [ ] Update `versionName` with semantic versioning
- [ ] Create migration if schema changes
- [ ] Increment database version if schema changes
- [ ] Add new migration to `ALL_MIGRATIONS` array
- [ ] Test migration with sample data
- [ ] Verify no `fallbackToDestructiveMigration()` in code
- [ ] Run all data persistence tests
- [ ] Test on actual device/emulator
- [ ] Document migration in Migrations.kt
- [ ] Update Expense entity if schema changed
- [ ] Verify backward compatibility

---

## Monitoring & Debugging

### View Migration Logs

Add this to see detailed migration info:

```kotlin
// In ExpenseDatabase.kt
database.openHelper.writableDatabase  // Triggers migration logs
```

Monitor Logcat for tags:
- `ExpenseDatabase`: Database initialization
- `RoomMigration`: Migration execution
- `DatabaseCallback`: Lifecycle events

### Version Debugging

```kotlin
val versionManager = VersionManager(context)
Log.d("VersionInfo", versionManager.getVersionInfo())
// Output: Previous: 1.0.0 (1) → Current: 1.1.0 (3)
```

---

## Conclusion

This data persistence strategy ensures that user data is always protected during app upgrades. By leveraging Room's migration system and maintaining proper version tracking, the Expense Tracker app can confidently release updates without risking user data loss.

Every upgrade is an opportunity to improve the app while keeping user data safe.
