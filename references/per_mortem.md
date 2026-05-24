# 🛡️ Ledger System Per-Mortem & System Architecture Review

A comprehensive pre-mortem and post-mortem ("per-mortem") security, lifecycle, concurrency, and performance analysis of the **LedgerPro** accounting system, aligned with the corporate guidelines from `ayush016/android-lead-agent-skills`.

---

## 🔍 Executive Architectural Overview
LedgerPro utilizes a modern offline-first **MVVM + MVI Unidirectional Data Flow (UDF)** architecture, with a local SQLite database backed by Android Jetpack Room for strict double-entry ledger bookkeeping. 

- **Primary Source of Truth**: Room Datastore (`AppDatabase`, `LedgerRepository`).
- **State Management Engine**: StateFlow models mapped into `LedgerViewModel` and exposed via cold and hot streaming flows.
- **Consumption Model**: Jetpack Compose styled strictly under Material Design 3 (M3).

---

## 🛑 1. Memory Leak & Background State Collection Risks

### ⚠️ Potential Failure Mode
Up until this review, all screen consumers utilized Compose's native `.collectAsState()` extension:
```kotlin
// ❌ Potential Leak Pattern:
val lang by viewModel.currentLanguage.collectAsState()
```
When an Android application transitions to the background (e.g., telephone call interruption, switching applications), `.collectAsState()` continues active collection of upstream flows. This:
1. Causes unnecessary CPU wakeups and rapid battery exhaustion.
2. Leads to potential memory retention of stale Jetpack Compose nodes.

### 🛡️ Remediation (Done)
We performed a systematic, codebase-wide optimization to replace standard active flow collections with **lifecycle-aware flow collections** via `.collectAsStateWithLifecycle()`, imported from `androidx.lifecycle:lifecycle-runtime-compose`.
```kotlin
// ✅ Shielded Lifecycle-Aware Pattern:
val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
```
This automatically scales down collection when the lifecycle state falls below `Lifecycle.State.STARTED` (background/stopped states) and automatically resumes upon app return, minimizing resource leak profiles.

---

## 💸 2. Financial Integrity & Transaction Rolled-back Vulnerabilities

### ⚠️ Potential Failure Mode
A journal entry ledger requires absolute transactional atomicity. If a multi-row journal entry (e.g., balanced debit and credit legs) is inserted, any failure (such as syntax exception, disk exhaustion, or concurrent task conflict) during a sub-row entry could lead to an *unbalanced ledger state* if parts of the entry succeed while others fail.

### 🛡️ System Audit Results
1. **At the Dao Layer**: Inside `VoucherDao.kt` (refer file `/app/src/main/java/com/example/data/VoucherDao.kt`), insertions must be encapsulated within standard Room `@Transaction` methods:
    ```kotlin
    @Transaction
    suspend fun saveVoucher(...) { ... }
    ```
    This ensures that either *all* legs of a transaction insert successfully, or the entire operation rolls back to preserve mathematical consistency.
2. **Double-Entry Validation**: In `LedgerViewModel.kt`, saving active entries is explicitly blocked unless the double-entry balance checks out:
    ```kotlin
    val isBalanced = liveValidationState.value.third
    if (!isBalanced) {
        _uiMessage.value = "Cannot save: Journal entry is not balanced!"
        return@launch
    }
    ```
    *Recommendation for Future Expansion*: Force unique transaction UUID constraints on the `VoucherHeader` code block to prevent double-click / double-submission network or UI entry replication.

---

## 🛠️ 3. Concurrency Thread Collision Risk (Main Thread Blocking)

### ⚠️ Potential Failure Mode
Android's Main UI Thread must never execute heavy file operations, database calculations, or statement aggregation. If a user queries a statement with thousands of records, blocking the main thread for >5 seconds will trigger the dreaded **ANR (Application Not Responding)** dialog.

### 🛡️ System Audit Results
- **Coroutine Dispatching**: The repository utilizes the highly optimized `Dispatchers.IO` coroutine context for all repository operations:
  ```kotlin
  suspend fun recalculateSnapshots() = withContext(Dispatchers.IO) { ... }
  ```
- **In-Memory Calculations**: When generating extensive Trial Balance queries, the repository reads from Flow buffers using `flowOn(Dispatchers.IO)`.
- *Recommendation for Future Expansion*: Encapsulate recursive balance math inside room database views or cached triggers to offload high-depth subtree math from Kotlin runtime allocation to SQLite's optimized internal query engine.

---

## 🗄️ 4. Local DB Destructive Migrations Caution

### ⚠️ Potential Failure Mode
The Room database builder contains:
```kotlin
Room.databaseBuilder(...)
    .fallbackToDestructiveMigration(true)
    .build()
```
*Vulnerability*: This is highly productive during prototype development phases. However, in production, if a user updates the application to a new schema version without a declared migration script, **Room will wipe out the entire sqlite database and rebuild it empty**—leading to irreversible loss of critical ledgers and financial logs.

### 🛡️ Pre-Mortem Remediation Plan
1. **Migration Strategies**: Prepare explicit migration scripts:
    ```kotlin
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE Account ADD COLUMN parent_code TEXT")
        }
    }
    ```
2. **Backup System Activation**: LedgerPro already possesses a local offline backup feature and URI intent importer. Always encourage the user to perform a manual backup before major structural updates to guarantee physical data durability.
