package com.example.data

import android.content.Context
import android.util.Log
import com.example.util.FinancialUtils
import com.example.util.VoucherValidationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

data class TrialBalanceReportRow(
    val accountCode: String,
    val accountName: String,
    val accountType: AccountType,
    val openingDebit: Long, // Scaled by 1,000,000
    val openingCredit: Long,
    val periodDebit: Long,
    val periodCredit: Long,
    val closingDebit: Long,
    val closingCredit: Long
)

class LedgerRepository(private val db: AppDatabase) {

    private val accountDao = db.accountDao()
    private val voucherDao = db.voucherDao()
    private val currencyDao = db.currencyDao()
    private val fiscalYearDao = db.fiscalYearDao()
    private val auditLogDao = db.auditLogDao()
    private val snapshotDao = db.accountBalanceSnapshotDao()
    private val exchangeRateHistoryDao = db.exchangeRateHistoryDao()
    private val customerDao = db.customerDao()
    private val supplierDao = db.supplierDao()
    private val cashBoxDao = db.cashBoxDao()
    private val bankDao = db.bankDao()
    private val measurementDao = db.measurementDao()

    // Flow listings
    val rootAccounts: Flow<List<Account>> = accountDao.getRootAccounts()
    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()
    val currencies: Flow<List<Currency>> = currencyDao.getAllCurrencies()
    val fiscalYears: Flow<List<FiscalYear>> = fiscalYearDao.getAllFiscalYears()
    val activeFiscalYears: Flow<List<FiscalYear>> = fiscalYearDao.getActiveFiscalYears()
    val voucherHeaders: Flow<List<VoucherHeader>> = voucherDao.getAllVoucherHeaders()
    val allVoucherLines: Flow<List<VoucherLine>> = voucherDao.getAllVoucherLinesFlow()
    val auditLogs: Flow<List<AuditLog>> = auditLogDao.getAllLogs()
    val customers: Flow<List<Customer>> = customerDao.getAllCustomersFlow()
    val suppliers: Flow<List<Supplier>> = supplierDao.getAllSuppliersFlow()
    val customerGroups: Flow<List<CustomerGroup>> = customerDao.getAllCustomerGroupsFlow()
    val supplierGroups: Flow<List<SupplierGroup>> = supplierDao.getAllSupplierGroupsFlow()
    val allSnapshots: Flow<List<AccountBalanceSnapshot>> = snapshotDao.getAllSnapshotsFlow()
    val cashBoxes: Flow<List<CashBox>> = cashBoxDao.getAllCashBoxesFlow()
    val banks: Flow<List<Bank>> = bankDao.getAllBanksFlow()
    val allBranches: Flow<List<BankBranch>> = bankDao.getAllBranchesFlow()
    val allBankAccounts: Flow<List<BankAccount>> = bankDao.getAllBankAccountsFlow()
    val measurementHeaders: Flow<List<MeasurementHeader>> = measurementDao.getAllMeasurementHeadersFlow()

    suspend fun getActiveFiscalYearsSuspend(): List<FiscalYear> = withContext(Dispatchers.IO) {
        fiscalYearDao.getActiveFiscalYearsSuspend()
    }

    suspend fun getVoucherLinesSuspend(headerId: Long): List<VoucherLine> = withContext(Dispatchers.IO) {
        voucherDao.getVoucherLinesForHeader(headerId)
    }

    // Seeding API
    suspend fun checkAndSeedDatabase() {
        withContext(Dispatchers.IO) {
            DatabaseSeeder.seedIfEmpty(db)
            recalculateSnapshots() // Initial snapshots calculation
        }
    }

    // Account functions
    fun getSubAccounts(parentId: Long): Flow<List<Account>> = accountDao.getSubAccounts(parentId)

    suspend fun getAllVoucherLines(): List<VoucherLine> = withContext(Dispatchers.IO) {
        voucherDao.getAllVoucherLines()
    }

    fun getRecursiveBalance(accountId: Long): Flow<Long> {
        return accountDao.getRecursiveAccountBalance(accountId)
            .combine(flow { emit(0L) }) { dbBal, _ -> dbBal ?: 0L }
    }

    suspend fun getAccountById(id: Long): Account? = withContext(Dispatchers.IO) {
        accountDao.getAccountById(id)
    }

    suspend fun getCurrencyById(id: Long): Currency? = withContext(Dispatchers.IO) {
        currencyDao.getCurrencyById(id)
    }

    suspend fun getBaseCurrency(): Currency? = withContext(Dispatchers.IO) {
        currencyDao.getBaseCurrency()
    }

    suspend fun createAccount(account: Account): Long = withContext(Dispatchers.IO) {
        val existing = accountDao.getAccountByCode(account.accountCode)
        if (existing != null) {
            throw IllegalArgumentException("Account code '${account.accountCode}' already exists!")
        }
        val id = accountDao.insert(account)
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "CHART_OF_ACCOUNTS",
                action = "ACCOUNT_CREATED",
                details = "Created account '${account.accountCode} - ${account.name}' of type ${account.accountType}."
            )
        )
        id
    }

    suspend fun deleteAccount(account: Account) = withContext(Dispatchers.IO) {
        accountDao.delete(account)
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "CHART_OF_ACCOUNTS",
                action = "ACCOUNT_DELETED",
                details = "Deleted account '${account.accountCode} - ${account.name}'."
            )
        )
    }

    suspend fun updateAccount(account: Account) = withContext(Dispatchers.IO) {
        accountDao.update(account)
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "CHART_OF_ACCOUNTS",
                action = "ACCOUNT_UPDATED",
                details = "Updated account details for '${account.accountCode} - ${account.name}'."
            )
        )
    }

    // Voucher Line Flow
    fun getVoucherLines(headerId: Long): Flow<List<VoucherLine>> = voucherDao.getVoucherLinesForHeaderFlow(headerId)

    suspend fun getVoucherHeaderById(id: Long): VoucherHeader? = withContext(Dispatchers.IO) {
        voucherDao.getVoucherHeaderById(id)
    }

    // Voucher Workflows
    suspend fun saveDraftVoucher(header: VoucherHeader, lines: List<VoucherLine>): Long = withContext(Dispatchers.IO) {
        // Enforce draft state if not already posted
        val existingHeader = if (header.id != 0L) voucherDao.getVoucherHeaderById(header.id) else null
        if (existingHeader != null && existingHeader.isPosted) {
            throw IllegalStateException("Voucher is posted and immutable!")
        }

        val fy = fiscalYearDao.getFiscalYearById(header.fiscalYearId)
            ?: throw IllegalArgumentException("Selected Fiscal Year not found.")
        if (fy.isLocked) {
            throw IllegalStateException("The fiscal period of '${fy.name}' is closed/locked.")
        }

        val draftHeader = header.copy(isPosted = false)
        val savedId = voucherDao.saveVoucher(draftHeader, lines)

        auditLogDao.insert(
            AuditLog(
                voucherId = savedId,
                voucherNo = header.voucherNo,
                action = "DRAFT_SAVED",
                details = "Voucher draft saved. Total Base: ${FinancialUtils.formatBase(header.totalAmountBase)}. Lines count: ${lines.size}."
            )
        )
        savedId
    }

    suspend fun postVoucher(headerId: Long): VoucherValidationEngine.ValidationResult = withContext(Dispatchers.IO) {
        val header = voucherDao.getVoucherHeaderById(headerId)
            ?: return@withContext VoucherValidationEngine.ValidationResult.Error("Voucher header not found.")

        if (header.isPosted) {
            return@withContext VoucherValidationEngine.ValidationResult.Success // Already posted
        }

        // Check Fiscal Year Lock status
        val fy = fiscalYearDao.getFiscalYearById(header.fiscalYearId)
            ?: return@withContext VoucherValidationEngine.ValidationResult.Error("Fiscal Year not found.")
        if (fy.isLocked) {
            return@withContext VoucherValidationEngine.ValidationResult.Error("Fiscal period '${fy.name}' is closed and locked for auditing.")
        }

        val lines = voucherDao.getVoucherLinesForHeader(headerId)
        val validation = VoucherValidationEngine().validateVoucher(header, lines)

        if (validation is VoucherValidationEngine.ValidationResult.Success) {
            // Update posted status
            val postedHeader = header.copy(isPosted = true)
            voucherDao.updateHeader(postedHeader)

            // Dynamic recalculating balances and saving snapshot
            recalculateSnapshots()

            auditLogDao.insert(
                AuditLog(
                    voucherId = headerId,
                    voucherNo = header.voucherNo,
                    action = "POSTED",
                    details = "Voucher successfully validated and posted to Ledger. Totals: ${FinancialUtils.formatBase(header.totalAmountBase)}."
                )
            )
        }
        validation
    }

    suspend fun unpostVoucher(headerId: Long) = withContext(Dispatchers.IO) {
        val header = voucherDao.getVoucherHeaderById(headerId)
            ?: throw IllegalArgumentException("Voucher not found.")

        val fy = fiscalYearDao.getFiscalYearById(header.fiscalYearId)
            ?: throw IllegalArgumentException("Fiscal Year not found.")
        if (fy.isLocked) {
            throw IllegalStateException("Cannot unpost from locked fiscal period '${fy.name}'.")
        }

        val unpostedHeader = header.copy(isPosted = false)
        voucherDao.updateHeader(unpostedHeader)

        // Dynamic recalculating balances
        recalculateSnapshots()

        auditLogDao.insert(
            AuditLog(
                voucherId = headerId,
                voucherNo = header.voucherNo,
                action = "UNPOSTED",
                details = "Voucher unposted and rolled back to Draft status."
            )
        )
    }

    suspend fun deleteVoucher(headerId: Long) = withContext(Dispatchers.IO) {
        val header = voucherDao.getVoucherHeaderById(headerId) ?: return@withContext
        if (header.isPosted) {
            throw IllegalStateException("Posted vouchers cannot be deleted. You must unpost them first.")
        }
        voucherDao.deleteHeader(header)
        auditLogDao.insert(
            AuditLog(
                voucherId = headerId,
                voucherNo = header.voucherNo,
                action = "DELETED",
                details = "Draft voucher deleted successfully."
            )
        )
    }

    /**
     * Compute and cache balances inside AccountBalanceSnapshot for rapid retrieval in views
     */
    suspend fun recalculateSnapshots() = withContext(Dispatchers.IO) {
        snapshotDao.clearAll()
        val calculatedSnapshots = snapshotDao.calculateRecursiveBalances(System.currentTimeMillis())
        snapshotDao.insertAll(calculatedSnapshots)
        Log.d("LedgerRepository", "Account list cached. Total records: ${calculatedSnapshots.size}")
    }

    // Report Queries
    fun getTrialBalance(startDate: Long, endDate: Long): Flow<List<TrialBalanceReportRow>> = flow {
        val accounts = accountDao.getAllAccountsSuspend().filter { !it.isGroup } // Only leaf accounts
        val headers = voucherDao.getAllVoucherHeadersSuspend().filter { it.isPosted }
        val allLines = voucherDao.getAllVoucherLines()
        
        val rows = mutableListOf<TrialBalanceReportRow>()

        for (acc in accounts) {
            val accLines = allLines.filter { it.accountId == acc.id }
            
            // 1. Fetch opening lines (in-memory)
            val linesBefore = accLines.filter { line ->
                val h = headers.find { it.id == line.headerId }
                h != null && h.date < startDate
            }
            
            val openingBal = linesBefore.sumOf { if (it.debit > 0) it.amountBase else -it.amountBase }

            // 2. Fetch period lines (in-memory)
            val linesPeriod = accLines.filter { line ->
                val h = headers.find { it.id == line.headerId }
                h != null && h.date in startDate..endDate
            }

            val periodDebit = linesPeriod.sumOf { if (it.debit > 0) it.amountBase else 0L }
            val periodCredit = linesPeriod.sumOf { if (it.credit > 0) it.amountBase else 0L }

            val closingBal = openingBal + periodDebit - periodCredit

            val openingDebit = if (openingBal >= 0L) openingBal else 0L
            val openingCredit = if (openingBal < 0L) -openingBal else 0L

            val closingDebit = if (closingBal >= 0L) closingBal else 0L
            val closingCredit = if (closingBal < 0L) -closingBal else 0L

            if (openingDebit > 0 || openingCredit > 0 || periodDebit > 0 || periodCredit > 0 || closingDebit > 0 || closingCredit > 0) {
                rows.add(
                    TrialBalanceReportRow(
                        accountCode = acc.accountCode,
                        accountName = acc.name,
                        accountType = acc.accountType,
                        openingDebit = openingDebit,
                        openingCredit = openingCredit,
                        periodDebit = periodDebit,
                        periodCredit = periodCredit,
                        closingDebit = closingDebit,
                        closingCredit = closingCredit
                    )
                )
            }
        }
        emit(rows.sortedBy { it.accountCode })
    }.flowOn(Dispatchers.IO)

    // Fiscal period management
    suspend fun setFiscalYearLocked(id: Long, isLocked: Boolean) = withContext(Dispatchers.IO) {
        val fy = fiscalYearDao.getFiscalYearById(id) ?: return@withContext
        val updated = fy.copy(isLocked = isLocked)
        fiscalYearDao.update(updated)
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "FISCAL_YEAR",
                action = if (isLocked) "PERIOD_CLOSED" else "PERIOD_OPENED",
                details = "Fiscal Year '${fy.name}' is now ${if (isLocked) "LOCKED/CLOSED" else "UNLOCKED/ACTIVE"}."
            )
        )
    }

    suspend fun createFiscalYear(fy: FiscalYear) = withContext(Dispatchers.IO) {
        val id = fiscalYearDao.insert(fy)
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "FISCAL_YEAR",
                action = "PERIOD_CREATED",
                details = "Opened new fiscal period: '${fy.name}'."
            )
        )
        id
    }

    // Dynamic currency management
    suspend fun createCurrency(currency: Currency): Long = withContext(Dispatchers.IO) {
        val id = currencyDao.insert(currency)
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "CURRENCIES",
                action = "CURRENCY_CREATED",
                details = "Registered foreign currency '${currency.code} - ${currency.name}'"
            )
        )
        id
    }

    // Customer setup with automatic dynamic account creation under accounts receivable (1103)
    suspend fun createCustomer(name: String, phone: String, email: String, existingAccountId: Long?, groupName: String = "", groupId: Long? = null): Long = withContext(Dispatchers.IO) {
        var activeAccountLinkId: Long = 0L
        
        if (existingAccountId != null && existingAccountId > 0L) {
            activeAccountLinkId = existingAccountId
            val accName = accountDao.getAccountById(existingAccountId)?.name ?: "#$existingAccountId"
            auditLogDao.insert(
                AuditLog(
                    voucherId = 0,
                    voucherNo = "CUSTOMERS",
                    action = "CUSTOMER_LINKED",
                    details = "Registered customer '$name' and linked to existing CoA Account '$accName'."
                )
            )
        } else {
            // Check if there is an account in the system with exact matching name
            val accounts = accountDao.getAllAccountsSuspend()
            val matchEn = accounts.find { it.name.trim().equals(name.trim(), ignoreCase = true) && !it.isGroup }
            
            if (matchEn != null) {
                activeAccountLinkId = matchEn.id
                auditLogDao.insert(
                    AuditLog(
                        voucherId = 0,
                        voucherNo = "CUSTOMERS",
                        action = "CUSTOMER_AUTO_LINKED",
                        details = "Registered customer '$name' and automatically linked to matching account name '${matchEn.accountCode} - ${matchEn.name}'."
                    )
                )
            } else {
                // Open new account under Accounts Receivable Parent "1103"
                val receivablesParent = accounts.find { it.accountCode == "1103" }
                    ?: throw IllegalStateException("Accounts Receivable (1103) group not seeded.")
                
                // Fetch subaccounts of 1103 to find max suffix code
                val children = accountDao.getSubAccountsSuspend(receivablesParent.id)
                val maxSuffix = children.mapNotNull { child ->
                    child.accountCode.removePrefix("1103").toIntOrNull()
                }.maxOrNull() ?: 0
                
                val nextSuffix = maxSuffix + 1
                val nextCodeStr = "1103" + String.format("%03d", nextSuffix)
                
                val lyCur = currencyDao.getBaseCurrency() ?: throw IllegalStateException("Base currency not initialized.")
                val newAccountId = accountDao.insert(
                    Account(
                        accountCode = nextCodeStr,
                        name = name,
                        parentId = receivablesParent.id,
                        accountType = AccountType.ASSET,
                        currencyId = lyCur.id,
                        isGroup = false
                    )
                )
                
                activeAccountLinkId = newAccountId
                auditLogDao.insert(
                    AuditLog(
                        voucherId = 0,
                        voucherNo = "CUSTOMERS",
                        action = "AUTO_ACCOUNT_CREATED",
                        details = "Opened sub-account '$nextCodeStr - $name' under Accounts Receivable."
                    )
                )
            }
        }
        
        val custId = customerDao.insert(
            Customer(
                name = name,
                phone = phone,
                email = email,
                accountId = activeAccountLinkId,
                groupName = groupName,
                groupId = groupId
            )
        )
        
        recalculateSnapshots()
        custId
    }

    suspend fun deleteCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        customerDao.delete(customer)
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "CUSTOMERS",
                action = "CUSTOMER_DELETED",
                details = "Deleted customer registry profile for '${customer.name}'"
            )
        )
    }

    suspend fun updateCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        customerDao.update(customer)
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "CUSTOMERS",
                action = "CUSTOMER_UPDATED",
                details = "Updated customer profile for '${customer.name}'"
            )
        )
    }

    // Supplier functions
    suspend fun createSupplier(name: String, phone: String, email: String, existingAccountId: Long?, groupName: String = "", groupId: Long? = null): Long = withContext(Dispatchers.IO) {
        var activeAccountLinkId: Long = 0L
        
        if (existingAccountId != null && existingAccountId > 0L) {
            activeAccountLinkId = existingAccountId
            val accName = accountDao.getAccountById(existingAccountId)?.name ?: "#$existingAccountId"
            auditLogDao.insert(
                AuditLog(
                    voucherId = 0,
                    voucherNo = "SUPPLIERS",
                    action = "SUPPLIER_LINKED",
                    details = "Registered supplier '$name' and linked to existing CoA Account '$accName'."
                )
            )
        } else {
            val accounts = accountDao.getAllAccountsSuspend()
            val matchEn = accounts.find { it.name.trim().equals(name.trim(), ignoreCase = true) && !it.isGroup }
            
            if (matchEn != null) {
                activeAccountLinkId = matchEn.id
                auditLogDao.insert(
                    AuditLog(
                        voucherId = 0,
                        voucherNo = "SUPPLIERS",
                        action = "SUPPLIER_AUTO_LINKED",
                        details = "Registered supplier '$name' and automatically linked to matching account name '${matchEn.accountCode} - ${matchEn.name}'."
                    )
                )
            } else {
                // Open new account under Accounts Payable Parent "2101"
                var payablesParent = accounts.find { it.accountCode == "2101" }
                if (payablesParent == null) {
                    val liabilitiesParent = accounts.find { it.accountCode == "21" }
                        ?: throw IllegalStateException("Current Liabilities (21) group not found.")
                    val lyCur = currencyDao.getBaseCurrency() ?: throw IllegalStateException("Base currency not initialized.")
                    val newParentId = accountDao.insert(
                        Account(
                            accountCode = "2101",
                            name = "Accounts Payable",
                            parentId = liabilitiesParent.id,
                            accountType = AccountType.LIABILITY,
                            currencyId = lyCur.id,
                            isGroup = true
                        )
                    )
                    payablesParent = accountDao.getAccountById(newParentId)!!
                } else if (!payablesParent.isGroup) {
                    accountDao.update(payablesParent.copy(isGroup = true))
                }
                
                // Fetch subaccounts of 2101 to find max suffix code
                val children = accountDao.getSubAccountsSuspend(payablesParent.id)
                val maxSuffix = children.mapNotNull { child ->
                    child.accountCode.removePrefix("2101").toIntOrNull()
                }.maxOrNull() ?: 0
                
                val nextSuffix = maxSuffix + 1
                val nextCodeStr = "2101" + String.format("%03d", nextSuffix)
                
                val lyCur = currencyDao.getBaseCurrency() ?: throw IllegalStateException("Base currency not initialized.")
                val newAccountId = accountDao.insert(
                    Account(
                        accountCode = nextCodeStr,
                        name = name,
                        parentId = payablesParent.id,
                        accountType = AccountType.LIABILITY,
                        currencyId = lyCur.id,
                        isGroup = false
                    )
                )
                
                activeAccountLinkId = newAccountId
                auditLogDao.insert(
                    AuditLog(
                        voucherId = 0,
                        voucherNo = "SUPPLIERS",
                        action = "AUTO_ACCOUNT_CREATED",
                        details = "Opened sub-account '$nextCodeStr - $name' under Accounts Payable."
                    )
                )
            }
        }
        
        val supplierId = supplierDao.insert(
            Supplier(
                name = name,
                phone = phone,
                email = email,
                accountId = activeAccountLinkId,
                groupName = groupName,
                groupId = groupId
            )
        )
        
        recalculateSnapshots()
        supplierId
    }

    suspend fun deleteSupplier(supplier: Supplier) = withContext(Dispatchers.IO) {
        supplierDao.delete(supplier)
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "SUPPLIERS",
                action = "SUPPLIER_DELETED",
                details = "Deleted supplier profile for '${supplier.name}'"
            )
        )
    }

    suspend fun updateSupplier(supplier: Supplier) = withContext(Dispatchers.IO) {
        supplierDao.update(supplier)
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "SUPPLIERS",
                action = "SUPPLIER_UPDATED",
                details = "Updated supplier profile for '${supplier.name}'"
            )
        )
    }

    // Cash Box Operations
    suspend fun createCashBox(name: String, managerName: String, phone: String, existingAccountId: Long?): Long = withContext(Dispatchers.IO) {
        var activeAccountLinkId: Long = 0L
        if (existingAccountId != null && existingAccountId > 0L) {
            activeAccountLinkId = existingAccountId
            val accName = accountDao.getAccountById(existingAccountId)?.name ?: "#$existingAccountId"
            auditLogDao.insert(
                AuditLog(
                    voucherId = 0,
                    voucherNo = "CASH_BOXES",
                    action = "CASH_BOX_LINKED",
                    details = "Registered cash box '$name' and linked to existing CoA Account '$accName'."
                )
            )
        } else {
            val accounts = accountDao.getAllAccountsSuspend()
            val matchEn = accounts.find { it.name.trim().equals(name.trim(), ignoreCase = true) && !it.isGroup }
            if (matchEn != null) {
                activeAccountLinkId = matchEn.id
                auditLogDao.insert(
                    AuditLog(
                        voucherId = 0,
                        voucherNo = "CASH_BOXES",
                        action = "CASH_BOX_AUTO_LINKED",
                        details = "Registered cash box '$name' and automatically linked to matching account name '${matchEn.accountCode} - ${matchEn.name}'."
                    )
                )
            } else {
                // Ensure parent 1101 is group
                var cashParent = accounts.find { it.accountCode == "1101" }
                if (cashParent == null) {
                    val currentAssets = accounts.find { it.accountCode == "11" }
                        ?: throw IllegalStateException("Current Assets (11) group not found.")
                    val lyCur = currencyDao.getBaseCurrency() ?: throw IllegalStateException("Base currency not initialized.")
                    val newParentId = accountDao.insert(
                        Account(
                            accountCode = "1101",
                            name = "Cash on Hand",
                            parentId = currentAssets.id,
                            accountType = AccountType.ASSET,
                            currencyId = lyCur.id,
                            isGroup = true
                        )
                    )
                    cashParent = accountDao.getAccountById(newParentId)!!
                } else if (!cashParent.isGroup) {
                    accountDao.update(cashParent.copy(isGroup = true))
                }

                // Find max suffix under 1101
                val children = accountDao.getSubAccountsSuspend(cashParent.id)
                val maxSuffix = children.mapNotNull { child ->
                    child.accountCode.removePrefix("1101").toIntOrNull()
                }.maxOrNull() ?: 0
                val nextSuffix = maxSuffix + 1
                val nextCodeStr = "1101" + String.format("%03d", nextSuffix)

                val lyCur = currencyDao.getBaseCurrency() ?: throw IllegalStateException("Base currency not initialized.")
                val newAccountId = accountDao.insert(
                    Account(
                        accountCode = nextCodeStr,
                        name = name,
                        parentId = cashParent.id,
                        accountType = AccountType.ASSET,
                        currencyId = lyCur.id,
                        isGroup = false
                    )
                )
                activeAccountLinkId = newAccountId
                auditLogDao.insert(
                    AuditLog(
                        voucherId = 0,
                        voucherNo = "CASH_BOXES",
                        action = "AUTO_ACCOUNT_CREATED",
                        details = "Opened cash sub-account '$nextCodeStr - $name' under Cash on Hand."
                    )
                )
            }
        }

        val cbId = cashBoxDao.insert(
            CashBox(
                name = name,
                managerName = managerName,
                phone = phone,
                accountId = activeAccountLinkId
            )
        )
        recalculateSnapshots()
        cbId
    }

    suspend fun deleteCashBox(cashBox: CashBox) = withContext(Dispatchers.IO) {
        cashBoxDao.delete(cashBox)
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "CASH_BOXES",
                action = "CASH_BOX_DELETED",
                details = "Deleted cash box register profile for '${cashBox.name}'"
            )
        )
        recalculateSnapshots()
    }

    suspend fun updateCashBox(cashBox: CashBox) = withContext(Dispatchers.IO) {
        cashBoxDao.update(cashBox)
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "CASH_BOXES",
                action = "CASH_BOX_UPDATED",
                details = "Updated cash box manager info and phone for '${cashBox.name}'"
            )
        )
        recalculateSnapshots()
    }

    // Banks and branches
    fun getBranchesForBank(bankId: Long): Flow<List<BankBranch>> = bankDao.getBranchesForBankFlow(bankId)
    fun getAccountsForBranch(branchId: Long): Flow<List<BankAccount>> = bankDao.getAccountsForBranchFlow(branchId)

    suspend fun createBank(name: String): Long = withContext(Dispatchers.IO) {
        val bid = bankDao.insert(Bank(name = name))
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "BANKS",
                action = "BANK_CREATED",
                details = "Registered financial institution '$name'."
            )
        )
        bid
    }

    suspend fun updateBank(bank: Bank) = withContext(Dispatchers.IO) {
        bankDao.update(bank)
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "BANKS",
                action = "BANK_UPDATED",
                details = "Updated bank name details to '${bank.name}'."
            )
        )
    }

    suspend fun deleteBank(bank: Bank) = withContext(Dispatchers.IO) {
        bankDao.delete(bank)
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "BANKS",
                action = "BANK_DELETED",
                details = "Deleted bank profile '${bank.name}' and CASCADE branch relations."
            )
        )
    }

    suspend fun createBranch(bankId: Long, name: String, code: String, managerName: String): Long = withContext(Dispatchers.IO) {
        val brid = bankDao.insertBranch(BankBranch(bankId = bankId, name = name, code = code, managerName = managerName))
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "BANKS",
                action = "BRANCH_CREATED",
                details = "Opened branch '$name' under Bank #$bankId."
            )
        )
        brid
    }

    suspend fun updateBranch(branch: BankBranch) = withContext(Dispatchers.IO) {
        bankDao.updateBranch(branch)
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "BANKS",
                action = "BRANCH_UPDATED",
                details = "Updated branch info to '$branch'."
            )
        )
    }

    suspend fun deleteBranch(branch: BankBranch) = withContext(Dispatchers.IO) {
        bankDao.deleteBranch(branch)
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "BANKS",
                action = "BRANCH_DELETED",
                details = "Deleted branch '${branch.name}' and CASCADE account relations."
            )
        )
    }

    suspend fun createBankAccount(
        branchId: Long,
        accountName: String,
        accountNumber: String,
        iban: String,
        existingAccountId: Long?
    ): Long = withContext(Dispatchers.IO) {
        var activeAccountLinkId: Long = 0L
        val dispName = "$accountName - $accountNumber"
        
        if (existingAccountId != null && existingAccountId > 0L) {
            activeAccountLinkId = existingAccountId
            val accName = accountDao.getAccountById(existingAccountId)?.name ?: "#$existingAccountId"
            auditLogDao.insert(
                AuditLog(
                    voucherId = 0,
                    voucherNo = "BANKS",
                    action = "BANK_ACCOUNT_LINKED",
                    details = "Linked bank account '$dispName' to existing CoA Account '$accName'."
                )
            )
        } else {
            val accounts = accountDao.getAllAccountsSuspend()
            val matchEn = accounts.find { it.name.trim().equals(dispName.trim(), ignoreCase = true) && !it.isGroup }
            if (matchEn != null) {
                activeAccountLinkId = matchEn.id
                auditLogDao.insert(
                    AuditLog(
                        voucherId = 0,
                        voucherNo = "BANKS",
                        action = "BANK_ACCOUNT_AUTO_LINKED",
                        details = "Linked bank account '$dispName' to matching account name '${matchEn.accountCode} - ${matchEn.name}'."
                    )
                )
            } else {
                // Ensure parent 1104 is group
                var bankParent = accounts.find { it.accountCode == "1104" }
                if (bankParent == null) {
                    val currentAssets = accounts.find { it.accountCode == "11" }
                        ?: throw IllegalStateException("Current Assets (11) group not found.")
                    val lyCur = currencyDao.getBaseCurrency() ?: throw IllegalStateException("Base currency not initialized.")
                    val newParentId = accountDao.insert(
                        Account(
                            accountCode = "1104",
                            name = "Bank Accounts (Current)",
                            parentId = currentAssets.id,
                            accountType = AccountType.ASSET,
                            currencyId = lyCur.id,
                            isGroup = true
                        )
                    )
                    bankParent = accountDao.getAccountById(newParentId)!!
                } else if (!bankParent.isGroup) {
                    accountDao.update(bankParent.copy(isGroup = true))
                }

                // Find max suffix under 1104
                val children = accountDao.getSubAccountsSuspend(bankParent.id)
                val maxSuffix = children.mapNotNull { child ->
                    child.accountCode.removePrefix("1104").toIntOrNull()
                }.maxOrNull() ?: 0
                val nextSuffix = maxSuffix + 1
                val nextCodeStr = "1104" + String.format("%03d", nextSuffix)

                val lyCur = currencyDao.getBaseCurrency() ?: throw IllegalStateException("Base currency not initialized.")
                val newAccountId = accountDao.insert(
                    Account(
                        accountCode = nextCodeStr,
                        name = dispName,
                        parentId = bankParent.id,
                        accountType = AccountType.ASSET,
                        currencyId = lyCur.id,
                        isGroup = false
                    )
                )
                activeAccountLinkId = newAccountId
                auditLogDao.insert(
                    AuditLog(
                        voucherId = 0,
                        voucherNo = "BANKS",
                        action = "AUTO_ACCOUNT_CREATED",
                        details = "Opened banking sub-account '$nextCodeStr - $dispName' under Bank Accounts."
                    )
                )
            }
        }

        val baid = bankDao.insertBankAccount(
            BankAccount(
                branchId = branchId,
                accountName = accountName,
                accountNumber = accountNumber,
                iban = iban,
                accountId = activeAccountLinkId
            )
        )
        recalculateSnapshots()
        baid
    }

    suspend fun updateBankAccount(bankAccount: BankAccount) = withContext(Dispatchers.IO) {
        bankDao.updateBankAccount(bankAccount)
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "BANKS",
                action = "BANK_ACCOUNT_UPDATED",
                details = "Updated banking profiling details count for account id #${bankAccount.id}."
            )
        )
        recalculateSnapshots()
    }

    suspend fun deleteBankAccount(bankAccount: BankAccount) = withContext(Dispatchers.IO) {
        bankDao.deleteBankAccount(bankAccount)
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "BANKS",
                action = "BANK_ACCOUNT_DELETED",
                details = "Deleted banking registry file for account '${bankAccount.accountName}'."
            )
        )
        recalculateSnapshots()
    }

    // Customer Group Operations
    suspend fun createCustomerGroup(name: String, description: String = ""): Long = withContext(Dispatchers.IO) {
        val id = customerDao.insertGroup(CustomerGroup(name = name, description = description))
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "CUSTOMERS",
                action = "CUSTOMER_GROUP_CREATED",
                details = "Created customer group '$name'."
            )
        )
        id
    }

    suspend fun updateCustomerGroup(group: CustomerGroup) = withContext(Dispatchers.IO) {
        customerDao.updateGroup(group)
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "CUSTOMERS",
                action = "CUSTOMER_GROUP_UPDATED",
                details = "Updated customer group '${group.name}'."
            )
        )
    }

    suspend fun deleteCustomerGroup(group: CustomerGroup) = withContext(Dispatchers.IO) {
        customerDao.deleteGroup(group)
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "CUSTOMERS",
                action = "CUSTOMER_GROUP_DELETED",
                details = "Deleted customer group '${group.name}'."
            )
        )
    }

    // Supplier Group Operations
    suspend fun createSupplierGroup(name: String, description: String = ""): Long = withContext(Dispatchers.IO) {
        val id = supplierDao.insertGroup(SupplierGroup(name = name, description = description))
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "SUPPLIERS",
                action = "SUPPLIER_GROUP_CREATED",
                details = "Created supplier group '$name'."
            )
        )
        id
    }

    suspend fun updateSupplierGroup(group: SupplierGroup) = withContext(Dispatchers.IO) {
        supplierDao.updateGroup(group)
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "SUPPLIERS",
                action = "SUPPLIER_GROUP_UPDATED",
                details = "Updated supplier group '${group.name}'."
            )
        )
    }

    suspend fun deleteSupplierGroup(group: SupplierGroup) = withContext(Dispatchers.IO) {
        supplierDao.deleteGroup(group)
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "SUPPLIERS",
                action = "SUPPLIER_GROUP_DELETED",
                details = "Deleted supplier group '${group.name}'."
            )
        )
    }

    // Measurement operations
    fun getMeasurementLinesForHeaderFlow(headerId: Long): Flow<List<MeasurementLine>> {
        return measurementDao.getMeasurementLinesForHeaderFlow(headerId)
    }

    suspend fun getMeasurementLinesForHeader(headerId: Long): List<MeasurementLine> = withContext(Dispatchers.IO) {
        measurementDao.getMeasurementLinesForHeader(headerId)
    }

    suspend fun saveMeasurement(header: MeasurementHeader, lines: List<MeasurementLine>): Long = withContext(Dispatchers.IO) {
        val id = measurementDao.saveMeasurement(header, lines)
        auditLogDao.insert(
            AuditLog(
                voucherId = id,
                voucherNo = "MEASUREMENT",
                action = if (header.id == 0L) "MEASUREMENT_CREATED" else "MEASUREMENT_UPDATED",
                details = "Saved measurement invoice for '${header.customerName}' with total sum of ${FinancialUtils.formatBase(header.totalAmount)}."
            )
        )
        id
    }

    suspend fun deleteMeasurement(header: MeasurementHeader) = withContext(Dispatchers.IO) {
        measurementDao.deleteHeader(header)
        auditLogDao.insert(
            AuditLog(
                voucherId = header.id,
                voucherNo = "MEASUREMENT",
                action = "MEASUREMENT_DELETED",
                details = "Deleted measurement invoice #${header.id} for '${header.customerName}'."
            )
        )
    }

    suspend fun updateMeasurementHeader(header: MeasurementHeader) = withContext(Dispatchers.IO) {
        measurementDao.updateHeader(header)
    }
}
