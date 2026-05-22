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

class LedgerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = LedgerRepository(db)

    // Global App States
    val isSeeding = MutableStateFlow(true)
    val accounts = repository.allAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val rootAccounts = repository.rootAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val currencies = repository.currencies.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val fiscalYears = repository.fiscalYears.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val vouchers = repository.voucherHeaders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val auditLogs = repository.auditLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customers = repository.customers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val accountSnapshots = repository.allSnapshots.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentLanguage = MutableStateFlow("ar") // Set default to Arabic! Or Toggleable
    val isLibyanMode = MutableStateFlow(true) // Set default to Libyan local style!
    val isDarkMode = MutableStateFlow(true) // Track Dark Theme, true by default
    val currentThemeStyle = MutableStateFlow(com.example.ui.theme.ThemeStyle.CLASSIC_SKY)
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
                val fys = repository.activeFiscalYears.first()
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
    fun createNewVoucherForm() {
        formVoucherNo.value = "VCH-${System.currentTimeMillis().toString().takeLast(6)}"
        formDescription.value = ""
        formVoucherType.value = VoucherType.JOURNAL
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
            val dbLines = repository.getVoucherLines(header.id).first()
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
                    _uiMessage.value = "All line items must have a valid account selected."
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

    suspend fun getAccountStatement(accountId: Long): List<AccountStatementRow> {
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
        return rowsWithHeader.map { (header, line) ->
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

    fun addCustomer(name: String, phone: String, email: String, existingAccountId: Long?) {
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
                    existingAccountId = existingAccountId
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

    suspend fun getVoucherLines(headerId: Long): List<VoucherLine> {
        return repository.getVoucherLines(headerId).first()
    }
}
