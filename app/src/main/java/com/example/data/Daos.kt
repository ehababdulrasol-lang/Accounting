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
    suspend fun getRecursiveAccountBalanceSuspend(accountId: Long): Long?

    @Query("SELECT * FROM accounts WHERE parentId IS NULL ORDER BY accountCode")
    fun getRootAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE parentId = :parentId ORDER BY accountCode")
    fun getSubAccounts(parentId: Long): Flow<List<Account>>

    @Query("SELECT * FROM accounts ORDER BY accountCode")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts ORDER BY accountCode")
    suspend fun getAllAccountsSuspend(): List<Account>

    @Query("SELECT * FROM accounts WHERE parentId = :parentId ORDER BY accountCode")
    suspend fun getSubAccountsSuspend(parentId: Long): List<Account>

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

    @Query("SELECT * FROM voucher_headers ORDER BY date DESC, id DESC")
    suspend fun getAllVoucherHeadersSuspend(): List<VoucherHeader>

    @Query("SELECT * FROM voucher_headers WHERE id = :id")
    suspend fun getVoucherHeaderById(id: Long): VoucherHeader?

    @Query("SELECT * FROM voucher_lines WHERE headerId = :headerId")
    fun getVoucherLinesForHeaderFlow(headerId: Long): Flow<List<VoucherLine>>

    @Query("SELECT * FROM voucher_lines WHERE headerId = :headerId")
    suspend fun getVoucherLinesForHeader(headerId: Long): List<VoucherLine>

    @Query("SELECT * FROM voucher_lines")
    suspend fun getAllVoucherLines(): List<VoucherLine>

    @Query("SELECT * FROM voucher_lines")
    fun getAllVoucherLinesFlow(): Flow<List<VoucherLine>>

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

    @Query("SELECT * FROM fiscal_years WHERE isLocked = 0 AND isClosed = 0 ORDER BY startDate DESC")
    fun getActiveFiscalYears(): Flow<List<FiscalYear>>

    @Query("SELECT * FROM fiscal_years WHERE isLocked = 0 AND isClosed = 0 ORDER BY startDate DESC")
    suspend fun getActiveFiscalYearsSuspend(): List<FiscalYear>

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

    @Transaction
    @Query("""
        WITH RECURSIVE ParentChild(ancestorId, descendantId) AS (
            SELECT id AS ancestorId, id AS descendantId FROM accounts
            UNION ALL
            SELECT pc.ancestorId, a.id FROM accounts a
            JOIN ParentChild pc ON a.parentId = pc.descendantId
        )
        SELECT 
            a.id AS accountId,
            COALESCE(SUM(CASE WHEN vl.debit > 0 THEN vl.amountBase ELSE -vl.amountBase END), 0) AS balance,
            :updateTime AS lastUpdated
        FROM accounts a
        LEFT JOIN ParentChild pc ON a.id = pc.ancestorId
        LEFT JOIN voucher_lines vl ON pc.descendantId = vl.accountId
        LEFT JOIN voucher_headers vh ON vl.headerId = vh.id AND vh.isPosted = 1
        GROUP BY a.id
    """)
    suspend fun calculateRecursiveBalances(updateTime: Long): List<AccountBalanceSnapshot>

    @Transaction
    @Query("""
        WITH RECURSIVE ParentChild(ancestorId, descendantId) AS (
            SELECT id AS ancestorId, id AS descendantId FROM accounts
            UNION ALL
            SELECT pc.ancestorId, a.id FROM accounts a
            JOIN ParentChild pc ON a.parentId = pc.descendantId
        )
        SELECT 
            a.id AS accountId,
            COALESCE(SUM(CASE WHEN vl.debit > 0 THEN vl.amountBase ELSE -vl.amountBase END), 0) AS balance,
            :updateTime AS lastUpdated
        FROM accounts a
        LEFT JOIN ParentChild pc ON a.id = pc.ancestorId
        LEFT JOIN voucher_lines vl ON pc.descendantId = vl.accountId
        LEFT JOIN voucher_headers vh ON vl.headerId = vh.id AND vh.isPosted = 1 AND vh.date BETWEEN :startDate AND :endDate
        GROUP BY a.id
    """)
    suspend fun calculateRecursiveBalancesForPeriod(startDate: Long, endDate: Long, updateTime: Long): List<AccountBalanceSnapshot>
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

    @Query("SELECT * FROM customer_groups ORDER BY name")
    fun getAllCustomerGroupsFlow(): Flow<List<CustomerGroup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: CustomerGroup): Long

    @Update
    suspend fun updateGroup(group: CustomerGroup)

    @Delete
    suspend fun deleteGroup(group: CustomerGroup)
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

    @Query("SELECT * FROM supplier_groups ORDER BY name")
    fun getAllSupplierGroupsFlow(): Flow<List<SupplierGroup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: SupplierGroup): Long

    @Update
    suspend fun updateGroup(group: SupplierGroup)

    @Delete
    suspend fun deleteGroup(group: SupplierGroup)
}

@Dao
interface CashBoxDao {
    @Query("SELECT * FROM cash_boxes ORDER BY name")
    fun getAllCashBoxesFlow(): Flow<List<CashBox>>

    @Query("SELECT * FROM cash_boxes WHERE id = :id")
    suspend fun getCashBoxById(id: Long): CashBox?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cashBox: CashBox): Long

    @Update
    suspend fun update(cashBox: CashBox)

    @Delete
    suspend fun delete(cashBox: CashBox)
}

@Dao
interface BankDao {
    @Query("SELECT * FROM banks ORDER BY name")
    fun getAllBanksFlow(): Flow<List<Bank>>

    @Query("SELECT * FROM banks WHERE id = :id")
    suspend fun getBankById(id: Long): Bank?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bank: Bank): Long

    @Update
    suspend fun update(bank: Bank)

    @Delete
    suspend fun delete(bank: Bank)

    // Branch operations
    @Query("SELECT * FROM bank_branches WHERE bankId = :bankId ORDER BY name")
    fun getBranchesForBankFlow(bankId: Long): Flow<List<BankBranch>>

    @Query("SELECT * FROM bank_branches ORDER BY name")
    fun getAllBranchesFlow(): Flow<List<BankBranch>>

    @Query("SELECT * FROM bank_branches WHERE id = :id")
    suspend fun getBranchById(id: Long): BankBranch?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranch(branch: BankBranch): Long

    @Update
    suspend fun updateBranch(branch: BankBranch)

    @Delete
    suspend fun deleteBranch(branch: BankBranch)

    // Bank Account operations
    @Query("SELECT * FROM bank_accounts WHERE branchId = :branchId ORDER BY accountName")
    fun getAccountsForBranchFlow(branchId: Long): Flow<List<BankAccount>>

    @Query("SELECT * FROM bank_accounts ORDER BY accountName")
    fun getAllBankAccountsFlow(): Flow<List<BankAccount>>

    @Query("SELECT * FROM bank_accounts WHERE id = :id")
    suspend fun getBankAccountById(id: Long): BankAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankAccount(account: BankAccount): Long

    @Update
    suspend fun updateBankAccount(account: BankAccount)

    @Delete
    suspend fun deleteBankAccount(account: BankAccount)
}

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurement_headers ORDER BY date DESC, id DESC")
    fun getAllMeasurementHeadersFlow(): Flow<List<MeasurementHeader>>

    @Query("SELECT * FROM measurement_headers WHERE id = :headerId")
    suspend fun getMeasurementHeaderById(headerId: Long): MeasurementHeader?

    @Query("SELECT * FROM measurement_lines WHERE headerId = :headerId")
    fun getMeasurementLinesForHeaderFlow(headerId: Long): Flow<List<MeasurementLine>>

    @Query("SELECT * FROM measurement_lines WHERE headerId = :headerId")
    suspend fun getMeasurementLinesForHeader(headerId: Long): List<MeasurementLine>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeader(header: MeasurementHeader): Long

    @Update
    suspend fun updateHeader(header: MeasurementHeader)

    @Delete
    suspend fun deleteHeader(header: MeasurementHeader)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLines(lines: List<MeasurementLine>)

    @Query("DELETE FROM measurement_lines WHERE headerId = :headerId")
    suspend fun deleteLinesForHeader(headerId: Long)

    @Transaction
    suspend fun saveMeasurement(header: MeasurementHeader, lines: List<MeasurementLine>): Long {
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
