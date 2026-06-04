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
import androidx.compose.foundation.verticalScroll
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
import com.example.data.Customer
import com.example.ui.Localization
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.ui.viewmodel.LedgerViewModel
import com.example.util.FinancialUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val customerGroups by viewModel.customerGroups.collectAsStateWithLifecycle()
    val allAccounts by viewModel.accounts.collectAsStateWithLifecycle()
    val leafAccounts by viewModel.leafAccounts.collectAsStateWithLifecycle()
    val snapshots by viewModel.accountSnapshots.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }
    var showSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Group Management state
    var selectedGroup by remember { mutableStateOf<com.example.data.CustomerGroup?>(null) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<com.example.data.CustomerGroup?>(null) }
    var groupToDelete by remember { mutableStateOf<com.example.data.CustomerGroup?>(null) }

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
                    text = Localization.translate(Localization.Key.CUSTOMER_MANAGEMENT, lang),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (lang == "ar") "تتبع ديون وأرصدة العملاء مع ربط تلقائي ومستندات تدقيق المعايير الدولية IFRS." else "Track customer outstanding balances with automated balance tracking & IFRS audit trail compliance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Group Carousel section
                Text(
                    text = if (lang == "ar") "تصنيفات ومجموعات العملاء" else "Customer Categories & Groups",
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
                                    imageVector = Icons.Filled.People,
                                    contentDescription = null,
                                    tint = if (selectedGroup == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Badge(
                                    containerColor = if (selectedGroup == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    contentColor = if (selectedGroup == null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ) {
                                    Text(text = "${customers.size}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Text(
                                text = if (lang == "ar") "كل العملاء" else "All Customers",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedGroup == null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // 2. Custom Groups Cards
                    customerGroups.forEach { group ->
                        val groupCount = remember(customers, group) {
                            customers.count { it.groupId == group.id || it.groupName.trim().equals(group.name.trim(), ignoreCase = true) }
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
                    label = { Text(Localization.translate(Localization.Key.SEARCH_CUSTOMER, lang)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("customer_search"),
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

                val filtered = customers.filter {
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
                                Icons.Filled.PeopleOutline,
                                contentDescription = "",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = Localization.translate(Localization.Key.NO_CUSTOMERS_FOUND, lang),
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
                        grouped.forEach { (groupName, customerList) ->
                            item {
                                val title = if (groupName.isBlank()) {
                                    if (lang == "ar") "عملاء عامون (غير مصنفين)" else "General / Uncategorized"
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
                                            text = "${customerList.size}",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            itemsIndexed(customerList) { idx, customer ->
                                com.example.ui.StaggeredItem(index = idx) {
                                    val linkedAccount = allAccounts.find { it.id == customer.accountId }
                                    val balance = remember(customer.accountId, snapshots) {
                                        snapshots.find { it.accountId == customer.accountId }?.balance ?: 0L
                                    }

                                    CustomerCard(
                                        customer = customer,
                                        account = linkedAccount,
                                        balance = balance,
                                        onDelete = { customerToDelete = customer },
                                        onEdit = { editingCustomer = customer },
                                        onViewStatement = {
                                            viewModel.statementTargetAccountId.value = customer.accountId
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

            // FAB
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("add_customer_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Customer")
            }

            if (showAddDialog) {
                AddCustomerDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { name, phone, email, linkAccId, groupName, groupId ->
                        viewModel.addCustomer(name, phone, email, linkAccId, groupName, groupId)
                        showAddDialog = false
                        showSuccessMessage = if (lang == "ar") "تم تسجيل ملف العميل بنجاح" else "Customer registered successfully!"
                    },
                    leafAccounts = leafAccounts,
                    customerGroups = customerGroups,
                    lang = lang
                )
            }

            if (editingCustomer != null) {
                EditCustomerDialog(
                    customer = editingCustomer!!,
                    onDismiss = { editingCustomer = null },
                    onConfirm = { name, phone, email, groupName, groupId ->
                        viewModel.updateCustomer(editingCustomer!!.copy(name = name, phone = phone, email = email, groupName = groupName, groupId = groupId))
                        editingCustomer = null
                        showSuccessMessage = if (lang == "ar") "تم تعديل بيانات العميل بنجاح" else "Customer details updated successfully!"
                    },
                    customerGroups = customerGroups,
                    lang = lang
                )
            }

            // Customer Group Actions
            if (showAddGroupDialog) {
                AddCustomerGroupDialog(
                    onDismiss = { showAddGroupDialog = false },
                    onConfirm = { name, desc ->
                        viewModel.addCustomerGroup(name, desc)
                        showAddGroupDialog = false
                        showSuccessMessage = if (lang == "ar") "تم إضافة مجموعة العملاء بنجاح" else "Customer group created successfully!"
                    },
                    lang = lang
                )
            }

            if (editingGroup != null) {
                EditCustomerGroupDialog(
                    group = editingGroup!!,
                    onDismiss = { editingGroup = null },
                    onConfirm = { updated ->
                        viewModel.updateCustomerGroup(updated)
                        editingGroup = null
                        showSuccessMessage = if (lang == "ar") "تم تعديل بيانات المجموعة بنجاح" else "Group updated successfully!"
                    },
                    lang = lang
                )
            }

            if (groupToDelete != null) {
                val gName = groupToDelete!!.name
                AnimatedDeleteConfirmDialog(
                    title = if (lang == "ar") "تأكيد حذف مجموعة عملاء" else "Confirm Group Deletion",
                    message = if (lang == "ar") "هل أنت متأكد من رغبتك في حذف المجموعة: $gName؟ لن يتم حذف العملاء التابعين لها." else "Are you sure you want to delete group: $gName? Customers under it will not be deleted.",
                    lang = lang,
                    onConfirm = {
                        val toDel = groupToDelete!!
                        if (selectedGroup?.id == toDel.id) {
                            selectedGroup = null
                        }
                        viewModel.deleteCustomerGroup(toDel)
                        groupToDelete = null
                        showSuccessMessage = if (lang == "ar") "تم حذف مجموعة العملاء بنجاح" else "Customer group deleted successfully!"
                    },
                    onDismiss = { groupToDelete = null }
                )
            }

            if (customerToDelete != null) {
                val cName = customerToDelete!!.name
                AnimatedDeleteConfirmDialog(
                    title = if (lang == "ar") "تأكيد حذف العميل" else "Confirm Customer Deletion",
                    message = if (lang == "ar") "هل أنت متأكد من رغبتك في حذف ملف العميل: $cName؟" else "Are you sure you want to delete customer profile: $cName?",
                    lang = lang,
                    onConfirm = {
                        val toDel = customerToDelete!!
                        viewModel.deleteCustomer(toDel)
                        customerToDelete = null
                        showSuccessMessage = if (lang == "ar") "تم حذف ملف العميل بنجاح" else "Customer deleted successfully!"
                    },
                    onDismiss = { customerToDelete = null }
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
fun CustomerCard(
    customer: Customer,
    account: Account?,
    balance: Long,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onViewStatement: () -> Unit,
    lang: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("customer_card_${customer.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
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
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (customer.phone.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                            Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                            Spacer(Modifier.width(6.dp))
                            Text(customer.phone, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                    if (customer.email.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                            Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                            Spacer(Modifier.width(6.dp))
                            Text(customer.email, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${Localization.translate(Localization.Key.ACCOUNT, lang)}: ${account?.accountCode ?: "---"} - ${account?.name ?: "---"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Balance
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (lang == "ar") "الرصيد المالي" else "Balance",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = FinancialUtils.formatBase(balance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (balance >= 0L) EmeraldGreen else RoseRed,
                        modifier = Modifier.testTag("customer_balance_${customer.id}")
                    )
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
                    modifier = Modifier.weight(1f).height(38.dp).testTag("customer_statement_${customer.id}"),
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
                    modifier = Modifier.weight(1f).height(38.dp).testTag("customer_edit_${customer.id}"),
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
                    onClick = onDelete,
                    modifier = Modifier.size(38.dp).testTag("delete_customer_${customer.id}")
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = RoseRed)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCustomerDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Long?) -> Unit,
    customerGroups: List<com.example.data.CustomerGroup>,
    lang: String
) {
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var name by remember { mutableStateOf(customer.name) }
    var phone by remember { mutableStateOf(customer.phone) }
    var email by remember { mutableStateOf(customer.email) }
    var groupName by remember { mutableStateOf(customer.groupName) }
    var groupId by remember { mutableStateOf<Long?>(customer.groupId) }

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
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 8.dp)
        ) {
            // FIXED HEADER
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
                        text = if (lang == "ar") "تعديل بيانات العميل" else "Edit Customer Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (lang == "ar") "تحديث معلومات الاتصال والربط المالي بالعميل" else "Update customer contact and ledger details",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // SCROLLABLE FORM BODY
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Localization.translate(Localization.Key.CUSTOMER_NAME, lang)) },
                    leadingIcon = { Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_cust_input_name"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(Localization.translate(Localization.Key.PHONE, lang)) },
                    leadingIcon = { Icon(Icons.Filled.Phone, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_cust_input_phone"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(Localization.translate(Localization.Key.EMAIL, lang)) },
                    leadingIcon = { Icon(Icons.Filled.Email, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_cust_input_email"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(Modifier.height(14.dp))

                // Beautiful dropdown group selection
                Box(modifier = Modifier.fillMaxWidth()) {
                    val selectedGroupText = remember(groupId, customerGroups, groupName) {
                        customerGroups.find { it.id == groupId }?.name ?: groupName.ifBlank { if (lang == "ar") "عام / غير مصنف" else "General" }
                    }

                    OutlinedTextField(
                        value = selectedGroupText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (lang == "ar") "مجموعة العميل" else "Customer Group") },
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
                        customerGroups.forEach { group ->
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
            }

            Spacer(Modifier.height(16.dp))

            // FIXED FOOTER
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
                    modifier = Modifier.testTag("edit_cust_btn_cancel").height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(Localization.translate(Localization.Key.CANCEL, lang))
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            keyboardController?.hide()
                            onConfirm(name, phone, email, groupName, groupId)
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.testTag("edit_cust_btn_save").height(48.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Long?, String, Long?) -> Unit,
    leafAccounts: List<Account>,
    customerGroups: List<com.example.data.CustomerGroup>,
    lang: String
) {
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var groupId by remember { mutableStateOf<Long?>(null) }

    var expandedGroupDropdown by remember { mutableStateOf(false) }

    // Strategies: 0 = Auto-open new, 1 = Link to existing matching name, 2 = Choose exist manually
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
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 8.dp)
        ) {
            // FIXED HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = Localization.translate(Localization.Key.ADD_NEW_CUSTOMER, lang),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (lang == "ar") "أدخل معلومات العميل الجديد لإنشاء ملف وربطه محاسبياً" else "Add new customer profile to track receivables and audit trails",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // SCROLLABLE FORM BODY
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Localization.translate(Localization.Key.CUSTOMER_NAME, lang)) },
                    leadingIcon = { Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cust_input_name"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(Localization.translate(Localization.Key.PHONE, lang)) },
                    leadingIcon = { Icon(Icons.Filled.Phone, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cust_input_phone"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(Localization.translate(Localization.Key.EMAIL, lang)) },
                    leadingIcon = { Icon(Icons.Filled.Email, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cust_input_email"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(Modifier.height(14.dp))

                // Beautiful dropdown group selection
                Box(modifier = Modifier.fillMaxWidth()) {
                    val selectedGroupText = remember(groupId, customerGroups, groupName) {
                        customerGroups.find { it.id == groupId }?.name ?: groupName.ifBlank { if (lang == "ar") "عام / غير مصنف" else "General" }
                    }

                    OutlinedTextField(
                        value = selectedGroupText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (lang == "ar") "مجموعة العميل" else "Customer Group") },
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
                        customerGroups.forEach { group ->
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
                    text = Localization.translate(Localization.Key.C_ACCOUNT_LINKING, lang),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Selectable Custom cards for strategies
                // 1. Auto-open
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
                                text = Localization.translate(Localization.Key.AUTO_OPEN_ACCOUNT, lang),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (lang == "ar") "سيقوم النظام بإنشاء حساب أستاذ مستقل بالدليل بشكل آلي تمامًا" else "System will auto-create a ledger account in CoA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // 2. Link existing matching
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
                                    text = "${Localization.translate(Localization.Key.LINK_EXISTING_MATCH, lang)} (${matchExist.accountCode})",
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

                // 3. Link existing manually
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
                                text = Localization.translate(Localization.Key.LINK_EXISTING, lang),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (lang == "ar") "تحديد حساب يدويًا من شجرة الحسابات دون قيود مطابقة الاسم" else "Choose any manual active account from Chart of Accounts",
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
                        value = currentSelection?.let { "${it.accountCode} - ${it.name}" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(Localization.translate(Localization.Key.SELECT_EXISTING_COA, lang)) },
                        leadingIcon = { Icon(Icons.Filled.AccountBalance, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            IconButton(onClick = { showAccountSearchDialog = true }) {
                                Icon(Icons.Filled.Search, contentDescription = "Search")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAccountSearchDialog = true }
                            .testTag("cust_select_exist_trigger"),
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
            }

            Spacer(Modifier.height(16.dp))

            // FIXED FOOTER
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
                    modifier = Modifier.testTag("cust_btn_cancel").height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(Localization.translate(Localization.Key.CANCEL, lang))
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            keyboardController?.hide()
                            val linkId = when (linkStrategy) {
                                1 -> matchExist?.id
                                2 -> selectedExistAccountId
                                else -> null // Auto open
                            }
                            onConfirm(name, phone, email, linkId, groupName, groupId)
                        }
                    },
                    enabled = name.isNotBlank() && (linkStrategy != 2 || selectedExistAccountId != null),
                    modifier = Modifier.testTag("cust_btn_save").height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(Localization.translate(Localization.Key.SAVE_CUSTOMER, lang))
                }
            }
        }
    }
}

// Group Details dialogs
@Composable
fun AddCustomerGroupDialog(
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
                    text = if (lang == "ar") "إضافة مجموعة عملاء جديدة" else "Add Customer Group",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (lang == "ar") "اسم المجموعة" else "Group Name") },
                    modifier = Modifier.fillMaxWidth().testTag("add_group_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text(if (lang == "ar") "وصف المجموعة" else "Description") },
                    modifier = Modifier.fillMaxWidth().testTag("add_group_desc_input"),
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
fun EditCustomerGroupDialog(
    group: com.example.data.CustomerGroup,
    onDismiss: () -> Unit,
    onConfirm: (com.example.data.CustomerGroup) -> Unit,
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
                    text = if (lang == "ar") "تعديل مجموعة العملاء" else "Edit Customer Group",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (lang == "ar") "اسم المجموعة" else "Group Name") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_group_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text(if (lang == "ar") "وصف المجموعة" else "Description") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_group_desc_input"),
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
