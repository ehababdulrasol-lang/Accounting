package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.Account
import com.example.data.AccountType
import com.example.data.Currency
import com.example.data.VoucherType
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.ui.viewmodel.EditLineItem
import com.example.ui.viewmodel.LedgerViewModel
import com.example.util.FinancialUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherEditorScreen(
    viewModel: LedgerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val allAccounts by viewModel.accounts.collectAsState()
    val leafAccounts = remember(allAccounts) { allAccounts.filter { !it.isGroup } }

    val currencies by viewModel.currencies.collectAsState()
    val fiscalYears by viewModel.fiscalYears.collectAsState()

    val formVoucherNo by viewModel.formVoucherNo.collectAsState()
    val formDescription by viewModel.formDescription.collectAsState()
    val formVoucherType by viewModel.formVoucherType.collectAsState()
    val formFiscalYearId by viewModel.formFiscalYearId.collectAsState()
    val formLines by viewModel.formLines.collectAsState()
    val editingVoucherId by viewModel.editingVoucherId.collectAsState()

    // Live Double Entry checks
    val validationTriple by viewModel.liveValidationState.collectAsState()
    val debitTotalBase = validationTriple.first
    val creditTotalBase = validationTriple.second
    val isBalanced = validationTriple.third

    var fyDropdownExpanded by remember { mutableStateOf(false) }
    var isHeaderCollapsed by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Upper Navigation & Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (editingVoucherId == null) {
                    if (lang == "ar") "إنشاء سند قيد محاسبي" else "Create Ledger Voucher"
                } else {
                    if (lang == "ar") "تعديل مسودة سند القيد" else "Edit Voucher Draft"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.weight(1f))
            
            // Trigger saving
            Button(
                onClick = {
                    viewModel.saveActiveVoucher()
                    onNavigateBack()
                },
                modifier = Modifier.testTag("save_voucher_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (lang == "ar") "حفظ كمسودة" else "Save Draft")
            }
        }

        Spacer(Modifier.height(16.dp))

        // Document Form Header details
        if (isHeaderCollapsed) {
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isHeaderCollapsed = false }
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        val summaryText = if (lang == "ar") {
                            "بيان السند: ${formVoucherNo.ifBlank { "بلا رقم" }} • ${formDescription.ifBlank { "بلا شرح" }}"
                        } else {
                            "Voucher: ${formVoucherNo.ifBlank { "N/A" }} • ${formDescription.ifBlank { "N/A" }}"
                        }
                        Text(
                            text = summaryText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = if (lang == "ar") "تعديل التفاصيل ✎" else "Edit Details ✎",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lang == "ar") "بيانات السند الأساسية" else "Voucher Primary Details",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = { isHeaderCollapsed = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (lang == "ar") "تصغير الجزء العلوي" else "Collapse Header")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = formVoucherNo,
                            onValueChange = { viewModel.formVoucherNo.value = it },
                            label = { Text(if (lang == "ar") "رقم السند/القيد" else "Voucher ID/No") },
                            modifier = Modifier.weight(1f).testTag("voucher_number_input")
                        )

                        // Voucher Category select
                        var typeDropdownExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = typeDropdownExpanded,
                            onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            val classificationText = when (formVoucherType) {
                                VoucherType.JOURNAL -> if (lang == "ar") "قيد تسوية يومية" else "Journal Entry"
                                VoucherType.RECEIPT -> if (lang == "ar") "سند قبض مالي" else "Receipt Voucher"
                                VoucherType.PAYMENT -> if (lang == "ar") "سند صرف نقدي" else "Payment Voucher"
                            }
                            OutlinedTextField(
                                value = classificationText,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(if (lang == "ar") "نوع التصنيف" else "Classification") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = typeDropdownExpanded,
                                onDismissRequest = { typeDropdownExpanded = false }
                            ) {
                                VoucherType.values().forEach { type ->
                                    val labelText = when (type) {
                                        VoucherType.JOURNAL -> if (lang == "ar") "قيد تسوية يومية" else "Journal Entry"
                                        VoucherType.RECEIPT -> if (lang == "ar") "سند قبض مالي" else "Receipt Voucher"
                                        VoucherType.PAYMENT -> if (lang == "ar") "سند صرف نقدي" else "Payment Voucher"
                                    }
                                    DropdownMenuItem(
                                        text = { Text(labelText) },
                                        onClick = {
                                            viewModel.formVoucherType.value = type
                                            typeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Fiscal Period select
                        ExposedDropdownMenuBox(
                            expanded = fyDropdownExpanded,
                            onExpandedChange = { fyDropdownExpanded = !fyDropdownExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            val activeFy = fiscalYears.find { it.id == formFiscalYearId }
                            val activeFyName = if (activeFy != null) {
                                if (lang == "ar") activeFy.name.replace("FY", "سنة") else activeFy.name
                            } else {
                                if (lang == "ar") "اختر الفترة" else "Select Period"
                            }
                            OutlinedTextField(
                                value = activeFyName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(if (lang == "ar") "الفترة المالية" else "Fiscal Period") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fyDropdownExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = fyDropdownExpanded,
                                onDismissRequest = { fyDropdownExpanded = false }
                            ) {
                                fiscalYears.forEach { fy ->
                                    val fyName = if (lang == "ar") fy.name.replace("FY", "سنة") else fy.name
                                    DropdownMenuItem(
                                        text = { 
                                            Row {
                                                Text(fyName)
                                                if (fy.isLocked) {
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(if (lang == "ar") "(مغلق)" else "(LOCKED)", color = RoseRed, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.formFiscalYearId.value = fy.id
                                            fyDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Optional timestamp date representation
                        OutlinedTextField(
                            value = if (lang == "ar") "تلقائي (الوقت الحالي)" else "Auto (Current Time)",
                            onValueChange = {},
                            enabled = false,
                            label = { Text(if (lang == "ar") "تاريخ السند" else "Voucher Entry Date") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = formDescription,
                        onValueChange = { viewModel.formDescription.value = it },
                        label = { Text(if (lang == "ar") "بيان القيد / شرح السند المحاسبي" else "Description / Narration Memo") },
                        modifier = Modifier.fillMaxWidth().testTag("voucher_narration_input")
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Ledger Summary balancing strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isBalanced) EmeraldGreen.copy(alpha = 0.1f)
                    else RoseRed.copy(alpha = 0.1f)
                )
                .border(
                    1.dp,
                    if (isBalanced) EmeraldGreen.copy(alpha = 0.3f)
                    else RoseRed.copy(alpha = 0.3f),
                    RoundedCornerShape(8.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isBalanced) Icons.Filled.CheckCircle else Icons.Filled.Dangerous,
                contentDescription = null,
                tint = if (isBalanced) EmeraldGreen else RoseRed,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = if (isBalanced) {
                        if (lang == "ar") "القيد متوازن في الدفاتر (تم التحقق بنجاح)" else "Ledger Balanced (ACID Verification OK)"
                    } else {
                        if (lang == "ar") "مزدوج القيد محاسبياً غير متطابق!" else "Double-Entry Mismatch!"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isBalanced) EmeraldGreen else RoseRed
                )
                Text(
                    text = if (lang == "ar") {
                        val isLibyanMode = true // or read from flow, formatting is standard
                        val currencySymbol = "د.ل"
                        "إجمالي المدين: ${FinancialUtils.formatBase(debitTotalBase)} • إجمالي الدائن: ${FinancialUtils.formatBase(creditTotalBase)} • الفرق: ${FinancialUtils.formatBase(Math.abs(debitTotalBase - creditTotalBase))} ($currencySymbol)"
                    } else {
                        "Total Dr: ${FinancialUtils.formatBase(debitTotalBase)}  |  Total Cr: ${FinancialUtils.formatBase(creditTotalBase)}  |  Diff: ${FinancialUtils.formatBase(Math.abs(debitTotalBase - creditTotalBase))} (LYD)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (lang == "ar") "أسطر وبنود القيد المالي" else "Ledger Rows",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            TextButton(
                onClick = { viewModel.addVoucherLineRow() },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("add_voucher_line_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (lang == "ar") "إضافة بند/سطر مالي" else "Add Entry Row")
            }
        }

        Spacer(Modifier.height(8.dp))

        // Scrollable listing of rows
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(formLines, key = { _, item -> item.tempId }) { idx, line ->
                VoucherLineRowItem(
                    index = idx,
                    line = line,
                    leafAccounts = leafAccounts,
                    currencies = currencies,
                    lang = lang,
                    onUpdate = { updatedLine ->
                        viewModel.updateVoucherLineRow(idx, updatedLine)
                    },
                    onRemove = {
                        viewModel.removeVoucherLineRow(idx)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherLineRowItem(
    index: Int,
    line: EditLineItem,
    leafAccounts: List<Account>,
    currencies: List<Currency>,
    lang: String,
    onUpdate: (EditLineItem) -> Unit,
    onRemove: () -> Unit
) {
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var currencyDropdownExpanded by remember { mutableStateOf(false) }

    val activeCurrency = currencies.find { it.id == line.currencyId } ?: currencies.firstOrNull()

    val activeAcc = leafAccounts.find { it.id == line.accountId }
    var showAccountSearchDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle Row Index Indicator
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Account Selection
                val activeSelectionText = activeAcc?.let {
                    "${it.accountCode} - ${com.example.ui.Localization.getAccountName(it.accountCode, it.name, lang)}"
                } ?: (if (lang == "ar") "حدد حساباً محاسبياً" else "Select Account")

                OutlinedTextField(
                    value = activeSelectionText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (lang == "ar") "الحساب المستهدف" else "Target Account") },
                    trailingIcon = {
                        IconButton(onClick = { showAccountSearchDialog = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showAccountSearchDialog = true }
                        .testTag("voucher_row_account_input_$index")
                )

                AccountSearchDialog(
                    show = showAccountSearchDialog,
                    onDismiss = { showAccountSearchDialog = false },
                    accounts = leafAccounts,
                    lang = lang,
                    onSelect = { acc ->
                        onUpdate(line.copy(accountId = acc.id, currencyId = acc.currencyId))
                    }
                )

                Spacer(Modifier.width(8.dp))

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Remove Row",
                        tint = RoseRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Money entries row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = line.debitStr,
                    onValueChange = { input ->
                        // Clear credit if debit is input
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        onUpdate(line.copy(debitStr = filtered, creditStr = ""))
                    },
                    label = { Text(if (lang == "ar") "المدين (Dr)" else "Debit (Dr)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("voucher_row_debit_input_$index"),
                    placeholder = { Text("0.00") }
                )

                OutlinedTextField(
                    value = line.creditStr,
                    onValueChange = { input ->
                        // Clear debit if credit is input
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        onUpdate(line.copy(creditStr = filtered, debitStr = ""))
                    },
                    label = { Text(if (lang == "ar") "الدائن (Cr)" else "Credit (Cr)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("voucher_row_credit_input_$index"),
                    placeholder = { Text("0.00") }
                )
            }

            Spacer(Modifier.height(8.dp))

            // Currency conversion parameters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Currency dropdown
                ExposedDropdownMenuBox(
                    expanded = currencyDropdownExpanded,
                    onExpandedChange = { currencyDropdownExpanded = !currencyDropdownExpanded },
                    modifier = Modifier.weight(1.2f)
                ) {
                    OutlinedTextField(
                        value = activeCurrency?.code ?: "LYD",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (lang == "ar") "العملة" else "Currency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = currencyDropdownExpanded,
                        onDismissRequest = { currencyDropdownExpanded = false }
                    ) {
                        currencies.forEach { curr ->
                            val currName = if (lang == "ar") {
                                when (curr.code) {
                                    "LYD" -> "دينار ليبي"
                                    "USD" -> "دولار أمريكي"
                                    "EUR" -> "يورو أوروبي"
                                    else -> curr.name
                                }
                            } else {
                                curr.name
                            }
                            DropdownMenuItem(
                                text = { Text("${curr.code} - $currName") },
                                onClick = {
                                    val isBase = curr.isBase
                                    val rate = if (isBase) "1.0" else line.exchangeRateStr
                                    onUpdate(line.copy(currencyId = curr.id, exchangeRateStr = rate))
                                    currencyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Exchange Rate (only enabled if non-base currency selected)
                val isNonBase = activeCurrency != null && !activeCurrency.isBase
                OutlinedTextField(
                    value = line.exchangeRateStr,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        onUpdate(line.copy(exchangeRateStr = filtered))
                    },
                    label = { Text(if (lang == "ar") "سعر الصرف" else "Exchange Rate") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    enabled = isNonBase,
                    modifier = Modifier.weight(1f)
                )

                // Memo narration line
                OutlinedTextField(
                    value = line.memo,
                    onValueChange = { onUpdate(line.copy(memo = it)) },
                    label = { Text(if (lang == "ar") "بيان/شرح السطر" else "Line Memo") },
                    placeholder = { Text(if (lang == "ar") "ملاحظات السطر اختيارية" else "Optional row notes") },
                    modifier = Modifier.weight(1.8f)
                )
            }
        }
    }
}
