package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Account
import com.example.data.AccountType
import com.example.data.Currency
import com.example.data.VoucherType
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.EditLineItem
import com.example.ui.viewmodel.LedgerViewModel
import com.example.util.FinancialUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherEditorScreen(
    viewModel: LedgerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val allAccounts by viewModel.accounts.collectAsStateWithLifecycle()
    val leafAccounts = remember(allAccounts) { allAccounts.filter { !it.isGroup } }

    val currencies by viewModel.currencies.collectAsStateWithLifecycle()
    val fiscalYears by viewModel.fiscalYears.collectAsStateWithLifecycle()

    val formVoucherNo by viewModel.formVoucherNo.collectAsStateWithLifecycle()
    val formDescription by viewModel.formDescription.collectAsStateWithLifecycle()
    val formVoucherType by viewModel.formVoucherType.collectAsStateWithLifecycle()
    val formFiscalYearId by viewModel.formFiscalYearId.collectAsStateWithLifecycle()
    val formLines by viewModel.formLines.collectAsStateWithLifecycle()
    val editingVoucherId by viewModel.editingVoucherId.collectAsStateWithLifecycle()
    val formDate by viewModel.formDate.collectAsStateWithLifecycle()

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val formattedFormDate = remember(formDate) { dateFormatter.format(Date(formDate)) }

    // Live Double Entry check vectors
    val validationTriple by viewModel.liveValidationState.collectAsStateWithLifecycle()
    val debitTotalBase = validationTriple.first
    val creditTotalBase = validationTriple.second
    val isBalanced = validationTriple.third

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    var fyDropdownExpanded by remember { mutableStateOf(false) }
    var isHeaderCollapsed by remember { mutableStateOf(false) }

    val activeFiscalYear = remember(formFiscalYearId, fiscalYears) {
        fiscalYears.find { it.id == formFiscalYearId }
    }

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = formDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selected ->
                            viewModel.formDate.value = selected
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(if (lang == "ar") "موافق" else "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(if (lang == "ar") "إلغاء" else "Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Modal/Dialog state managers for Line rows inputs
    var showLineDialog by remember { mutableStateOf(false) }
    var editingLineIndex by remember { mutableStateOf<Int?>(null) } // null means adding a new line
    var dialogAccountId by remember { mutableStateOf(0L) }
    var dialogSideIsDebit by remember { mutableStateOf(true) } // true = Debit, false = Credit
    var dialogAmountStr by remember { mutableStateOf("") }
    var dialogCurrencyId by remember { mutableStateOf(1L) }
    var dialogExchangeRateStr by remember { mutableStateOf("1.0") }
    var dialogMemo by remember { mutableStateOf("") }

    // Dual form temporary states for PAYMENT and RECEIPT
    var dualAmountStr by remember(formLines) {
        val firstLine = formLines.firstOrNull()
        mutableStateOf(firstLine?.let { if (it.debitStr.isNotEmpty()) it.debitStr else it.creditStr } ?: "")
    }
    var dualCurrencyId by remember(formLines) {
        mutableStateOf(formLines.firstOrNull()?.currencyId ?: 1L)
    }
    var dualExchangeRateStr by remember(formLines) {
        mutableStateOf(formLines.firstOrNull()?.exchangeRateStr ?: "1.0")
    }
    var payeeAccountId by remember(formLines) {
        mutableStateOf(formLines.getOrNull(0)?.accountId ?: 0L)
    }
    var payerAccountId by remember(formLines) {
        mutableStateOf(formLines.getOrNull(1)?.accountId ?: 0L)
    }
    var payeeMemo by remember(formLines) {
        mutableStateOf(formLines.getOrNull(0)?.memo ?: "")
    }
    var payerMemo by remember(formLines) {
        mutableStateOf(formLines.getOrNull(1)?.memo ?: "")
    }

    // A helper to push Dual values down into the standard formLines MutableStateFlow
    val updateDualVoucherState = { amount: String, currId: Long, rateStr: String, payerId: Long, payeeId: Long, payerNote: String, payeeNote: String ->
        val l0 = EditLineItem(
            tempId = formLines.getOrNull(0)?.tempId ?: System.nanoTime(),
            id = formLines.getOrNull(0)?.id ?: 0L,
            accountId = payeeId,
            debitStr = amount,
            creditStr = "",
            currencyId = currId,
            exchangeRateStr = rateStr,
            memo = payeeNote
        )
        val l1 = EditLineItem(
            tempId = formLines.getOrNull(1)?.tempId ?: System.nanoTime(),
            id = formLines.getOrNull(1)?.id ?: 0L,
            accountId = payerId,
            debitStr = "",
            creditStr = amount,
            currencyId = currId,
            exchangeRateStr = rateStr,
            memo = payerNote
        )
        viewModel.formLines.value = listOf(l0, l1)
    }

    // Force dual rows on receipt/payment
    LaunchedEffect(formVoucherType) {
        if (formVoucherType == VoucherType.RECEIPT || formVoucherType == VoucherType.PAYMENT) {
            if (formLines.size != 2) {
                val baseCurrId = currencies.firstOrNull()?.id ?: 1L
                val l0 = formLines.getOrNull(0) ?: EditLineItem(currencyId = baseCurrId, exchangeRateStr = "1.0")
                val l1 = formLines.getOrNull(1) ?: EditLineItem(currencyId = baseCurrId, exchangeRateStr = "1.0")
                
                // Force l0 to be debit, l1 to be credit
                val syncedL0 = l0.copy(
                    debitStr = if (l0.debitStr.isEmpty() && l0.creditStr.isNotEmpty()) l0.creditStr else if (l0.debitStr.isEmpty()) "0.0" else l0.debitStr,
                    creditStr = ""
                )
                val syncedL1 = l1.copy(
                    creditStr = if (l1.creditStr.isEmpty() && l1.debitStr.isNotEmpty()) l1.debitStr else if (l1.creditStr.isEmpty()) "0.0" else l1.creditStr,
                    debitStr = ""
                )
                viewModel.formLines.value = listOf(syncedL0, syncedL1)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Upper Navigation & Action Bar Redesign
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onNavigateBack()
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (editingVoucherId == null) {
                        if (lang == "ar") "صياغة سند جديد" else "Draft New Voucher"
                    } else {
                        if (lang == "ar") "تعديل تفاصيل المسودة" else "Edit Draft Voucher"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (lang == "ar") "توجيه مزدوج القيد ماليًا" else "Double-entry ledger bookkeeper",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            // Save Draft master CTA
            Button(
                onClick = {
                    if (isBalanced) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        viewModel.saveActiveVoucher()
                        onNavigateBack()
                    }
                },
                enabled = isBalanced,
                modifier = Modifier
                    .testTag("save_voucher_button")
                    .shadow(if (isBalanced) 4.dp else 0.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBalanced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (lang == "ar") "حفظ مسودة" else "Save Draft",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Document Form Header details with high-end expand/collapse transitions
        AnimatedContent(
            targetState = isHeaderCollapsed,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "HeaderDetailsTrans"
        ) { collapsed ->
            if (collapsed) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isHeaderCollapsed = false }
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            val summary = if (lang == "ar") {
                                "السند: ${formVoucherNo.ifBlank { "بلا رقم" }} • ${formDescription.ifBlank { "بلا شرح" }} • $formattedFormDate"
                            } else {
                                "Voucher: ${formVoucherNo.ifBlank { "N/A" }} • ${formDescription.ifBlank { "No Narration" }} • $formattedFormDate"
                            }
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Text(
                            text = if (lang == "ar") "تعديل البيانات الأساسية ✎" else "Define Primary ✎",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                    )
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.FactCheck,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (lang == "ar") "بيانات السند الأساسية" else "Primary Ledger Meta",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            TextButton(
                                onClick = { isHeaderCollapsed = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (lang == "ar") "تصغير اللوحة" else "Collapse",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. Voucher Type Selection Dropdown Box
                            var typeDropdownExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = typeDropdownExpanded,
                                onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                val classificationText = when (formVoucherType) {
                                    VoucherType.JOURNAL -> if (lang == "ar") "تسوية" else "Journal"
                                    VoucherType.RECEIPT -> if (lang == "ar") "قبض" else "Receipt"
                                    VoucherType.PAYMENT -> if (lang == "ar") "صرف" else "Payment"
                                }
                                OutlinedTextField(
                                    value = classificationText,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(if (lang == "ar") "نوع السند" else "Type") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                                )
                                ExposedDropdownMenu(
                                    expanded = typeDropdownExpanded,
                                    onDismissRequest = { typeDropdownExpanded = false }
                                ) {
                                    VoucherType.values().forEach { type ->
                                        val labelText = when (type) {
                                            VoucherType.JOURNAL -> if (lang == "ar") "قيد تسوية يومية (Daily Journal)" else "Journal Entry"
                                            VoucherType.RECEIPT -> if (lang == "ar") "سند قبض مالي (Inward Receipt)" else "Receipt Voucher"
                                            VoucherType.PAYMENT -> if (lang == "ar") "سند صرف نقدي (Cash Outflow)" else "Payment Voucher"
                                        }
                                        DropdownMenuItem(
                                            text = { Text(labelText, style = MaterialTheme.typography.bodyMedium) },
                                            onClick = {
                                                viewModel.formVoucherType.value = type
                                                typeDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // 2. Voucher Number Input TextField
                            OutlinedTextField(
                                value = formVoucherNo,
                                onValueChange = { viewModel.formVoucherNo.value = it },
                                label = { Text(if (lang == "ar") "رقم السند" else "Voucher No") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("voucher_number_input")
                            )

                            // 3. Fiscal Cycle Selection Dropdown Box
                            ExposedDropdownMenuBox(
                                expanded = fyDropdownExpanded,
                                onExpandedChange = { fyDropdownExpanded = !fyDropdownExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                val activeFyName = if (activeFiscalYear != null) {
                                    if (lang == "ar") activeFiscalYear.name.replace("FY", "سنة") else activeFiscalYear.name
                                } else {
                                    if (lang == "ar") "اختر الفترة" else "Select Period"
                                }
                                OutlinedTextField(
                                    value = activeFyName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(if (lang == "ar") "السنة المالية" else "Fiscal Year") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fyDropdownExpanded) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                                )
                                ExposedDropdownMenu(
                                    expanded = fyDropdownExpanded,
                                    onDismissRequest = { fyDropdownExpanded = false }
                                ) {
                                    fiscalYears.forEach { fy ->
                                        val fyName = if (lang == "ar") fy.name.replace("FY", "سنة") else fy.name
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(fyName, style = MaterialTheme.typography.bodyMedium)
                                                    if (fy.isLocked) {
                                                        Spacer(Modifier.width(8.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(RoseRed.copy(alpha = 0.15f))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = if (lang == "ar") "مقفل" else "LOCKED",
                                                                color = RoseRed,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
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
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Column 1: Configurable Date Picker TextField with click recognition
                            Box(
                                modifier = Modifier
                                    .weight(1.1f)
                            ) {
                                OutlinedTextField(
                                    value = formattedFormDate,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(if (lang == "ar") "تاريخ المعاملة" else "Posting Date") },
                                    trailingIcon = { Icon(Icons.Filled.DateRange, null, modifier = Modifier.size(16.dp)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                // Overlaid touch interceptor
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable {
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                            showDatePicker = true
                                        }
                                )
                            }

                            // Column 2: Compact Description / Voucher Narration Memo Input Field
                            OutlinedTextField(
                                value = formDescription,
                                onValueChange = { viewModel.formDescription.value = it },
                                label = { Text(if (lang == "ar") "بيان الشرح العام للسند" else "General Narration") },
                                placeholder = { Text(if (lang == "ar") "اكتب شرحاً للموازنة والمستندات..." else "Provide internal memo...") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                modifier = Modifier
                                    .weight(1.9f)
                                    .testTag("voucher_narration_input")
                            )
                        }
                    }
                }
            }
        }

        // Ledger Summary Balancing Console Redesign
        val diffBase = Math.abs(debitTotalBase - creditTotalBase)
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = if (isBalanced) EmeraldGreen.copy(alpha = 0.3f) else RoseRed.copy(alpha = 0.3f)),
            colors = CardDefaults.cardColors(
                containerColor = if (isBalanced) EmeraldGreen.copy(alpha = 0.04f) else RoseRed.copy(alpha = 0.04f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.3.dp,
                if (isBalanced) EmeraldGreen.copy(alpha = 0.3f) else RoseRed.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Header of status dashboard
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(
                                color = if (isBalanced) EmeraldGreen.copy(alpha = 0.15f) else RoseRed.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBalanced) Icons.Filled.CheckCircle else Icons.Filled.Dangerous,
                            contentDescription = null,
                            tint = if (isBalanced) EmeraldGreen else RoseRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBalanced) {
                                if (lang == "ar") "توازن القيد متطابق ومثالي (آمن)" else "Ledger Entries Balanced (ACID Verified)"
                            } else {
                                if (lang == "ar") "غير متطابق! يوجد فرق بالمدين/الدائن" else "Double-Entry Out of Balance!"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isBalanced) EmeraldGreen else RoseRed
                        )
                        Text(
                            text = if (lang == "ar") {
                                "إجمالي المدين: ${FinancialUtils.formatBase(debitTotalBase)} د.ل • الدائن: ${FinancialUtils.formatBase(creditTotalBase)} د.ل"
                            } else {
                                "Dr Total: ${FinancialUtils.formatBase(debitTotalBase)} LYD  |  Cr Total: ${FinancialUtils.formatBase(creditTotalBase)} LYD"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Interactive progress comparison gauge bar
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                ) {
                    if (isBalanced) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(EmeraldGreen)
                        )
                    } else {
                        val totalBase = (debitTotalBase + creditTotalBase).coerceAtLeast(1L)
                        val debitWeight = (debitTotalBase.toFloat() / totalBase).coerceIn(0.05f, 0.95f)
                        val creditWeight = 1f - debitWeight

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(debitWeight)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(creditWeight)
                                .background(RoseRed)
                        )
                    }
                }

                // Smart Auto Balance gaps fixer
                if (!isBalanced && diffBase > 0 && formLines.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        ElevatedButton(
                            onClick = {
                                val isDrMore = debitTotalBase > creditTotalBase
                                val amountStr = String.format("%.2f", diffBase / 100.0)
                                val lastIndex = formLines.lastIndex
                                if (lastIndex >= 0) {
                                    val lastLine = formLines[lastIndex]
                                    if (isDrMore) {
                                        viewModel.updateVoucherLineRow(
                                            lastIndex,
                                            lastLine.copy(creditStr = amountStr, debitStr = "")
                                        )
                                    } else {
                                        viewModel.updateVoucherLineRow(
                                            lastIndex,
                                            lastLine.copy(debitStr = amountStr, creditStr = "")
                                        )
                                    }
                                }
                            },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Build,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            val dynamicAmountStr = FinancialUtils.formatBase(diffBase)
                            Text(
                                text = if (lang == "ar") "موازنة تلقائية بالفرق (+$dynamicAmountStr)" else "Auto-Balance Offset (+$dynamicAmountStr)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        if (formVoucherType == VoucherType.RECEIPT || formVoucherType == VoucherType.PAYMENT) {
            // Simplified dual-account form section for receipt and payment vouchers
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header info
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (formVoucherType == VoucherType.RECEIPT) Icons.Filled.ArrowCircleDown else Icons.Filled.ArrowCircleUp,
                                contentDescription = null,
                                tint = if (formVoucherType == VoucherType.RECEIPT) EmeraldGreen else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (formVoucherType == VoucherType.RECEIPT) {
                                    if (lang == "ar") "تفاصيل المعاملة (سند القبض المزدوج المالي)" else "Inward Receipt Voucher (Dual Account)"
                                } else {
                                    if (lang == "ar") "تفاصيل المعاملة (سند صرف مالي مبسط)" else "Simplified Payment Voucher (Dual Account)"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // SECTION 1: Amount, Currency, and Exchange Rate Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = dualAmountStr,
                                onValueChange = {
                                    dualAmountStr = it
                                    updateDualVoucherState(it, dualCurrencyId, dualExchangeRateStr, payerAccountId, payeeAccountId, payerMemo, payeeMemo)
                                },
                                label = { Text(if (lang == "ar") "المبلغ المالي" else "Amount") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                leadingIcon = { Icon(Icons.Filled.Money, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.weight(1.2f).testTag("dual_amount_input"),
                                singleLine = true
                            )

                            // Currency Dropdown Selector box
                            var dualCurrExpanded by remember { mutableStateOf(false) }
                            val activeCurrency = currencies.find { it.id == dualCurrencyId } ?: currencies.firstOrNull()
                            ExposedDropdownMenuBox(
                                expanded = dualCurrExpanded,
                                onExpandedChange = { dualCurrExpanded = !dualCurrExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = activeCurrency?.code ?: "LYD",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(if (lang == "ar") "العملة" else "Currency") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dualCurrExpanded) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                                )
                                ExposedDropdownMenu(
                                    expanded = dualCurrExpanded,
                                    onDismissRequest = { dualCurrExpanded = false }
                                ) {
                                    currencies.forEach { cur ->
                                        DropdownMenuItem(
                                            text = { Text("${cur.code} - ${cur.name}", style = MaterialTheme.typography.bodyMedium) },
                                            onClick = {
                                                dualCurrencyId = cur.id
                                                dualCurrExpanded = false
                                                updateDualVoucherState(dualAmountStr, cur.id, dualExchangeRateStr, payerAccountId, payeeAccountId, payerMemo, payeeMemo)
                                            }
                                        )
                                    }
                                }
                            }

                            if (activeCurrency?.isBase == false) {
                                OutlinedTextField(
                                    value = dualExchangeRateStr,
                                    onValueChange = {
                                        dualExchangeRateStr = it
                                        updateDualVoucherState(dualAmountStr, dualCurrencyId, it, payerAccountId, payeeAccountId, payerMemo, payeeMemo)
                                    },
                                    label = { Text(if (lang == "ar") "سعر الصرف" else "Rate") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(0.8f).testTag("dual_rate_input"),
                                    singleLine = true
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // SECTION 2: Recipient Account & Narration
                        Text(
                            text = if (formVoucherType == VoucherType.RECEIPT) {
                                if (lang == "ar") "١. حساب المستلم (مدفوع له) (الطرف المدين Debit) 🏦 [الخزينة أو البنك المحلي]"
                                else "1. Receiving Vault (Paid to / Dr Side) 🏦 [Vault or Local Bank]"
                            } else {
                                if (lang == "ar") "١. المستفيد / حساب مدفوع له (الطرف المدين Debit) 👤 [مورد، عهدة موظف، مصاريف]"
                                else "1. Payee / Recipient Account (Dr Side) 👤 [Vendor, Employee Vault, Expense]"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var payeeExpanded by remember { mutableStateOf(false) }
                            val activePayee = leafAccounts.find { it.id == payeeAccountId }
                            ExposedDropdownMenuBox(
                                expanded = payeeExpanded,
                                onExpandedChange = { payeeExpanded = !payeeExpanded },
                                modifier = Modifier.weight(1.8f)
                            ) {
                                OutlinedTextField(
                                    value = activePayee?.let { "${it.accountCode} - ${com.example.ui.Localization.getAccountName(it.accountCode, it.name, lang)}" } ?: (if (lang == "ar") "اختر حساب مدفوع له..." else "Choose Recipient..."),
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payeeExpanded) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable).testTag("dual_payee_select")
                                )
                                ExposedDropdownMenu(
                                    expanded = payeeExpanded,
                                    onDismissRequest = { payeeExpanded = false }
                                ) {
                                    leafAccounts.forEach { acc ->
                                        DropdownMenuItem(
                                            text = { Text("${acc.accountCode} - ${com.example.ui.Localization.getAccountName(acc.accountCode, acc.name, lang)}", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium) },
                                            onClick = {
                                                payeeAccountId = acc.id
                                                payeeExpanded = false
                                                updateDualVoucherState(dualAmountStr, dualCurrencyId, dualExchangeRateStr, payerAccountId, acc.id, payerMemo, payeeMemo)
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = payeeMemo,
                                onValueChange = {
                                    payeeMemo = it
                                    updateDualVoucherState(dualAmountStr, dualCurrencyId, dualExchangeRateStr, payerAccountId, payeeAccountId, payerMemo, it)
                                },
                                label = { Text(if (lang == "ar") "البيان (شرح المستلم)" else "Narration / Statement") },
                                singleLine = true,
                                modifier = Modifier.weight(1.4f).testTag("dual_payee_memo_input"),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        // SECTION 3: Payer Account & Narration
                        Text(
                            text = if (formVoucherType == VoucherType.RECEIPT) {
                                if (lang == "ar") "٢. المسدد / حساب دافع (الطرف الدائن Credit) 📤 [الزبون أو مصدر خارجي]"
                                else "2. Payer Account (Paid From / Cr Side) 📤 [Client or Contributor Source]"
                            } else {
                                if (lang == "ar") "٢. مصدر الدفع / حساب دافع (الطرف الدائن Credit) 📤 [الخزينة المفرجة عن النقود]"
                                else "2. Payment Vault / Payer (Cr Side) 📤 [The Vault Disbursing Cash]"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = RoseRed
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var payerExpanded by remember { mutableStateOf(false) }
                            val activePayer = leafAccounts.find { it.id == payerAccountId }
                            ExposedDropdownMenuBox(
                                expanded = payerExpanded,
                                onExpandedChange = { payerExpanded = !payerExpanded },
                                modifier = Modifier.weight(1.8f)
                            ) {
                                OutlinedTextField(
                                    value = activePayer?.let { "${it.accountCode} - ${com.example.ui.Localization.getAccountName(it.accountCode, it.name, lang)}" } ?: (if (lang == "ar") "اختر حساب دافع..." else "Choose Payer..."),
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payerExpanded) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable).testTag("dual_payer_select")
                                )
                                ExposedDropdownMenu(
                                    expanded = payerExpanded,
                                    onDismissRequest = { payerExpanded = false }
                                ) {
                                    leafAccounts.forEach { acc ->
                                        DropdownMenuItem(
                                            text = { Text("${acc.accountCode} - ${com.example.ui.Localization.getAccountName(acc.accountCode, acc.name, lang)}", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium) },
                                            onClick = {
                                                payerAccountId = acc.id
                                                payerExpanded = false
                                                updateDualVoucherState(dualAmountStr, dualCurrencyId, dualExchangeRateStr, acc.id, payeeAccountId, payerMemo, payeeMemo)
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = payerMemo,
                                onValueChange = {
                                    payerMemo = it
                                    updateDualVoucherState(dualAmountStr, dualCurrencyId, dualExchangeRateStr, payerAccountId, payeeAccountId, it, payeeMemo)
                                },
                                label = { Text(if (lang == "ar") "البيان (شرح الدافع)" else "Narration / Statement") },
                                singleLine = true,
                                modifier = Modifier.weight(1.4f).testTag("dual_payer_memo_input"),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                            )
                        }
                    }
                }
            }
        } else {
            // General Journal Multi-line voucher table list
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (lang == "ar") "أسطر وبنود الحركات اليومية" else "Voucher Journal Rows",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (lang == "ar") "يشتمل السند على طرفين مدين ودائن على الأقل لإقفاله" else "At least 2 offsets needed for general ledger journals",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
                
                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        editingLineIndex = null
                        dialogAccountId = 0L
                        dialogSideIsDebit = true
                        dialogAmountStr = ""
                        dialogCurrencyId = currencies.firstOrNull()?.id ?: 1L
                        dialogExchangeRateStr = "1.0"
                        dialogMemo = ""
                        showLineDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("add_voucher_line_button")
                ) {
                    Icon(Icons.Filled.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (lang == "ar") "إضافة بند" else "Add Line",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Scrollable listing of rows with smooth staggered entry sliding transitions
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                itemsIndexed(formLines, key = { _, item -> item.tempId }) { idx, line ->
                    val activeAcc = leafAccounts.find { it.id == line.accountId }
                    val isDebit = line.debitStr.isNotEmpty()
                    val amountText = if (isDebit) line.debitStr else line.creditStr
                    val activeCurrency = currencies.find { it.id == line.currencyId } ?: currencies.firstOrNull()

                    com.example.ui.StaggeredItem(index = idx) {
                        VoucherLineItemCard(
                            index = idx,
                            accountName = activeAcc?.let { "${it.accountCode} - ${com.example.ui.Localization.getAccountName(it.accountCode, it.name, lang)}" } ?: if (lang == "ar") "حدد الحساب المحاسبي من التعديل" else "Not Specified (Edit to map)",
                            isDebit = isDebit,
                            amount = amountText,
                            currencyCode = activeCurrency?.code ?: "LYD",
                            memo = line.memo,
                            onEdit = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                editingLineIndex = idx
                                dialogAccountId = line.accountId
                                dialogSideIsDebit = isDebit
                                dialogAmountStr = amountText
                                dialogCurrencyId = line.currencyId
                                dialogExchangeRateStr = line.exchangeRateStr
                                dialogMemo = line.memo
                                showLineDialog = true
                            },
                            onRemove = {
                                viewModel.removeVoucherLineRow(idx)
                            },
                            lang = lang
                        )
                    }
                }
            }
        }
    }

    // High performance popup voucher sub-row details dialog
    if (showLineDialog) {
        VoucherLineEditorDialog(
            show = showLineDialog,
            onDismiss = { showLineDialog = false },
            lang = lang,
            isEditMode = (editingLineIndex != null),
            leafAccounts = leafAccounts,
            currencies = currencies,
            initialAccountId = dialogAccountId,
            initialSideIsDebit = dialogSideIsDebit,
            initialAmountStr = dialogAmountStr,
            initialCurrencyId = dialogCurrencyId,
            initialExchangeRateStr = dialogExchangeRateStr,
            initialMemo = dialogMemo,
            onApply = { accountId, isDebit, amountStr, currId, rateStr, memo ->
                val lineItem = EditLineItem(
                    tempId = if (editingLineIndex == null) System.nanoTime() else formLines[editingLineIndex!!].tempId,
                    id = if (editingLineIndex == null) 0L else formLines[editingLineIndex!!].id,
                    accountId = accountId,
                    debitStr = if (isDebit) amountStr else "",
                    creditStr = if (!isDebit) amountStr else "",
                    currencyId = currId,
                    exchangeRateStr = rateStr,
                    memo = memo
                )
                val newList = formLines.toMutableList()
                if (editingLineIndex != null) {
                    newList[editingLineIndex!!] = lineItem
                } else {
                    newList.add(lineItem)
                }
                viewModel.formLines.value = newList
            }
        )
    }
}

@Composable
fun VoucherLineItemCard(
    index: Int,
    accountName: String,
    isDebit: Boolean,
    amount: String,
    currencyCode: String,
    memo: String,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    lang: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDebit) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            else RoseRed.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Badge indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = if (isDebit) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else RoseRed.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDebit) MaterialTheme.colorScheme.primary else RoseRed
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = accountName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (memo.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = memo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isDebit) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else RoseRed.copy(alpha = 0.12f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isDebit) {
                                if (lang == "ar") "مدين" else "DEBIT"
                            } else {
                                if (lang == "ar") "دائن" else "CREDIT"
                            },
                            color = if (isDebit) MaterialTheme.colorScheme.primary else RoseRed,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    if (currencyCode.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "($currencyCode)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Amount representation Column
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    text = if (amount.isBlank()) "0.00" else amount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = if (isDebit) MaterialTheme.colorScheme.primary else RoseRed
                )
            }

            Spacer(Modifier.width(4.dp))

            // Action triggers
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = RoseRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherLineEditorDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    lang: String,
    isEditMode: Boolean,
    leafAccounts: List<Account>,
    currencies: List<Currency>,
    initialAccountId: Long,
    initialSideIsDebit: Boolean,
    initialAmountStr: String,
    initialCurrencyId: Long,
    initialExchangeRateStr: String,
    initialMemo: String,
    onApply: (accountId: Long, isDebit: Boolean, amountStr: String, currencyId: Long, exchangeRateStr: String, memo: String) -> Unit
) {
    if (!show) return

    var accountId by remember { mutableStateOf(initialAccountId) }
    var sideIsDebit by remember { mutableStateOf(initialSideIsDebit) }
    var amountStr by remember { mutableStateOf(initialAmountStr) }
    var currencyId by remember { mutableStateOf(initialCurrencyId) }
    var exchangeRateStr by remember { mutableStateOf(initialExchangeRateStr) }
    var memo by remember { mutableStateOf(initialMemo) }

    var showAccountSearch by remember { mutableStateOf(false) }
    var currencyDropdownExpanded by remember { mutableStateOf(false) }

    val activeAcc = leafAccounts.find { it.id == accountId }
    val activeCurrency = currencies.find { it.id == currencyId } ?: currencies.firstOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .widthIn(max = 500.dp),
        confirmButton = {},
        dismissButton = {},
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Filled.EditNote else Icons.Filled.LibraryAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = if (isEditMode) {
                        if (lang == "ar") "تعديل سطر الحركة ماليًا" else "Update Transaction Line"
                    } else {
                        if (lang == "ar") "إضافة سطر حركة جديد" else "Add New Transaction Line"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Account Search trigger
                Column {
                    Text(
                        text = if (lang == "ar") "الحساب المحاسبي" else "Ledger Account",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))

                    val selectionLabel = activeAcc?.let {
                        "${it.accountCode} - ${com.example.ui.Localization.getAccountName(it.accountCode, it.name, lang)}"
                    } ?: if (lang == "ar") "اضغط للبحث واختيار حساب مالي..." else "Tap to search & select account..."

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable { showAccountSearch = true }
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = selectionLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (activeAcc != null) FontWeight.Bold else FontWeight.Medium,
                                color = if (activeAcc != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    AccountSearchDialog(
                        show = showAccountSearch,
                        onDismiss = { showAccountSearch = false },
                        accounts = leafAccounts,
                        lang = lang,
                        onSelect = { acc ->
                            accountId = acc.id
                            currencyId = acc.currencyId
                        }
                    )
                }

                // 2. Type Selector (Debit vs Credit)
                Column {
                    Text(
                        text = if (lang == "ar") "طبيعة الحركة" else "Entry Side",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Debit Selector
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { sideIsDebit = true },
                            colors = CardDefaults.cardColors(
                                containerColor = if (sideIsDebit) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                }
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                if (sideIsDebit) 2.dp else 1.dp,
                                if (sideIsDebit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (sideIsDebit) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    text = if (lang == "ar") "مدين (Debit)" else "Debit (Dr)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (sideIsDebit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // Credit Selector
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { sideIsDebit = false },
                            colors = CardDefaults.cardColors(
                                containerColor = if (!sideIsDebit) {
                                    RoseRed.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                }
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                if (!sideIsDebit) 2.dp else 1.dp,
                                if (!sideIsDebit) RoseRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!sideIsDebit) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = RoseRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    text = if (lang == "ar") "دائن (Credit)" else "Credit (Cr)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (!sideIsDebit) RoseRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // 3. Amount and currency selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            amountStr = filtered
                        },
                        label = { Text(if (lang == "ar") "القيمة المالية" else "Amount") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (sideIsDebit) MaterialTheme.colorScheme.primary else RoseRed,
                            focusedLabelColor = if (sideIsDebit) MaterialTheme.colorScheme.primary else RoseRed
                        ),
                        modifier = Modifier.weight(1.2f)
                    )

                    // Currency Dropdown Selector box
                    ExposedDropdownMenuBox(
                        expanded = currencyDropdownExpanded,
                        onExpandedChange = { currencyDropdownExpanded = !currencyDropdownExpanded },
                        modifier = Modifier.weight(0.8f)
                    ) {
                        OutlinedTextField(
                            value = activeCurrency?.code ?: "LYD",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (lang == "ar") "العملة" else "Currency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyDropdownExpanded) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
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
                                    text = { Text("${curr.code} - $currName", style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        currencyId = curr.id
                                        if (curr.isBase) {
                                            exchangeRateStr = "1.0"
                                        }
                                        currencyDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Exchange Rate (only display if non-base currency selected)
                val isNonBase = activeCurrency != null && !activeCurrency.isBase
                AnimatedVisibility(visible = isNonBase) {
                    OutlinedTextField(
                        value = exchangeRateStr,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            exchangeRateStr = filtered
                        },
                        label = { Text(if (lang == "ar") "سعر الصرف لليورو / الدولار" else "Exchange Rate") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 4. Item Memo (شرح البند الفرعي)
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text(if (lang == "ar") "شرح بند الحركة التفصيلي" else "Line Narration / Memo") },
                    placeholder = { Text(if (lang == "ar") "اكتب بياناً تفصيلياً للبند..." else "Write specific detailing note...") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Dialog Action Buttons row at the bottom
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (lang == "ar") "إلغاء الأمر" else "Cancel",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val isFormValid = accountId != 0L && amountStr.isNotBlank() && (amountStr.toDoubleOrNull() ?: 0.0) > 0.0
                    Button(
                        onClick = {
                            if (isFormValid) {
                                onApply(accountId, sideIsDebit, amountStr, currencyId, exchangeRateStr, memo)
                                onDismiss()
                            }
                        },
                        enabled = isFormValid,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (isEditMode) {
                                if (lang == "ar") "تعديل وحفظ" else "Apply Changes"
                            } else {
                                if (lang == "ar") "إدراج السطر" else "Add Line"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    )
}
