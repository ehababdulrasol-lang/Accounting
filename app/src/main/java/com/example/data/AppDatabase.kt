package com.example.data

import android.content.Context
import androidx.room.*

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
        Customer::class
    ],
    version = 3,
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ledger_pro_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
