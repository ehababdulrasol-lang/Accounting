package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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
    var searchTxt by remember { mutableStateOf("") }

    val filteredHeaders = remember(headers, selectedTypeFilter, selectedStatusFilter, searchTxt) {
        headers.filter { header ->
            val typeMatch = selectedTypeFilter == null || header.type == selectedTypeFilter
            val statusMatch = selectedStatusFilter == null || header.isPosted == selectedStatusFilter
            val searchMatch = searchTxt.isBlank() ||
                    header.voucherNo.contains(searchTxt, ignoreCase = true) ||
                    header.description.contains(searchTxt, ignoreCase = true)
            typeMatch && statusMatch && searchMatch
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Screen Header Section with Modern Flow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Localization.translate(Localization.Key.VOUCHER_TX_JOURNAL, lang),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (lang == "ar") "أرشيف القيود وسندات اليومية العامة للنظام ماليًا" else "General ledger entries and daily voucher registry",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                
                // Add Quick Voucher shortcut header
                IconButton(
                    onClick = {
                        viewModel.createNewVoucherForm()
                        onNavigateToEditor()
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "New",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            val draftCount = remember(headers) { headers.count { !it.isPosted } }
            val postedCount = remember(headers) { headers.count { it.isPosted } }
            val totalAmount = remember(headers) { headers.sumOf { it.totalAmountBase } }

            // High Fidelity Unified Statistical Deck
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .shadow(12.dp, shape = RoundedCornerShape(16.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (lang == "ar") "موجز النشاط المالي" else "Financial Activity Summary",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cumulative Volume Panel
                        Column(modifier = Modifier.weight(1.3f)) {
                            Text(
                                text = if (lang == "ar") "إجمالي القيمة المتداولة" else "Cumulative Flow",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = FinancialUtils.formatBase(totalAmount),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (isLibyan) "د.ل" else "LYD",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = Bold,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        }

                        // Divider line
                        Box(modifier = Modifier.width(1.dp).height(38.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)))

                        // Unlocked Drafts Section
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (lang == "ar") "مسودات قيد التحقق" else "Pending Drafts",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "$draftCount",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Locked Posted Section
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(EmeraldGreen, CircleShape))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (lang == "ar") "القيود المعتمدة" else "Approved / Posted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "$postedCount",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                        }
                    }
                }
            }

            // Interactive Search Entry on the Journal
            OutlinedTextField(
                value = searchTxt,
                onValueChange = { searchTxt = it },
                placeholder = { Text(if (lang == "ar") "ابحث برقم السند أو بملاحظات البيان..." else "Search voucher # or narration...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                trailingIcon = {
                    if (searchTxt.isNotEmpty()) {
                        IconButton(onClick = { searchTxt = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            // Horizontal Pill filtering layout (Unified Row Filters)
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                // Voucher Type Tabs Scroll row
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val types = listOf(null) + VoucherType.values()
                    types.forEach { t ->
                        val selected = selectedTypeFilter == t
                        val typeLabel = when (t) {
                            null -> if (lang == "ar") "كل التصاميم" else "All Entries"
                            VoucherType.JOURNAL -> Localization.translate(Localization.Key.ST_JOURNAL, lang)
                            VoucherType.RECEIPT -> Localization.translate(Localization.Key.ST_RECEIPT, lang)
                            VoucherType.PAYMENT -> Localization.translate(Localization.Key.ST_PAYMENT, lang)
                        }

                        val containerColor = if (selected) {
                            when (t) {
                                null -> MaterialTheme.colorScheme.primary
                                VoucherType.JOURNAL -> MaterialTheme.colorScheme.primary
                                VoucherType.RECEIPT -> EmeraldGreen
                                VoucherType.PAYMENT -> RoseRed
                            }
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        }

                        val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(containerColor)
                                .clickable { selectedTypeFilter = t }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = typeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Voucher Status Filter Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statuses = listOf(
                        null to (if (lang == "ar") "كل الحالات" else "Any Status"),
                        false to Localization.translate(Localization.Key.DRAFT, lang),
                        true to Localization.translate(Localization.Key.POSTED, lang)
                    )

                    statuses.forEach { (status, label) ->
                        val isSelected = selectedStatusFilter == status
                        ElevatedAssistChip(
                            onClick = { selectedStatusFilter = status },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.elevatedAssistChipColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                                labelColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent
                            ),
                            leadingIcon = {
                                if (status == true) {
                                    Box(modifier = Modifier.size(6.dp).background(EmeraldGreen, CircleShape))
                                } else if (status == false) {
                                    Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                }
                            }
                        )
                    }
                }
            }

            // Journal Feed List Representation
            if (filteredHeaders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Inbox,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (lang == "ar") "لا توجد أي قيود أو سندات مطابقة!" else "This journal filter is currently empty.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (lang == "ar") "يرجى تغيير فلاتر التصفية أو صياغة قيد محاسبي جديد" else "Try clearing your filters or tap add below to begin logging transactions.",
                            style = MaterialTheme.typography.bodySmall,
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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
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
        SmallFloatingActionButton(
            onClick = {
                viewModel.createNewVoucherForm()
                onNavigateToEditor()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("create_voucher_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "New Voucher", modifier = Modifier.size(20.dp))
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

    // Left Accent Banner Color representing the category of the ledger sheet
    val accentColor = when (header.type) {
        VoucherType.JOURNAL -> MaterialTheme.colorScheme.primary
        VoucherType.RECEIPT -> EmeraldGreen
        VoucherType.PAYMENT -> RoseRed
    }

    var isMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("voucher_header_card_${header.voucherNo}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Lateral side stripe
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .matchParentSize()
                    .align(if (lang == "ar") Alignment.CenterEnd else Alignment.CenterStart)
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (lang == "ar") 16.dp else 20.dp, end = if (lang == "ar") 20.dp else 16.dp)
                    .padding(vertical = 16.dp)
            ) {
                // Top Metadata line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Styled Code Label
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.12f))
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
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = header.voucherNo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(Modifier.weight(1f))

                    // Minimal Indicator Tag of Authorization State
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (header.isPosted) EmeraldGreen.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (header.isPosted) EmeraldGreen else MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (header.isPosted) {
                                    if (lang == "ar") "مُعتمد" else "POSTED"
                                } else {
                                    if (lang == "ar") "مسوّدة" else "DRAFT"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (header.isPosted) EmeraldGreen else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Description Block
                Spacer(Modifier.height(10.dp))
                Text(
                    text = header.description.ifBlank {
                        if (lang == "ar") "لا تتوفر مذكرات توضيحية لهذه القيود الحالية" else "No explanation or memoirs attached"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (header.description.isBlank()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Date, Period + Grand Amount Block
                Spacer(Modifier.height(14.dp))
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
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "$formattedDate  •  $fiscalYearName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.weight(1f))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = FinancialUtils.formatBase(header.totalAmountBase),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isLibyan) "د.ل" else "LYD",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontWeight = Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                // Decorative Invoice dotted dividing trace
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            )
                    )
                }

                // Expanded Actions Footer Panel on Each Header card
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Quick Icon based actions for printing
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onPrint,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("print_voucher_item_${header.voucherNo}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Print,
                                contentDescription = "Print Voucher",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (!header.isPosted) {
                            Spacer(Modifier.width(4.dp))
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = RoseRed.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Large dynamic state action button
                    if (!header.isPosted) {
                        Button(
                            onClick = onPost,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldGreen,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AssignmentTurnedIn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (lang == "ar") "اعتماد وترحيل" else "Post Ledger",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = onUnpost,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Undo,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (lang == "ar") "إرجاع لمسودة" else "Revert Draft",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
