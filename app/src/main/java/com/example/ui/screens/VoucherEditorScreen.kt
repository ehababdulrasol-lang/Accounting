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

    // Live Double Entry check vectors
    val validationTriple by viewModel.liveValidationState.collectAsState()
    val debitTotalBase = validationTriple.first
    val creditTotalBase = validationTriple.second
    val isBalanced = validationTriple.third

    var fyDropdownExpanded by remember { mutableStateOf(false) }
    var isHeaderCollapsed by remember { mutableStateOf(false) }

    val activeFiscalYear = remember(formFiscalYearId, fiscalYears) {
        fiscalYears.find { it.id == formFiscalYearId }
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
                onClick = onNavigateBack,
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
                    text = if (lang == "ar") "توجيه مزدوج القيد ماليًا" else "Double-entry ledger ledger bookkeeper",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            // Save Draft master CTA
            Button(
                onClick = {
                    viewModel.saveActiveVoucher()
                    onNavigateBack()
                },
                modifier = Modifier
                    .testTag("save_voucher_button")
                    .shadow(4.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
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
                                "السند: ${formVoucherNo.ifBlank { "بلا رقم" }} • ${formDescription.ifBlank { "بلا شرح" }}"
                            } else {
                                "Voucher: ${formVoucherNo.ifBlank { "N/A" }} • ${formDescription.ifBlank { "No Narration" }}"
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
                            text = if (lang == "ar") "تعديل التفاصيل ✎" else "Define Primary ✎",
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

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = formVoucherNo,
                                onValueChange = { viewModel.formVoucherNo.value = it },
                                label = { Text(if (lang == "ar") "رقم السند" else "Voucher No") },
                                trailingIcon = { Icon(Icons.Filled.Tag, null, modifier = Modifier.size(16.dp)) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("voucher_number_input")
                            )

                            // Voucher Category select drop grid
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
                                    label = { Text(if (lang == "ar") "نوع السند" else "Voucher Type") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
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
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Fiscal Period select drop grid
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
                                    label = { Text(if (lang == "ar") "السنة المالية" else "Fiscal Cycle") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fyDropdownExpanded) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
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

                            // Optional auto-generated timestamp representation
                            OutlinedTextField(
                                value = if (lang == "ar") "تاريخ قفل تلقائي" else "Auto Realtime",
                                onValueChange = {},
                                enabled = false,
                                label = { Text(if (lang == "ar") "تاريخ المعاملة" else "Entry Posting Date") },
                                trailingIcon = { Icon(Icons.Filled.DateRange, null, modifier = Modifier.size(16.dp)) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                             )
                        }

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = formDescription,
                            onValueChange = { viewModel.formDescription.value = it },
                            label = { Text(if (lang == "ar") "بيان الشرح العام للسند / مذكرات اليومية" else "General Narration Memo / Document Explain Statement") },
                            placeholder = { Text(if (lang == "ar") "اكتب شرحاً للموازنة والمستندات..." else "Provide internal/external auditing memo...") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("voucher_narration_input")
                        )
                    }
                }
            }
        }

        // Ledger Summary Balancing Console Redesign (The absolute spotlight of accounting UI!)
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
                        // Perfectly centered stable gauge
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(EmeraldGreen)
                        )
                    } else {
                        // Split gauge proportional representation
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

        // Ledger Accounts rows section
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
                onClick = { viewModel.addVoucherLineRow() },
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
                    text = if (lang == "ar") "إضافة سطر مالي" else "Add Line",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Scrollable listing of rows with smooth transitions
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
    var currencyDropdownExpanded by remember { mutableStateOf(false) }

    val activeCurrency = currencies.find { it.id == line.currencyId } ?: currencies.firstOrNull()
    val activeAcc = leafAccounts.find { it.id == line.accountId }
    var showAccountSearchDialog by remember { mutableStateOf(false) }

    val isDebitActive = line.debitStr.isNotBlank()
    val isCreditActive = line.creditStr.isNotBlank()

    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp), spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDebitActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            else if (isCreditActive) RoseRed.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // First row: Circular key index + Account target and delete control button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Monospaced circular card indicator
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = if (isDebitActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else if (isCreditActive) RoseRed.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDebitActive) MaterialTheme.colorScheme.primary
                        else if (isCreditActive) RoseRed
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Modern Searchable Account Trigger text block
                val activeSelectionText = activeAcc?.let {
                    "${it.accountCode} - ${com.example.ui.Localization.getAccountName(it.accountCode, it.name, lang)}"
                } ?: (if (lang == "ar") "حدد حساباً محاسبياً مستهدفاً..." else "Tap to link a ledger account...")

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .clickable { showAccountSearchDialog = true }
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = activeSelectionText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (activeAcc != null) FontWeight.Bold else FontWeight.Medium,
                            color = if (activeAcc != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

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

                // Delete Entry row trigger
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(34.dp)
                        .background(RoseRed.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Remove Row",
                        tint = RoseRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Money entries row side by side with responsive visual states
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = line.debitStr,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        onUpdate(line.copy(debitStr = filtered, creditStr = ""))
                    },
                    label = {
                        Text(
                            text = if (lang == "ar") "المدين (Dr)" else "Debit (Dr)",
                            fontWeight = if (isDebitActive) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = if (isDebitActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.02f) else Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("voucher_row_debit_input_$index"),
                    placeholder = { Text("0.00", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) }
                )

                OutlinedTextField(
                    value = line.creditStr,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        onUpdate(line.copy(creditStr = filtered, debitStr = ""))
                    },
                    label = {
                        Text(
                            text = if (lang == "ar") "الدائن (Cr)" else "Credit (Cr)",
                            fontWeight = if (isCreditActive) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoseRed,
                        focusedLabelColor = RoseRed,
                        unfocusedContainerColor = if (isCreditActive) RoseRed.copy(alpha = 0.02f) else Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("voucher_row_credit_input_$index"),
                    placeholder = { Text("0.00", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) }
                )
            }

            Spacer(Modifier.height(10.dp))

            // Multi currency logic + Exchange Rates (Adaptive UI grid saving vertical clutter)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Currency dropdown capsule selection
                ExposedDropdownMenuBox(
                    expanded = currencyDropdownExpanded,
                    onExpandedChange = { currencyDropdownExpanded = !currencyDropdownExpanded },
                    modifier = Modifier.weight(1.1f)
                ) {
                    OutlinedTextField(
                        value = activeCurrency?.code ?: "LYD",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (lang == "ar") "العملة" else "Currency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyDropdownExpanded) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
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
                                    val isBase = curr.isBase
                                    val rate = if (isBase) "1.0" else line.exchangeRateStr
                                    onUpdate(line.copy(currencyId = curr.id, exchangeRateStr = rate))
                                    currencyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Exchange Rate: Only visible dynamically if non-base currency selected! Otherwise hidden or simple base tag.
                val isNonBase = activeCurrency != null && !activeCurrency.isBase
                if (isNonBase) {
                    OutlinedTextField(
                        value = line.exchangeRateStr,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            onUpdate(line.copy(exchangeRateStr = filtered))
                        },
                        label = { Text(if (lang == "ar") "سعر الصرف" else "Rate") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(0.9f)
                    )
                }

                // Line audit notes
                OutlinedTextField(
                    value = line.memo,
                    onValueChange = { onUpdate(line.copy(memo = it)) },
                    label = { Text(if (lang == "ar") "بيان البند الفرعي" else "Line Narration") },
                    placeholder = { Text(if (lang == "ar") "ملاحظات سطرية..." else "Line detail...") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.8f)
                )
            }
        }
    }
}
