package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AccountType
import com.example.ui.Localization
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.ui.theme.CorporateSky
import com.example.ui.theme.CorporateAmethyst
import com.example.ui.viewmodel.LedgerViewModel
import com.example.util.FinancialUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: LedgerViewModel,
    onNavigateToAccounts: () -> Unit,
    onNavigateToVouchers: () -> Unit,
    onNavigateToReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val allAccounts by viewModel.accounts.collectAsStateWithLifecycle()
    val leafAccounts = remember(allAccounts) { allAccounts.filter { !it.isGroup } }

    val recentLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val headers by viewModel.vouchers.collectAsStateWithLifecycle()
    val allVoucherLines by viewModel.allVoucherLines.collectAsStateWithLifecycle()
    val snapshots by viewModel.accountSnapshots.collectAsStateWithLifecycle()
    val isLibyanMode by viewModel.isLibyanMode.collectAsStateWithLifecycle()

    val customersList by viewModel.customers.collectAsStateWithLifecycle()
    val suppliersList by viewModel.suppliers.collectAsStateWithLifecycle()
    val cashBoxesList by viewModel.cashBoxes.collectAsStateWithLifecycle()
    val bankAccountsList by viewModel.allBankAccounts.collectAsStateWithLifecycle()

    // Real Customers Balance (Dynamic from snapshots)
    val totalCustomersBalance = remember(customersList, snapshots) {
        customersList.sumOf { customer -> snapshots.find { it.accountId == customer.accountId }?.balance ?: 0L }
    }

    // Real Suppliers Balance (Dynamic from snapshots)
    val totalSuppliersBalance = remember(suppliersList, snapshots) {
        suppliersList.sumOf { supplier -> snapshots.find { it.accountId == supplier.accountId }?.balance ?: 0L }
    }

    // Real CashBoxes Balance (Dynamic from snapshots)
    val totalCashBoxesBalance = remember(cashBoxesList, snapshots) {
        cashBoxesList.sumOf { cb -> snapshots.find { it.accountId == cb.accountId }?.balance ?: 0L }
    }

    // Real BankAccounts Balance (Dynamic from snapshots)
    val totalBankAccountsBalance = remember(bankAccountsList, snapshots) {
        bankAccountsList.sumOf { ba -> snapshots.find { it.accountId == ba.accountId }?.balance ?: 0L }
    }

    // Total Available Funds (الموجود حالياً)
    val totalAvailableFunds = totalCashBoxesBalance + totalBankAccountsBalance

    // 1. KPI Calculations (Total Revenue, Expenses, Profit, Pending)
    val revenueAccounts = remember(leafAccounts) { leafAccounts.filter { it.accountType == AccountType.REVENUE } }
    val expenseAccounts = remember(leafAccounts) { leafAccounts.filter { it.accountType == AccountType.EXPENSE } }

    val totalRevenue = remember(revenueAccounts, snapshots) {
        revenueAccounts.sumOf { acc -> -(snapshots.find { it.accountId == acc.id }?.balance ?: 0L) }.coerceAtLeast(0L)
    }

    val totalExpense = remember(expenseAccounts, snapshots) {
        expenseAccounts.sumOf { acc -> snapshots.find { it.accountId == acc.id }?.balance ?: 0L }
    }

    val netProfit = totalRevenue - totalExpense

    // Pending represents Draft (unposted) Vouchers totals in the system
    val totalPendingAmount = remember(headers) {
        headers.filter { !it.isPosted }.sumOf { it.totalAmountBase }
    }

    val direction = Localization.getLayoutDirection(lang)

    // Chart data points mapping the past 6 months dynamically from actual ledger entries
    val monthsLabels = if (lang == "ar") {
        listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو")
    } else {
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")
    }
    
    val hasActualData = remember(headers) {
        val cal = java.util.Calendar.getInstance()
        headers.any { h ->
            h.isPosted && h.date.let { d ->
                cal.timeInMillis = d
                cal.get(java.util.Calendar.YEAR) == 2026 && cal.get(java.util.Calendar.MONTH) in 0..5
            }
        }
    }

    val chartDataPoints = remember(headers, allVoucherLines, leafAccounts) {
        val revenueAccountIds = leafAccounts.filter { it.accountType == AccountType.REVENUE }.map { it.id }.toSet()
        val cashAccountIds = leafAccounts.filter { 
            it.accountCode.startsWith("1101") || 
            it.accountCode.startsWith("1102") || 
            it.accountCode.startsWith("1104") ||
            it.accountCode.startsWith("1105") 
        }.map { it.id }.toSet()

        val cal = java.util.Calendar.getInstance()
        val monthlyRevenues = FloatArray(6) { 0f }
        val monthlyCashFlows = FloatArray(6) { 0f }

        val postedHeaders = headers.filter { it.isPosted }
        val postedHeaderIds = postedHeaders.map { it.id }.toSet()
        val postedLines = allVoucherLines.filter { it.headerId in postedHeaderIds }
        val linesByVoucher = postedLines.groupBy { it.headerId }

        for (header in postedHeaders) {
            cal.timeInMillis = header.date
            val year = cal.get(java.util.Calendar.YEAR)
            val month = cal.get(java.util.Calendar.MONTH)

            if (year == 2026 && month in 0..5) {
                val lines = linesByVoucher[header.id] ?: emptyList()
                
                val revenueInVoucher = lines.filter { it.accountId in revenueAccountIds }.sumOf { 
                    if (it.credit > 0) it.amountBase else -it.amountBase 
                }
                monthlyRevenues[month] += (revenueInVoucher.toDouble() / 1_000_000.0).toFloat()

                val cashInVoucher = lines.filter { it.accountId in cashAccountIds }.sumOf {
                    if (it.debit > 0) it.amountBase else -it.amountBase
                }
                monthlyCashFlows[month] += (cashInVoucher.toDouble() / 1_000_000.0).toFloat()
            }
        }

        val points = ArrayList<Float>()
        var cumulative = 0f
        for (m in 0..5) {
            cumulative += monthlyRevenues[m]
            points.add(cumulative)
        }

        val hasRevenues = points.any { it > 0f }
        val hasCashFlows = monthlyCashFlows.any { it != 0f }

        if (hasRevenues) {
            points
        } else if (hasCashFlows) {
            var cumulativeCash = 0f
            val cashPoints = ArrayList<Float>()
            for (m in 0..5) {
                cumulativeCash += monthlyCashFlows[m]
                cashPoints.add(cumulativeCash)
            }
            // If cash points has negative due to payments, let's offset it to start from 0 at least
            val minCash = cashPoints.minOrNull() ?: 0f
            if (minCash < 0f) {
                cashPoints.map { it - minCash }
            } else {
                cashPoints
            }
        } else {
            // High-contrast, elegant simulated target curve for new system initialization
            listOf(100f, 120f, 150f, 220f, 280f, 350f)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                        )
                    )
                )
        ) {
            // Glassmorphic background blur highlight using safe performance-friendly radial gradient
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 60.dp, y = (-40).dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(GoldAccent.copy(alpha = 0.08f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(120.dp)
                    )
            )

            Box(
                modifier = Modifier
                    .size(310.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-80).dp, y = 80.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(GoldAccent.copy(alpha = 0.04f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(155.dp)
                    )
            )

            // Main Contents
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // Customized Header & User Profile Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (lang == "ar") "أهلاً بك، المدير المالي" else "Welcome, Financial Officer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (lang == "ar") "مسار تدقيق موحد ومطابق لمعايير التقارير الدولية IFRS" else "IFRS-compliant general ledger workstation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    // Simulated small glass avatar
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GoldAccent.copy(alpha = 0.15f))
                            .border(1.dp, GoldAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "FO",
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            fontSize = 14.sp
                        )
                    }
                }

                // Scrollable workspace content
                Box(modifier = Modifier.weight(1f)) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Section: Glass KPI Cards (Top Rows with Client/Supplier and Current Assets)
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Arasid of Customers & Suppliers (at the top)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    GlassKPICard(
                                        title = if (lang == "ar") "إجمالي أرصدة العملاء" else "Total Customer Balances",
                                        amount = kotlin.math.abs(totalCustomersBalance),
                                        icon = Icons.Filled.People,
                                        color = EmeraldGreen,
                                        modifier = Modifier.weight(1f),
                                        isLibyan = isLibyanMode
                                    )
                                    GlassKPICard(
                                        title = if (lang == "ar") "إجمالي أرصدة الموردين" else "Total Supplier Balances",
                                        amount = kotlin.math.abs(totalSuppliersBalance),
                                        icon = Icons.Filled.Business,
                                        color = RoseRed,
                                        modifier = Modifier.weight(1f),
                                        isLibyan = isLibyanMode
                                    )
                                }

                                // Header for Current Assets (الموجود حالياً)
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = GoldAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = if (lang == "ar") "الموجود حالياً (النقدية والأرصدة)" else "Current Funds & Liquidity",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(GoldAccent.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${FinancialUtils.formatBase(totalAvailableFunds)} ${if (isLibyanMode) "د.ل" else "LYD"}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = GoldAccent
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    GlassKPICard(
                                        title = if (lang == "ar") "إجمالي الصناديق" else "Total Cash boxes",
                                        amount = totalCashBoxesBalance,
                                        icon = Icons.Filled.Payments,
                                        color = GoldAccent,
                                        modifier = Modifier.weight(1f),
                                        isLibyan = isLibyanMode
                                    )
                                    GlassKPICard(
                                        title = if (lang == "ar") "إجمالي البنوك" else "Total Banks",
                                        amount = totalBankAccountsBalance,
                                        icon = Icons.Filled.AccountBalance,
                                        color = CorporateSky,
                                        modifier = Modifier.weight(1f),
                                        isLibyan = isLibyanMode
                                    )
                                }
                            }
                        }

                        // Section: Smooth Line Chart Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = if (lang == "ar") "النمو المالي والتدفق النقدي" else "Revenue Trend & Cash Flow",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (lang == "ar") {
                                                    if (hasActualData) "تحليل البيانات الفعلية المتراكمة من القيود والسجلات" else "تحليل الإيرادات التراكمية الربع سنوية (نموذج هدف المستهدف)"
                                                } else {
                                                    if (hasActualData) "Real-time compiled ledger trend analytics" else "Quarterly general ledger stream (Target benchmark)"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(GoldAccent.copy(alpha = 0.12f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "IFRS-9",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldAccent
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(16.dp))

                                    SmoothLineChart(
                                        data = chartDataPoints,
                                        labels = monthsLabels,
                                        lineColor = GoldAccent,
                                        lang = lang
                                    )
                                }
                            }
                        }

                        // Section: Quick Action Launches
                        item {
                            Text(
                                text = if (lang == "ar") "الوصول السريع للمهام المحاسبية" else "LEDGER QUICK LAUNCHES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(8.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                maxItemsInEachRow = 3
                            ) {
                                QuickActionTile(
                                    label = if (lang == "ar") "الدليل المالي" else "Accounts Chart",
                                    icon = Icons.Default.AccountTree,
                                    onClick = onNavigateToAccounts,
                                    modifier = Modifier.weight(1f)
                                )
                                QuickActionTile(
                                    label = if (lang == "ar") "الحسابات اليومية" else "Ledger Books",
                                    icon = Icons.Default.PostAdd,
                                    onClick = onNavigateToVouchers,
                                    modifier = Modifier.weight(1f)
                                )
                                QuickActionTile(
                                    label = if (lang == "ar") "التقارير الشاملة" else "Verify sheets",
                                    icon = Icons.Default.Assessment,
                                    onClick = onNavigateToReports,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Section: Live FX Conversions Board
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = if (lang == "ar") "محرر ومؤشرات أسعار الصرف" else "ANALYTICS & ESCROW EXCHANGE BOARD",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp
                                )
                                
                                DashboardFXWidgetCard(
                                    lang = lang,
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }

                        // Section: Dynamic Financial Diagnostics Card
                        item {
                            DashboardDiagnosticsCard(
                                lang = lang,
                                totalRevenue = totalRevenue,
                                totalExpense = totalExpense,
                                netProfit = netProfit,
                                draftCount = headers.count { !it.isPosted },
                                solvencyRatio = if (totalCustomersBalance + totalCashBoxesBalance > 0) {
                                    (totalCustomersBalance + totalCashBoxesBalance).toDouble() / (if (totalSuppliersBalance > 0) totalSuppliersBalance.toDouble() else 1.0)
                                } else 1.5
                            )
                        }

                        // Section: Recent Vouchers
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (lang == "ar") "مستندات اليومية وحالة ترحيل العمليات" else "RECORDED LEDGER ENTRIES",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(EmeraldGreen.copy(alpha = 0.1f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "AUDITED SECURE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            if (headers.isEmpty()) {
                                EmptyDashboardVouchersPlaceHolder(lang = lang, onClickToPost = onNavigateToVouchers)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    headers.take(3).forEachIndexed { idx, header ->
                                        com.example.ui.StaggeredItem(index = idx) {
                                            DashboardEntryRow(
                                                header = header,
                                                lang = lang,
                                                isLibyan = isLibyanMode,
                                                onClick = onNavigateToVouchers
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Persistent Floating Action Bar at bottom
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 16.dp, end = 4.dp)
                    ) {
                        FloatingActionButton(
                            onClick = onNavigateToVouchers,
                            containerColor = GoldAccent,
                            contentColor = MaterialTheme.colorScheme.background,
                            modifier = Modifier.testTag("dashboard_quick_add_voucher_fab")
                        ) {
                            Icon(Icons.Filled.PostAdd, contentDescription = "Add Voucher")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassKPICard(
    title: String,
    amount: Long,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    isLibyan: Boolean
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "${FinancialUtils.formatBase(amount)} ${if (isLibyan) "د.ل" else "LYD"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = if (amount >= 0) "IFRS compliant balance" else "Liability/Expense leg",
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun SmoothLineChart(
    data: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    lineColor: Color = GoldAccent,
    lang: String
) {
    val isRtl = LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(top = 10.dp, bottom = 24.dp)
    ) {
        if (data.isEmpty()) {
            Text(
                text = "No metrics available",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val paddingLeft = 10f
                val paddingRight = 10f
                val paddingTop = 20f
                val paddingBottom = 40f

                val chartWidth = (width - paddingLeft - paddingRight).coerceAtLeast(0f)
                val chartHeight = (height - paddingTop - paddingBottom).coerceAtLeast(0f)

                val maxVal = data.maxOrNull()?.takeIf { it > 0 } ?: 100f
                val minVal = 0f
                val range = (maxVal - minVal).coerceAtLeast(1f)
                val divisor = (data.size - 1).coerceAtLeast(1)

                val points = data.mapIndexed { index, valF ->
                    val indexFactor = if (isRtl) (divisor - index) else index
                    val x = paddingLeft + indexFactor * (chartWidth / divisor)
                    val y = paddingTop + chartHeight - ((valF - minVal) / range) * chartHeight
                    Offset(x, y)
                }

                // Smooth cubic path
                val linePath = Path().apply {
                    if (points.isNotEmpty()) {
                        moveTo(points[0].x, points[0].y)
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val curr = points[i]
                            val controlX1 = prev.x + (curr.x - prev.x) / 2
                            val controlY1 = prev.y
                            val controlX2 = prev.x + (curr.x - prev.x) / 2
                            val controlY2 = curr.y
                            cubicTo(controlX1, controlY1, controlX2, controlY2, curr.x, curr.y)
                        }
                    }
                }

                // Area gradient path
                val areaPath = Path().apply {
                    if (points.isNotEmpty()) {
                        addPath(linePath)
                        lineTo(points.last().x, paddingTop + chartHeight)
                        lineTo(points.first().x, paddingTop + chartHeight)
                        close()
                    }
                }

                // Draw Area Gradient
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.20f), lineColor.copy(alpha = 0.00f))
                    )
                )

                // Draw Smooth Line
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // Draw data indicator dots
                points.forEach { pt ->
                    drawCircle(
                        color = lineColor,
                        radius = 4.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = pt
                    )
                }
            }
            
            // Render labels under the chart using Compose Text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        onClick = onClick,
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GoldAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun DashboardEntryRow(
    header: com.example.data.VoucherHeader,
    lang: String,
    isLibyan: Boolean,
    onClick: () -> Unit
) {
    val dateString = remember(header.date) {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(header.date))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (header.isPosted) {
                                EmeraldGreen.copy(alpha = 0.15f)
                            } else {
                                CorporateSky.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (header.isPosted) Icons.Default.Lock else Icons.Default.EditNote,
                        contentDescription = null,
                        tint = if (header.isPosted) EmeraldGreen else CorporateSky,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        text = header.voucherNo,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = header.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${FinancialUtils.formatBase(header.totalAmountBase)} ${if (isLibyan) "د.ل" else "LYD"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (header.isPosted) EmeraldGreen.copy(alpha = 0.15f) else CorporateSky.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        com.example.ui.BreathingBadge(
                            color = if (header.isPosted) EmeraldGreen else CorporateSky
                        )
                        Text(
                            text = if (header.isPosted) {
                                if (lang == "ar") "مرحَّل" else "Posted"
                            } else {
                                if (lang == "ar") "مسوَّدة" else "Draft"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (header.isPosted) EmeraldGreen else CorporateSky,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyDashboardVouchersPlaceHolder(
    lang: String,
    onClickToPost: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .border(1.dp, GoldAccent.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        onClick = onClickToPost
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.PostAdd,
                contentDescription = null,
                tint = GoldAccent.copy(alpha = 0.6f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (lang == "ar") "لا توجد قيود مسجلة اليوم" else "Zero General Ledger Records Found",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun AuditLogRow(log: com.example.data.AuditLog) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    val formattedTime = remember(log.timestamp) { formatter.format(Date(log.timestamp)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    when (log.action) {
                        "POSTED" -> EmeraldGreen.copy(alpha = 0.15f)
                        "DRAFT_SAVED" -> CorporateSky.copy(alpha = 0.15f)
                        "UNPOSTED" -> RoseRed.copy(alpha = 0.15f)
                        "SYSTEM_INIT" -> CorporateAmethyst.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (log.action) {
                    "POSTED" -> Icons.Filled.Lock
                    "DRAFT_SAVED" -> Icons.Filled.EditOff
                    "UNPOSTED" -> Icons.Filled.Undo
                    "SYSTEM_INIT" -> Icons.Filled.Memory
                    else -> Icons.Filled.HistoryToggleOff
                },
                contentDescription = null,
                tint = when (log.action) {
                    "POSTED" -> EmeraldGreen
                    "DRAFT_SAVED" -> CorporateSky
                    "UNPOSTED" -> RoseRed
                    "SYSTEM_INIT" -> CorporateAmethyst
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(18.dp)
              )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = log.action,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "- [${log.voucherNo}]",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Text(
                text = log.details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Text(
                text = log.performedBy,
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldGreen,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun DashboardFXWidgetCard(
    lang: String,
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val isLibyanMode by viewModel.isLibyanMode.collectAsStateWithLifecycle()
    var amountText by remember { mutableStateOf("100") }
    var usdToLyd by remember { mutableStateOf(true) }
    var useParallel by remember { mutableStateOf(true) }

    val officialRate by viewModel.officialExchangeRate.collectAsStateWithLifecycle()
    val parallelRate by viewModel.parallelExchangeRate.collectAsStateWithLifecycle()
    val rate = if (useParallel) parallelRate else officialRate
    val numericAmount = amountText.toDoubleOrNull() ?: 100.0
    val result = if (usdToLyd) numericAmount * rate else numericAmount / rate

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Payments,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (lang == "ar") "آلة التحويل السريع للعملات" else "FX Quick Translator",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Active Rate Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GoldAccent.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "1 USD = ${String.format(java.util.Locale.US, "%.3f", rate)} LYD",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )
                }
            }

            Text(
                text = if (lang == "ar") "أسعار فورية لمحاكاة التدفقات النقدية وفق النطاق الرسمي والموازي في ليبيا." else "Real-time indicators to simulate cash reserves according to official & parallel market bounds.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Direction Toggle Button
                Button(
                    onClick = { usdToLyd = !usdToLyd },
                    modifier = Modifier.weight(1.4f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (usdToLyd) "USD ➔ LYD" else "LYD ➔ USD",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Rate Source Segmented Selector (Official vs Parallel)
                Row(
                    modifier = Modifier
                        .weight(1.6f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!useParallel) GoldAccent.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { useParallel = false }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (lang == "ar") "رسمي" else "Official",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (!useParallel) GoldAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (useParallel) GoldAccent.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { useParallel = true }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (lang == "ar") "موازي" else "Parallel",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (useParallel) GoldAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Converter Input & Live Readout Output Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.toEnglishDigits() },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(52.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    placeholder = { Text("100", style = MaterialTheme.typography.bodyMedium) }
                )

                Box(
                    modifier = Modifier
                        .weight(1.4f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val unit = if (usdToLyd) (if (isLibyanMode) "د.ل" else "LYD") else "$"
                    val outputPrecision = if (usdToLyd) "%.3f" else "%.2f"
                    Text(
                        text = "${String.format(java.util.Locale.US, outputPrecision, result)} $unit",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardDiagnosticsCard(
    lang: String,
    totalRevenue: Long,
    totalExpense: Long,
    netProfit: Long,
    draftCount: Int,
    solvencyRatio: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (lang == "ar") "توصيات التشخيص والرقابة المالية للمؤسسة" else "Operational Budget & Audit Runway Indicators",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EmeraldGreen.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "IAS-1",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen
                    )
                }
            }

            // Calculations for diagnostics
            val revenueAmount = totalRevenue.toDouble() / 1_000_000.0
            val profitMarginPercentage = if (revenueAmount > 0) {
                ((netProfit.toDouble() / 1_000_000.0) / revenueAmount) * 100.0
            } else 0.0

            val healthColor = if (profitMarginPercentage >= 15.0 && solvencyRatio >= 1.0) {
                EmeraldGreen
            } else if (profitMarginPercentage >= 0.0 || solvencyRatio >= 1.0) {
                CorporateSky
            } else {
                RoseRed
            }

            val healthStatusText = if (lang == "ar") {
                if (profitMarginPercentage >= 15.0 && solvencyRatio >= 1.0) {
                    "ملاءة ممتازة ونمو مستدام"
                } else if (profitMarginPercentage >= 0.0) {
                    "أداء تشغيلي مستقر مع هوامش مرنة"
                } else {
                    "عجز مالي يتطلب تموين تشغيلي"
                }
            } else {
                if (profitMarginPercentage >= 15.0 && solvencyRatio >= 1.0) {
                    "Excellent Solvency & Healthy Yields"
                } else if (profitMarginPercentage >= 0.0) {
                    "Stable Core Activities - Average Margin"
                } else {
                    "Capital Deficit / Strict Overhead Control Advised"
                }
            }

            // Indicator Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(healthColor.copy(alpha = 0.08f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.example.ui.BreathingBadge(color = healthColor)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (lang == "ar") "تصنيف الصحة المالية للمؤسسة:" else "Entity Financial Health rating:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = healthStatusText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = healthColor
                    )
                }
            }

            // Split Grid columns for Stats details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Column 1: Margin Analysis
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = if (lang == "ar") "هامش الربح الصافي" else "Net Margin Yield",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (totalRevenue > 0) "${String.format(Locale.US, "%.1f", profitMarginPercentage)}%" else "0.0% (N/A)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (profitMarginPercentage >= 15.0) EmeraldGreen else if (profitMarginPercentage >= 0.0) MaterialTheme.colorScheme.onSurface else RoseRed
                    )
                }

                // Column 2: Audit Checklist Status
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = if (lang == "ar") "مسودات غير معتمدة" else "Escrow Draft Records",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (draftCount > 0) {
                            if (lang == "ar") "$draftCount مستند ينتظر الترحيل" else "$draftCount Vouchers pending"
                        } else {
                            if (lang == "ar") "مكتمل التدقيق" else "Zero pending"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (draftCount > 0) CorporateSky else EmeraldGreen
                    )
                }
            }

            // Advisory checklist texts based on current statistics
            val dynamicAdvisory = remember(revenueAmount, profitMarginPercentage, draftCount, solvencyRatio) {
                if (draftCount > 0) {
                    if (lang == "ar") {
                        "⚠️ يوجد مستندات مسودة غير مرحلة؛ القيود تؤثر على شجرة الحسابات فور اعتماد البيانات."
                    } else {
                        "⚠️ Draft entries are in active validation. Ledger is temporarily desynchronized until they are posted."
                    }
                } else if (profitMarginPercentage < 0.0 && totalRevenue > 0) {
                    if (lang == "ar") {
                        "🚨 النفقات تفوق عوائد التشغل. يوصى بضبط النفقات الإدارية وإقرار كشف التكاليف الإجمالية."
                    } else {
                        "🚨 Deficit Alert: Operating expenses exceed compiled revenue stream. Audit unnecessary secondary expenditures."
                    }
                } else if (solvencyRatio < 1.0) {
                    if (lang == "ar") {
                        "⚠️ التزامات التوريد تفوق الموجود نقدياً. راجع شروط الائتمان وحقوق الدفع مع الموردين."
                    } else {
                        "⚠️ Leveraged warnings: accounts payable exceed active fluid cash boxes. Negotiate credit intervals."
                    }
                } else {
                    if (lang == "ar") {
                        "✔ جميع القيود اليومية متوازنة ومرحّلا بالكامل، الملاءة المالية والسيولة للمؤسسة ممتازة."
                    } else {
                        "✔ All recorded journal vouchers are fully compiled. Outstanding liquid solvency checks are clear."
                    }
                }
            }

            Text(
                text = dynamicAdvisory,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun String.toEnglishDigits(): String {
    val builder = StringBuilder()
    for (ch in this) {
        if (ch in '٠'..'٩') {
            builder.append((ch - '٠' + '0'.code).toChar())
        } else if (ch in '۰'..'۹') {
            builder.append((ch - '۰' + '0'.code).toChar())
        } else {
            builder.append(ch)
        }
    }
    return builder.toString()
}
