package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.Account
import com.example.data.Supplier
import com.example.ui.Localization
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RoseRed
import com.example.ui.viewmodel.LedgerViewModel
import com.example.util.FinancialUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val allAccounts by viewModel.accounts.collectAsState()
    val leafAccounts by viewModel.leafAccounts.collectAsState()
    val snapshots by viewModel.accountSnapshots.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingSupplier by remember { mutableStateOf<Supplier?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val direction = Localization.getLayoutDirection(lang)

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Text(
                    text = if (lang == "ar") "سجل ومتابعة الموردين" else "Suppliers & Accounts Payable",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (lang == "ar") "تنظيم وجدولة حسابات الدائنين والربط المحاسبي الممنهج وفق معايير الإقفال الدولي والتزامات التوريد." else "Organize creditors accounts and systematic ledger linkages under international reporting standards (IFRS) and procurement cycles.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(if (lang == "ar") "البحث عن مورد..." else "Search suppliers...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("supplier_search"),
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true
                )

                val filtered = suppliers.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.phone.contains(searchQuery) ||
                    it.email.contains(searchQuery, ignoreCase = true)
                }

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Storefront,
                                contentDescription = "",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = if (lang == "ar") "لا يوجد موردون مسجلون حالياً. انقر على زر الإضافة لتسجيل أول مورد." else "No suppliers registered yet. Press the add button to register your first supplier profile.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filtered) { supplier ->
                            val linkedAccount = allAccounts.find { it.id == supplier.accountId }
                            val balance = remember(supplier.accountId, snapshots) {
                                snapshots.find { it.accountId == supplier.accountId }?.balance ?: 0L
                            }

                            SupplierCard(
                                supplier = supplier,
                                account = linkedAccount,
                                balance = balance,
                                onDelete = { viewModel.deleteSupplier(supplier) },
                                onEdit = { editingSupplier = supplier },
                                onViewStatement = {
                                    viewModel.statementTargetAccountId.value = supplier.accountId
                                    viewModel.navigateToTabFlow.tryEmit(7)
                                },
                                lang = lang
                            )
                        }
                    }
                }
            }

            // Floating Action Button
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = GoldAccent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("add_supplier_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Supplier")
            }

            if (showAddDialog) {
                val liabilitiesLeafs = leafAccounts.filter { it.accountType == com.example.data.AccountType.LIABILITY }
                AddSupplierDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { name, phone, email, linkAccId ->
                        viewModel.addSupplier(name, phone, email, linkAccId)
                        showAddDialog = false
                    },
                    leafAccounts = if (liabilitiesLeafs.isNotEmpty()) liabilitiesLeafs else leafAccounts,
                    lang = lang
                )
            }

            if (editingSupplier != null) {
                EditSupplierDialog(
                    supplier = editingSupplier!!,
                    onDismiss = { editingSupplier = null },
                    onConfirm = { name, phone, email, creditLimit ->
                        viewModel.updateSupplier(editingSupplier!!.copy(name = name, phone = phone, email = email, creditLimit = creditLimit))
                        editingSupplier = null
                    },
                    lang = lang
                )
            }
        }
    }
}

@Composable
fun SupplierCard(
    supplier: Supplier,
    account: Account?,
    balance: Long,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onViewStatement: () -> Unit,
    lang: String
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("supplier_card_${supplier.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Name and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Business,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = supplier.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (lang == "ar") "مستند التوريد والدفع الآجل IFRS" else "Procurement Creditor IFRS Profile",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )

            // Dynamic balances
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (lang == "ar") "الرصيد المستحق (دائن)" else "Outstanding Balance (AP)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    // Display outstanding balance nicely
                    val absVal = Math.abs(balance)
                    val displayBalance = if (lang == "ar") {
                        "${FinancialUtils.formatBase(absVal)} د.ل"
                    } else {
                        "LYD ${FinancialUtils.formatBase(absVal)}"
                    }

                    // Colored balance indicators
                    Text(
                        text = displayBalance,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (balance != 0L) RoseRed else EmeraldGreen
                    )
                }

                if (account != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (lang == "ar") "الحساب المقترن بالدليل" else "General Ledger Account",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${account.accountCode} - ${Localization.getAccountName(account.accountCode, account.name, lang)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Phone and email
            Row(modifier = Modifier.fillMaxWidth()) {
                if (supplier.phone.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = supplier.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                }

                if (supplier.email.isNotEmpty()) {
                    Row(
                        modifier = Modifier.weight(1.2f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = supplier.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Account Statement button
                FilledTonalButton(
                    onClick = onViewStatement,
                    modifier = Modifier.weight(1f).height(38.dp).testTag("supplier_statement_${supplier.id}"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Filled.Book, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (lang == "ar") "كشف الحساب" else "Statement",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Edit button
                FilledTonalButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f).height(38.dp).testTag("supplier_edit_${supplier.id}"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (lang == "ar") "تعديل" else "Edit",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Delete button
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(38.dp).testTag("delete_supplier_${supplier.id}")
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = RoseRed)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(if (lang == "ar") "مسح ملف المورد" else "Delete Supplier Profile") },
            text = {
                Text(
                    text = if (lang == "ar") 
                        "هل أنت متأكد من رغبتك في حذف ملف المورد الحالي [${supplier.name}]؟ سيتم فصل الربط المالي المباشر."
                        else 
                        "Are you sure you want to completely remove this supplier profile [${supplier.name}]? Ledger transactions will remain stored inside linked CoA."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                ) {
                    Text(if (lang == "ar") "حذف" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(if (lang == "ar") "إلغاء الأمر" else "Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSupplierDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Long?) -> Unit,
    leafAccounts: List<Account>,
    lang: String
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    // Strategies: 0 = Auto-open new under 2101, 1 = Link to existing matching name, 2 = Choose exist manually
    var linkStrategy by remember { mutableStateOf(0) }
    var selectedExistAccountId by remember { mutableStateOf<Long?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var accountSearchQuery by remember { mutableStateOf("") }

    // Match checking
    val matchExist = remember(name) {
        leafAccounts.find { it.name.trim().equals(name.trim(), ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(428.dp)
                .wrapContentHeight()
                .padding(8.dp)
                .testTag("add_supplier_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Store,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (lang == "ar") "تسجيل ملف مورد جديد" else "Register Supplier Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (lang == "ar") "اسم المورد الكامل" else "Supplier Full Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supp_input_name"),
                    singleLine = true
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(if (lang == "ar") "رقم الهاتف والاتصال" else "Phone Number") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supp_input_phone"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(if (lang == "ar") "البريد الإلكتروني" else "Email Address") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supp_input_email"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = if (lang == "ar") "إعدادات الربط المالي واستحقاق اليومية" else "Payables Ledger Account Setup",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(Modifier.height(8.dp))

                // Options
                // 1. Auto-open
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { linkStrategy = 0 }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = linkStrategy == 0, onClick = { linkStrategy = 0 })
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (lang == "ar") "فتح حساب فرعي ذو رمز آلي تتبعاً لحساب الدائنين الرئيسي (2101)" else "Auto-create a linked AP account under payables (2101)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 2. Link existing matching
                if (matchExist != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { linkStrategy = 1 }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = linkStrategy == 1, onClick = { linkStrategy = 1 })
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (lang == "ar") "ربط المورد بحساب شجرة مطابق (${matchExist.accountCode})" else "Link to matching account (${matchExist.accountCode})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                    }
                }

                // 3. Link existing manually
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { linkStrategy = 2 }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = linkStrategy == 2, onClick = { linkStrategy = 2 })
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (lang == "ar") "اختيار حساب موجود مسبقاً يدوياً..." else "Manually select an existing payable ledger account...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (linkStrategy == 2) {
                    Spacer(Modifier.height(10.dp))

                    val currentSelection = leafAccounts.find { it.id == selectedExistAccountId }
                    var showAccountSearchDialog by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = currentSelection?.let { "${it.accountCode} - ${Localization.getAccountName(it.accountCode, it.name, lang)}" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (lang == "ar") "الحساب المقترن يدوياً" else "Manual AP Account") },
                        trailingIcon = {
                            IconButton(onClick = { showAccountSearchDialog = true }) {
                                Icon(Icons.Filled.Search, contentDescription = "Search")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAccountSearchDialog = true }
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (lang == "ar") "إلغاء" else "Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val linkId = when (linkStrategy) {
                                    1 -> matchExist?.id
                                    2 -> selectedExistAccountId
                                        else -> null
                                }
                                onConfirm(name, phone, email, linkId)
                            }
                        },
                        enabled = name.isNotBlank() && (linkStrategy != 2 || selectedExistAccountId != null)
                    ) {
                        Text(if (lang == "ar") "تسجيل وحفظ" else "Save Supplier")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSupplierDialog(
    supplier: Supplier,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Long) -> Unit,
    lang: String
) {
    var name by remember { mutableStateOf(supplier.name) }
    var phone by remember { mutableStateOf(supplier.phone) }
    var email by remember { mutableStateOf(supplier.email) }
    var limitInput by remember { mutableStateOf((supplier.creditLimit / 1000.0).toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(400.dp)
                .wrapContentHeight()
                .padding(8.dp)
                .testTag("edit_supplier_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (lang == "ar") "تعديل بيانات المورد" else "Edit Supplier Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (lang == "ar") "اسم المورد" else "Supplier Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_supp_input_name"),
                    singleLine = true
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(if (lang == "ar") "رقم الهاتف" else "Phone") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_supp_input_phone"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(if (lang == "ar") "البريد الإلكتروني" else "Email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_supp_input_email"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it },
                    label = { Text(if (lang == "ar") "الحد الائتماني" else "Credit Limit") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_supp_input_limit"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("edit_supp_btn_cancel")) {
                        Text(Localization.translate(Localization.Key.CANCEL, lang))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val limitMilli = (limitInput.toDoubleOrNull() ?: 0.0) * 1000.0
                                onConfirm(name, phone, email, limitMilli.toLong())
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.testTag("edit_supp_btn_save")
                    ) {
                        Text(if (lang == "ar") "حفظ التعديلات" else "Save Changes")
                    }
                }
            }
        }
    }
}

