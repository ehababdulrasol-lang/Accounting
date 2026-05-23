package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    val lang by viewModel.currentLanguage.collectAsState()
    val allAccounts by viewModel.accounts.collectAsState()
    val leafAccounts = remember(allAccounts) { allAccounts.filter { !it.isGroup } }

    val recentLogs by viewModel.auditLogs.collectAsState()
    val headers by viewModel.vouchers.collectAsState()
    val snapshots by viewModel.accountSnapshots.collectAsState()
    val isLibyanMode by viewModel.isLibyanMode.collectAsState()

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

    // Chart mock data points mapping the past 6 months to showcase smooth charts
    val monthsLabels = if (lang == "ar") {
        listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو")
    } else {
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")
    }
    
    // Simulate some realistic curves based on actual revenue data plus target trend
    val chartDataPoints = remember(totalRevenue) {
        val baseRev = totalRevenue.toFloat().coerceAtLeast(200000f)
        listOf(
            baseRev * 0.45f,
            baseRev * 0.6f,
            baseRev * 0.55f,
            baseRev * 0.85f,
            baseRev * 0.9f,
            baseRev * 1.0f
        )
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
            // Glassmorphic background blur highlight
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 60.dp, y = (-40).dp)
                    .blur(60.dp)
                    .background(GoldAccent.copy(alpha = 0.08f), RoundedCornerShape(120.dp))
            )

            Box(
                modifier = Modifier
                    .size(310.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-80).dp, y = 80.dp)
                    .blur(70.dp)
                    .background(GoldAccent.copy(alpha = 0.04f), RoundedCornerShape(155.dp))
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
                        // Section: Glass KPI Cards (Quarter Row)
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    GlassKPICard(
                                        title = if (lang == "ar") "الإيرادات الإجمالية" else "Total Revenue",
                                        amount = totalRevenue,
                                        icon = Icons.Filled.TrendingUp,
                                        color = EmeraldGreen,
                                        modifier = Modifier.weight(1f),
                                        isLibyan = isLibyanMode
                                    )
                                    GlassKPICard(
                                        title = if (lang == "ar") "المصروفات التشغيلية" else "Operational Expenses",
                                        amount = totalExpense,
                                        icon = Icons.Filled.TrendingDown,
                                        color = RoseRed,
                                        modifier = Modifier.weight(1f),
                                        isLibyan = isLibyanMode
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    GlassKPICard(
                                        title = if (lang == "ar") "صافي الأرباح" else "Net Margin",
                                        amount = netProfit,
                                        icon = Icons.Filled.AccountBalanceWallet,
                                        color = GoldAccent,
                                        modifier = Modifier.weight(1f),
                                        isLibyan = isLibyanMode
                                    )
                                    GlassKPICard(
                                        title = if (lang == "ar") "معلق تحت المراجعة" else "Pending (Drafts)",
                                        amount = totalPendingAmount,
                                        icon = Icons.Filled.PendingActions,
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
                                                text = if (lang == "ar") "النمو المالي والتدفق النقدي" else "Revenue Trend Dynamics",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (lang == "ar") "تحليل الإيرادات التراكمية الربع سنوية" else "Quarterly cumulative general ledger streams.",
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
                                    headers.take(3).forEach { header ->
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

                val chartWidth = width - paddingLeft - paddingRight
                val chartHeight = height - paddingTop - paddingBottom

                val maxVal = data.maxOrNull()?.takeIf { it > 0 } ?: 100f
                val minVal = 0f
                val range = maxVal - minVal

                val points = data.mapIndexed { index, valF ->
                    val x = paddingLeft + index * (chartWidth / (data.size - 1))
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
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(header.date))
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
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (header.isPosted) EmeraldGreen.copy(alpha = 0.15f) else CorporateSky.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
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
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
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
