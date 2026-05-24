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
import androidx.compose.material.icons.outlined.*
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
import com.example.data.CashBox
import com.example.ui.Localization
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.ui.viewmodel.LedgerViewModel
import com.example.util.FinancialUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashBoxesScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val cashBoxes by viewModel.cashBoxes.collectAsStateWithLifecycle()
    val allAccounts by viewModel.accounts.collectAsStateWithLifecycle()
    val leafAccounts by viewModel.leafAccounts.collectAsStateWithLifecycle()
    val snapshots by viewModel.accountSnapshots.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingCashBox by remember { mutableStateOf<CashBox?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var cashBoxToDelete by remember { mutableStateOf<CashBox?>(null) }
    var showSuccessMessage by remember { mutableStateOf<String?>(null) }

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
                    text = if (lang == "ar") "إدارة الصناديق والخزائن" else "Cash Boxes & Funds",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (lang == "ar") "إدارة الصناديق النقدية وتحصيل الأموال مع ربط تلقائي لشجرة الحسابات واستعراض فوري للأرصدة." else "Manage cash boxes, safe vaults, and cash flow with automated account provisioning & instant balance auditing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(if (lang == "ar") "بحث في الصناديق..." else "Search cash boxes...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("cashbox_search"),
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

                val filtered = cashBoxes.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.managerName.contains(searchQuery, ignoreCase = true) ||
                    it.phone.contains(searchQuery)
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
                                Icons.Outlined.AddBox,
                                contentDescription = "",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = if (lang == "ar") "لا توجد صناديق مضافة حالياً. انقر على الزر لإضافة أول صندوق" else "No cash boxes registered yet. Touch the plus button to add a fund box.",
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
                        items(filtered) { cashBox ->
                            val linkedAccount = allAccounts.find { it.id == cashBox.accountId }
                            val balance = remember(cashBox.accountId, snapshots) {
                                snapshots.find { it.accountId == cashBox.accountId }?.balance ?: 0L
                            }

                            CashBoxCard(
                                cashBox = cashBox,
                                account = linkedAccount,
                                balance = balance,
                                onDelete = { cashBoxToDelete = cashBox },
                                onEdit = { editingCashBox = cashBox },
                                lang = lang
                            )
                        }
                    }
                }
            }

            // FAB
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("add_cashbox_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Cash Box")
            }

            if (showAddDialog) {
                AddCashBoxDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { name, manager, phone, accountId ->
                        viewModel.addCashBox(name, manager, phone, accountId)
                        showAddDialog = false
                        showSuccessMessage = if (lang == "ar") "تم تسجيل الصندوق بنجاح" else "Cash box registered successfully!"
                    },
                    leafAccounts = leafAccounts,
                    lang = lang
                )
            }

            if (editingCashBox != null) {
                EditCashBoxDialog(
                    cashBox = editingCashBox!!,
                    onDismiss = { editingCashBox = null },
                    onConfirm = { name, custodian, phone ->
                        viewModel.updateCashBox(editingCashBox!!.copy(name = name, managerName = custodian, phone = phone))
                        editingCashBox = null
                        showSuccessMessage = if (lang == "ar") "تم تعديل بيانات الصندوق بنجاح" else "Cash box details updated successfully!"
                    },
                    lang = lang
                )
            }

            if (cashBoxToDelete != null) {
                val cbName = cashBoxToDelete!!.name
                AnimatedDeleteConfirmDialog(
                    title = if (lang == "ar") "تأكيد حذف الصندوق" else "Confirm Cash Box Deletion",
                    message = if (lang == "ar") "هل أنت متأكد من رغبتك في حذف الصندوق/الخزينة: $cbName؟" else "Are you sure you want to delete cash box: $cbName?",
                    lang = lang,
                    onConfirm = {
                        val toDel = cashBoxToDelete!!
                        viewModel.deleteCashBox(toDel)
                        cashBoxToDelete = null
                        showSuccessMessage = if (lang == "ar") "تم حذف الصندوق بنجاح" else "Cash box deleted successfully!"
                    },
                    onDismiss = { cashBoxToDelete = null }
                )
            }

            if (showSuccessMessage != null) {
                SuccessTickDialog(
                    message = showSuccessMessage!!,
                    lang = lang,
                    onDismiss = { showSuccessMessage = null }
                )
            }
        }
    }
}

@Composable
fun CashBoxCard(
    cashBox: CashBox,
    account: Account?,
    balance: Long,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    lang: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = cashBox.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (cashBox.managerName.isNotEmpty()) {
                            Text(
                                text = (if (lang == "ar") "أمين الصندوق: " else "Custodian: ") + cashBox.managerName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_cashbox_${cashBox.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_cashbox_${cashBox.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = RoseRed
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Balance Indicator
                Column {
                    Text(
                        text = if (lang == "ar") "الرصيد الدفتري" else "Book Balance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = FinancialUtils.formatBase(balance),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (balance >= 0L) EmeraldGreen else RoseRed
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (lang == "ar") "د.ل" else "LYD",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // General Ledger mapping
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (lang == "ar") "الحساب المقترن" else "CoA Mapping",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(2.dp))
                    if (account != null) {
                        Text(
                            text = "${account.accountCode} - ${account.name}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = if (lang == "ar") "غير مربوط برمجياً" else "Unlinked",
                            style = MaterialTheme.typography.bodySmall,
                            color = RoseRed
                        )
                    }
                }
            }

            if (cashBox.phone.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = cashBox.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun AddCashBoxDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, manager: String, phone: String, accountId: Long?) -> Unit,
    leafAccounts: List<Account>,
    lang: String
) {
    var name by remember { mutableStateOf("") }
    var managerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    var coaAccountOption by remember { mutableStateOf(0) } // 0 = Auto-Create CoA account under Cash (1101), 1 = Manual select CoA
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("add_cashbox_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = if (lang == "ar") "إضافة صندوق نقدي جديد" else "Register Cash Box",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (lang == "ar") "يرجى كتابة التفاصيل الأساسية لضبط الصندوق باليومية العامة للشركة." else "Provide safe attributes for automatic physical ledger allocation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    // Name Field
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (lang == "ar") "اسم الصندوق / الخزينة" else "Cash Box Name") },
                        modifier = Modifier.fillMaxWidth().testTag("cashbox_name_input"),
                        singleLine = true
                    )
                }

                item {
                    // Manager name field
                    OutlinedTextField(
                        value = managerName,
                        onValueChange = { managerName = it },
                        label = { Text(if (lang == "ar") "أمين الصندوق (المشرف)" else "Custodian Manager") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    // Phone field
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(if (lang == "ar") "رقم الهاتف" else "Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                }

                item {
                    // Mode select
                    Text(
                        text = if (lang == "ar") "تهيئة حساب الأستاذ المالي" else "Accounting Ledger Selection",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = coaAccountOption == 0,
                                onClick = { coaAccountOption = 0 }
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (lang == "ar") "توليد حساب تلقائي برقم آلي تحت الأصول المتداولة (1101)" else "Create ledger account automatically (1101xxx)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = coaAccountOption == 1,
                                onClick = { coaAccountOption = 1 }
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (lang == "ar") "اختيار حساب مالي من شجرة الحسابات الحالية" else "Link to existing Chart of Accounts manually",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (coaAccountOption == 1) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val displayText = selectedAccountId?.let { id ->
                                leafAccounts.find { it.id == id }?.let { "${it.accountCode} - ${it.name}" }
                            } ?: (if (lang == "ar") "اختر الحساب المحاسبي..." else "Select Account...")

                            OutlinedTextField(
                                value = displayText,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { dropdownExpanded = true },
                                trailingIcon = {
                                    IconButton(onClick = { dropdownExpanded = true }) {
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                    }
                                }
                            )

                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 250.dp)
                            ) {
                                leafAccounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text("${acc.accountCode} - ${acc.name}") },
                                        onClick = {
                                            selectedAccountId = acc.id
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    // Actions row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(if (lang == "ar") "إلغاء الأمر" else "Cancel")
                        }

                        Button(
                            onClick = {
                                val linkId = if (coaAccountOption == 0) null else selectedAccountId
                                onConfirm(name, managerName, phone, linkId)
                            },
                            enabled = name.isNotBlank() && (coaAccountOption == 0 || selectedAccountId != null),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("confirm_add_cashbox")
                        ) {
                            Text(if (lang == "ar") "حفظ الصندوق" else "Create Fund")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCashBoxDialog(
    cashBox: CashBox,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
    lang: String
) {
    var name by remember { mutableStateOf(cashBox.name) }
    var managerName by remember { mutableStateOf(cashBox.managerName) }
    var phone by remember { mutableStateOf(cashBox.phone) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("edit_cashbox_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (lang == "ar") "تعديل صندوق أو خزينة" else "Edit Cash Box",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (lang == "ar") "اسم الصندوق / الخزينة" else "Cash Box Name") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_cb_input_name"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = managerName,
                    onValueChange = { managerName = it },
                    label = { Text(if (lang == "ar") "أمين الصندوق (المشرف)" else "Custodian") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_cb_input_manager"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(if (lang == "ar") "رقم هاتف المشرف" else "Custodian Phone") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_cb_input_phone"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (lang == "ar") "إلغاء الأمر" else "Cancel")
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(name, managerName, phone)
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.testTag("edit_cb_btn_save")
                    ) {
                        Text(if (lang == "ar") "حفظ التعديلات" else "Save Changes")
                    }
                }
            }
        }
    }
}
