package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

enum class AccountType {
    ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
}

enum class VoucherType {
    JOURNAL, RECEIPT, PAYMENT
}

@Entity(tableName = "fiscal_years")
data class FiscalYear(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startDate: Long,
    val endDate: Long,
    val isLocked: Boolean = false
)

@Entity(tableName = "currencies")
data class Currency(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String, // e.g. "LYD", "USD"
    val name: String,
    val isBase: Boolean = false,
    val decimalPlaces: Int = 3 // LYD uses 3, USD uses 2
)

@Entity(
    tableName = "accounts",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Currency::class,
            parentColumns = ["id"],
            childColumns = ["currencyId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountCode: String, // e.g., "1101", "1101001"
    val name: String,
    val parentId: Long? = null,
    val accountType: AccountType,
    val currencyId: Long,
    val isGroup: Boolean = false, // If true, cannot have direct transactions
    val isSystemAccount: Boolean = false
)

@Entity(
    tableName = "voucher_headers",
    foreignKeys = [
        ForeignKey(
            entity = FiscalYear::class,
            parentColumns = ["id"],
            childColumns = ["fiscalYearId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class VoucherHeader(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val voucherNo: String,
    val date: Long, // Timestamp
    val type: VoucherType,
    val description: String,
    val totalAmountBase: Long, // base currency amount of the debit ledger
    val isPosted: Boolean = false,
    val fiscalYearId: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "voucher_lines",
    foreignKeys = [
        ForeignKey(
            entity = VoucherHeader::class,
            parentColumns = ["id"],
            childColumns = ["headerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Currency::class,
            parentColumns = ["id"],
            childColumns = ["currencyId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class VoucherLine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val headerId: Long,
    val accountId: Long,
    val debit: Long = 0L, // Store scaled by currency's decimalPlaces
    val credit: Long = 0L, // Store scaled by currency's decimalPlaces
    val currencyId: Long,
    val exchangeRate: Double,
    val amountBase: Long, // debit/credit converted to base currency * 1,000,000
    val memo: String? = null
) {
    val isDebit: Boolean get() = debit > 0
}

@Entity(
    tableName = "account_balance_snapshots",
    primaryKeys = ["accountId"]
)
data class AccountBalanceSnapshot(
    val accountId: Long,
    val balance: Long, // Scaled by 1,000,000
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val voucherId: Long,
    val voucherNo: String,
    val action: String, // "DRAFT_CREATED", "POSTED", "MODIFIED", "UNPOSTED"
    val timestamp: Long = System.currentTimeMillis(),
    val details: String,
    val performedBy: String = "Senior Auditor"
)

@Entity(
    tableName = "exchange_rate_history",
    foreignKeys = [
        ForeignKey(
            entity = Currency::class,
            parentColumns = ["id"],
            childColumns = ["currencyId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExchangeRateHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val currencyId: Long,
    val rate: Double,
    val date: Long
)

@Entity(
    tableName = "customers",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["accountId"], unique = false)
    ]
)
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val accountId: Long,
    val groupName: String = ""
)

@Entity(
    tableName = "suppliers",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["accountId"], unique = false)
    ]
)
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val accountId: Long,
    val creditLimit: Long = 0L, // credit limit in base currency
    val groupName: String = ""
)

@Entity(
    tableName = "cash_boxes",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["accountId"], unique = false)
    ]
)
data class CashBox(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val managerName: String = "",
    val phone: String = "",
    val accountId: Long
)

@Entity(tableName = "banks")
data class Bank(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "bank_branches",
    foreignKeys = [
         ForeignKey(
             entity = Bank::class,
             parentColumns = ["id"],
             childColumns = ["bankId"],
             onDelete = ForeignKey.CASCADE
         )
    ],
    indices = [
        Index(value = ["bankId"], unique = false)
    ]
)
data class BankBranch(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bankId: Long,
    val name: String,
    val code: String = "",
    val managerName: String = ""
)

@Entity(
    tableName = "bank_accounts",
    foreignKeys = [
         ForeignKey(
             entity = BankBranch::class,
             parentColumns = ["id"],
             childColumns = ["branchId"],
             onDelete = ForeignKey.CASCADE
         ),
         ForeignKey(
             entity = Account::class,
             parentColumns = ["id"],
             childColumns = ["accountId"],
             onDelete = ForeignKey.RESTRICT
         )
    ],
    indices = [
        Index(value = ["branchId"], unique = false),
        Index(value = ["accountId"], unique = false)
    ]
)
data class BankAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val branchId: Long,
    val accountName: String,
    val accountNumber: String,
    val iban: String = "",
    val accountId: Long
)


