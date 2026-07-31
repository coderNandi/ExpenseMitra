# Release Checklist: Data Preservation Edition

Use this checklist before every release to ensure user data is preserved and the app upgrades seamlessly.

---

## Pre-Release Phase (1 week before release)

### Code Review
- [ ] All feature code reviewed and approved
- [ ] No `fallbackToDestructiveMigration()` calls in database code
- [ ] No `clearDatabase()` or similar destructive operations
- [ ] All database queries are read-only or data-preserving

### Database Schema Changes
- [ ] Identified all schema changes needed for new features
- [ ] Created migration file in `data/database/migration/Migrations.kt`
- [ ] Documented each migration with comments explaining the change
- [ ] Tested migration with sample data (50, 500, and 1000+ records)

### Version Numbers
- [ ] Determined new version numbers following semantic versioning
  - Breaking changes? → Major version (2.0.0)
  - New features? → Minor version (1.1.0)
  - Bug fixes only? → Patch version (1.0.1)
- [ ] Updated `app/build.gradle.kts`:
  - `versionCode = X` (always increment by exactly 1)
  - `versionName = "X.Y.Z"` (semantic versioning)

### Database Configuration
- [ ] If schema changed:
  - [ ] Incremented `@Database version` in `ExpenseDatabase.kt`
  - [ ] Updated `Expense.kt` data model with new fields
  - [ ] Added new migration to `ALL_MIGRATIONS` array
  - [ ] Verified migration is safe (no data loss)

---

## Testing Phase (3-5 days before release)

### Unit Tests
```bash
./gradlew test
```
- [ ] All unit tests pass
- [ ] New code has unit test coverage
- [ ] No test failures related to database operations

### Instrumented Tests (must use Android device/emulator)
```bash
./gradlew connectedAndroidTest
```
- [ ] All data persistence tests pass
- [ ] Fresh install test passes (add 50 expenses, verify all present)
- [ ] Upgrade test passes (data survives version change)
- [ ] Filtered query test passes (queries work after upgrade)
- [ ] Large dataset test passes (1000+ records)

### Integration Tests
- [ ] Test complete upgrade flow:
  1. Install v1.0.0 with 100 test expenses
  2. Close and reopen app (verify data)
  3. Upgrade to new version
  4. Reopen app (verify all 100 expenses present)
  5. Add new expenses with new features
  6. Verify new and old expenses both visible

### Device Testing (Critical)
- [ ] Install on physical Android device
- [ ] Test on at least 2 different Android versions
- [ ] Test with minimum SDK version (API 24)
- [ ] Test with target SDK version (API 36)
- [ ] Perform upgrade over existing installation
- [ ] Verify no data loss
- [ ] Check app logs for migration errors:
  ```bash
  ./adb logcat | grep -E "ExpenseDatabase|RoomMigration|DatabaseCallback"
  ```

### Performance Testing
- [ ] Measure app startup time before and after upgrade
- [ ] Measure database query performance
- [ ] Verify no excessive memory usage
- [ ] Test with low storage device (< 1 GB free space)

---

## Pre-Release Build Phase (1-2 days before release)

### Build Verification
- [ ] Clean build succeeds:
  ```bash
  ./gradlew clean build
  ```
- [ ] No compilation warnings (especially around database)
- [ ] No ProGuard/R8 issues
- [ ] Lint passes (or baseline created for existing issues):
  ```bash
  ./gradlew lint
  ```

### APK Generation
- [ ] Debug APK builds successfully
- [ ] Release APK builds successfully
- [ ] APK sizes reasonable (no unexpected bloat)
- [ ] APK can be installed on test devices

### Documentation
- [ ] Updated CHANGELOG with migration details
- [ ] Updated DATA_PERSISTENCE_GUIDE.md if needed
- [ ] Updated QUICK_START.md for next release
- [ ] Added comments to all migration code
- [ ] Documented breaking changes (if any)

### Git/Version Control
- [ ] Created release branch from main
- [ ] All changes committed with clear messages
- [ ] Tag created with version name: `v1.2.0`
- [ ] Version tag associated with release commit

---

## Release Day Phase

### Final Verification
- [ ] Version numbers correct:
  - `versionCode = X`
  - `versionName = "X.Y.Z"`
- [ ] All migrations present in `ALL_MIGRATIONS`
- [ ] No temporary debug code remaining
- [ ] No hardcoded test data in release build
- [ ] Privacy/security check complete

### Store Listing (if using Play Store)
- [ ] Release notes written
- [ ] Notes mention data preservation if major release
- [ ] Notes mention new features clearly
- [ ] Notes mention bugfixes if present
- [ ] Screenshots updated if UI changed

### Release Upload
- [ ] Upload signed release APK
- [ ] Mark as staged rollout (not 100% immediately)
  - Start with 5% → 10% → 50% → 100%
  - Allows catching issues before full release
- [ ] Set minimum API level to 24
- [ ] Set target API level to 36

### Monitoring Post-Release
- [ ] Monitor crash reports (first 24 hours)
- [ ] Check Logcat for migration errors
- [ ] Monitor user feedback/reviews
- [ ] Have rollback plan ready if critical issue found
- [ ] Track performance metrics

---

## Post-Release Phase

### User Communication
- [ ] Notify users of release (if using in-app notifications)
- [ ] Announcement includes migration transparency
- [ ] Provide support contact if issues

### Data Verification
- [ ] Spot-check user databases (if possible)
- [ ] Query for common issues:
  ```sql
  SELECT COUNT(*) FROM expenses;  -- Should be non-zero for active users
  SELECT * FROM expenses LIMIT 1; -- Spot check a record
  ```
- [ ] Monitor user reports for data loss

### Metrics Tracking
- [ ] Crash rate normal (no increase)
- [ ] Migration performance acceptable
- [ ] User retention normal (no churn from upgrade)
- [ ] ANR (app not responding) reports minimal

### Gradual Rollout Progression
- [ ] 5% → stable (24 hours) → 10%
- [ ] 10% → stable (24 hours) → 50%
- [ ] 50% → stable (48 hours) → 100%
- [ ] Each stage: no crash spikes, user complaints

### Documentation Update
- [ ] Update version tracking in CHANGELOG
- [ ] Archive any migration-specific documentation
- [ ] Update next developer about schema changes
- [ ] Post mortem any issues encountered

---

## Rollback Plan (If Needed)

If critical issues discovered post-release:

- [ ] Stop rollout immediately (don't exceed current percentage)
- [ ] Identify root cause
- [ ] Create hotfix release:
  ```
  Previous: 1.2.0 (versionCode: 20)
  Hotfix: 1.2.1 (versionCode: 21)
  ```
- [ ] Re-run entire test suite with hotfix
- [ ] Deploy hotfix release (5% → 100% carefully)
- [ ] Document what went wrong and how to prevent

---

## Data Preservation Verification Queries

Run these on production databases to verify migration success:

```sql
-- Total expense count
SELECT COUNT(*) as total_expenses FROM expenses;

-- Verify no corruption (all records have required fields)
SELECT COUNT(*) FROM expenses WHERE description IS NULL;  -- Should be 0
SELECT COUNT(*) FROM expenses WHERE amount IS NULL;       -- Should be 0
SELECT COUNT(*) FROM expenses WHERE date IS NULL;         -- Should be 0

-- Check for valid amounts (positive, reasonable)
SELECT COUNT(*) FROM expenses WHERE amount <= 0;          -- Should be low/0

-- Verify new columns exist and have data (if added)
PRAGMA table_info(expenses);                              -- Shows schema

-- Check for orphaned records (if adding foreign keys)
SELECT COUNT(*) FROM recurring_expenses 
WHERE expense_id NOT IN (SELECT id FROM expenses);        -- Should be 0
```

---

## Quick Reference: Version Increment Pattern

```
1.0.0 (versionCode: 1)
↓ (patch: bug fix)
1.0.1 (versionCode: 2)
↓ (patch: bug fix)
1.0.2 (versionCode: 3)
↓ (minor: new feature)
1.1.0 (versionCode: 4)
↓ (patch: bug fix)
1.1.1 (versionCode: 5)
↓ (minor: new feature)
1.2.0 (versionCode: 6)
↓ (major: breaking changes)
2.0.0 (versionCode: 7)
```

**Rule**: `versionCode` = cumulative release number (always +1)
**Rule**: `versionName` = semantic versioning (MAJOR.MINOR.PATCH)

---

## Common Mistakes to Avoid

❌ **DON'T DO THIS:**
- Forget to increment versionCode
- Use same versionCode for different releases
- Skip database version increment
- Forget to add migration to ALL_MIGRATIONS
- Use fallbackToDestructiveMigration()
- Delete database to "reset"
- Release without testing

✅ **DO THIS INSTEAD:**
- Always increment versionCode by exactly 1
- Always follow semantic versioning
- Increment database version when schema changes
- Add ALL migrations to ALL_MIGRATIONS array
- Test with real user data
- Verify migrations in isolation
- Test complete upgrade flow

---

## Questions to Ask Before Release

1. **Data Preservation**: Will any user data be lost in this release?
   - Answer: NO ✓
   
2. **Migration Path**: What versions can upgrade to this version?
   - Answer: All supported versions (usually last 5 major releases)
   
3. **Rollback**: Can users downgrade if issues found?
   - Answer: Database forward-compatible, code-level rollback requires Play Store intervention
   
4. **Performance**: Will migration cause app slowdown?
   - Answer: < 5 seconds for typical datasets
   
5. **Monitoring**: How will we know if migration failed?
   - Answer: Crash reports, user feedback, analytics metrics

---

## Success Criteria

Release is successful if:

✅ 0 crashes related to migration
✅ 0 reports of data loss
✅ Upgrade completes in < 5 seconds
✅ All user data accessible after upgrade
✅ New features work as intended
✅ No regression in existing features
✅ App startup time not significantly increased

---

## Support Contacts

If issues occur post-release:

1. **Developer**: Check Logcat for migration errors
2. **QA**: Run data preservation tests
3. **Product**: Communicate with users about status
4. **DevOps**: Prepare rollback if needed
5. **Database**: Backup user data before any manual fixes

---

## Sign-Off

Before clicking "Release":

- Reviewed by: _________________ Date: _______
- Tested by: _________________ Date: _______
- Approved by: _________________ Date: _______

**Remember**: Your users trust you with their data. Don't disappoint them! 🙏
