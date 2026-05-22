package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.Account
import com.example.data.AccountType
import com.example.data.AccountBalanceSnapshot
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.ui.viewmodel.LedgerViewModel
import com.example.util.FinancialUtils

import androidx.compose.ui.platform.LocalLayoutDirection
import com.example.ui.Localization

@Composable
fun AccountsScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val isLibyan by viewModel.isLibyanMode.collectAsState()
    val allAccounts by viewModel.accounts.collectAsState()
    val rootAccounts = remember(allAccounts) { allAccounts.filter { it.parentId == null } }
    val snapshots by viewModel.accountSnapshots.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedParentAccount by remember { mutableStateOf<Account?>(null) }
    var statementAccount by remember { mutableStateOf<Account?>(null) }
    var editAccount by remember { mutableStateOf<Account?>(null) }

    val direction = Localization.getLayoutDirection(lang)

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = Localization.translate(Localization.Key.CHART_OF_ACCOUNTS, lang),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (lang == "ar") "قم بتهيئة وإدارة دورتك وشجرة حساباتك المالية مع التجميع الفوري للأرصدة." else "Manage your double-entry accounts with real-time multi-level balance consolidation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (rootAccounts.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
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
                        items(rootAccounts) { rootAcc ->
                            AccountTreeRow(
                                account = rootAcc,
                                allAccounts = allAccounts,
                                depth = 0,
                                viewModel = viewModel,
                                snapshots = snapshots,
                                onAddSub = {
                                    selectedParentAccount = it
                                    showAddDialog = true
                                },
                                onDelete = { viewModel.deleteAccount(it) },
                                onViewStatement = {
                                    statementAccount = it
                                },
                                onEdit = {
                                    editAccount = it
                                }
                            )
                        }
                    }
                }
            }

            // Floating Action Button to create a root group or account
            FloatingActionButton(
                onClick = {
                    selectedParentAccount = null
                    showAddDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("add_account_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Account")
            }

            if (showAddDialog) {
                AddAccountDialog(
                    parentAccount = selectedParentAccount,
                    currencies = viewModel.currencies.collectAsState().value,
                    onDismiss = { showAddDialog = false },
                    onSave = { code, name, type, currencyId, isGroup ->
                        viewModel.addAccount(
                            code = code,
                            name = name,
                            parentId = selectedParentAccount?.id,
                            type = type,
                            currencyId = currencyId,
                            isGroup = isGroup
                        )
                        showAddDialog = false
                    },
                    viewModel = viewModel
                )
            }

            if (statementAccount != null) {
                AccountStatementDialog(
                    account = statementAccount!!,
                    viewModel = viewModel,
                    onDismiss = { statementAccount = null }
                )
            }

            if (editAccount != null) {
                EditAccountDialog(
                    account = editAccount!!,
                    onDismiss = { editAccount = null },
                    onSave = { code, name ->
                        viewModel.updateAccount(editAccount!!, name, code)
                        editAccount = null
                    },
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun AccountTreeRow(
    account: Account,
    allAccounts: List<Account>,
    depth: Int,
    viewModel: LedgerViewModel,
    snapshots: List<AccountBalanceSnapshot>,
    onAddSub: (Account) -> Unit,
    onDelete: (Account) -> Unit,
    onViewStatement: (Account) -> Unit,
    onEdit: (Account) -> Unit
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val isLibyan by viewModel.isLibyanMode.collectAsState()
    var isExpanded by remember { mutableStateOf(depth < 1) } // Default expand roots
    val subAccounts = remember(allAccounts, account) {
        allAccounts.filter { it.parentId == account.id }
    }
    val hasSub = subAccounts.isNotEmpty()

    val balance = remember(account.id, snapshots) {
        snapshots.find { it.accountId == account.id }?.balance ?: 0L
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 12).dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { if (account.isGroup) isExpanded = !isExpanded }
                .padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (account.isGroup) {
                    if (isExpanded) Icons.Filled.FolderOpen else Icons.Filled.Folder
                } else {
                    Icons.Filled.Notes
                },
                contentDescription = null,
                tint = if (account.isGroup) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = account.accountCode,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = Localization.getAccountName(account.accountCode, account.name, lang),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (account.isGroup) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = {},
                        label = { Text(account.accountType.name, style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.height(18.dp).padding(0.dp)
                    )
                    if (account.isSystemAccount) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (lang == "ar") "[مغلق للنظام]" else "[System Override]",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Real-time recursive balance display
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = FinancialUtils.formatBase(balance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        balance > 0L -> EmeraldGreen
                        balance < 0L -> RoseRed
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    }
                )
                Text(
                    text = if (isLibyan) "د.ل (الأساسية)" else "LYD (Base)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            Spacer(Modifier.width(8.dp))

            // Action options for accounts
            Box {
                var showMenu by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Options",
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    // Account statement action
                    DropdownMenuItem(
                        text = { Text(Localization.translate(Localization.Key.VIEW_STATEMENT, lang)) },
                        onClick = {
                            showMenu = false
                            onViewStatement(account)
                        },
                        leadingIcon = { Icon(Icons.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    )

                    if (!account.isSystemAccount && !hasSub) {
                        DropdownMenuItem(
                            text = { Text(Localization.translate(Localization.Key.DELETE, lang)) },
                            onClick = {
                                showMenu = false
                                onDelete(account)
                            },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = RoseRed) }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text(if (lang == "ar") "تعديل الحساب" else "Edit Account Details") },
                        onClick = {
                            showMenu = false
                            onEdit(account)
                        },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    )

                    if (account.isGroup) {
                        DropdownMenuItem(
                            text = { Text(Localization.translate(Localization.Key.ADD_ACCOUNT, lang)) },
                            onClick = {
                                showMenu = false
                                onAddSub(account)
                            },
                            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) }
                        )
                    }
                }
            }
        }

        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

        if (account.isGroup && isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                subAccounts.forEach { child ->
                    AccountTreeRow(
                        account = child,
                        allAccounts = allAccounts,
                        depth = depth + 1,
                        viewModel = viewModel,
                        snapshots = snapshots,
                        onAddSub = onAddSub,
                        onDelete = onDelete,
                        onViewStatement = onViewStatement,
                        onEdit = onEdit
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountDialog(
    parentAccount: Account?,
    currencies: List<com.example.data.Currency>,
    onDismiss: () -> Unit,
    onSave: (code: String, name: String, type: AccountType, currencyId: Long, isGroup: Boolean) -> Unit,
    viewModel: LedgerViewModel
) {
    val lang by viewModel.currentLanguage.collectAsState()
    var code by remember { mutableStateOf(parentAccount?.let { "${it.accountCode}01" } ?: "") }
    var name by remember { mutableStateOf("") }
    var isGroup by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(parentAccount?.accountType ?: AccountType.ASSET) }
    var selectedCurrencyId by remember { mutableStateOf(parentAccount?.currencyId ?: 1L) }

    var typeMenuExpanded by remember { mutableStateOf(false) }
    var currencyMenuExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = if (parentAccount != null) {
                        if (lang == "ar") "إضافة حساب فرعي تحت ${parentAccount.name}" else "Add Account under ${parentAccount.name}"
                    } else {
                        if (lang == "ar") "إضافة حساب رئيسي جديد" else "Add New Base Account"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(if (lang == "ar") "رمز الحساب (رقمي مثال: 1101)" else "Account Code (Numeric e.g. 1101)") },
                    modifier = Modifier.fillMaxWidth().testTag("add_account_code_input")
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Localization.translate(Localization.Key.NAME, lang)) },
                    modifier = Modifier.fillMaxWidth().testTag("add_account_name_input")
                )
                Spacer(Modifier.height(12.dp))

                // If no parent, allow choosing accounting type. If parent exists, freeze it to parent's type
                if (parentAccount == null) {
                    ExposedDropdownMenuBox(
                        expanded = typeMenuExpanded,
                        onExpandedChange = { typeMenuExpanded = !typeMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedType.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(Localization.translate(Localization.Key.TYPE, lang)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = typeMenuExpanded,
                            onDismissRequest = { typeMenuExpanded = false }
                        ) {
                            AccountType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name) },
                                    onClick = {
                                        selectedType = type
                                        typeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                } else {
                    OutlinedTextField(
                        value = if (lang == "ar") "موروث: ${selectedType.name}" else "Inherited: ${selectedType.name}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (lang == "ar") "نوع الحساب" else "Account Type (Inherited)") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // Currency selector
                ExposedDropdownMenuBox(
                    expanded = currencyMenuExpanded,
                    onExpandedChange = { currencyMenuExpanded = !currencyMenuExpanded }
                ) {
                    val activeCurrency = currencies.find { it.id == selectedCurrencyId } ?: currencies.firstOrNull()
                    OutlinedTextField(
                        value = activeCurrency?.let { "${it.code} (${it.name})" } ?: (if (lang == "ar") "اختر العملة" else "Select Currency"),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(Localization.translate(Localization.Key.CURRENCY, lang)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = currencyMenuExpanded,
                        onDismissRequest = { currencyMenuExpanded = false }
                    ) {
                        currencies.forEach { curr ->
                            DropdownMenuItem(
                                text = { Text("${curr.code} - ${curr.name}") },
                                onClick = {
                                    selectedCurrencyId = curr.id
                                    currencyMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Account vs Group selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isGroup = !isGroup }
                ) {
                    Checkbox(
                        checked = isGroup,
                        onCheckedChange = { isGroup = it }
                    )
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(
                            text = if (lang == "ar") "هل هو حساب فئة (رئيسي)؟" else "Is Group Account",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (lang == "ar") "عند تفعيله، سيعمل هذا الحساب كمجلد فئات ولا يمكن تسجيل قيود ومعاملات مالية عليه مباشرة." else "If checked, this account acts as a category folder and cannot receive transactions directly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(Localization.translate(Localization.Key.CANCEL, lang))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(code, name, selectedType, selectedCurrencyId, isGroup) },
                        enabled = code.isNotBlank() && name.isNotBlank(),
                        modifier = Modifier.testTag("save_account_button")
                    ) {
                        Text(Localization.translate(Localization.Key.CREATE, lang))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountStatementDialog(
    account: com.example.data.Account,
    viewModel: LedgerViewModel,
    onDismiss: () -> Unit
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val isLibyan by viewModel.isLibyanMode.collectAsState()
    var statementRows by remember { mutableStateOf<List<com.example.ui.viewmodel.AccountStatementRow>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(account.id) {
        isLoading = true
        statementRows = viewModel.getAccountStatement(account.id)
        isLoading = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Localization.translate(Localization.Key.STATEMENT_TITLE, lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${account.accountCode} - ${account.name}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (isLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (statementRows.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Localization.translate(Localization.Key.NO_STATEMENT_TXS, lang),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    // Summary cards
                    val totalDebit = statementRows.sumOf { it.debit }
                    val totalCredit = statementRows.sumOf { it.credit }
                    val currentBal = statementRows.lastOrNull()?.runningBalance ?: 0L

                    val unitStr = if (isLibyan) "د.ل" else "LYD"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total debit card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (lang == "ar") "مجموع المدين" else "Total Debit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${FinancialUtils.formatBase(totalDebit)} $unitStr",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                            }
                        }

                        // Total credit card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (lang == "ar") "مجموع الدائن" else "Total Credit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${FinancialUtils.formatBase(totalCredit)} $unitStr",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = RoseRed
                                )
                            }
                        }

                        // Balance card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (lang == "ar") "الرصيد النهائي" else "Final Balance",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${FinancialUtils.formatBase(currentBal)} $unitStr",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentBal >= 0) EmeraldGreen else RoseRed
                                )
                            }
                        }
                    }

                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lang == "ar") "التاريخ" else "Date",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.2f)
                        )
                        Text(
                            text = if (lang == "ar") "البيان / السند" else "Memo / Ref",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.8f)
                        )
                        Text(
                            text = if (lang == "ar") "مدين" else "Debit",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (lang == "ar") "دائن" else "Credit",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    val sdf = remember { java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault()) }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(statementRows) { row ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sdf.format(java.util.Date(row.date)),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1.2f)
                                    )

                                    Column(modifier = Modifier.weight(1.8f)) {
                                        Text(
                                            text = row.memo,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "#${row.voucherNo}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Text(
                                        text = if (row.debit > 0) FinancialUtils.formatBase(row.debit) else "-",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (row.debit > 0) FontWeight.Bold else FontWeight.Normal,
                                        color = if (row.debit > 0) EmeraldGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.weight(1f)
                                    )

                                    Text(
                                        text = if (row.credit > 0) FinancialUtils.formatBase(row.credit) else "-",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (row.credit > 0) FontWeight.Bold else FontWeight.Normal,
                                        color = if (row.credit > 0) RoseRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                        .padding(vertical = 4.dp, horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "${Localization.translate(Localization.Key.RUNNING_BALANCE, lang)}: ${FinancialUtils.formatBase(row.runningBalance)} $unitStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (row.runningBalance >= 0) EmeraldGreen else RoseRed
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountDialog(
    account: Account,
    onDismiss: () -> Unit,
    onSave: (code: String, name: String) -> Unit,
    viewModel: LedgerViewModel
) {
    val lang by viewModel.currentLanguage.collectAsState()
    var code by remember { mutableStateOf(account.accountCode) }
    var name by remember { mutableStateOf(account.name) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = if (lang == "ar") "تعديل الحساب: ${account.name}" else "Edit Account: ${account.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(if (lang == "ar") "رمز الحساب" else "Account Code") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_account_code_input")
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (lang == "ar") "اسم الحساب" else "Account Name") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_account_name_input")
                )
                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (lang == "ar") "إلغاء الأمر" else "Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(code, name) },
                        modifier = Modifier.testTag("edit_account_save_button")
                    ) {
                        Text(if (lang == "ar") "حفظ التعديلات" else "Save Changes")
                    }
                }
            }
        }
    }
}

