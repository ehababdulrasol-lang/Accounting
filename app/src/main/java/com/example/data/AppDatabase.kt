package com.example.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    @TypeConverter
    fun fromAccountType(value: AccountType): String = value.name

    @TypeConverter
    fun toAccountType(value: String): AccountType = AccountType.valueOf(value)

    @TypeConverter
    fun fromVoucherType(value: VoucherType): String = value.name

    @TypeConverter
    fun toVoucherType(value: String): VoucherType = VoucherType.valueOf(value)
}

@Database(
    entities = [
        FiscalYear::class,
        Currency::class,
        Account::class,
        VoucherHeader::class,
        VoucherLine::class,
        AccountBalanceSnapshot::class,
        AuditLog::class,
        ExchangeRateHistory::class,
        Customer::class,
        Supplier::class,
        CashBox::class,
        Bank::class,
        BankBranch::class,
        BankAccount::class,
        CustomerGroup::class,
        SupplierGroup::class,
        MeasurementHeader::class,
        MeasurementLine::class,
        Notification::class
    ],
    version = 11,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun voucherDao(): VoucherDao
    abstract fun currencyDao(): CurrencyDao
    abstract fun fiscalYearDao(): FiscalYearDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun accountBalanceSnapshotDao(): AccountBalanceSnapshotDao
    abstract fun exchangeRateHistoryDao(): ExchangeRateHistoryDao
    abstract fun customerDao(): CustomerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun cashBoxDao(): CashBoxDao
    abstract fun bankDao(): BankDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                createSnapshotTriggers(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                createSnapshotTriggers(db)
            }

            private fun createSnapshotTriggers(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS trg_voucher_lines_insert
                    AFTER INSERT ON voucher_lines
                    BEGIN
                        DELETE FROM account_balance_snapshots;
                        INSERT INTO account_balance_snapshots (accountId, balance, lastUpdated)
                        WITH RECURSIVE ParentChild(ancestorId, descendantId) AS (
                            SELECT id AS ancestorId, id AS descendantId FROM accounts
                            UNION ALL
                            SELECT pc.ancestorId, a.id FROM accounts a
                            JOIN ParentChild pc ON a.parentId = pc.descendantId
                        )
                        SELECT 
                            a.id AS accountId,
                            COALESCE(SUM(CASE WHEN vl.debit > 0 THEN vl.amountBase ELSE -vl.amountBase END), 0) AS balance,
                            CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER) AS lastUpdated
                        FROM accounts a
                        LEFT JOIN ParentChild pc ON a.id = pc.ancestorId
                        LEFT JOIN voucher_lines vl ON pc.descendantId = vl.accountId
                        LEFT JOIN voucher_headers vh ON vl.headerId = vh.id AND vh.isPosted = 1
                        GROUP BY a.id;
                    END;
                """.trimIndent())

                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS trg_voucher_lines_update
                    AFTER UPDATE ON voucher_lines
                    BEGIN
                        DELETE FROM account_balance_snapshots;
                        INSERT INTO account_balance_snapshots (accountId, balance, lastUpdated)
                        WITH RECURSIVE ParentChild(ancestorId, descendantId) AS (
                            SELECT id AS ancestorId, id AS descendantId FROM accounts
                            UNION ALL
                            SELECT pc.ancestorId, a.id FROM accounts a
                            JOIN ParentChild pc ON a.parentId = pc.descendantId
                        )
                        SELECT 
                            a.id AS accountId,
                            COALESCE(SUM(CASE WHEN vl.debit > 0 THEN vl.amountBase ELSE -vl.amountBase END), 0) AS balance,
                            CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER) AS lastUpdated
                        FROM accounts a
                        LEFT JOIN ParentChild pc ON a.id = pc.ancestorId
                        LEFT JOIN voucher_lines vl ON pc.descendantId = vl.accountId
                        LEFT JOIN voucher_headers vh ON vl.headerId = vh.id AND vh.isPosted = 1
                        GROUP BY a.id;
                    END;
                """.trimIndent())

                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS trg_voucher_lines_delete
                    AFTER DELETE ON voucher_lines
                    BEGIN
                        DELETE FROM account_balance_snapshots;
                        INSERT INTO account_balance_snapshots (accountId, balance, lastUpdated)
                        WITH RECURSIVE ParentChild(ancestorId, descendantId) AS (
                            SELECT id AS ancestorId, id AS descendantId FROM accounts
                            UNION ALL
                            SELECT pc.ancestorId, a.id FROM accounts a
                            JOIN ParentChild pc ON a.parentId = pc.descendantId
                        )
                        SELECT 
                            a.id AS accountId,
                            COALESCE(SUM(CASE WHEN vl.debit > 0 THEN vl.amountBase ELSE -vl.amountBase END), 0) AS balance,
                            CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER) AS lastUpdated
                        FROM accounts a
                        LEFT JOIN ParentChild pc ON a.id = pc.ancestorId
                        LEFT JOIN voucher_lines vl ON pc.descendantId = vl.accountId
                        LEFT JOIN voucher_headers vh ON vl.headerId = vh.id AND vh.isPosted = 1
                        GROUP BY a.id;
                    END;
                """.trimIndent())

                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS trg_voucher_headers_update
                    AFTER UPDATE OF isPosted ON voucher_headers
                    BEGIN
                        DELETE FROM account_balance_snapshots;
                        INSERT INTO account_balance_snapshots (accountId, balance, lastUpdated)
                        WITH RECURSIVE ParentChild(ancestorId, descendantId) AS (
                            SELECT id AS ancestorId, id AS descendantId FROM accounts
                            UNION ALL
                            SELECT pc.ancestorId, a.id FROM accounts a
                            JOIN ParentChild pc ON a.parentId = pc.descendantId
                        )
                        SELECT 
                            a.id AS accountId,
                            COALESCE(SUM(CASE WHEN vl.debit > 0 THEN vl.amountBase ELSE -vl.amountBase END), 0) AS balance,
                            CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER) AS lastUpdated
                        FROM accounts a
                        LEFT JOIN ParentChild pc ON a.id = pc.ancestorId
                        LEFT JOIN voucher_lines vl ON pc.descendantId = vl.accountId
                        LEFT JOIN voucher_headers vh ON vl.headerId = vh.id AND vh.isPosted = 1
                        GROUP BY a.id;
                    END;
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ledger_pro_database"
                )
                .addCallback(CALLBACK)
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun resetInstance() {
            INSTANCE = null
        }
    }
}
