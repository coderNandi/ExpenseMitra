# Quick Start: Releasing a New Version

## Step 1: Update Version Numbers

Edit `app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 2           // ← Increment by 1
    versionName = "1.0.1"     // ← Update following semantic versioning
}
```

## Step 2: Implement Database Schema Changes (if any)

If your new features require database changes:

### Add a Column

Edit `app/src/main/java/com/example/expense/data/database/migration/Migrations.kt`:

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d(TAG, "Adding new_field column to expenses table")
        database.execSQL(
            "ALTER TABLE expenses ADD COLUMN new_field TEXT NOT NULL DEFAULT 'default_value'"
        )
    }
}

// Add to ALL_MIGRATIONS array:
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2  // ← Add here
)
```

### Create a New Table

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """CREATE TABLE new_table (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                data TEXT NOT NULL
            )"""
        )
    }
}
```

## Step 3: Update Database Version

Edit `app/src/main/java/com/example/expense/data/database/ExpenseDatabase.kt`:

```kotlin
@Database(entities = [Expense::class], version = 2, exportSchema = true)  // ← Increment version
abstract class ExpenseDatabase : RoomDatabase()
```

## Step 4: Update Data Models (if any)

Edit `app/src/main/java/com/example/expense/data/model/Expense.kt`:

```kotlin
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val amount: Double,
    val date: String,
    val timestamp: String,
    val newField: String = "default_value"  // ← Add new field with default
)
```

## Step 5: Test Everything

```bash
# Build the project
./gradlew build

# Run data persistence tests
./gradlew connectedAndroidTest -Dtest=DataPersistenceTest

# Run all tests
./gradlew connectedAndroidTest
```

## Step 6: Release!

Your app now:
- ✅ Has updated version numbers
- ✅ Automatically migrates data for all users
- ✅ Preserves all expense records
- ✅ Works seamlessly for users upgrading from older versions

---

## Troubleshooting

### "Migration from X to Y not found"

**Cause**: Missing migration in `ALL_MIGRATIONS`

**Fix**:
```kotlin
// In Migrations.kt
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2,
    // ↓ Add any missing migrations here
    MIGRATION_2_3
)
```

### "Database is locked" during testing

**Cause**: Previous test instance didn't close properly

**Fix**: Clear app data before running tests:
```bash
./gradlew connectedAndroidTest -clearPackageData
```

### Data missing after upgrade

**Cause**: Migration wasn't called or failed silently

**Debug**:
```kotlin
val versionManager = VersionManager(context)
Log.d("Debug", versionManager.getVersionInfo())  // Check version detection

// Check database integrity
val count = database.expenseDao().observeAllExpenses().first().size
Log.d("Debug", "Total expenses: $count")
```

---

## Key Files

| File | Purpose |
|------|---------|
| `build.gradle.kts` | Version numbers (versionCode, versionName) |
| `ExpenseDatabase.kt` | Database version number, migration setup |
| `Migrations.kt` | All migration implementations |
| `VersionManager.kt` | Version detection and tracking |
| `Expense.kt` | Data model (update when schema changes) |
| `DATA_PERSISTENCE_GUIDE.md` | Comprehensive strategy guide |

---

## Remember

1. **Always increment versionCode** - Play Store won't accept duplicate codes
2. **Test migrations** - Don't release without testing on actual data
3. **Preserve data** - Never use `fallbackToDestructiveMigration()` in production
4. **Document migrations** - Add comments explaining what changed and why
5. **Version users** - Track app versions for analytics and support

**Your users' data is sacred. Protect it! 🔒**
