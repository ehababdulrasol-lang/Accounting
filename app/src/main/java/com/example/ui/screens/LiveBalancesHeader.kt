package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RoseRed
import com.example.ui.theme.CorporateSky
import com.example.ui.theme.CorporateAmethyst
import com.example.ui.viewmodel.LedgerViewModel
import com.example.util.FinancialUtils
import kotlin.math.abs

@Composable
fun LiveBalancesHeader(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val allAccounts by viewModel.accounts.collectAsStateWithLifecycle()
    val snapshots by viewModel.accountSnapshots.collectAsStateWithLifecycle()
    val isLibyanMode by viewModel.isLibyanMode.collectAsStateWithLifecycle()

    val customersList by viewModel.customers.collectAsStateWithLifecycle()
    val suppliersList by viewModel.suppliers.collectAsStateWithLifecycle()
    val cashBoxesList by viewModel.cashBoxes.collectAsStateWithLifecycle()
    val bankAccountsList by viewModel.allBankAccounts.collectAsStateWithLifecycle()

    // 1. Live Cash Balance (Funds in Cash Boxes and Bank Accounts starting with 1101, 1102, 1104, 1105)
    val liveCashBalance = remember(allAccounts, snapshots) {
        allAccounts.filter {
            !it.isGroup && (
                it.accountCode.startsWith("1101") ||
                it.accountCode.startsWith("1102") ||
                it.accountCode.startsWith("1104") ||
                it.accountCode.startsWith("1105")
            )
        }.sumOf { snapshots.find { s -> s.accountId == it.id }?.balance ?: 0L }
    }

    // 2. Real accounts receivable from 1201 accounts
    val liveAccountsReceivable = remember(allAccounts, snapshots) {
        allAccounts.filter {
            !it.isGroup && it.accountCode.startsWith("1201")
        }.sumOf { snapshots.find { s -> s.accountId == it.id }?.balance ?: 0L }
    }

    // 3. Real accounts payable from 2101 accounts
    val liveAccountsPayable = remember(allAccounts, snapshots) {
        allAccounts.filter {
            !it.isGroup && it.accountCode.startsWith("2101")
        }.sumOf { -(snapshots.find { s -> s.accountId == it.id }?.balance ?: 0L) }
    }

    var selectedDetailType by remember { mutableStateOf<String?>(null) } // "CASH", "AR", "AP"

    val currencySuffix = if (isLibyanMode) {
        if (lang == "ar") "د.ل" else "LYD"
    } else {
        if (lang == "ar") "دولار" else "USD"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .border(1.dp, GoldAccent.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreen)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (lang == "ar") "مؤشرات السيولة والالتزامات الحية" else "LIVE LIQUIDITY & DEBT INDEX",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldAccent,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (lang == "ar") 
                        "سجل مطابق للدفتر المالي العام وتحديثات الحسابات الفورية" 
                    else 
                        "Real-time synchronized indicators with double-entry safety constraints",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }

            // Small badge for currency type
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CorporateSky.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (lang == "ar") "صرف فوري" else "Real-Time",
                    style = MaterialTheme.typography.labelSmall,
                    color = CorporateSky,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Responsive Cards using BoxWithConstraints
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isWide = maxWidth >= 550.dp
            
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LiveBalanceCard(
                        title = if (lang == "ar") "الموجود النقدي السائل" else "Total Cash Reserves",
                        amount = liveCashBalance,
                        icon = Icons.Filled.Payments,
                        color = EmeraldGreen,
                        lang = lang,
                        currencySuffix = currencySuffix,
                        modifier = Modifier.weight(1f).testTag("live_bal_cash"),
                        onClick = { selectedDetailType = "CASH" }
                    )

                    LiveBalanceCard(
                        title = if (lang == "ar") "أرصدة العملاء المدينة" else "Accounts Receivable",
                        amount = liveAccountsReceivable,
                        icon = Icons.Filled.TrendingUp,
                        color = CorporateSky,
                        lang = lang,
                        currencySuffix = currencySuffix,
                        modifier = Modifier.weight(1f).testTag("live_bal_ar"),
                        onClick = { selectedDetailType = "AR" }
                    )

                    LiveBalanceCard(
                        title = if (lang == "ar") "التزامات الموردين الدائنة" else "Accounts Payable",
                        amount = liveAccountsPayable,
                        icon = Icons.Filled.TrendingDown,
                        color = RoseRed,
                        lang = lang,
                        currencySuffix = currencySuffix,
                        modifier = Modifier.weight(1f).testTag("live_bal_ap"),
                        onClick = { selectedDetailType = "AP" }
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiveBalanceCard(
                        title = if (lang == "ar") "الموجود النقدي السائل" else "Total Cash Reserves",
                        amount = liveCashBalance,
                        icon = Icons.Filled.Payments,
                        color = EmeraldGreen,
                        lang = lang,
                        currencySuffix = currencySuffix,
                        modifier = Modifier.fillMaxWidth().testTag("live_bal_cash_stacked"),
                        onClick = { selectedDetailType = "CASH" }
                    )

                    LiveBalanceCard(
                        title = if (lang == "ar") "أرصدة العملاء المدينة" else "Accounts Receivable",
                        amount = liveAccountsReceivable,
                        icon = Icons.Filled.TrendingUp,
                        color = CorporateSky,
                        lang = lang,
                        currencySuffix = currencySuffix,
                        modifier = Modifier.fillMaxWidth().testTag("live_bal_ar_stacked"),
                        onClick = { selectedDetailType = "AR" }
                    )

                    LiveBalanceCard(
                        title = if (lang == "ar") "التزامات الموردين الدائنة" else "Accounts Payable",
                        amount = liveAccountsPayable,
                        icon = Icons.Filled.TrendingDown,
                        color = RoseRed,
                        lang = lang,
                        currencySuffix = currencySuffix,
                        modifier = Modifier.fillMaxWidth().testTag("live_bal_ap_stacked"),
                        onClick = { selectedDetailType = "AP" }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = if (lang == "ar") "💡 اضغط على أي بطاقة لعرض تفاصيل الحسابات وتوزيع الأرصدة والتحليل الاستراتيجي." else "💡 Click any card to show individual account balances and security evaluations.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

    // Detail Popups
    selectedDetailType?.let { type ->
        LiveBalanceDetailDialog(
            type = type,
            lang = lang,
            currencySuffix = currencySuffix,
            cashBoxes = cashBoxesList,
            bankAccounts = bankAccountsList,
            customers = customersList,
            suppliers = suppliersList,
            snapshots = snapshots,
            onDismiss = { selectedDetailType = null }
        )
    }
}

@Composable
fun LiveBalanceCard(
    title: String,
    amount: Long,
    icon: ImageVector,
    color: Color,
    lang: String,
    currencySuffix: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                            color.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = color
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "${FinancialUtils.formatBase(amount)} $currencySuffix",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = color
            )

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.QueryStats,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (lang == "ar") "عرض الاستقصاء والتفصيل" else "Audit Breakdown",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun LiveBalanceDetailDialog(
    type: String,
    lang: String,
    currencySuffix: String,
    cashBoxes: List<com.example.data.CashBox>,
    bankAccounts: List<com.example.data.BankAccount>,
    customers: List<com.example.data.Customer>,
    suppliers: List<com.example.data.Supplier>,
    snapshots: List<com.example.data.AccountBalanceSnapshot>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(2.dp, GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with icon
                val title = when (type) {
                    "CASH" -> if (lang == "ar") "سيولة الصناديق والحسابات الجارية" else "Cash & Bank Balance Detailed Audit"
                    "AR" -> if (lang == "ar") "سجل ديون ومستحقات العملاء" else "Detailed Customer Receivables Log"
                    else -> if (lang == "ar") "التزامات مستحقات الموردين" else "Supplier Accounts Payable Breakdown"
                }

                val primaryColor = when (type) {
                    "CASH" -> EmeraldGreen
                    "AR" -> CorporateSky
                    else -> RoseRed
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(primaryColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (type) {
                                    "CASH" -> Icons.Filled.AccountBalance
                                    "AR" -> Icons.Filled.People
                                    else -> Icons.Filled.Business
                                },
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_audit_btn")) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(Modifier.height(16.dp))

                // Render Content list depending on Type
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    when (type) {
                        "CASH" -> {
                            val itemsList = remember(cashBoxes, bankAccounts, snapshots) {
                                val list = mutableListOf<Pair<String, Long>>()
                                cashBoxes.forEach { cb ->
                                    val b = snapshots.find { it.accountId == cb.accountId }?.balance ?: 0L
                                    list.add(Pair("${cb.name} (${if (lang == "ar") "صندوق" else "Cashbox"})", b))
                                }
                                bankAccounts.forEach { ba ->
                                    val b = snapshots.find { it.accountId == ba.accountId }?.balance ?: 0L
                                    list.add(Pair("${ba.accountName} - ${ba.accountNumber}", b))
                                }
                                list
                            }

                            if (itemsList.isEmpty()) {
                                EmptyDetailState(lang)
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(itemsList) { (name, balance) ->
                                        DetailItemRow(name = name, balance = balance, color = EmeraldGreen, suffix = currencySuffix)
                                    }
                                }
                            }
                        }

                        "AR" -> {
                            val debtorCustomers = remember(customers, snapshots) {
                                customers.map { customer ->
                                    val b = snapshots.find { it.accountId == customer.accountId }?.balance ?: 0L
                                    customer.name to b
                                }.sortedByDescending { it.second }
                            }

                            if (debtorCustomers.isEmpty()) {
                                EmptyDetailState(lang)
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(debtorCustomers) { (name, balance) ->
                                        DetailItemRow(name = name, balance = balance, color = CorporateSky, suffix = currencySuffix)
                                    }
                                }
                            }
                        }

                        "AP" -> {
                            val supplierPayables = remember(suppliers, snapshots) {
                                suppliers.map { supplier ->
                                    val b = snapshots.find { it.accountId == supplier.accountId }?.balance ?: 0L
                                    supplier.name to (-b) // Payables are inverted representation of Credit liability balances
                                }.sortedByDescending { it.second }
                            }

                            if (supplierPayables.isEmpty()) {
                                EmptyDetailState(lang)
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(supplierPayables) { (name, balance) ->
                                        DetailItemRow(name = name, balance = balance, color = RoseRed, suffix = currencySuffix)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(Modifier.height(16.dp))

                // Advisory Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lightbulb,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            val advisoryTitle = when (type) {
                                "CASH" -> if (lang == "ar") "تقييم كفاية السيولة النقدية" else "Liquidity Position evaluation"
                                "AR" -> if (lang == "ar") "دورة تحصيل الديون ومستحقات المبيعات" else "Collection Cycle Audit"
                                else -> if (lang == "ar") "تحليل نسب تغطية الالتزامات" else "Liability Coverage Index"
                            }
                            
                            val advisoryMsg = when (type) {
                                "CASH" -> if (lang == "ar") 
                                    "تعتبر السيولة مستقرة وموزعة بأمان. يوصى بمطابقة أرصدة الصناديق اليومية بانتظام لترسيخ الالتزام والمراقبة المستمرة لمخاطر غسيل الأموال." 
                                else 
                                    "Cash assets are balanced. Keep active tracking buffers on primary vaults to maintain safe margin coverage."
                                "AR" -> if (lang == "ar") 
                                    "يُنصح بمراجعة فترات الائتمان الممنوحة للعملاء وتحصيل المستحقات المتأخرة لتجنب تحولها إلى ديون مشكوك في تحصيلها." 
                                else 
                                    "Audit credit terms periodically. Active receivables should be collected within 30 days to limit aging debt escalation."
                                else -> if (lang == "ar") 
                                    "يجب التأكد من أن التدفقات النقدية الخارجة متطابقة مع آجال استحقاق فواتير المشتريات لتجنب توقف التوريدات التشغيلية." 
                                else 
                                    "Align operational expenditures with supplier terms to eliminate risk of credit-hold or delivery disruptions."
                            }

                            Text(
                                text = advisoryTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = advisoryMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItemRow(
    name: String,
    balance: Long,
    color: Color,
    suffix: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "${FinancialUtils.formatBase(balance)} $suffix",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun EmptyDetailState(lang: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (lang == "ar") "لا توجد حسابات أو أرصدة مدرجة في هذا النطاق" else "No matching accounts or snapshots registered.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            textAlign = TextAlign.Center
        )
    }
}
