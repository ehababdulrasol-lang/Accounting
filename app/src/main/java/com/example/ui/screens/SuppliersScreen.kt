package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val supplierGroups by viewModel.supplierGroups.collectAsStateWithLifecycle()
    val allAccounts by viewModel.accounts.collectAsStateWithLifecycle()
    val leafAccounts by viewModel.leafAccounts.collectAsStateWithLifecycle()
    val snapshots by viewModel.accountSnapshots.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingSupplier by remember { mutableStateOf<Supplier?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var supplierToDelete by remember { mutableStateOf<Supplier?>(null) }
    var showSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Group Management state
    var selectedGroup by remember { mutableStateOf<com.example.data.SupplierGroup?>(null) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<com.example.data.SupplierGroup?>(null) }
    var groupToDelete by remember { mutableStateOf<com.example.data.SupplierGroup?>(null) }

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
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Group Carousel section
                Text(
                    text = if (lang == "ar") "تصنيفات ومجموعات الموردين" else "Supplier Categories & Groups",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. "All" Card
                    Card(
                        modifier = Modifier
                            .width(130.dp)
                            .height(85.dp)
                            .clickable { selectedGroup = null },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedGroup == null) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            }
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedGroup == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Storefront,
                                    contentDescription = null,
                                    tint = if (selectedGroup == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Badge(
                                    containerColor = if (selectedGroup == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    contentColor = if (selectedGroup == null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ) {
                                    Text(text = "${suppliers.size}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Text(
                                text = if (lang == "ar") "كل الموردين" else "All Suppliers",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedGroup == null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // 2. Custom Groups Cards
                    supplierGroups.forEach { group ->
                        val groupCount = remember(suppliers, group) {
                            suppliers.count { it.groupId == group.id || it.groupName.trim().equals(group.name.trim(), ignoreCase = true) }
                        }
                        val isSelected = selectedGroup?.id == group.id

                        Card(
                            modifier = Modifier
                                .width(160.dp)
                                .height(85.dp)
                                .clickable { selectedGroup = group },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                }
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp).fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FolderOpen,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        IconButton(
                                            onClick = { editingGroup = group },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(11.dp))
                                        }
                                        IconButton(
                                            onClick = { groupToDelete = group },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Filled.Delete, null, tint = RoseRed, modifier = Modifier.size(11.dp))
                                        }
                                        Badge(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        ) {
                                            Text(text = "$groupCount", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // 3. Add Group Card
                    Card(
                        modifier = Modifier
                            .width(130.dp)
                            .height(85.dp)
                            .clickable { showAddGroupDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (lang == "ar") "مجموعة جديدة" else "New Group",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

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
                    val matchesSearch = it.name.contains(searchQuery, ignoreCase = true) ||
                                        it.phone.contains(searchQuery) ||
                                        it.email.contains(searchQuery, ignoreCase = true)
                    val matchesGroup = if (selectedGroup != null) {
                        it.groupId == selectedGroup!!.id || it.groupName.trim().equals(selectedGroup!!.name.trim(), ignoreCase = true)
                    } else {
                        true
                    }
                    matchesSearch && matchesGroup
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
                    val grouped = remember(filtered, lang) {
                        filtered.groupBy { it.groupName.trim() }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        grouped.forEach { (groupName, supplierList) ->
                            item {
                                val title = if (groupName.isBlank()) {
                                    if (lang == "ar") "موردون عامون (غير مصنفين)" else "General / Uncategorized"
                                } else {
                                    groupName
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Folder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "${supplierList.size}",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            itemsIndexed(supplierList) { idx, supplier ->
                                com.example.ui.StaggeredItem(index = idx) {
                                    val linkedAccount = allAccounts.find { it.id == supplier.accountId }
                                    val balance = remember(supplier.accountId, snapshots) {
                                        snapshots.find { it.accountId == supplier.accountId }?.balance ?: 0L
                                    }

                                    SupplierCard(
                                        supplier = supplier,
                                        account = linkedAccount,
                                        balance = balance,
                                        onDelete = { supplierToDelete = supplier },
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
                    onConfirm = { name, phone, email, linkAccId, groupName, groupId ->
                        viewModel.addSupplier(name, phone, email, linkAccId, groupName, groupId)
                        showAddDialog = false
                        showSuccessMessage = if (lang == "ar") "تم تسجيل ملف المورد بنجاح" else "Supplier registered successfully!"
                    },
                    leafAccounts = if (liabilitiesLeafs.isNotEmpty()) liabilitiesLeafs else leafAccounts,
                    supplierGroups = supplierGroups,
                    lang = lang
                )
            }

            if (editingSupplier != null) {
                EditSupplierDialog(
                    supplier = editingSupplier!!,
                    onDismiss = { editingSupplier = null },
                    onConfirm = { name, phone, email, creditLimit, groupName, groupId ->
                        viewModel.updateSupplier(editingSupplier!!.copy(name = name, phone = phone, email = email, creditLimit = creditLimit, groupName = groupName, groupId = groupId))
                        editingSupplier = null
                        showSuccessMessage = if (lang == "ar") "تم تعديل بيانات المورد بنجاح" else "Supplier details updated successfully!"
                    },
                    supplierGroups = supplierGroups,
                    lang = lang
                )
            }

            // Supplier Group Actions
            if (showAddGroupDialog) {
                AddSupplierGroupDialog(
                    onDismiss = { showAddGroupDialog = false },
                    onConfirm = { name, desc ->
                        viewModel.addSupplierGroup(name, desc)
                        showAddGroupDialog = false
                        showSuccessMessage = if (lang == "ar") "تم إضافة مجموعة الموردين بنجاح" else "Supplier group created successfully!"
                    },
                    lang = lang
                )
            }

            if (editingGroup != null) {
                EditSupplierGroupDialog(
                    group = editingGroup!!,
                    onDismiss = { editingGroup = null },
                    onConfirm = { updated ->
                        viewModel.updateSupplierGroup(updated)
                        editingGroup = null
                        showSuccessMessage = if (lang == "ar") "تم تعديل بيانات المجموعة بنجاح" else "Group updated successfully!"
                    },
                    lang = lang
                )
            }

            if (groupToDelete != null) {
                val gName = groupToDelete!!.name
                AnimatedDeleteConfirmDialog(
                    title = if (lang == "ar") "تأكيد حذف مجموعة موردين" else "Confirm Group Deletion",
                    message = if (lang == "ar") "هل أنت متأكد من رغبتك في حذف المجموعة: $gName؟ لن يتم حذف الموردين التابعين لها." else "Are you sure you want to delete group: $gName? Suppliers under it will not be deleted.",
                    lang = lang,
                    onConfirm = {
                        val toDel = groupToDelete!!
                        if (selectedGroup?.id == toDel.id) {
                            selectedGroup = null
                        }
                        viewModel.deleteSupplierGroup(toDel)
                        groupToDelete = null
                        showSuccessMessage = if (lang == "ar") "تم حذف مجموعة الموردين بنجاح" else "Supplier group deleted successfully!"
                    },
                    onDismiss = { groupToDelete = null }
                )
            }

            if (supplierToDelete != null) {
                val sName = supplierToDelete!!.name
                AnimatedDeleteConfirmDialog(
                    title = if (lang == "ar") "تأكيد حذف المورد" else "Confirm Supplier Deletion",
                    message = if (lang == "ar") "هل أنت متأكد من رغبتك في حذف ملف المورد: $sName؟" else "Are you sure you want to delete supplier profile: $sName?",
                    lang = lang,
                    onConfirm = {
                        val toDel = supplierToDelete!!
                        viewModel.deleteSupplier(toDel)
                        supplierToDelete = null
                        showSuccessMessage = if (lang == "ar") "تم حذف ملف المورد بنجاح" else "Supplier deleted successfully!"
                    },
                    onDismiss = { supplierToDelete = null }
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
    onConfirm: (String, String, String, Long?, String, Long?) -> Unit,
    leafAccounts: List<Account>,
    supplierGroups: List<com.example.data.SupplierGroup>,
    lang: String
) {
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var groupId by remember { mutableStateOf<Long?>(null) }

    var expandedGroupDropdown by remember { mutableStateOf(false) }

    // Strategies: 0 = Auto-open new under 2101, 1 = Link to existing matching name, 2 = Choose exist manually
    var linkStrategy by remember { mutableStateOf(0) }
    var selectedExistAccountId by remember { mutableStateOf<Long?>(null) }

    // Match checking
    val matchExist = remember(name) {
        leafAccounts.find { it.name.trim().equals(name.trim(), ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Store,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (lang == "ar") "تسجيل ملف مورد جديد" else "Register Supplier Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (lang == "ar") "أدخل معلومات المورد الجديد لإنشاء ملف وربطه محاسبياً" else "Add new supplier profile to track payables and ledger audit trails",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(if (lang == "ar") "اسم المورد الكامل" else "Supplier Full Name") },
                leadingIcon = { Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("supp_input_name"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(if (lang == "ar") "رقم الهاتف والاتصال" else "Phone Number") },
                leadingIcon = { Icon(Icons.Filled.Phone, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("supp_input_phone"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(if (lang == "ar") "البريد الإلكتروني" else "Email Address") },
                leadingIcon = { Icon(Icons.Filled.Email, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("supp_input_email"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(Modifier.height(14.dp))

            // Elegant Group selection dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                val selectedGroupText = remember(groupId, supplierGroups, groupName) {
                    supplierGroups.find { it.id == groupId }?.name ?: groupName.ifBlank { if (lang == "ar") "عام / غير مصنف" else "General" }
                }

                OutlinedTextField(
                    value = selectedGroupText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (lang == "ar") "مجموعة الموردين" else "Supplier Group") },
                    leadingIcon = { Icon(Icons.Filled.Folder, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) },
                    trailingIcon = {
                        IconButton(onClick = { expandedGroupDropdown = !expandedGroupDropdown }) {
                            Icon(Icons.Filled.ArrowDropDown, null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedGroupDropdown = true },
                    shape = RoundedCornerShape(12.dp)
                )

                DropdownMenu(
                    expanded = expandedGroupDropdown,
                    onDismissRequest = { expandedGroupDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    DropdownMenuItem(
                        text = { Text(if (lang == "ar") "عام / غير مصنف" else "General / Uncategorized") },
                        onClick = {
                            groupId = null
                            groupName = ""
                            expandedGroupDropdown = false
                        }
                    )
                    supplierGroups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.name) },
                            onClick = {
                                groupId = group.id
                                groupName = group.name
                                expandedGroupDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = if (lang == "ar") "إعدادات الربط المالي واستحقاق اليومية" else "Payables Ledger Account Setup",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Auto-open option
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (linkStrategy == 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (linkStrategy == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { linkStrategy = 0 }
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = linkStrategy == 0, onClick = { linkStrategy = 0 })
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (lang == "ar") "إنشاء حساب تلقائي" else "Auto-create a linked AP account",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (lang == "ar") "سيقوم النظام بإنشاء حساب فرعي ذو رمز آلي تتبعاً لحساب الدائنين الرئيسي (2101)" else "Auto-create a linked AP account under payables (2101)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Link existing matching option
            if (matchExist != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (linkStrategy == 1) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (linkStrategy == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { linkStrategy = 1 }
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = linkStrategy == 1, onClick = { linkStrategy = 1 })
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (lang == "ar") "ربط بحساب مطابق (${matchExist.accountCode})" else "Link to matching account (${matchExist.accountCode})",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = EmeraldGreen
                            )
                            Text(
                                text = if (lang == "ar") "ربط الملف بحساب مالي مطابق الاسم مسجل مسبقاً بالشجرة" else "Link to an existing ledger account with matching name",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Link existing manually option
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (linkStrategy == 2) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (linkStrategy == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { linkStrategy = 2 }
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = linkStrategy == 2, onClick = { linkStrategy = 2 })
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (lang == "ar") "ربط يدوي بحساب موجود" else "Manually select existing AP account",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (lang == "ar") "تحديد حساب مالي يدويًا من شجرة الحسابات دون قيود مطابقة الاسم" else "Choose any manual active account from Chart of Accounts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            if (linkStrategy == 2) {
                Spacer(Modifier.height(14.dp))
                
                val currentSelection = leafAccounts.find { it.id == selectedExistAccountId }
                var showAccountSearchDialog by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = currentSelection?.let { "${it.accountCode} - ${Localization.getAccountName(it.accountCode, it.name, lang)}" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (lang == "ar") "الحساب المقترن يدوياً" else "Manual AP Account") },
                    leadingIcon = { Icon(Icons.Filled.AccountBalance, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        IconButton(onClick = { showAccountSearchDialog = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAccountSearchDialog = true },
                    shape = RoundedCornerShape(12.dp)
                )

                AccountSearchDialog(
                    show = showAccountSearchDialog,
                    onDismiss = { showAccountSearchDialog = false },
                    accounts = leafAccounts,
                    lang = lang,
                    onSelect = { acc ->
                        selectedExistAccountId = acc.id
                    }
                )
            }

            Spacer(Modifier.height(30.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        keyboardController?.hide()
                        onDismiss()
                    },
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (lang == "ar") "إلغاء" else "Cancel")
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            keyboardController?.hide()
                            val linkId = when (linkStrategy) {
                                1 -> matchExist?.id
                                2 -> selectedExistAccountId
                                else -> null
                            }
                            onConfirm(name, phone, email, linkId, groupName, groupId)
                        }
                    },
                    enabled = name.isNotBlank() && (linkStrategy != 2 || selectedExistAccountId != null),
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (lang == "ar") "تسجيل وحفظ" else "Save Supplier")
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
    onConfirm: (String, String, String, Long, String, Long?) -> Unit,
    supplierGroups: List<com.example.data.SupplierGroup>,
    lang: String
) {
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var name by remember { mutableStateOf(supplier.name) }
    var phone by remember { mutableStateOf(supplier.phone) }
    var email by remember { mutableStateOf(supplier.email) }
    var groupName by remember { mutableStateOf(supplier.groupName) }
    var groupId by remember { mutableStateOf<Long?>(supplier.groupId) }
    var limitInput by remember { mutableStateOf((supplier.creditLimit / 1000.0).toString()) }

    var expandedGroupDropdown by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (lang == "ar") "تعديل بيانات المورد" else "Edit Supplier Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (lang == "ar") "تحديث معلومات المورد والحدود الائتمانية" else "Update supplier contact and credit terms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(if (lang == "ar") "اسم المورد" else "Supplier Name") },
                leadingIcon = { Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_supp_input_name"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(if (lang == "ar") "رقم الهاتف" else "Phone") },
                leadingIcon = { Icon(Icons.Filled.Phone, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_supp_input_phone"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(if (lang == "ar") "البريد الإلكتروني" else "Email") },
                leadingIcon = { Icon(Icons.Filled.Email, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_supp_input_email"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(Modifier.height(14.dp))

            // Elegant Group selection dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                val selectedGroupText = remember(groupId, supplierGroups, groupName) {
                    supplierGroups.find { it.id == groupId }?.name ?: groupName.ifBlank { if (lang == "ar") "عام / غير مصنف" else "General" }
                }

                OutlinedTextField(
                    value = selectedGroupText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (lang == "ar") "مجموعة الموردين" else "Supplier Group") },
                    leadingIcon = { Icon(Icons.Filled.Folder, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) },
                    trailingIcon = {
                        IconButton(onClick = { expandedGroupDropdown = !expandedGroupDropdown }) {
                            Icon(Icons.Filled.ArrowDropDown, null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedGroupDropdown = true },
                    shape = RoundedCornerShape(12.dp)
                )

                DropdownMenu(
                    expanded = expandedGroupDropdown,
                    onDismissRequest = { expandedGroupDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    DropdownMenuItem(
                        text = { Text(if (lang == "ar") "عام / غير مصنف" else "General / Uncategorized") },
                        onClick = {
                            groupId = null
                            groupName = ""
                            expandedGroupDropdown = false
                        }
                    )
                    supplierGroups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.name) },
                            onClick = {
                                groupId = group.id
                                groupName = group.name
                                expandedGroupDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = limitInput,
                onValueChange = { limitInput = it },
                label = { Text(if (lang == "ar") "الحد الائتماني" else "Credit Limit") },
                leadingIcon = { Icon(Icons.Filled.AttachMoney, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_supp_input_limit"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(Modifier.height(30.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        keyboardController?.hide()
                        onDismiss()
                    },
                    modifier = Modifier.testTag("edit_supp_btn_cancel").height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(Localization.translate(Localization.Key.CANCEL, lang))
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            keyboardController?.hide()
                            val limitMilli = (limitInput.toDoubleOrNull() ?: 0.0) * 1000.0
                            onConfirm(name, phone, email, limitMilli.toLong(), groupName, groupId)
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.testTag("edit_supp_btn_save").height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (lang == "ar") "حفظ التعديلات" else "Save Changes")
                }
            }
        }
    }
}

// Group Details Dialogs
@Composable
fun AddSupplierGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit,
    lang: String
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (lang == "ar") "إضافة مجموعة موردين جديدة" else "Add Supplier Group",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (lang == "ar") "اسم المجموعة" else "Group Name") },
                    modifier = Modifier.fillMaxWidth().testTag("add_supp_group_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text(if (lang == "ar") "وصف المجموعة" else "Description") },
                    modifier = Modifier.fillMaxWidth().testTag("add_supp_group_desc_input"),
                    shape = RoundedCornerShape(12.dp),
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
                        onClick = { onConfirm(name, desc) },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (lang == "ar") "إنشاء المجموعة" else "Create Group")
                    }
                }
            }
        }
    }
}

@Composable
fun EditSupplierGroupDialog(
    group: com.example.data.SupplierGroup,
    onDismiss: () -> Unit,
    onConfirm: (com.example.data.SupplierGroup) -> Unit,
    lang: String
) {
    var name by remember { mutableStateOf(group.name) }
    var desc by remember { mutableStateOf(group.description) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (lang == "ar") "تعديل مجموعة الموردين" else "Edit Supplier Group",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (lang == "ar") "اسم المجموعة" else "Group Name") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_supp_group_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text(if (lang == "ar") "وصف المجموعة" else "Description") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_supp_group_desc_input"),
                    shape = RoundedCornerShape(12.dp),
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
                        onClick = { onConfirm(group.copy(name = name, description = desc)) },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (lang == "ar") "حفظ التعديلات" else "Save Changes")
                    }
                }
            }
        }
    }
}

