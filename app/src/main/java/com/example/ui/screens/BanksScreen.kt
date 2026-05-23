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
import com.example.data.Bank
import com.example.data.BankBranch
import com.example.data.BankAccount
import com.example.ui.Localization
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.ui.viewmodel.LedgerViewModel
import com.example.util.FinancialUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BanksScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val banks by viewModel.banks.collectAsState()
    val allBranches by viewModel.allBranches.collectAsState()
    val allBankAccounts by viewModel.allBankAccounts.collectAsState()
    val leafAccounts by viewModel.leafAccounts.collectAsState()
    val snapshots by viewModel.accountSnapshots.collectAsState()
    val allAccounts by viewModel.accounts.collectAsState()

    var selectedBank by remember { mutableStateOf<Bank?>(null) }
    var showAddBankDialog by remember { mutableStateOf(false) }
    var showAddBranchDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf<BankBranch?>(null) } // holds active branch to add account
    var editingBranch by remember { mutableStateOf<BankBranch?>(null) }
    var editingBankAccount by remember { mutableStateOf<BankAccount?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val direction = Localization.getLayoutDirection(lang)

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (selectedBank == null) {
                    // MAIN BANKS LIST VIEW
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (lang == "ar") "الدليل المصرفي والبنوك" else "Bank Directory",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (lang == "ar") "إدارة بيانات البنوك التجارية والفرعية وتعيين حسابات الأستاذ العام المعنية." else "Directory of commercial banks, managing multiple branches and ledger accounts.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(if (lang == "ar") "بحث في البنوك..." else "Search banks...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("bank_search"),
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

                    val filteredBanks = banks.filter {
                        it.name.contains(searchQuery, ignoreCase = true)
                    }

                    if (filteredBanks.isEmpty()) {
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
                                    Icons.Outlined.AccountBalance,
                                    contentDescription = "",
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = if (lang == "ar") "لم يتم تسجيل أي بنوك حالياً. انقر على زر الإضافة لإدراج بنك." else "No financial institutions registered yet. Touch the FAB to append a bank.",
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
                            items(filteredBanks) { bank ->
                                val branchCount = allBranches.count { it.bankId == bank.id }
                                val accsInBankIds = allBranches.filter { it.bankId == bank.id }.map { it.id }
                                val accountCount = allBankAccounts.count { it.branchId in accsInBankIds }

                                BankRowCard(
                                    bank = bank,
                                    branchCount = branchCount,
                                    accountCount = accountCount,
                                    lang = lang,
                                    onClick = { selectedBank = bank },
                                    onDelete = { viewModel.deleteBank(bank) }
                                )
                            }
                        }
                    }
                } else {
                    // DEEP VIEW: BRANCHES AND ACCOUNTS OF SELECTED BANK
                    val activeBank = selectedBank!!
                    val bankBranches = allBranches.filter { it.bankId == activeBank.id }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedBank = null }) {
                            Icon(
                                imageVector = if (lang == "ar") Icons.Filled.ArrowForward else Icons.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = activeBank.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (lang == "ar") "تفاصيل الفروع والحسابات الجارية المربوطة والمحاسبية." else "Details of branches, and linked continuous deposit accounts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Action buttons to add Branch
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (lang == "ar") "الفروع التابعة للشركة" else "Active Corporate Branches",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Button(
                                onClick = { showAddBranchDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (lang == "ar") "إضافة فرع جديد" else "Add Branch")
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (bankBranches.isEmpty()) {
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
                                    Icons.Outlined.Store,
                                    contentDescription = "",
                                    modifier = Modifier.size(54.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = if (lang == "ar") "لا توجد فروع مسجلة لهذا البنك. انقر على 'إضافة فرع جديد' للمتابعة." else "No branches configured. Tap 'Add Branch' above.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(bankBranches) { branch ->
                                val branchAccounts = allBankAccounts.filter { it.branchId == branch.id }

                                BranchDetailsCard(
                                    branch = branch,
                                    accounts = branchAccounts,
                                    allAccounts = allAccounts,
                                    snapshots = snapshots,
                                    lang = lang,
                                    onDeleteBranch = { viewModel.deleteBranch(branch) },
                                    onEditBranch = { editingBranch = branch },
                                    onAddNewAccount = { showAddAccountDialog = branch },
                                    onEditAccount = { acc -> editingBankAccount = acc },
                                    onDeleteAccount = { acc -> viewModel.deleteBankAccount(acc) }
                                )
                            }
                        }
                    }
                }
            }

            // FAB for Adding Bank (Only visible when selectedBank is null)
            if (selectedBank == null) {
                FloatingActionButton(
                    onClick = { showAddBankDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                        .testTag("add_bank_fab"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Bank")
                }
            }

            // Dialogs
            if (showAddBankDialog) {
                AddBankDialog(
                    onDismiss = { showAddBankDialog = false },
                    onConfirm = { name ->
                        viewModel.addBank(name)
                        showAddBankDialog = false
                    },
                    lang = lang
                )
            }

            if (showAddBranchDialog && selectedBank != null) {
                AddBranchDialog(
                    onDismiss = { showAddBranchDialog = false },
                    onConfirm = { name, code, manager ->
                        viewModel.addBranch(selectedBank!!.id, name, code, manager)
                        showAddBranchDialog = false
                    },
                    lang = lang
                )
            }

            if (showAddAccountDialog != null) {
                val branch = showAddAccountDialog!!
                AddBankAccountDialog(
                    branch = branch,
                    onDismiss = { showAddAccountDialog = null },
                    onConfirm = { accName, accNum, iban, existingId ->
                        viewModel.addBankAccount(branch.id, accName, accNum, iban, existingId)
                        showAddAccountDialog = null
                    },
                    leafAccounts = leafAccounts,
                    lang = lang
                )
            }

            if (editingBranch != null) {
                EditBranchDialog(
                    branch = editingBranch!!,
                    onDismiss = { editingBranch = null },
                    onConfirm = { name, code, manager ->
                        viewModel.updateBranch(editingBranch!!.copy(name = name, code = code, managerName = manager))
                        editingBranch = null
                    },
                    lang = lang
                )
            }

            if (editingBankAccount != null) {
                EditBankAccountDialog(
                    bankAccount = editingBankAccount!!,
                    onDismiss = { editingBankAccount = null },
                    onConfirm = { name, number, iban ->
                        viewModel.updateBankAccount(editingBankAccount!!.copy(accountName = name, accountNumber = number, iban = iban))
                        editingBankAccount = null
                    },
                    lang = lang
                )
            }
        }
    }
}

@Composable
fun BankRowCard(
    bank: Bank,
    branchCount: Int,
    accountCount: Int,
    lang: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = bank.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (lang == "ar") {
                            "عدد الفروع: $branchCount | عدد الحسابات المقترنة: $accountCount"
                        } else {
                            "Branches: $branchCount | Configured Accounts: $accountCount"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = RoseRed)
                }
                Icon(
                    imageVector = if (lang == "ar") Icons.Filled.ChevronLeft else Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun BranchDetailsCard(
    branch: BankBranch,
    accounts: List<BankAccount>,
    allAccounts: List<Account>,
    snapshots: List<com.example.data.AccountBalanceSnapshot>,
    lang: String,
    onDeleteBranch: () -> Unit,
    onEditBranch: () -> Unit,
    onAddNewAccount: () -> Unit,
    onEditAccount: (BankAccount) -> Unit,
    onDeleteAccount: (BankAccount) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Branch metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable { expanded = !expanded }.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                        contentDescription = "Toggle Expand",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = branch.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (branch.code.isNotEmpty()) {
                                Spacer(Modifier.width(8.dp))
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(text = branch.code, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        if (branch.managerName.isNotEmpty()) {
                            Text(
                                text = (if (lang == "ar") "مدير الفرع: " else "Manager: ") + branch.managerName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onAddNewAccount) {
                        Icon(Icons.Filled.AddCard, contentDescription = "Add Account", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onEditBranch) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Branch", tint = MaterialTheme.colorScheme.secondary)
                    }
                    IconButton(onClick = onDeleteBranch) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete Branch", tint = RoseRed)
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    Spacer(Modifier.height(12.dp))

                    if (accounts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (lang == "ar") "لم يتم ربط حسابات مصرفية بهذا الفرع بعد." else "No banking accounts coupled with this branch.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        // Table header style or dynamic layout
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            accounts.forEach { bankAccount ->
                                val linkedAcc = allAccounts.find { it.id == bankAccount.accountId }
                                val balance = remember(bankAccount.accountId, snapshots) {
                                    snapshots.find { it.accountId == bankAccount.accountId }?.balance ?: 0L
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = bankAccount.accountName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${if (lang == "ar") "رقم الحساب: " else "Acc: "}${bankAccount.accountNumber}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (bankAccount.iban.isNotEmpty()) {
                                            Text(
                                                text = "IBAN: ${bankAccount.iban}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }

                                        Spacer(Modifier.height(4.dp))
                                        // Accounting ledger code
                                        if (linkedAcc != null) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Filled.Link,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = "${linkedAcc.accountCode} - ${linkedAcc.name}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    // Balance and Delete action
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = FinancialUtils.formatBase(balance),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = if (balance >= 0) EmeraldGreen else RoseRed
                                            )
                                            Text(
                                                text = if (lang == "ar") "د.ل" else "LYD",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { onEditAccount(bankAccount) }) {
                                                Icon(
                                                    imageVector = Icons.Filled.Edit,
                                                    contentDescription = "Edit Account",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            IconButton(onClick = { onDeleteAccount(bankAccount) }) {
                                                Icon(
                                                    imageVector = Icons.Filled.DeleteOutline,
                                                    contentDescription = "Delete Account",
                                                    tint = RoseRed,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddBankDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit,
    lang: String
) {
    var name by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (lang == "ar") "إيداع مؤسسة مالية جديدة" else "Register Bank",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (lang == "ar") "اسم البنك الأساسي (مثال: التجاري الوطني)" else "Institution Name") },
                    modifier = Modifier.fillMaxWidth().testTag("bank_name_input"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (lang == "ar") "إلغاء الأمر" else "Cancel")
                    }
                    Button(
                        onClick = { onConfirm(name) },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("confirm_add_bank")
                    ) {
                        Text(if (lang == "ar") "حفظ البنك" else "Save Bank")
                    }
                }
            }
        }
    }
}

@Composable
fun AddBranchDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, code: String, manager: String) -> Unit,
    lang: String
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var manager by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (lang == "ar") "إدارة وربط فرع مصرفي" else "Establish Bank Branch",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (lang == "ar") "اسم الفرع (مثل: فرع الساعدي)" else "Branch Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(if (lang == "ar") "رمز الفرع (تلقائي)" else "Branch Swift/Sort Code") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = manager,
                    onValueChange = { manager = it },
                    label = { Text(if (lang == "ar") "مدير الفرع المعتمد" else "Authorized Manager") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (lang == "ar") "إلغاء" else "Cancel")
                    }
                    Button(
                        onClick = { onConfirm(name, code, manager) },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if (lang == "ar") "إضافة فرع" else "Create Branch")
                    }
                }
            }
        }
    }
}

@Composable
fun AddBankAccountDialog(
    branch: BankBranch,
    onDismiss: () -> Unit,
    onConfirm: (accName: String, accNum: String, iban: String, existingAccountId: Long?) -> Unit,
    leafAccounts: List<Account>,
    lang: String
) {
    var accName by remember { mutableStateOf("") }
    var accNumber by remember { mutableStateOf("") }
    var iban by remember { mutableStateOf("") }

    var coaAccountOption by remember { mutableStateOf(0) } // 0 = Auto Create, 1 = Manual link
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = if (lang == "ar") "إضافة حساب مصرفي تحت: ${branch.name}" else "Coupling Bank Account",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (lang == "ar") "تعبئة التفاصيل اللازمة لتوليد القيود بشكل آلي." else "Couple bank balance records under general ledger asset directories automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    OutlinedTextField(
                        value = accName,
                        onValueChange = { accName = it },
                        label = { Text(if (lang == "ar") "اسم الحساب (مثال: الحساب الجاري العام)" else "Account Name Banner") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = accNumber,
                        onValueChange = { accNumber = it },
                        label = { Text(if (lang == "ar") "رقم الحساب الجاري" else "Account Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = iban,
                        onValueChange = { iban = it },
                        label = { Text(if (lang == "ar") "رقم الآيبان (IBAN) اختيارى" else "IBAN String (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Text(
                        text = if (lang == "ar") "شجرة الحسابات (إعدادات الأستاذ العام)" else "General Ledger Core Alignment",
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
                                text = if (lang == "ar") "تخصيص حساب دفتر أستاذ آلي تحت أصول البنوك (1104)" else "Auto-allocate a new CoA asset register (1104xxx)",
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
                                text = if (lang == "ar") "ربط الحساب المالي الجاري بترميز يدوي متواجد مسبقاً" else "Bind manually to an existing general ledger register",
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
                            } ?: (if (lang == "ar") "اختر الحساب المصرفي الأستاذ..." else "Link financial Account...")

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
                                onConfirm(accName, accNumber, iban, linkId)
                            },
                            enabled = accName.isNotBlank() && accNumber.isNotBlank() && (coaAccountOption == 0 || selectedAccountId != null),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (lang == "ar") "تخزين الحساب" else "Link Account")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBranchDialog(
    branch: BankBranch,
    onDismiss: () -> Unit,
    onConfirm: (name: String, code: String, manager: String) -> Unit,
    lang: String
) {
    var name by remember { mutableStateOf(branch.name) }
    var code by remember { mutableStateOf(branch.code) }
    var manager by remember { mutableStateOf(branch.managerName) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.testTag("edit_branch_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (lang == "ar") "تعديل بيانات الفرع" else "Edit Branch Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (lang == "ar") "اسم الفرع" else "Branch Name") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_branch_input_name"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(if (lang == "ar") "رمز الفرع" else "Branch Code") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_branch_input_code"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = manager,
                    onValueChange = { manager = it },
                    label = { Text(if (lang == "ar") "مدير الفرع" else "Branch Manager") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_branch_input_manager"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("edit_branch_btn_cancel")) {
                        Text(if (lang == "ar") "إلغاء" else "Cancel")
                    }
                    Button(
                        onClick = { onConfirm(name, code, manager) },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("edit_branch_btn_save")
                    ) {
                        Text(if (lang == "ar") "حفظ التعديلات" else "Save Changes")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBankAccountDialog(
    bankAccount: BankAccount,
    onDismiss: () -> Unit,
    onConfirm: (name: String, number: String, iban: String) -> Unit,
    lang: String
) {
    var accountName by remember { mutableStateOf(bankAccount.accountName) }
    var accountNumber by remember { mutableStateOf(bankAccount.accountNumber) }
    var iban by remember { mutableStateOf(bankAccount.iban) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.testTag("edit_bankaccount_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (lang == "ar") "تعديل بيانات الحساب الجاري" else "Edit Bank Account Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text(if (lang == "ar") "اسم الحساب الجاري" else "Account Name") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_ba_input_name"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text(if (lang == "ar") "رقم الحساب" else "Account Number") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_ba_input_number"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = iban,
                    onValueChange = { iban = it },
                    label = { Text(if (lang == "ar") "رمز الـ IBAN" else "IBAN Code") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_ba_input_iban"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("edit_ba_btn_cancel")) {
                        Text(if (lang == "ar") "إلغاء" else "Cancel")
                    }
                    Button(
                        onClick = { onConfirm(accountName, accountNumber, iban) },
                        enabled = accountName.isNotBlank() && accountNumber.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("edit_ba_btn_save")
                    ) {
                        Text(if (lang == "ar") "حفظ التعديلات" else "Save Changes")
                    }
                }
            }
        }
    }
}
