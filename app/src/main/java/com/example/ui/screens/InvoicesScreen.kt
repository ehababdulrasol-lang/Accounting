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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.viewmodel.LedgerViewModel
import com.example.util.FinancialUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isLibyan by viewModel.isLibyanMode.collectAsStateWithLifecycle()
    val isArabic = lang == "ar"

    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Sales, 1 = Purchase
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedInvoiceForView by remember { mutableStateOf<InvoiceHeader?>(null) }

    // Filtered invoices
    val filteredInvoices = remember(invoices, selectedTab, searchQuery) {
        invoices.filter {
            val typeMatches = if (selectedTab == 0) it.type == InvoiceType.SALES else it.type == InvoiceType.PURCHASE
            val queryMatches = searchQuery.isBlank() || 
                    it.invoiceNo.contains(searchQuery, ignoreCase = true) || 
                    it.counterPartyName.contains(searchQuery, ignoreCase = true) ||
                    it.notes.contains(searchQuery, ignoreCase = true)
            typeMatches && queryMatches
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Secondary Head Style
                Text(
                    text = if (isArabic) "إدارة الفواتير المدمجة" else "Invoices & Billing Hub",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Premium Segmented Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isArabic) "فواتير البيع" else "Sales Invoices",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingDown, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isArabic) "فواتير الشراء" else "Purchase Invoices",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isArabic) "ابحث برقم الفاتورة أو العميل..." else "Search by invoice # or name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (isArabic) "فاتورة جديدة" else "New Invoice") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (filteredInvoices.isEmpty()) {
                // Empty state illustration
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isArabic) "لا توجد فواتير مسجلة" else "No invoices recorded yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isArabic) 
                            "انقر على زر 'فاتورة جديدة' لإضافة أول فاتورة وترحيلها للحسابات العامة" 
                            else "Click 'New Invoice' to create and post your first transaction",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
                ) {
                    items(filteredInvoices, key = { it.id }) { invoice ->
                        InvoiceItemCard(
                            invoice = invoice,
                            isArabic = isArabic,
                            isLibyan = isLibyan,
                            onView = { selectedInvoiceForView = invoice },
                            onDelete = { viewModel.deleteInvoice(invoice) },
                            onPost = { viewModel.postInvoiceToLedger(invoice) }
                        )
                    }
                }
            }
        }
    }

    // Detail modal
    selectedInvoiceForView?.let { invoice ->
        InvoiceDetailDialog(
            invoice = invoice,
            viewModel = viewModel,
            isArabic = isArabic,
            isLibyan = isLibyan,
            onDismiss = { selectedInvoiceForView = null }
        )
    }

    // Invoice Create full modal
    if (showCreateDialog) {
        InvoiceCreateDialog(
            initialType = if (selectedTab == 0) InvoiceType.SALES else InvoiceType.PURCHASE,
            viewModel = viewModel,
            isArabic = isArabic,
            isLibyan = isLibyan,
            accounts = accounts,
            customers = customers,
            suppliers = suppliers,
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
fun InvoiceItemCard(
    invoice: InvoiceHeader,
    isArabic: Boolean,
    isLibyan: Boolean,
    onView: () -> Unit,
    onDelete: () -> Unit,
    onPost: () -> Unit
) {
    var showConfirmDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onView() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header Info
                Column {
                    Text(
                        text = "${if (invoice.type == InvoiceType.SALES) "SL-" else "PR-"}${invoice.invoiceNo}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(invoice.date)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Balance Badge & Status
                Surface(
                    color = if (invoice.isPosted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (invoice.isPosted) {
                            if (isArabic) "مُرحّل وحسابي" else "Posted to GL"
                        } else {
                            if (isArabic) "مسودة قيد" else "Draft"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (invoice.isPosted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Counterparty
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (invoice.type == InvoiceType.SALES) Icons.Default.Person else Icons.Default.Business,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${if (invoice.type == InvoiceType.SALES) {
                        if (isArabic) "العميل: " else "Customer: "
                    } else {
                        if (isArabic) "المورد: " else "Supplier: "
                    }}${invoice.counterPartyName}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Price Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (invoice.paymentMode == "CASH") {
                                if (isArabic) "نقدي" else "Cash"
                            } else {
                                if (isArabic) "آجل" else "Credit"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "${FinancialUtils.formatBase(invoice.totalAmount)} ${if (isLibyan) "د.ل" else "LYD"}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Options inside Card
            if (!invoice.isPosted) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showConfirmDelete) {
                        Text(
                            text = if (isArabic) "تأكيد الحذف؟" else "Delete?",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    onDelete()
                                    showConfirmDelete = false
                                }
                                .padding(8.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "إلغاء" else "Cancel",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .clickable { showConfirmDelete = false }
                                .padding(8.dp)
                        )
                    } else {
                        IconButton(onClick = { showConfirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { onPost() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isArabic) "ترحيل للحسابات" else "Post to Ledger",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceDetailDialog(
    invoice: InvoiceHeader,
    viewModel: LedgerViewModel,
    isArabic: Boolean,
    isLibyan: Boolean,
    onDismiss: () -> Unit
) {
    val linesFlow = remember(invoice.id) { viewModel.getInvoiceLines(invoice.id) }
    val lines by linesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (invoice.type == InvoiceType.SALES) {
                            if (isArabic) "تفاصيل فاتورة مبيعات" else "Sales Invoice Details"
                        } else {
                            if (isArabic) "تفاصيل فاتورة مشتريات" else "Purchase Invoice Details"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Metadata Details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = if (isArabic) "رقم الفاتورة:" else "Invoice No:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(text = "${if (invoice.type == InvoiceType.SALES) "SL-" else "PR-"}${invoice.invoiceNo}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = if (isArabic) "التاريخ:" else "Invoice Date:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(invoice.date)), style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = if (invoice.type == InvoiceType.SALES) {
                                if (isArabic) "العميل المستحق:" else "Customer:"
                            } else {
                                if (isArabic) "المورد الدائن:" else "Supplier:"
                            }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(text = invoice.counterPartyName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = if (isArabic) "طريقة السداد:" else "Payment Mode:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(text = if (invoice.paymentMode == "CASH") (if (isArabic) "نقدي" else "Cash") else (if (isArabic) "آجل على الحساب" else "Credit / On Account"), style = MaterialTheme.typography.bodyMedium)
                        }
                        if (invoice.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = if (isArabic) "ملاحظات الفاتورة:" else "Notes:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            Text(text = invoice.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Itemized lines
                Text(
                    text = if (isArabic) "بنود الفاتورة ومواد التسوية" else "Itemized Line Products",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    items(lines) { line ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = line.itemDescription,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                Text(
                                    text = "${FinancialUtils.formatBase(line.lineTotal)} ${if (isLibyan) "د.ل" else "LYD"}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${if (isArabic) "الكمية:" else "Qty:"} ${line.quantity} | ${if (isArabic) "سعر الوحدة:" else "Unit Price:"} ${line.unitPrice}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (line.discountPercent > 0.0 || line.taxPercent > 0.0) {
                                    Text(
                                        text = "${if (line.discountPercent > 0.0) "${if (isArabic) "خصم" else "Disc"} ${line.discountPercent}%" else ""} ${if (line.taxPercent > 0.0) "${if (isArabic) "ضريبة" else "Tax"} ${line.taxPercent}%" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Summary Calculation section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "إجمالي قيمة الفاتورة:" else "Total Net Amount:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${FinancialUtils.formatBase(invoice.totalAmount)} ${if (isLibyan) "د.ل" else "LYD"}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isArabic) "إغلاق التفاصيل" else "Close Screen")
                }
            }
        }
    }
}

// Struct for local draft state
data class LocalInvoiceLineEntry(
    val id: Long = System.nanoTime(),
    val itemDescription: String = "",
    val quantity: String = "1",
    val unitPrice: String = "",
    val discountPercent: String = "0",
    val taxPercent: String = "0"
) {
    val isValid: Boolean get() = itemDescription.isNotBlank() && (unitPrice.toDoubleOrNull() ?: 0.0) > 0.0 && (quantity.toDoubleOrNull() ?: 0.0) > 0.0

    val calculatedLineTotal: Long get() {
        val qty = quantity.toDoubleOrNull() ?: 0.0
        val price = unitPrice.toDoubleOrNull() ?: 0.0
        val baseGross = qty * price
        val discPercent = discountPercent.toDoubleOrNull() ?: 0.0
        val txPercent = taxPercent.toDoubleOrNull() ?: 0.0

        val discAmt = baseGross * (discPercent / 100.0)
        val grossAfterDisc = baseGross - discAmt
        val taxAmt = grossAfterDisc * (txPercent / 100.0)
        
        val netTotal = grossAfterDisc + taxAmt

        return (netTotal * FinancialUtils.BASE_SCALE_FACTOR).toLong()
    }
}

@Composable
fun InvoiceCreateDialog(
    initialType: InvoiceType,
    viewModel: LedgerViewModel,
    isArabic: Boolean,
    isLibyan: Boolean,
    accounts: List<Account>,
    customers: List<Customer>,
    suppliers: List<Supplier>,
    onDismiss: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    var selectedType by remember { mutableStateOf(initialType) }
    var invoiceNoStr by remember { mutableStateOf("") }
    var paymentModeStr by remember { mutableStateOf("CREDIT") } // "CREDIT" or "CASH"
    var notesStr by remember { mutableStateOf("") }

    // Account mapping (linked accounts)
    val partyAccounts = remember(selectedType, accounts, customers, suppliers) {
        if (selectedType == InvoiceType.SALES) {
            customers.mapNotNull { cust ->
                accounts.find { it.id == cust.accountId }?.let { acc ->
                    Pair(acc, cust.name)
                }
            }
        } else {
            suppliers.mapNotNull { supp ->
                accounts.find { it.id == supp.accountId }?.let { acc ->
                    Pair(acc, supp.name)
                }
            }
        }
    }

    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var explicitCounterPartyName by remember { mutableStateOf("") }

    // Initialize automatically first match from accounts receivable or payable
    LaunchedEffect(partyAccounts) {
        if (partyAccounts.isNotEmpty() && selectedAccountId == null) {
            selectedAccountId = partyAccounts.first().first.id
            explicitCounterPartyName = partyAccounts.first().second
        }
    }

    // List of Line entries
    var lineEntries by remember { mutableStateOf(listOf(LocalInvoiceLineEntry())) }

    // Aggregate Calculations
    val calculatedSubtotal = lineEntries.sumOf { it.calculatedLineTotal }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Dialog Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "تحرير وصياغة فاتورة" else "Draft New Standard Invoice",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Scrollable fields
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Type Selection Panel
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedType == InvoiceType.SALES,
                                onClick = {
                                    selectedType = InvoiceType.SALES
                                    selectedAccountId = null
                                },
                                label = { Text(if (isArabic) "بيع مخرجات" else "Sales Invoice") },
                                leadingIcon = {
                                    if (selectedType == InvoiceType.SALES) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedType == InvoiceType.PURCHASE,
                                onClick = {
                                    selectedType = InvoiceType.PURCHASE
                                    selectedAccountId = null
                                },
                                label = { Text(if (isArabic) "شراء مدخلات" else "Purchase Invoice") },
                                leadingIcon = {
                                    if (selectedType == InvoiceType.PURCHASE) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Metadata inputs row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = invoiceNoStr,
                                onValueChange = { invoiceNoStr = it },
                                label = { Text(if (isArabic) "رقم الفاتورة" else "Invoice #") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            // Payment mode
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) "طريقة السداد" else "Payment Type",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .clip(RoundedCornerShape(8.dp)),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isArabic) "آجل" else "Credit",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(if (paymentModeStr == "CREDIT") MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                            .clickable { paymentModeStr = "CREDIT" }
                                            .wrapContentHeight(Alignment.CenterVertically)
                                    )
                                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
                                    Text(
                                        text = if (isArabic) "نقدي" else "Cash",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(if (paymentModeStr == "CASH") MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                            .clickable { paymentModeStr = "CASH" }
                                            .wrapContentHeight(Alignment.CenterVertically)
                                    )
                                }
                            }
                        }
                    }

                    // Party Picker
                    item {
                        if (partyAccounts.isEmpty()) {
                            Text(
                                text = if (selectedType == InvoiceType.SALES) {
                                    if (isArabic) "⚠️ لا يوجد عملاء مسجلين! يرجى تهيئة العملاء أولاً مع ربط حساباتهم" else "⚠️ No customers found! Register a customer with accounts linked."
                                } else {
                                    if (isArabic) "⚠️ لا يوجد موردين مسجلين! يرجى تهيئة الموردين أولاً مع ربط حساباتهم" else "⚠️ No suppliers found! Register a supplier with accounts linked."
                                },
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            var dropdownExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = explicitCounterPartyName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = {
                                        Text(
                                            if (selectedType == InvoiceType.SALES) {
                                                if (isArabic) "اختر العميل المستحق" else "Select Customer Name"
                                            } else {
                                                if (isArabic) "اختر المورد الدائن" else "Select Supplier Name"
                                            }
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { dropdownExpanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                DropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.95f)
                                ) {
                                    partyAccounts.forEach { (acc, name) ->
                                        DropdownMenuItem(
                                            text = { Text("$name (${acc.accountCode})") },
                                            onClick = {
                                                selectedAccountId = acc.id
                                                explicitCounterPartyName = name
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Notes
                    item {
                        OutlinedTextField(
                            value = notesStr,
                            onValueChange = { notesStr = it },
                            label = { Text(if (isArabic) "شرح الفاتورة وملاحظات ترحيل القيد" else "Invoice Description / Memo") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Line Items Title Section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) "بنود الفاتورة ومواد التسوية" else "Itemized Line Products",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            IconButton(
                                onClick = {
                                    lineEntries = lineEntries + LocalInvoiceLineEntry()
                                }
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    // Fields form for each line item
                    items(lineEntries.size) { index ->
                        val item = lineEntries[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${if (isArabic) "بند رقم" else "Item #"} ${index + 1}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (lineEntries.size > 1) {
                                        IconButton(
                                            onClick = {
                                                val mutable = lineEntries.toMutableList()
                                                mutable.removeAt(index)
                                                lineEntries = mutable
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.RemoveCircle, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                OutlinedTextField(
                                    value = item.itemDescription,
                                    onValueChange = { desc ->
                                        lineEntries = lineEntries.toMutableList().apply {
                                            this[index] = this[index].copy(itemDescription = desc)
                                        }
                                    },
                                    label = { Text(if (isArabic) "وصف المكون / السلعة" else "Product / Item Description") },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = item.quantity,
                                        onValueChange = { qty ->
                                            lineEntries = lineEntries.toMutableList().apply {
                                                this[index] = this[index].copy(quantity = qty)
                                            }
                                        },
                                        label = { Text(if (isArabic) "الكمية" else "Qty") },
                                        shape = RoundedCornerShape(8.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = item.unitPrice,
                                        onValueChange = { price ->
                                            lineEntries = lineEntries.toMutableList().apply {
                                                this[index] = this[index].copy(unitPrice = price)
                                            }
                                        },
                                        label = { Text(if (isArabic) "السعر الفرعي" else "Unit Price") },
                                        shape = RoundedCornerShape(8.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                                        modifier = Modifier.weight(1.5f),
                                        singleLine = true
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = item.discountPercent,
                                        onValueChange = { disc ->
                                            lineEntries = lineEntries.toMutableList().apply {
                                                this[index] = this[index].copy(discountPercent = disc)
                                            }
                                        },
                                        label = { Text(if (isArabic) "الخصم %" else "Disc %") },
                                        shape = RoundedCornerShape(8.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = item.taxPercent,
                                        onValueChange = { tax ->
                                            lineEntries = lineEntries.toMutableList().apply {
                                                this[index] = this[index].copy(taxPercent = tax)
                                            }
                                        },
                                        label = { Text(if (isArabic) "الضريبة %" else "Tax %") },
                                        shape = RoundedCornerShape(8.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )

                                    Column(
                                        modifier = Modifier
                                            .weight(1.5f)
                                            .padding(top = 10.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = if (isArabic) "إجمالي فرعي" else "Subtotal",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${FinancialUtils.formatBase(item.calculatedLineTotal)} ${if (isLibyan) "د.ل" else "LYD"}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Summary Calculation section
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "القيمة الإجمالية الصافية:" else "Estimated Net Total:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${FinancialUtils.formatBase(calculatedSubtotal)} ${if (isLibyan) "د.ل" else "LYD"}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isArabic) "إلغاء وتجاهل" else "Cancel")
                    }

                    Button(
                        onClick = {
                            val validLines = lineEntries.filter { it.isValid }
                            if (validLines.isEmpty()) {
                                return@Button
                            }

                            val accountIdResolved = selectedAccountId ?: 0L

                            // Generate random standard Code
                            val docNo = invoiceNoStr.ifBlank { System.currentTimeMillis().toString().takeLast(6) }

                            val header = InvoiceHeader(
                                invoiceNo = docNo,
                                date = System.currentTimeMillis(),
                                type = selectedType,
                                accountId = accountIdResolved,
                                counterPartyName = explicitCounterPartyName.ifBlank { "Cash Client" },
                                paymentMode = paymentModeStr,
                                notes = notesStr,
                                totalAmount = calculatedSubtotal,
                                isPosted = false
                            )

                            val lines = validLines.map {
                                InvoiceLine(
                                    invoiceId = 0,
                                    itemDescription = it.itemDescription,
                                    quantity = it.quantity.toDoubleOrNull() ?: 1.0,
                                    unitPrice = it.unitPrice.toDoubleOrNull() ?: 0.0,
                                    discountPercent = it.discountPercent.toDoubleOrNull() ?: 0.0,
                                    taxPercent = it.taxPercent.toDoubleOrNull() ?: 0.0,
                                    lineTotal = it.calculatedLineTotal
                                )
                            }

                            viewModel.saveInvoice(header, lines, onFinish = onDismiss)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = lineEntries.any { it.isValid } && (paymentModeStr == "CASH" || selectedAccountId != null),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "حفظ كمسودة" else "Save as Draft",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
