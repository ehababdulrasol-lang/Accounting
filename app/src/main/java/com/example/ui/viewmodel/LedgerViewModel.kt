package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.FinancialUtils
import com.example.util.VoucherValidationEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class EditLineItem(
    val tempId: Long = System.nanoTime(),
    val id: Long = 0,
    val accountId: Long = 0,
    val debitStr: String = "",
    val creditStr: String = "",
    val currencyId: Long = 1,
    val exchangeRateStr: String = "1.0",
    val memo: String = ""
) {
    val debitDouble: Double get() = debitStr.toDoubleOrNull() ?: 0.0
    val creditDouble: Double get() = creditStr.toDoubleOrNull() ?: 0.0
    val exchangeRate: Double get() = exchangeRateStr.toDoubleOrNull() ?: 1.0
}

data class AccountStatementRow(
    val date: Long,
    val voucherNo: String,
    val voucherId: Long,
    val memo: String,
    val debit: Long,
    val credit: Long,
    val runningBalance: Long
)

data class CashFlowStatement(
    val openingBalance: Long,
    val operatingInflow: Long,
    val operatingOutflow: Long,
    val investingInflow: Long,
    val investingOutflow: Long,
    val financingInflow: Long,
    val financingOutflow: Long,
    val closingBalance: Long
)

class LedgerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = LedgerRepository(db)

    // Global App States
    val isSeeding = MutableStateFlow(true)
    val navigateToTabFlow = kotlinx.coroutines.flow.MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 1)
    val statementTargetAccountId = MutableStateFlow<Long?>(null)
    val accounts = repository.allAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val rootAccounts = repository.rootAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val currencies = repository.currencies.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val fiscalYears = repository.fiscalYears.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val vouchers = repository.voucherHeaders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allVoucherLines = repository.allVoucherLines.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val auditLogs = repository.auditLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customers = repository.customers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val suppliers = repository.suppliers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val accountSnapshots = repository.allSnapshots.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val cashBoxes = repository.cashBoxes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val banks = repository.banks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allBranches = repository.allBranches.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allBankAccounts = repository.allBankAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentLanguage = MutableStateFlow("ar") // Set default to Arabic! Or Toggleable
    val isLibyanMode = MutableStateFlow(true) // Set default to Libyan local style!
    val isDarkMode = MutableStateFlow(true) // Track Dark Theme, true by default
    val currentThemeStyle = MutableStateFlow(com.example.ui.theme.ThemeStyle.CLASSIC_SKY)

    private val prefs = application.getSharedPreferences("ledger_settings", android.content.Context.MODE_PRIVATE)
    val officialExchangeRate = MutableStateFlow(prefs.getFloat("official_rate", 4.82f).toDouble())
    val parallelExchangeRate = MutableStateFlow(prefs.getFloat("parallel_rate", 7.15f).toDouble())

    fun updateOfficialRate(rate: Double) {
        officialExchangeRate.value = rate
        prefs.edit().putFloat("official_rate", rate.toFloat()).apply()
    }

    fun updateParallelRate(rate: Double) {
        parallelExchangeRate.value = rate
        prefs.edit().putFloat("parallel_rate", rate.toFloat()).apply()
    }
    val localBackups = MutableStateFlow<List<java.io.File>>(emptyList())
    val leafAccounts = accounts.map { list -> list.filter { !it.isGroup } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Feedback States
    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    // Voucher Form State
    val formVoucherNo = MutableStateFlow("")
    val formDescription = MutableStateFlow("")
    val formVoucherType = MutableStateFlow(VoucherType.JOURNAL)
    val formFiscalYearId = MutableStateFlow(0L)
    val formDate = MutableStateFlow(System.currentTimeMillis())
    val formLines = MutableStateFlow<List<EditLineItem>>(emptyList())
    
    // The active voucher being edited (if editing existing draft)
    val editingVoucherId = MutableStateFlow<Long?>(null)

    // Trial Balance Filter Dates (Start of Year to Current Date)
    val trialBalanceStart = MutableStateFlow(0L)
    val trialBalanceEnd = MutableStateFlow(0L)
    val trialBalanceRows = MutableStateFlow<List<TrialBalanceReportRow>>(emptyList())
    val trialBalanceLoading = MutableStateFlow(false)

    val cashFlowLoading = MutableStateFlow(false)
    val cashFlowStatement = MutableStateFlow<CashFlowStatement?>(null)

    init {
        // Initialize trial balance date range for FY 2026 default
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, 2026)
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        trialBalanceStart.value = cal.timeInMillis

        cal.set(Calendar.MONTH, Calendar.DECEMBER)
        cal.set(Calendar.DAY_OF_MONTH, 31)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        trialBalanceEnd.value = cal.timeInMillis

        viewModelScope.launch {
            try {
                repository.checkAndSeedDatabase()
                // Set default fiscal year id once loaded
                val fys = repository.getActiveFiscalYearsSuspend()
                if (fys.isNotEmpty()) {
                    formFiscalYearId.value = fys.first().id
                }
            } catch (e: Exception) {
                _uiMessage.value = "Init Error: ${e.localizedMessage}"
            } finally {
                isSeeding.value = false
                refreshTrialBalance()
            }
        }
    }

    fun showMessage(msg: String) {
        _uiMessage.value = msg
    }

    fun clearMessage() {
        _uiMessage.value = null
    }

    // Chart of Accounts Operations
    fun addAccount(code: String, name: String, parentId: Long?, type: AccountType, currencyId: Long, isGroup: Boolean) {
        viewModelScope.launch {
            try {
                if (code.isBlank() || name.isBlank()) {
                    _uiMessage.value = "Account code and name cannot be blank."
                    return@launch
                }
                repository.createAccount(
                    Account(
                        accountCode = code,
                        name = name,
                        parentId = parentId,
                        accountType = type,
                        currencyId = currencyId,
                        isGroup = isGroup
                    )
                )
                repository.recalculateSnapshots() // Cache refresh
                _uiMessage.value = "Account '$code - $name' successfully created."
            } catch (e: Exception) {
                _uiMessage.value = "Creation failed: ${e.localizedMessage}"
            }
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            try {
                repository.deleteAccount(account)
                repository.recalculateSnapshots()
                _uiMessage.value = "Account '${account.accountCode}' deleted."
            } catch (e: Exception) {
                _uiMessage.value = "Deletion failed: ${e.localizedMessage}"
            }
        }
    }

    fun updateAccount(account: Account, newName: String, newCode: String) {
        viewModelScope.launch {
            try {
                if (newName.isBlank() || newCode.isBlank()) {
                    _uiMessage.value = "Account code and name cannot be blank."
                    return@launch
                }
                val updated = account.copy(name = newName.trim(), accountCode = newCode.trim())
                repository.updateAccount(updated)
                repository.recalculateSnapshots()
                _uiMessage.value = "Account '${account.accountCode}' updated successfully."
            } catch (e: Exception) {
                _uiMessage.value = "Update failed: ${e.localizedMessage}"
            }
        }
    }

    fun getRecursiveBalance(accountId: Long): Flow<Long> {
        return repository.getRecursiveBalance(accountId)
    }

    fun getSubAccounts(parentId: Long): Flow<List<Account>> {
        return repository.getSubAccounts(parentId)
    }

    // Dynamic calculations for edited Voucher Lines
    val liveValidationState = formLines.combine(currencies) { lines, curList ->
        var totalDebitBase = 0L
        var totalCreditBase = 0L

        lines.forEach { line ->
            val cur = curList.find { it.id == line.currencyId } ?: return@forEach
            val baseScale = FinancialUtils.BASE_SCALE_FACTOR

            if (line.debitDouble > 0) {
                val origScaled = FinancialUtils.doubleToOriginalLong(line.debitDouble, cur.decimalPlaces)
                val baseScaled = FinancialUtils.toBaseAmount(origScaled, cur.decimalPlaces, line.exchangeRate)
                totalDebitBase += baseScaled
            }

            if (line.creditDouble > 0) {
                val origScaled = FinancialUtils.doubleToOriginalLong(line.creditDouble, cur.decimalPlaces)
                val baseScaled = FinancialUtils.toBaseAmount(origScaled, cur.decimalPlaces, line.exchangeRate)
                totalCreditBase += baseScaled
            }
        }

        val diff = Math.abs(totalDebitBase - totalCreditBase)
        val isBalanced = diff <= 10L && lines.count { it.accountId > 0L } >= 2

        Triple(totalDebitBase, totalCreditBase, isBalanced)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        Triple(0L, 0L, false)
    )

    // Voucher Actions
    fun createNewVoucherForm(forcedType: com.example.data.VoucherType? = null) {
        val finalType = forcedType ?: com.example.data.VoucherType.JOURNAL
        val prefix = when (finalType) {
            com.example.data.VoucherType.JOURNAL -> "JRN"
            com.example.data.VoucherType.RECEIPT -> "REC"
            com.example.data.VoucherType.PAYMENT -> "PAY"
        }
        formVoucherNo.value = "$prefix-${System.currentTimeMillis().toString().takeLast(6)}"
        formDescription.value = ""
        formVoucherType.value = finalType
        formDate.value = System.currentTimeMillis()
        editingVoucherId.value = null
        
        // Reset to default fiscal year matching current date
        viewModelScope.launch {
            val fys = fiscalYears.value
            if (fys.isNotEmpty()) {
                formFiscalYearId.value = fys.first().id
            }
        }

        // Initialize with 2 empty rows
        formLines.value = listOf(
            EditLineItem(currencyId = 1, exchangeRateStr = "1.0"),
            EditLineItem(currencyId = 1, exchangeRateStr = "1.0")
        )
    }

    fun editVoucherDraft(header: VoucherHeader) {
        if (header.isPosted) {
            _uiMessage.value = "Posted transactions are immutable and cannot be edited."
            return
        }

        editingVoucherId.value = header.id
        formVoucherNo.value = header.voucherNo
        formDescription.value = header.description
        formVoucherType.value = header.type
        formFiscalYearId.value = header.fiscalYearId
        formDate.value = header.date

        viewModelScope.launch {
            val dbLines = repository.getVoucherLinesSuspend(header.id)
            formLines.value = dbLines.map { line ->
                val acc = repository.getAccountById(line.accountId)
                val currency = repository.getCurrencyById(line.currencyId) ?: Currency(code="LYD", name="LYD", decimalPlaces=3)
                
                val debitStr = if (line.debit > 0) FinancialUtils.originalLongToDouble(line.debit, currency.decimalPlaces).toString() else ""
                val creditStr = if (line.credit > 0) FinancialUtils.originalLongToDouble(line.credit, currency.decimalPlaces).toString() else ""

                EditLineItem(
                    id = line.id,
                    accountId = line.accountId,
                    debitStr = debitStr,
                    creditStr = creditStr,
                    currencyId = line.currencyId,
                    exchangeRateStr = line.exchangeRate.toString(),
                    memo = line.memo ?: ""
                )
            }
        }
    }

    fun addVoucherLineRow() {
        val list = formLines.value.toMutableList()
        list.add(EditLineItem(currencyId = 1, exchangeRateStr = "1.0"))
        formLines.value = list
    }

    fun removeVoucherLineRow(index: Int) {
        val list = formLines.value.toMutableList()
        if (list.size > 2) {
            list.removeAt(index)
            formLines.value = list
        } else {
            _uiMessage.value = "Accounting entry must have at least 2 lines."
        }
    }

    fun updateVoucherLineRow(index: Int, updated: EditLineItem) {
        val list = formLines.value.toMutableList()
        list[index] = updated
        formLines.value = list
    }

    fun saveActiveVoucher() {
        viewModelScope.launch {
            try {
                if (formVoucherNo.value.isBlank()) {
                    _uiMessage.value = "Voucher code cannot be blank."
                    return@launch
                }

                val curList = currencies.value
                val domainLines = formLines.value.map { editLine ->
                    val cur = curList.find { it.id == editLine.currencyId }
                        ?: throw IllegalArgumentException("Selected currency not found.")
                    val debitScaled = FinancialUtils.doubleToOriginalLong(editLine.debitDouble, cur.decimalPlaces)
                    val creditScaled = FinancialUtils.doubleToOriginalLong(editLine.creditDouble, cur.decimalPlaces)
                    val baseAmt = if (debitScaled > 0L) {
                        FinancialUtils.toBaseAmount(debitScaled, cur.decimalPlaces, editLine.exchangeRate)
                    } else {
                        -FinancialUtils.toBaseAmount(creditScaled, cur.decimalPlaces, editLine.exchangeRate)
                    }

                    VoucherLine(
                        id = editLine.id,
                        headerId = editingVoucherId.value ?: 0L,
                        accountId = editLine.accountId,
                        debit = debitScaled,
                        credit = creditScaled,
                        currencyId = editLine.currencyId,
                        exchangeRate = editLine.exchangeRate,
                        amountBase = Math.abs(baseAmt), // Header validate uses unsigned
                        memo = editLine.memo
                    )
                }

                val totalBaseAmt = domainLines.sumOf { if (it.debit > 0) it.amountBase else 0L }
                val header = VoucherHeader(
                    id = editingVoucherId.value ?: 0L,
                    voucherNo = formVoucherNo.value,
                    date = formDate.value,
                    type = formVoucherType.value,
                    description = formDescription.value,
                    totalAmountBase = totalBaseAmt,
                    fiscalYearId = formFiscalYearId.value
                )

                // Validation check for draft saves optionally, but let's notify if entries are incomplete
                if (domainLines.any { it.accountId == 0L }) {
                    _uiMessage.value = if (currentLanguage.value == "ar") "يجب اختيار حساب صالح لكل بند من بنود القيد." else "All line items must have a valid account selected."
                    return@launch
                }

                // Enforce that the journal entry must be balanced to be saved/added
                val isBalanced = liveValidationState.value.third
                if (!isBalanced) {
                    _uiMessage.value = if (currentLanguage.value == "ar") "لا يمكن حفظ القيد لأنه غير متوازن الحسابات!" else "Cannot save: Journal entry is not balanced!"
                    return@launch
                }

                val savedId = repository.saveDraftVoucher(header, domainLines)
                _uiMessage.value = "Draft voucher #${formVoucherNo.value} saved successfully."
                editingVoucherId.value = savedId
            } catch (e: Exception) {
                _uiMessage.value = "Save failed: ${e.localizedMessage}"
            }
        }
    }

    fun postActiveVoucher(headerId: Long) {
        viewModelScope.launch {
            try {
                val result = repository.postVoucher(headerId)
                if (result is VoucherValidationEngine.ValidationResult.Success) {
                    _uiMessage.value = "Voucher successfully validated, posted, and frozen in ledger."
                    refreshTrialBalance()
                } else {
                    _uiMessage.value = (result as VoucherValidationEngine.ValidationResult.Error).message
                }
            } catch (e: Exception) {
                _uiMessage.value = "Posting error: ${e.localizedMessage}"
            }
        }
    }

    fun unpostActiveVoucher(headerId: Long) {
        viewModelScope.launch {
            try {
                repository.unpostVoucher(headerId)
                _uiMessage.value = "Posted voucher rolled back to draft."
                refreshTrialBalance()
            } catch (e: Exception) {
                _uiMessage.value = "Unposting error: ${e.localizedMessage}"
            }
        }
    }

    fun deleteActiveVoucher(headerId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteVoucher(headerId)
                _uiMessage.value = "Draft voucher deleted successfully."
                refreshTrialBalance()
            } catch (e: Exception) {
                _uiMessage.value = "Deletion error: ${e.localizedMessage}"
            }
        }
    }

    // Trial Balance Refreshing
    fun refreshTrialBalance() {
        viewModelScope.launch {
            trialBalanceLoading.value = true
            try {
                repository.getTrialBalance(trialBalanceStart.value, trialBalanceEnd.value)
                    .collect { rows ->
                        trialBalanceRows.value = rows
                    }
            } catch (e: Exception) {
                _uiMessage.value = "Trial Balance report generation failed: ${e.localizedMessage}"
            } finally {
                trialBalanceLoading.value = false
                refreshCashFlow()
            }
        }
    }

    // Fiscal Years Operations
    fun addFiscalYear(name: String, start: Long, end: Long) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    _uiMessage.value = "Fiscal period name cannot be blank."
                    return@launch
                }
                repository.createFiscalYear(FiscalYear(name = name, startDate = start, endDate = end))
                _uiMessage.value = "Fiscal Period '$name' created successfully."
            } catch (e: Exception) {
                _uiMessage.value = "Creation failed: ${e.localizedMessage}"
            }
        }
    }

    fun toggleFiscalYearLock(id: Long, currentLock: Boolean) {
        viewModelScope.launch {
            try {
                repository.setFiscalYearLocked(id, !currentLock)
                _uiMessage.value = "Fiscal year status updated successfully."
            } catch (e: Exception) {
                _uiMessage.value = "Status update failed: ${e.localizedMessage}"
            }
        }
    }

    // Language controller
    fun setLanguage(lang: String) {
        currentLanguage.value = lang
    }

    fun setLibyanMode(enabled: Boolean) {
        isLibyanMode.value = enabled
    }

    fun setDarkMode(enabled: Boolean) {
        isDarkMode.value = enabled
    }

    fun setThemeStyle(style: com.example.ui.theme.ThemeStyle) {
        currentThemeStyle.value = style
    }

    private fun getRecursiveSubAccountIds(accountId: Long, allAccounts: List<Account>): Set<Long> {
        val result = mutableSetOf(accountId)
        val toProcess = mutableListOf(accountId)
        while (toProcess.isNotEmpty()) {
            val current = toProcess.removeAt(0)
            val children = allAccounts.filter { it.parentId == current }.map { it.id }
            for (childId in children) {
                if (result.add(childId)) {
                    toProcess.add(childId)
                }
            }
        }
        return result
    }

    suspend fun getAccountStatement(accountId: Long): List<AccountStatementRow> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val allAccounts = accounts.value
        val targetIds = getRecursiveSubAccountIds(accountId, allAccounts)
        
        val postedHeaders = vouchers.value.filter { it.isPosted }
        val allLines = repository.getAllVoucherLines()
        
        val filteredLines = allLines.filter { line ->
            line.accountId in targetIds && postedHeaders.any { it.id == line.headerId }
        }
        
        val rowsWithHeader = filteredLines.mapNotNull { line ->
            val header = postedHeaders.find { it.id == line.headerId } ?: return@mapNotNull null
            header to line
        }.sortedWith(compareBy<Pair<VoucherHeader, VoucherLine>> { it.first.date }.thenBy { it.first.id }.thenBy { it.second.id })
        
        var running = 0L
        rowsWithHeader.map { (header, line) ->
            val debitVal = if (line.debit > 0) line.amountBase else 0L
            val creditVal = if (line.credit > 0) line.amountBase else 0L
            running += (debitVal - creditVal)
            AccountStatementRow(
                date = header.date,
                voucherNo = header.voucherNo,
                voucherId = header.id,
                memo = line.memo ?: "",
                debit = debitVal,
                credit = creditVal,
                runningBalance = running
            )
        }
    }

    fun addCurrency(code: String, name: String, decimals: Int) {
        viewModelScope.launch {
            try {
                if (code.isBlank() || name.isBlank()) {
                    _uiMessage.value = "Currency code and name are required."
                    return@launch
                }
                repository.createCurrency(
                    Currency(
                        code = code.uppercase().trim(),
                        name = name.trim(),
                        decimalPlaces = decimals
                    )
                )
                _uiMessage.value = "Currency '$code' added successfully."
            } catch (e: Exception) {
                _uiMessage.value = "Insertion failed: ${e.localizedMessage}"
            }
        }
    }

    fun addCustomer(name: String, phone: String, email: String, existingAccountId: Long?, groupName: String = "") {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    _uiMessage.value = "Customer name are required."
                    return@launch
                }
                repository.createCustomer(
                    name = name.trim(),
                    phone = phone.trim(),
                    email = email.trim(),
                    existingAccountId = existingAccountId,
                    groupName = groupName.trim()
                )
                _uiMessage.value = "Customer '$name' fully registered in general ledger."
            } catch (e: Exception) {
                _uiMessage.value = "Save customer failed: ${e.localizedMessage}"
            }
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            try {
                repository.deleteCustomer(customer)
                _uiMessage.value = "Customer profiling deleted."
            } catch (e: Exception) {
                _uiMessage.value = "Deletion failed: ${e.localizedMessage}"
            }
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            try {
                if (customer.name.isBlank()) {
                    _uiMessage.value = "Customer name is required."
                    return@launch
                }
                repository.updateCustomer(customer)
                _uiMessage.value = "Customer profiling updated."
            } catch (e: Exception) {
                _uiMessage.value = "Update failed: ${e.localizedMessage}"
            }
        }
    }

    fun addSupplier(name: String, phone: String, email: String, existingAccountId: Long?, groupName: String = "") {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    _uiMessage.value = "Supplier name is required."
                    return@launch
                }
                repository.createSupplier(
                    name = name.trim(),
                    phone = phone.trim(),
                    email = email.trim(),
                    existingAccountId = existingAccountId,
                    groupName = groupName.trim()
                )
                _uiMessage.value = "Supplier '$name' fully registered in general ledger."
            } catch (e: Exception) {
                _uiMessage.value = "Save supplier failed: ${e.localizedMessage}"
            }
        }
    }

    fun deleteSupplier(supplier: Supplier) {
        viewModelScope.launch {
            try {
                repository.deleteSupplier(supplier)
                _uiMessage.value = "Supplier profiling deleted."
            } catch (e: Exception) {
                _uiMessage.value = "Deletion failed: ${e.localizedMessage}"
            }
        }
    }

    fun updateSupplier(supplier: Supplier) {
        viewModelScope.launch {
            try {
                if (supplier.name.isBlank()) {
                    _uiMessage.value = "Supplier name is required."
                    return@launch
                }
                repository.updateSupplier(supplier)
                _uiMessage.value = "Supplier profiling updated."
            } catch (e: Exception) {
                _uiMessage.value = "Update failed: ${e.localizedMessage}"
            }
        }
    }

    fun refreshCashFlow() {
        viewModelScope.launch {
            cashFlowLoading.value = true
            try {
                val statement = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    val cashAccounts = accounts.value.filter { 
                        it.accountCode.startsWith("1101") || 
                        it.accountCode.startsWith("1102") || 
                        it.accountCode.startsWith("1104") ||
                        it.accountCode.startsWith("1105")
                    }
                    val cashIds = cashAccounts.map { it.id }.toSet()
                    
                    val headers = vouchers.value.filter { it.isPosted }
                    val allLines = repository.getAllVoucherLines()
                    
                    // 1. Calculate opening balance (all cash entries before start date)
                    val openingCash = allLines.filter { line ->
                        line.accountId in cashIds && headers.find { it.id == line.headerId }?.let { h -> h.date < trialBalanceStart.value } == true
                    }.sumOf { if (it.debit > 0) it.amountBase else -it.amountBase }
                    
                    // 2. Identify all lines of posted vouchers in the current period
                    val periodHeaders = headers.filter { it.date in trialBalanceStart.value..trialBalanceEnd.value }
                    val periodHeaderIds = periodHeaders.map { it.id }.toSet()
                    val periodLines = allLines.filter { it.headerId in periodHeaderIds }
                    
                    var opIn = 0L
                    var opOut = 0L
                    var invIn = 0L
                    var invOut = 0L
                    var finIn = 0L
                    var finOut = 0L
                    
                    // Group period lines by voucher
                    val voucherGroups = periodLines.groupBy { it.headerId }
                    
                    for ((voucherId, lines) in voucherGroups) {
                        val cashLinesInVoucher = lines.filter { it.accountId in cashIds }
                        if (cashLinesInVoucher.isEmpty()) continue
                        
                        val netCashChange = cashLinesInVoucher.sumOf { if (it.debit > 0) it.amountBase else -it.amountBase }
                        if (netCashChange == 0L) continue
                        
                        val companionLines = lines.filter { it.accountId !in cashIds }
                        
                        for (line in companionLines) {
                            val acc = accounts.value.find { it.id == line.accountId } ?: continue
                            when (acc.accountType) {
                                AccountType.REVENUE -> {
                                    if (netCashChange > 0) opIn += line.amountBase else opOut += line.amountBase
                                }
                                AccountType.EXPENSE -> {
                                    if (netCashChange < 0) opOut += line.amountBase else opIn += line.amountBase
                                }
                                AccountType.ASSET -> {
                                    if (acc.accountCode.startsWith("12") || acc.accountCode.startsWith("13")) {
                                        if (netCashChange < 0) invOut += line.amountBase else invIn += line.amountBase
                                    } else {
                                        if (netCashChange > 0) opIn += line.amountBase else opOut += line.amountBase
                                    }
                                }
                                AccountType.LIABILITY -> {
                                    if (acc.accountCode.startsWith("22") || acc.accountCode.startsWith("23")) {
                                        if (netCashChange > 0) finIn += line.amountBase else finOut += line.amountBase
                                    } else {
                                        if (netCashChange > 0) opIn += line.amountBase else opOut += line.amountBase
                                    }
                                }
                                AccountType.EQUITY -> {
                                    if (netCashChange > 0) finIn += line.amountBase else finOut += line.amountBase
                                }
                            }
                        }
                    }
                    
                    CashFlowStatement(
                        openingBalance = openingCash,
                        operatingInflow = opIn,
                        operatingOutflow = opOut,
                        investingInflow = invIn,
                        investingOutflow = invOut,
                        financingInflow = finIn,
                        financingOutflow = finOut,
                        closingBalance = openingCash + (opIn - opOut) + (invIn - invOut) + (finIn - finOut)
                    )
                }
                cashFlowStatement.value = statement
            } catch (e: Exception) {
                _uiMessage.value = "Cash Flow report generation failed: ${e.localizedMessage}"
            } finally {
                cashFlowLoading.value = false
            }
        }
    }

    suspend fun getVoucherLines(headerId: Long): List<VoucherLine> {
        return repository.getVoucherLinesSuspend(headerId)
    }

    suspend fun getAllVoucherLines(): List<VoucherLine> {
        return repository.getAllVoucherLines()
    }

    // Cash Box Operations
    fun addCashBox(name: String, managerName: String, phone: String, existingAccountId: Long?) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    _uiMessage.value = "Cash box name is required."
                    return@launch
                }
                repository.createCashBox(name.trim(), managerName.trim(), phone.trim(), existingAccountId)
                _uiMessage.value = "Cash box '$name' fully registered in general ledger."
            } catch (e: Exception) {
                _uiMessage.value = "Save cash box failed: ${e.localizedMessage}"
            }
        }
    }

    fun deleteCashBox(cashBox: CashBox) {
        viewModelScope.launch {
            try {
                repository.deleteCashBox(cashBox)
                _uiMessage.value = "Cash box registry deleted."
            } catch (e: Exception) {
                _uiMessage.value = "Deletion failed: ${e.localizedMessage}"
            }
        }
    }

    fun updateCashBox(cashBox: CashBox) {
        viewModelScope.launch {
            try {
                if (cashBox.name.isBlank()) {
                    _uiMessage.value = "Cash box name is required."
                    return@launch
                }
                repository.updateCashBox(cashBox)
                _uiMessage.value = "Cash box registry updated."
            } catch (e: Exception) {
                _uiMessage.value = "Update failed: ${e.localizedMessage}"
            }
        }
    }

    // Banks and branches
    fun addBank(name: String) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    _uiMessage.value = "Bank name is required."
                    return@launch
                }
                repository.createBank(name.trim())
                _uiMessage.value = "Bank '$name' created successfully."
            } catch (e: Exception) {
                _uiMessage.value = "Save bank failed: ${e.localizedMessage}"
            }
        }
    }

    fun updateBank(bank: Bank) {
        viewModelScope.launch {
            try {
                if (bank.name.isBlank()) {
                    _uiMessage.value = "Bank name is required."
                    return@launch
                }
                repository.updateBank(bank)
                _uiMessage.value = "Bank details updated."
            } catch (e: Exception) {
                _uiMessage.value = "Update failed: ${e.localizedMessage}"
            }
        }
    }

    fun deleteBank(bank: Bank) {
        viewModelScope.launch {
            try {
                repository.deleteBank(bank)
                _uiMessage.value = "Bank registry deleted."
            } catch (e: Exception) {
                _uiMessage.value = "Deletion failed: ${e.localizedMessage}"
            }
        }
    }

    fun addBranch(bankId: Long, name: String, code: String, managerName: String) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    _uiMessage.value = "Branch name is required."
                    return@launch
                }
                repository.createBranch(bankId, name.trim(), code.trim(), managerName.trim())
                _uiMessage.value = "Branch '$name' created successfully."
            } catch (e: Exception) {
                _uiMessage.value = "Save branch failed: ${e.localizedMessage}"
            }
        }
    }

    fun updateBranch(branch: BankBranch) {
        viewModelScope.launch {
            try {
                if (branch.name.isBlank()) {
                    _uiMessage.value = "Branch name is required."
                    return@launch
                }
                repository.updateBranch(branch)
                _uiMessage.value = "Branch details updated."
            } catch (e: Exception) {
                _uiMessage.value = "Update failed: ${e.localizedMessage}"
            }
        }
    }

    fun deleteBranch(branch: BankBranch) {
        viewModelScope.launch {
            try {
                repository.deleteBranch(branch)
                _uiMessage.value = "Branch deleted successfully."
            } catch (e: Exception) {
                _uiMessage.value = "Deletion failed: ${e.localizedMessage}"
            }
        }
    }

    fun addBankAccount(
        branchId: Long,
        accountName: String,
        accountNumber: String,
        iban: String,
        existingAccountId: Long?
    ) {
        viewModelScope.launch {
            try {
                if (accountName.isBlank() || accountNumber.isBlank()) {
                    _uiMessage.value = "Account Name and number are required."
                    return@launch
                }
                repository.createBankAccount(branchId, accountName.trim(), accountNumber.trim(), iban.trim(), existingAccountId)
                _uiMessage.value = "Bank account '$accountName' registered and opening ledger entries."
            } catch (e: Exception) {
                _uiMessage.value = "Save bank account failed: ${e.localizedMessage}"
            }
        }
    }

    fun updateBankAccount(bankAccount: BankAccount) {
        viewModelScope.launch {
            try {
                if (bankAccount.accountName.isBlank() || bankAccount.accountNumber.isBlank()) {
                    _uiMessage.value = "Account Name and number are required."
                    return@launch
                }
                repository.updateBankAccount(bankAccount)
                _uiMessage.value = "Bank account details updated."
            } catch (e: Exception) {
                _uiMessage.value = "Update failed: ${e.localizedMessage}"
            }
        }
    }

    fun deleteBankAccount(bankAccount: BankAccount) {
        viewModelScope.launch {
            try {
                repository.deleteBankAccount(bankAccount)
                _uiMessage.value = "Bank account deleted."
            } catch (e: Exception) {
                _uiMessage.value = "Deletion failed: ${e.localizedMessage}"
            }
        }
    }

    // Backup & Restore operations
    fun refreshLocalBackups(context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val files = com.example.data.DatabaseBackupHelper.getLocalBackupFiles(context)
            localBackups.value = files
        }
    }

    fun createBackup(context: android.content.Context) {
        viewModelScope.launch {
            val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.example.data.DatabaseBackupHelper.backupDatabaseLocal(context)
            }
            if (file != null) {
                _uiMessage.value = if (currentLanguage.value == "ar") "تم إنشاء النسخة الاحتياطية بنجاح" else "Backup created successfully"
                refreshLocalBackups(context)
            } else {
                _uiMessage.value = if (currentLanguage.value == "ar") "فشل إنشاء النسخة الاحتياطية" else "Backup creation failed"
            }
        }
    }

    fun restoreBackup(context: android.content.Context, file: java.io.File, onDone: () -> Unit) {
        viewModelScope.launch {
            val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.example.data.DatabaseBackupHelper.restoreDatabaseLocal(context, file)
            }
            if (success) {
                _uiMessage.value = if (currentLanguage.value == "ar") "تم استعادة البيانات بنجاح، جاري إعادة تشغيل التطبيق..." else "Data restored successfully, restarting app..."
                onDone()
            } else {
                _uiMessage.value = if (currentLanguage.value == "ar") "فشل استعادة البيانات" else "Restore failed"
            }
        }
    }

    fun deleteBackup(context: android.content.Context, file: java.io.File) {
        viewModelScope.launch {
            val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.example.data.DatabaseBackupHelper.deleteLocalBackup(file)
            }
            if (success) {
                _uiMessage.value = if (currentLanguage.value == "ar") "تم حذف النسخة الاحتياطية" else "Backup deleted"
                refreshLocalBackups(context)
            } else {
                _uiMessage.value = if (currentLanguage.value == "ar") "فشل حذف النسخة الاحتياطية" else "Failed to delete backup"
            }
        }
    }

    fun exportBackupToUri(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.example.data.DatabaseBackupHelper.exportDatabaseToUri(context, uri)
            }
            if (success) {
                _uiMessage.value = if (currentLanguage.value == "ar") "تم تصدير النسخة الاحتياطية بنجاح" else "Backup exported successfully"
            } else {
                _uiMessage.value = if (currentLanguage.value == "ar") "فشل تصدير النسخة الاحتياطية" else "Failed to export backup"
            }
        }
    }

    fun importBackupFromUri(context: android.content.Context, uri: android.net.Uri, onDone: () -> Unit) {
        viewModelScope.launch {
            val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.example.data.DatabaseBackupHelper.importDatabaseFromUri(context, uri)
            }
            if (success) {
                _uiMessage.value = if (currentLanguage.value == "ar") "تم استيراد النسخة الاحتياطية بنجاح، جاري إعادة تشغيل التطبيق..." else "Backup imported successfully, restarting app..."
                onDone()
            } else {
                _uiMessage.value = if (currentLanguage.value == "ar") "فشل استيراد النسخة الاحتياطية" else "Failed to import backup"
            }
        }
    }
}
