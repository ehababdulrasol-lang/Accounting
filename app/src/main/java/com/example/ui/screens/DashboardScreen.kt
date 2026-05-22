package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AccountType
import com.example.ui.Localization
import com.example.ui.theme.CorporateAmethyst
import com.example.ui.theme.CorporateSky
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.ui.viewmodel.LedgerViewModel
import com.example.util.FinancialUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    val cachedAssetsSum = remember(leafAccounts, snapshots) {
        leafAccounts.filter { it.accountType == AccountType.ASSET }
            .sumOf { acc -> snapshots.find { it.accountId == acc.id }?.balance ?: 0L }
    }

    val cachedLiabilitiesSum = remember(leafAccounts, snapshots) {
        leafAccounts.filter { it.accountType == AccountType.LIABILITY }
            .sumOf { acc -> -(snapshots.find { it.accountId == acc.id }?.balance ?: 0L) }
    }

    val cachedEquitySum = remember(leafAccounts, snapshots) {
        leafAccounts.filter { it.accountType == AccountType.EQUITY }
            .sumOf { acc -> -(snapshots.find { it.accountId == acc.id }?.balance ?: 0L) }
    }

    val liquidNetVal = cachedAssetsSum - cachedLiabilitiesSum

    val direction = Localization.getLayoutDirection(lang)

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Title
            item {
                Column {
                    Text(
                        text = Localization.translate(Localization.Key.APP_NAME, lang),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (lang == "ar") "كونسول مالي موحد للتدقيق والامتثال المحاسبي وفق المعايير الدولية." else "Consolidated financial audit workspace under IFRS compliance control rules.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }

            // Financial summary cards row
            item {
                val isLibyanMode by viewModel.isLibyanMode.collectAsState()
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DashboardStatCard(
                            title = Localization.translate(Localization.Key.TOTAL_ASSETS, lang).uppercase(),
                            amount = cachedAssetsSum,
                            icon = Icons.Filled.AccountBalance,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1.1f),
                            lang = lang,
                            isLibyan = isLibyanMode
                        )

                        DashboardStatCard(
                            title = Localization.translate(Localization.Key.TOTAL_LIABILITIES, lang).uppercase(),
                            amount = cachedLiabilitiesSum,
                            icon = Icons.Filled.TrendingDown,
                            color = RoseRed,
                            modifier = Modifier.weight(1f),
                            lang = lang,
                            isLibyan = isLibyanMode
                        )
                    }

                    // System Integrity & Financial Structure Board
                    val sumLE = cachedLiabilitiesSum + cachedEquitySum
                    val isBalanced = Math.abs(cachedAssetsSum - sumLE) <= 10L // micro unit tolerance
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isBalanced) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = if (isBalanced) EmeraldGreen else RoseRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (lang == "ar") "التوازن الهيكلي والامتثال المالي" else "Structural Balance & Compliance",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background((if (isBalanced) EmeraldGreen else RoseRed).copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isBalanced) "100% BALANCED" else "DISCREPANCY DETECTED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBalanced) EmeraldGreen else RoseRed
                                    )
                                }
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            
                            Text(
                                text = if (isBalanced) {
                                    if (lang == "ar") "المعادلة المحاسبية في توازن مثالي: الأصول تساوي تماماً الالتزامات وحقوق الملكية."
                                    else "The system of entries is in perfect double-entry equilibrium according to international benchmarks (IFRS)."
                                } else {
                                    if (lang == "ar") "هناك فارق قيد معلق! يرجى مراجعة المعاملات غير المرحلة وضمان موازنة القيود."
                                    else "System mismatch! The ledger shows an inequality. Review draft vouchers and transaction legs."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            
                            Spacer(Modifier.height(12.dp))
                            
                            // Visual horizontal stacked bar segment
                            val totalFunding = cachedLiabilitiesSum + cachedEquitySum
                            val totalRepresented = Math.max(cachedAssetsSum, totalFunding)
                            
                            if (totalRepresented > 0L) {
                                val assetPercentage = (cachedAssetsSum.toDouble() / totalRepresented.toDouble()).coerceIn(0.0, 1.0)
                                val liabilitiesPercentage = (cachedLiabilitiesSum.toDouble() / totalRepresented.toDouble()).coerceIn(0.0, 1.0)
                                val equityPercentage = (cachedEquitySum.toDouble() / totalRepresented.toDouble()).coerceIn(0.0, 1.0)
                                
                                Text(
                                    text = if (lang == "ar") "توزيع هيكلية التمويل (حقوق الملكية دائن باللون الأخضر مقابل الالتزامات دائن باللون الأحمر)" else "Funding Structure (Equity in Green vs Liabilities in Red)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                
                                // Beautiful custom stacked progress bar with matching colors
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                ) {
                                    // Equity portion in EmeraldGreen
                                    val eqW = equityPercentage.toFloat()
                                    if (eqW > 0.0001f && eqW.isFinite()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(eqW)
                                                .background(EmeraldGreen)
                                        )
                                    }
                                    // Liabilities portion in RoseRed
                                    val liabW = liabilitiesPercentage.toFloat()
                                    if (liabW > 0.0001f && liabW.isFinite()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(liabW)
                                                .background(RoseRed)
                                        )
                                    }
                                    // Mismatch indicator if out of balance
                                    val mismatchPercentage = Math.abs(assetPercentage - (liabilitiesPercentage + equityPercentage))
                                    val mismatchW = mismatchPercentage.toFloat()
                                    if (mismatchW > 0.0001f && mismatchW.isFinite() && !isBalanced) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(mismatchW)
                                                .background(CorporateAmethyst)
                                        )
                                    }
                                }
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(EmeraldGreen))
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "${if (lang == "ar") "حقوق الملكية" else "Equity"}: ${String.format("%.1f", equityPercentage * 100)}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(RoseRed))
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "${if (lang == "ar") "الالتزامات" else "Liabilities"}: ${String.format("%.1f", liabilitiesPercentage * 100)}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Launch tools grid
            item {
                Text(
                    text = Localization.translate(Localization.Key.QUICK_ACTIONS, lang).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickLaunchButton(
                        text = if (lang == "ar") "دليل الحسابات" else "Manage Chart",
                        desc = if (lang == "ar") "هيكلية الشجرة" else "Hierarchy",
                        icon = Icons.Filled.AccountTree,
                        onClick = onNavigateToAccounts,
                        modifier = Modifier.weight(1f)
                    )
                    QuickLaunchButton(
                        text = if (lang == "ar") "اليومية العامة" else "Post Voucher",
                        desc = if (lang == "ar") "تسجيل القيود" else "Double-Entry",
                        icon = Icons.Filled.PostAdd,
                        onClick = onNavigateToVouchers,
                        modifier = Modifier.weight(1f)
                    )
                    QuickLaunchButton(
                        text = if (lang == "ar") "ميزان المراجعة" else "Financials",
                        desc = if (lang == "ar") "التقارير الحسابية" else "Trial Sheets",
                        icon = Icons.Filled.DonutLarge,
                        onClick = onNavigateToReports,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Rolling Audits & Compliance logs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (lang == "ar") "مسار تدقيق العمليات المحاسبية" else "ROLLING AUDIT HISTORY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(EmeraldGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ACID SECURE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldGreen
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))

                if (recentLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (lang == "ar") "لا توجد نشاطات تدقيق مسجلة حاليا." else "No audit events recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        recentLogs.take(5).forEach { log ->
                            AuditLogRow(log = log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardStatCard(
    title: String,
    amount: Long,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    lang: String,
    isLibyan: Boolean
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "${FinancialUtils.formatBase(amount)} ${if (isLibyan) "د.ل" else "LYD"}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (amount >= 0L) EmeraldGreen else RoseRed
            )
            Text(
                text = if (lang == "ar") "القيمة المجمعة بالدينار" else "Consolidated Base",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun QuickLaunchButton(
    text: String,
    desc: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = borderStroke()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun borderStroke() = androidx.compose.foundation.BorderStroke(
    1.dp,
    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
)

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
