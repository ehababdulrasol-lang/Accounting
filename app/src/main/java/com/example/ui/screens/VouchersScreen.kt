package com.example.ui.screens

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
import com.example.data.VoucherHeader
import com.example.data.VoucherType
import com.example.ui.Localization
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.ui.viewmodel.LedgerViewModel
import com.example.util.FinancialUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VouchersScreen(
    viewModel: LedgerViewModel,
    onNavigateToEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val headers by viewModel.vouchers.collectAsState()
    val fyList by viewModel.fiscalYears.collectAsState()
    val lang by viewModel.currentLanguage.collectAsState()
    val isLibyan by viewModel.isLibyanMode.collectAsState()

    var selectedTypeFilter by remember { mutableStateOf<VoucherType?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<Boolean?>(null) } // true for Posted, false for Draft

    val filteredHeaders = remember(headers, selectedTypeFilter, selectedStatusFilter) {
        headers.filter { header ->
            val typeMatch = selectedTypeFilter == null || header.type == selectedTypeFilter
            val statusMatch = selectedStatusFilter == null || header.isPosted == selectedStatusFilter
            typeMatch && statusMatch
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = Localization.translate(Localization.Key.VOUCHER_TX_JOURNAL, lang),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (lang == "ar") "سندات القيود المسودة مرنة وبسيطة. عملية الترحيل تثبت السند وتجمده برمجياً في الصندوق ودفتر الأستاذ." else "Draft vouchers are flexible and auto-validated. Posting permanently records and locks entries in the financial ledger.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val draftCount = remember(headers) { headers.count { !it.isPosted } }
            val postedCount = remember(headers) { headers.count { it.isPosted } }
            val totalAmount = remember(headers) { headers.sumOf { it.totalAmountBase } }

            // Dynamic statistics row
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Vouchers Stat Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (lang == "ar") "إجمالي السندات" else "Total Vouchers",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${headers.size}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Posted Vouchers Stat Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.05f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (lang == "ar") "المُرحلة" else "Posted",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "$postedCount",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                    }
                }

                // Balance Stat Card
                Card(
                    modifier = Modifier.weight(1.2f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (lang == "ar") "إجمالي المعاملات" else "Total Volume",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = FinancialUtils.formatBase(totalAmount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = if (isLibyan) "د.ل" else "LYD",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // Dynamic filter bar
            Text(
                text = if (lang == "ar") "تصفية وفرز" else "FILTERS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(4.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Type filter buttons
                InputChip(
                    selected = selectedTypeFilter == null,
                    onClick = { selectedTypeFilter = null },
                    label = { Text(if (lang == "ar") "كل الأنواع" else "All Types") }
                )
                VoucherType.values().forEach { t ->
                    val typeLabel = when (t) {
                        VoucherType.JOURNAL -> Localization.translate(Localization.Key.ST_JOURNAL, lang)
                        VoucherType.RECEIPT -> Localization.translate(Localization.Key.ST_RECEIPT, lang)
                        VoucherType.PAYMENT -> Localization.translate(Localization.Key.ST_PAYMENT, lang)
                    }
                    InputChip(
                        selected = selectedTypeFilter == t,
                        onClick = { selectedTypeFilter = t },
                        label = { Text(typeLabel) }
                    )
                }

                Spacer(modifier = Modifier.width(4.dp).height(1.dp))

                // Status Filter
                InputChip(
                    selected = selectedStatusFilter == null,
                    onClick = { selectedStatusFilter = null },
                    label = { Text(if (lang == "ar") "كل الحالات" else "All Statuses") }
                )
                InputChip(
                    selected = selectedStatusFilter == false,
                    onClick = { selectedStatusFilter = false },
                    label = { Text(Localization.translate(Localization.Key.DRAFT, lang)) },
                    leadingIcon = { Badge(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) {} }
                )
                InputChip(
                    selected = selectedStatusFilter == true,
                    onClick = { selectedStatusFilter = true },
                    label = { Text(Localization.translate(Localization.Key.POSTED, lang)) },
                    leadingIcon = { Badge(containerColor = EmeraldGreen) {} }
                )
            }

            if (filteredHeaders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                        .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (lang == "ar") "لا توجد قيود/سندات تطابق الفلتر المحدد." else "No vouchers matched your filters.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                val context = androidx.compose.ui.platform.LocalContext.current
                val scope = rememberCoroutineScope()
                val accounts by viewModel.accounts.collectAsState()
                val currencies by viewModel.currencies.collectAsState()

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    items(filteredHeaders) { header ->
                        val fyName = fyList.find { it.id == header.fiscalYearId }?.name ?: "FY Unknown"
                        VoucherHeaderItem(
                            header = header,
                            fiscalYearName = fyName,
                            lang = lang,
                            isLibyan = isLibyan,
                            onClick = {
                                viewModel.editVoucherDraft(header)
                                onNavigateToEditor()
                            },
                            onPost = { viewModel.postActiveVoucher(header.id) },
                            onUnpost = { viewModel.unpostActiveVoucher(header.id) },
                            onDelete = { viewModel.deleteActiveVoucher(header.id) },
                            onPrint = {
                                scope.launch {
                                    val lines = viewModel.getVoucherLines(header.id)
                                    com.example.util.PrintUtils.printVoucher(
                                        context = context,
                                        voucher = header,
                                        lines = lines,
                                        allAccounts = accounts,
                                        currencies = currencies,
                                        lang = lang
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button to create a new double-entry voucher
        FloatingActionButton(
            onClick = {
                viewModel.createNewVoucherForm()
                onNavigateToEditor()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("create_voucher_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(Icons.Filled.Add, contentDescription = "New Voucher")
        }
    }
}

@Composable
fun VoucherHeaderItem(
    header: VoucherHeader,
    fiscalYearName: String,
    lang: String,
    isLibyan: Boolean,
    onClick: () -> Unit,
    onPost: () -> Unit,
    onUnpost: () -> Unit,
    onDelete: () -> Unit,
    onPrint: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val formattedDate = remember(header.date) { formatter.format(Date(header.date)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("voucher_header_card_${header.voucherNo}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type prefix indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (header.type) {
                                VoucherType.JOURNAL -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                VoucherType.RECEIPT -> EmeraldGreen.copy(alpha = 0.15f)
                                VoucherType.PAYMENT -> RoseRed.copy(alpha = 0.15f)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val typeText = when (header.type) {
                        VoucherType.JOURNAL -> Localization.translate(Localization.Key.ST_JOURNAL, lang)
                        VoucherType.RECEIPT -> Localization.translate(Localization.Key.ST_RECEIPT, lang)
                        VoucherType.PAYMENT -> Localization.translate(Localization.Key.ST_PAYMENT, lang)
                    }
                    Text(
                        text = typeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (header.type) {
                            VoucherType.JOURNAL -> MaterialTheme.colorScheme.primary
                            VoucherType.RECEIPT -> EmeraldGreen
                            VoucherType.PAYMENT -> RoseRed
                        }
                    )
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text = header.voucherNo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.weight(1f))

                // Posted vs Draft status pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (header.isPosted) EmeraldGreen.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (header.isPosted) {
                            if (lang == "ar") "مُرحّلْ ومحمي" else "POSTED (Locked)"
                        } else {
                            if (lang == "ar") "مسوّدة وقابل للتعديل" else "DRAFT (Adjustable)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (header.isPosted) EmeraldGreen else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            if (header.description.isNotBlank()) {
                Text(
                    text = header.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 2
                )
                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$formattedDate  •  $fiscalYearName",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(Modifier.weight(1f))

                Text(
                    text = FinancialUtils.formatBase(header.totalAmountBase),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (isLibyan) "د.ل" else "LYD",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            // Quick actions footer
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick printable document action
                IconButton(
                    onClick = onPrint,
                    modifier = Modifier.testTag("print_voucher_item_${header.voucherNo}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Print,
                        contentDescription = "Print Voucher",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(8.dp))

                if (!header.isPosted) {
                    // Draft quick actions
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = RoseRed)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (lang == "ar") "حذف المسودة" else "Delete Draft")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onPost,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (lang == "ar") "ترحيل مزدوج القيد" else "Post double-entry")
                    }
                } else {
                    // Posted quick actions
                    TextButton(
                        onClick = onUnpost,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (lang == "ar") "إلغاء الترحيل برمجياً" else "Revert to Draft (Unpost)")
                    }
                }
            }
        }
    }
}
