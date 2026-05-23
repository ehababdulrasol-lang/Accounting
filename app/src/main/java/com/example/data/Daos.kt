package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Transaction
    @Query("""
        WITH RECURSIVE SubAccounts AS (
            SELECT id FROM accounts WHERE id = :accountId
            UNION ALL
            SELECT a.id FROM accounts a INNER JOIN SubAccounts s ON a.parentId = s.id
        )
        SELECT 
            SUM(CASE WHEN vl.debit > 0 THEN vl.amountBase ELSE -vl.amountBase END) as balance
        FROM voucher_lines vl
        JOIN voucher_headers vh ON vl.headerId = vh.id
        WHERE vl.accountId IN SubAccounts AND vh.isPosted = 1
    """)
    fun getRecursiveAccountBalance(accountId: Long): Flow<Long?>

    @Query("SELECT * FROM accounts WHERE parentId IS NULL ORDER BY accountCode")
    fun getRootAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE parentId = :parentId ORDER BY accountCode")
    fun getSubAccounts(parentId: Long): Flow<List<Account>>

    @Query("SELECT * FROM accounts ORDER BY accountCode")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :accountId")
    suspend fun getAccountById(accountId: Long): Account?

    @Query("SELECT * FROM accounts WHERE accountCode = :code")
    suspend fun getAccountByCode(code: String): Account?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: Account): Long

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)
}

@Dao
interface VoucherDao {

    @Query("SELECT * FROM voucher_headers ORDER BY date DESC, id DESC")
    fun getAllVoucherHeaders(): Flow<List<VoucherHeader>>

    @Query("SELECT * FROM voucher_headers WHERE id = :id")
    suspend fun getVoucherHeaderById(id: Long): VoucherHeader?

    @Query("SELECT * FROM voucher_lines WHERE headerId = :headerId")
    fun getVoucherLinesForHeaderFlow(headerId: Long): Flow<List<VoucherLine>>

    @Query("SELECT * FROM voucher_lines WHERE headerId = :headerId")
    suspend fun getVoucherLinesForHeader(headerId: Long): List<VoucherLine>

    @Query("SELECT * FROM voucher_lines")
    suspend fun getAllVoucherLines(): List<VoucherLine>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeader(header: VoucherHeader): Long

    @Update
    suspend fun updateHeader(header: VoucherHeader)

    @Delete
    suspend fun deleteHeader(header: VoucherHeader)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLines(lines: List<VoucherLine>)

    @Query("DELETE FROM voucher_lines WHERE headerId = :headerId")
    suspend fun deleteLinesForHeader(headerId: Long)

    @Transaction
    suspend fun saveVoucher(header: VoucherHeader, lines: List<VoucherLine>): Long {
        val id = if (header.id == 0L) {
            insertHeader(header)
        } else {
            updateHeader(header)
            deleteLinesForHeader(header.id)
            header.id
        }
        val linesWithHeaderId = lines.map { it.copy(headerId = id) }
        insertLines(linesWithHeaderId)
        return id
    }
}

@Dao
interface CurrencyDao {
    @Query("SELECT * FROM currencies ORDER BY code")
    fun getAllCurrencies(): Flow<List<Currency>>

    @Query("SELECT * FROM currencies WHERE isBase = 1 LIMIT 1")
    suspend fun getBaseCurrency(): Currency?

    @Query("SELECT * FROM currencies WHERE id = :id")
    suspend fun getCurrencyById(id: Long): Currency?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(currency: Currency): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(currencies: List<Currency>)
}

@Dao
interface FiscalYearDao {
    @Query("SELECT * FROM fiscal_years ORDER BY startDate")
    fun getAllFiscalYears(): Flow<List<FiscalYear>>

    @Query("SELECT * FROM fiscal_years WHERE isLocked = 0 ORDER BY startDate DESC")
    fun getActiveFiscalYears(): Flow<List<FiscalYear>>

    @Query("SELECT * FROM fiscal_years WHERE id = :id")
    suspend fun getFiscalYearById(id: Long): FiscalYear?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fiscalYear: FiscalYear): Long

    @Update
    suspend fun update(fiscalYear: FiscalYear)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AuditLog): Long
}

@Dao
interface AccountBalanceSnapshotDao {
    @Query("SELECT * FROM account_balance_snapshots")
    suspend fun getAllSnapshots(): List<AccountBalanceSnapshot>

    @Query("SELECT * FROM account_balance_snapshots")
    fun getAllSnapshotsFlow(): Flow<List<AccountBalanceSnapshot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: AccountBalanceSnapshot)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(snapshots: List<AccountBalanceSnapshot>)

    @Query("DELETE FROM account_balance_snapshots")
    suspend fun clearAll()
}

@Dao
interface ExchangeRateHistoryDao {
    @Query("SELECT * FROM exchange_rate_history WHERE currencyId = :currencyId ORDER BY date DESC")
    fun getRateHistoryFlow(currencyId: Long): Flow<List<ExchangeRateHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rateHistory: ExchangeRateHistory): Long
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name")
    fun getAllCustomersFlow(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: Customer): Long

    @Update
    suspend fun update(customer: Customer)

    @Delete
    suspend fun delete(customer: Customer)
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name")
    fun getAllSuppliersFlow(): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun getSupplierById(id: Long): Supplier?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(supplier: Supplier): Long

    @Update
    suspend fun update(supplier: Supplier)

    @Delete
    suspend fun delete(supplier: Supplier)
}
