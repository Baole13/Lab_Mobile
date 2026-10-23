# Android Test 2 — Reusable Java + SQLite Starter

**Status:** Source template, **not build-verified** in this environment (Android SDK/Gradle wrapper binary unavailable here). To get a fully bootstrapped project, use the Android Studio project creation workflow below.

## Included in this template

- Java + XML Android application; no Jetpack/Room dependencies.
- Two demo roles: `TEACHER` and `STUDENT`.
- `SharedPreferences` stores a **demo account list (password hashes, not raw passwords)** and the active login session.
- SQLite `Account`, `Student`, `Teacher`, `Score` tables and sample rows.
- Generic List/Add/Edit/Delete form and display; live multi-field search on the list.
- Teacher: manages students, teachers, scores.
- Student: views only the signed-in student's profile and scores.
- Confirmation for deletions; SQLite foreign key constraints on.
- `Schema.java` centralizes table metadata and SQL creation statements.

## Fastest working setup in Android Studio

1. In Android Studio: **New Project → Empty Views Activity** (not a Jetpack Compose project).
2. Name: `StudentManagerStarter`; Package name: `com.example.studentmanager`; Language: **Java**; Minimum SDK: **23+**. Android Studio creates a fully functional `gradlew`, wrapper JAR, and project-local config.
3. Close your generated app or continue in Android Studio. Copy **`app/src/main/`** from this starter over **`app/src/main/`** in that project (replace existing manifest/layout/java source). Copy **`app/build.gradle`** if using Groovy Gradle; otherwise retain Android Studio's existing build script and ensure `namespace` and `applicationId` match `com.example.studentmanager`.
4. Gradle Sync and Run on emulator/device. Use the generated project's wrapper files; this zip includes `gradle-wrapper.properties` but **does not contain `gradlew` or `gradle-wrapper.jar`**.
5. If your new project was generated with Kotlin-DSL (`build.gradle.kts`), **keep that project’s Kotlin-DSL files**. You only need to copy `app/src/main/` because Java/XML starter uses no external dependencies.

The bundled Groovy build configuration targets `compileSdk 35`, `targetSdk 34`, JDK 17, Android Gradle Plugin 8.7.3 with Gradle 8.9. The recommended setup using Android Studio-generated Gradle files avoids version mismatches.

## Demo login

| Username | Password | Role | Home screen |
|----------|----------|------|-------------|
| teacher | 123456 | TEACHER | Student, Teacher and Score CRUD |
| student | 123456 | STUDENT | Own profile and scores (read only) |

## IMPORTANT: When the official exam tables arrive

1. Change `dal/Schema.java`: update `CREATE_SQL`, `Table` objects, `Field`s, and table names.
2. Increment `Schema.DB_VERSION` if a database has been created on the device. This starter's `onUpgrade()` **deletes and recreates all tables**, suitable for mock data only. Alternatively uninstall the app or clear its local data.
3. Change `MainActivity.java` to map menu items to assigned roles.
4. If login role names differ, update demo credentials, roles, and checks in `utils/AuthManager.java`, `MainActivity.java`, `EntityListActivity.java`, and `EditRecordActivity.java`.
5. Change test data in `DBHelper.onCreate()`: DELETE the Student/Teacher/Score sample inserts if their tables are different; replace with your official schema's sample rows. Modify the SQLite demo `Account` rows if the exam's Account schema changes.
6. For each new table, keep fields in `Schema.Table` and ensure an SQL `CREATE TABLE` exists. List and Edit screens render automatically.
7. If the scoring range is not `0–10`, remove/adjust the `score` check in `EditRecordActivity.save()` and the SQL CHECK in `Schema.CREATE_SQL`.
8. Search is already implemented. For custom filters, change the `where` conditions in `EntityListActivity.refresh()`.
9. SQL foreign keys in this example require pre-existing `Student.code` and `Teacher.code` values for score inserts.

**Mismatch in supplied example:** `Score.teacherCode` refers to a code that `Teacher(id,fullname,email,accountId)` did not originally contain. Starter adds `Teacher.code`, so scores can reference it. Change based on official prompt.

**Account storage note:** This demo saves a test account list and session in SharedPreferences, and also creates matching SQLite `Account` records to satisfy the sample table. It does not provide account registration/synchronization. If the exam requires accounts to be authenticated from SQLite, modify `AuthManager` to use DBHelper as the single source of truth and keep session in SharedPreferences.

**Security note:** This is an offline classroom demo; even SHA-256 alone is not a production-ready password-storage strategy. Never reuse hard-coded test credentials in production. Never commit real passwords or secrets.

## Quick test checklist

- [ ] Login as teacher succeeds; wrong password fails
- [ ] Teacher can add/edit/delete a student/teacher/score; exit and relaunch app to confirm persistence
- [ ] Student login only sees own profile/scores, cannot edit
- [ ] Logout clears the session, then user can log back in
- [ ] Foreign keys: attempting to add a Score for an unknown code reports an error
- [ ] Git repository pushed to GitHub or GitLab before the test

## Git commands

```bash
git init
git add .
git commit -m "Initial Android Test 2 starter"
git branch -M main
git remote add origin YOUR_REPO_URL
git push -u origin main
```

Team collaboration: 1 person handles schema/data, 1 handles screens, 1 handles testing/integration; use branches and small commits. Don't let multiple members edit the same files simultaneously.
