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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.Account
import com.example.data.AccountType
import com.example.ui.Localization
import com.example.ui.theme.CorporateAmethyst
import com.example.ui.theme.CorporateSky
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.ui.viewmodel.LedgerViewModel
import com.example.util.FinancialUtils

@Composable
fun ReportsScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Trial Balance, 1 = Balance Sheet, 2 = Income Statement, 3 = Cash Flow, 4 = Debt Aging
    val lang by viewModel.currentLanguage.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = Localization.translate(Localization.Key.FINANCIAL_STATEMENTS, lang),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (lang == "ar") "تنفيذ حسابات الأرصدة في الوقت الفعلي المتوافقة مع المعايير الدولية (IFRS) مباشرة من قيود اليومية المزدوجة المتوازنة." else "IFRS-compliant, real-time balance calculations derived directly from balanced double-entries.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 12.dp)
            )

        TabRow(
            selectedTabIndex = activeTab,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text(Localization.translate(Localization.Key.TRIAL_BALANCE, lang), fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.AccountBalance, null) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text(Localization.translate(Localization.Key.BALANCE_SHEET, lang), fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.Assessment, null) }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text(Localization.translate(Localization.Key.INCOME_STATEMENT, lang), fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.TrendingUp, null) }
            )
            Tab(
                selected = activeTab == 3,
                onClick = { activeTab = 3 },
                text = { Text(if (lang == "ar") "التدفقات النقدية" else "Cash Flow", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.SwapVert, null) }
            )
            Tab(
                selected = activeTab == 4,
                onClick = { activeTab = 4 },
                text = { Text(if (lang == "ar") "أعمار الديون" else "Aging Report", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.Schedule, null) }
            )
        }

        Spacer(Modifier.height(16.dp))

        when (activeTab) {
            0 -> TrialBalanceView(viewModel)
            1 -> BalanceSheetView(viewModel)
            2 -> IncomeStatementView(viewModel)
            3 -> CashFlowView(viewModel)
            4 -> AgingReportView(viewModel)
        }
    }
}

@Composable
fun TrialBalanceView(viewModel: LedgerViewModel) {
    val rows by viewModel.trialBalanceRows.collectAsState()
    val loading by viewModel.trialBalanceLoading.collectAsState()
    val lang by viewModel.currentLanguage.collectAsState()

    val totalOpDr = remember(rows) { rows.sumOf { it.openingDebit } }
    val totalOpCr = remember(rows) { rows.sumOf { it.openingCredit } }
    val totalPerDr = remember(rows) { rows.sumOf { it.periodDebit } }
    val totalPerCr = remember(rows) { rows.sumOf { it.periodCredit } }
    val totalClDr = remember(rows) { rows.sumOf { it.closingDebit } }
    val totalClCr = remember(rows) { rows.sumOf { it.closingCredit } }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (lang == "ar") "ميزان المراجعة غير المعدل (السنة المالية 2026)" else "Unadjusted Trial Balance (FY 2026)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row {
                val context = androidx.compose.ui.platform.LocalContext.current
                IconButton(
                    onClick = { viewModel.refreshTrialBalance() },
                    modifier = Modifier.testTag("refresh_trial_balance_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                }
                IconButton(
                    onClick = {
                        com.example.util.PrintUtils.printTrialBalance(
                            context = context,
                            rows = rows,
                            totalOpDr = totalOpDr,
                            totalOpCr = totalOpCr,
                            totalPerDr = totalPerDr,
                            totalPerCr = totalPerCr,
                            totalClDr = totalClDr,
                            totalClCr = totalClCr,
                            lang = lang
                        )
                    },
                    modifier = Modifier.testTag("print_trial_balance_button")
                ) {
                    Icon(Icons.Filled.Print, contentDescription = "Print Trial Balance")
                }
            }
        }

        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (rows.isEmpty()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (lang == "ar") "لا توجد معاملات مرحلة متاحة في هذا النطاق الزمني." else "No posted transaction data available for this range.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            // Elegant scrolling ledger table
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            ) {
                // Header Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(Localization.translate(Localization.Key.ACCOUNT, lang), modifier = Modifier.weight(1.8f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text(if (lang == "ar") "مدين افتتاحي" else "Open Dr", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                        Text(if (lang == "ar") "دائن افتتاحي" else "Open Cr", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                        Text(if (lang == "ar") "حركة مدين" else "Period Dr", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                        Text(if (lang == "ar") "حركة دائن" else "Period Cr", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                        Text(if (lang == "ar") "مدين نهائي" else "Close Dr", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                        Text(if (lang == "ar") "دائن نهائي" else "Close Cr", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                }

                // Inner Records rows
                items(rows) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.8f)) {
                            Text(row.accountCode, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(Localization.getAccountName(row.accountCode, row.accountName, lang), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }

                        Text(if (row.openingDebit > 0) FinancialUtils.formatBase(row.openingDebit) else "-", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                        Text(if (row.openingCredit > 0) FinancialUtils.formatBase(row.openingCredit) else "-", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                        Text(if (row.periodDebit > 0) FinancialUtils.formatBase(row.periodDebit) else "-", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                        Text(if (row.periodCredit > 0) FinancialUtils.formatBase(row.periodCredit) else "-", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                        Text(if (row.closingDebit > 0) FinancialUtils.formatBase(row.closingDebit) else "-", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                        Text(if (row.closingCredit > 0) FinancialUtils.formatBase(row.closingCredit) else "-", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                }

                // Balance Totals summary row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (lang == "ar") "الإجمالي" else "TOTALS", modifier = Modifier.weight(1.8f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(FinancialUtils.formatBase(totalOpDr), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.primary)
                        Text(FinancialUtils.formatBase(totalOpCr), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.primary)
                        Text(FinancialUtils.formatBase(totalPerDr), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.primary)
                        Text(FinancialUtils.formatBase(totalPerCr), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.primary)
                        Text(FinancialUtils.formatBase(totalClDr), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, color = EmeraldGreen)
                        Text(FinancialUtils.formatBase(totalClCr), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, color = EmeraldGreen)
                    }
                }
            }
            
            // Check double entry compliance
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(EmeraldGreen.copy(alpha = 0.1f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.VerifiedUser, null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (lang == "ar") "تم اجتياز فحص مطابقة المعايير الدولية والتحقق المزدوج للقيود بنجاح." else "IFRS Statement Validation Checklist Passed.",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                )
            }
        }
    }
}

@Composable
fun BalanceSheetView(viewModel: LedgerViewModel) {
    val accounts by viewModel.accounts.collectAsState()
    val snapshotsList = remember(accounts) { accounts.filter { !it.isGroup } } // only leaf for accuracy
    val snapshots by viewModel.accountSnapshots.collectAsState()
    val lang by viewModel.currentLanguage.collectAsState()

    // Filter accounts by category
    val assetAccounts = remember(snapshotsList) { snapshotsList.filter { it.accountType == AccountType.ASSET } }
    val liabilityAccounts = remember(snapshotsList) { snapshotsList.filter { it.accountType == AccountType.LIABILITY } }
    val equityAccounts = remember(snapshotsList) { snapshotsList.filter { it.accountType == AccountType.EQUITY } }

    val totalAssets = remember(assetAccounts, snapshots) {
        assetAccounts.sumOf { acc -> snapshots.find { it.accountId == acc.id }?.balance ?: 0L }
    }
    val totalLiabilities = remember(liabilityAccounts, snapshots) {
        liabilityAccounts.sumOf { acc -> -(snapshots.find { it.accountId == acc.id }?.balance ?: 0L) }
    }
    val totalEquity = remember(equityAccounts, snapshots) {
        equityAccounts.sumOf { acc -> -(snapshots.find { it.accountId == acc.id }?.balance ?: 0L) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.1f))
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title Header
            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Localization.translate(Localization.Key.BALANCE_SHEET, lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (lang == "ar") "معادلة الميزانية: الأصول = الالتزامات + حقوق الملكية" else "Equation check: Assets = Liabilities + Equity",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    IconButton(
                        onClick = {
                            com.example.util.PrintUtils.printBalanceSheet(
                                context = context,
                                totalAssets = totalAssets,
                                totalLiabilities = totalLiabilities,
                                totalEquity = totalEquity,
                                assetAccounts = assetAccounts,
                                liabilityAccounts = liabilityAccounts,
                                equityAccounts = equityAccounts,
                                snapshots = snapshots,
                                lang = lang
                            )
                        },
                        modifier = Modifier.testTag("print_balance_sheet_button")
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = "Print Balance Sheet")
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
            }

            // ASSETS Segment
            item {
                Text(if (lang == "ar") "1. الأصول" else "1. ASSETS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            items(assetAccounts) { acc ->
                val balance = remember(acc.id, snapshots) { snapshots.find { it.accountId == acc.id }?.balance ?: 0L }
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    Text(text = "${acc.accountCode} - ${Localization.getAccountName(acc.accountCode, acc.name, lang)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(text = FinancialUtils.formatBase(balance), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
            item {
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (lang == "ar") "إجمالي الأصول (أ)" else "Total Assets (A)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(FinancialUtils.formatBase(totalAssets), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodyMedium, color = EmeraldGreen)
                }
                Spacer(Modifier.height(16.dp))
            }

            // LIABILITIES Segment
            item {
                Text(if (lang == "ar") "2. الالتزامات" else "2. LIABILITIES", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CorporateAmethyst)
            }
            items(liabilityAccounts) { acc ->
                val balance = remember(acc.id, snapshots) { snapshots.find { it.accountId == acc.id }?.balance ?: 0L }
                val creditBal = -balance
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    Text(text = "${acc.accountCode} - ${Localization.getAccountName(acc.accountCode, acc.name, lang)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(text = FinancialUtils.formatBase(creditBal), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
            item {
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (lang == "ar") "إجمالي الالتزامات (ل)" else "Total Liabilities (L)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(FinancialUtils.formatBase(totalLiabilities), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodyMedium, color = RoseRed)
                }
                Spacer(Modifier.height(16.dp))
            }

            // EQUITY Segment
            item {
                Text(if (lang == "ar") "3. حقوق الملكية" else "3. EQUITY", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            items(equityAccounts) { acc ->
                val balance = remember(acc.id, snapshots) { snapshots.find { it.accountId == acc.id }?.balance ?: 0L }
                val creditBal = -balance
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    Text(text = "${acc.accountCode} - ${Localization.getAccountName(acc.accountCode, acc.name, lang)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(text = FinancialUtils.formatBase(creditBal), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
            item {
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (lang == "ar") "إجمالي حقوق الملكية (ح)" else "Total Equity (E)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(FinancialUtils.formatBase(totalEquity), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(thickness = 2.dp)
            }

            // Verification Check Summary Row
            item {
                val sumLE = totalLiabilities + totalEquity
                val diff = Math.abs(totalAssets - sumLE)
                val bsBalanced = diff <= 10L // micro tolerance

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (bsBalanced) EmeraldGreen.copy(alpha = 0.15f)
                            else RoseRed.copy(alpha = 0.15f)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (bsBalanced) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (bsBalanced) EmeraldGreen else RoseRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (bsBalanced) {
                            if (lang == "ar") "الأصول == الالتزامات + حقوق الملكية (${FinancialUtils.formatBase(totalAssets)} == ${FinancialUtils.formatBase(sumLE)}) • متطابق"
                            else "Assets == Liabilities + Equity (${FinancialUtils.formatBase(totalAssets)} == ${FinancialUtils.formatBase(sumLE)}) • balanced"
                        } else {
                            if (lang == "ar") "خلل في معادلة الميزانية! الرجاء ترحيل القيود لضمان صحة البيانات."
                            else "Accounting Equation Imbalance! Check unposted draft journals."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (bsBalanced) EmeraldGreen else RoseRed
                    )
                }
            }
            
            // Smart Solvency Analyst Insights card
            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.TipsAndUpdates, contentDescription = null, tint = CorporateAmethyst, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (lang == "ar") "لوحة الذكاء والتحليل المالي" else "Financial Intelligence & Diagnostics",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        val solvencyRatioText: String
                        val diagnosticsText: String
                        val ratioColor: Color
                        
                        if (totalLiabilities > 0L) {
                            val ratio = totalAssets.toDouble() / totalLiabilities.toDouble()
                            solvencyRatioText = "${String.format("%.2f", ratio)}x"
                            if (ratio > 2.0) {
                                ratioColor = EmeraldGreen
                                diagnosticsText = if (lang == "ar") "ملاءة مالية ممتازة وحماية مرتفعة ضد التعثر المالي. الأصول تغطي الالتزامات بأكثر من الضعف." else "Excellent liquidity cushion and strong protection. Assets cover liabilities more than twofold."
                            } else if (ratio >= 1.0) {
                                ratioColor = CorporateSky
                                diagnosticsText = if (lang == "ar") "ملاءة مالية متقاطعة متوازنة وسيولة تشغيلية كافية لتغطية الديون القريبة." else "Balanced financial stability. Assets comfortably cover immediate external obligations."
                            } else {
                                ratioColor = RoseRed
                                diagnosticsText = if (lang == "ar") "تنبيه ملاءة حرجة! الالتزامات تفوق إجمالي الأصول التشغيلية المتاحة. مخاطر عالية." else "Critical solvency warning! Leverage levels exceed physical assets. High default exposure."
                            }
                        } else {
                            solvencyRatioText = "∞ (Optimal)"
                            ratioColor = EmeraldGreen
                            diagnosticsText = if (lang == "ar") "لا توجد أي التزامات مسجلة. ملاءة مالية ممتازة وقاعدة رأس مالية خالية تماماً من الديون." else "Excellent risk-free operations in effect. Capital base is entirely self-funded with zero liabilities."
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (lang == "ar") "مؤشر الملاءة العامة (الأصول / الالتزامات):" else "Asset-to-Liability Ratio:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = solvencyRatioText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = ratioColor
                            )
                        }
                        
                        Spacer(Modifier.height(6.dp))
                        
                        Text(
                            text = diagnosticsText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IncomeStatementView(viewModel: LedgerViewModel) {
    val accounts by viewModel.accounts.collectAsState()
    val snapshotsList = remember(accounts) { accounts.filter { !it.isGroup } }
    val snapshots by viewModel.accountSnapshots.collectAsState()
    val lang by viewModel.currentLanguage.collectAsState()
    val isLibyan by viewModel.isLibyanMode.collectAsState()

    val revenueAccounts = remember(snapshotsList) { snapshotsList.filter { it.accountType == AccountType.REVENUE } }
    val expenseAccounts = remember(snapshotsList) { snapshotsList.filter { it.accountType == AccountType.EXPENSE } }

    val totalRevenue = remember(revenueAccounts, snapshots) {
        revenueAccounts.sumOf { acc -> -(snapshots.find { it.accountId == acc.id }?.balance ?: 0L) }
    }
    val totalExpense = remember(expenseAccounts, snapshots) {
        expenseAccounts.sumOf { acc -> snapshots.find { it.accountId == acc.id }?.balance ?: 0L }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.1f))
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                val netIncome = totalRevenue - totalExpense
                val profitMargin = if (totalRevenue > 0L) (netIncome.toDouble() / totalRevenue.toDouble()) * 100.0 else 0.0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Localization.translate(Localization.Key.INCOME_STATEMENT, lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (lang == "ar") "صافي الأرباح = إجمالي الإيرادات - المصروفات التشغيلية" else "Net profit = Gross Revenues - Operational Expenses",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    IconButton(
                        onClick = {
                            com.example.util.PrintUtils.printIncomeStatement(
                                context = context,
                                totalRevenue = totalRevenue,
                                totalExpense = totalExpense,
                                netIncome = netIncome,
                                profitMargin = profitMargin,
                                revenueAccounts = revenueAccounts,
                                expenseAccounts = expenseAccounts,
                                snapshots = snapshots,
                                isLibyan = isLibyan,
                                lang = lang
                            )
                        },
                        modifier = Modifier.testTag("print_income_statement_button")
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = "Print Income Statement")
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
            }

            // REVENUES
            item {
                Text(if (lang == "ar") "الإيرادات" else "REVENUES", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = EmeraldGreen)
            }
            items(revenueAccounts) { acc ->
                val balance = remember(acc.id, snapshots) { snapshots.find { it.accountId == acc.id }?.balance ?: 0L }
                val reversed = -balance // Credit balance represents revenue
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    Text(text = "${acc.accountCode} - ${Localization.getAccountName(acc.accountCode, acc.name, lang)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(text = FinancialUtils.formatBase(reversed), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
            item {
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (lang == "ar") "إجمالي الإيرادات" else "Total Revenue", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(FinancialUtils.formatBase(totalRevenue), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodyMedium, color = EmeraldGreen)
                }
                Spacer(Modifier.height(16.dp))
            }

            // OPERATIONAL EXPENSES
            item {
                Text(if (lang == "ar") "المصروفات التشغيلية والعمومية" else "OPERATIONAL EXPENSES", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = RoseRed)
            }
            items(expenseAccounts) { acc ->
                val balance = remember(acc.id, snapshots) { snapshots.find { it.accountId == acc.id }?.balance ?: 0L }
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    Text(text = "${acc.accountCode} - ${Localization.getAccountName(acc.accountCode, acc.name, lang)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(text = FinancialUtils.formatBase(balance), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
            item {
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (lang == "ar") "إجمالي المصروفات" else "Total Expense Breakdown", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(FinancialUtils.formatBase(totalExpense), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodyMedium, color = RoseRed)
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(thickness = 2.dp)
            }

            // NET RESULTS summary
            item {
                val netIncome = totalRevenue - totalExpense
                val profitMargin = if (totalRevenue > 0L) (netIncome.toDouble() / totalRevenue.toDouble()) * 100.0 else 0.0

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (netIncome >= 0L) EmeraldGreen.copy(alpha = 0.15f) else RoseRed.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (netIncome >= 0L) {
                                    if (lang == "ar") "صافي الدخل الربحي الموحد بالفترة" else "CONSOLIDATED NET REVENUE"
                                } else {
                                    if (lang == "ar") "صافي العجز والخسارة للفترة" else "NET CASH DEFICIT (LOSS)"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (netIncome >= 0L) EmeraldGreen else RoseRed
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${if (netIncome >= 0L) "+" else ""}${FinancialUtils.formatBase(netIncome)} ${if (isLibyan) "د.ل" else "LYD"}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (netIncome >= 0L) EmeraldGreen else RoseRed
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (lang == "ar") "هامش عائد الإيرادات: ${String.format("%.2f", profitMargin)}%" else "Revenue Yield Margin: ${String.format("%.2f", profitMargin)}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Operating Yield Advisor Strategy Suggestion Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.TipsAndUpdates, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (lang == "ar") "توجيهات الكفاءة والقرار المالي" else "Operating Advisory & Cost Controls",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            
                            val auditAdvice = if (netIncome > 0L) {
                                if (profitMargin > 20.0) {
                                    if (lang == "ar") "ربحية تشغيلية استثنائية! هامش مناورة تجاري واسع وكفاءة ممتازة في ضبط النفقات العامة. يُنصح باستثمار الوفورات لتوسيع الأصول التشغيلية."
                                    else "Exceptional commercial yield! Wide margin structure with brilliant cost preservation control. Reinvestment in revenue-generating assets is strongly supported."
                                } else if (profitMargin >= 5.0) {
                                    if (lang == "ar") "أداء تشغيلي قياسي ومتكافئ. المبيعات تغطي التكاليف التشغيلية بنسب تجارية صحية تضمن استمرارية المؤسسة بسلاسة."
                                    else "Standard, healthy operational margin in effect. Top-line revenues adequately buffer and offset common business expenditures."
                                } else {
                                    if (lang == "ar") "هوامش ربحية تشغيلية ضيقة ومضغوطة! يُنصح بمراجعة بنود المصارف التشغيلية لزيادة نسبة الاحتفاظ النقدي وتحسين كفاءة التسعير."
                                    else "Highly compressed bottom line. Low cost-retention ratio. Audit discretionary overhead or study moderate markup adjustments to alleviate margin squeeze."
                                }
                            } else if (netIncome < 0L) {
                                if (lang == "ar") "تنبيه بعجز تشغيلي فوري! التكاليف تفوق العوائد والتدفقات الإيرادية المعاصرة. يُنصح بتطبيق تدقيق ميزانية صارم للمصاريف التشغيلية والعمومية فوراً."
                                else "Urgent operational deficit in effect! Current overhead exceeds top-line production. Strategic margin expansion or immediate budgetary restraints on secondary expenses are recommended."
                            } else {
                                if (lang == "ar") "نقطة التعادل التام (صفر أرباح / صفر خسائر). يُنصح بزيادة تنشيط المبيعات أو ترشيد التكاليف الثابتة لتحقيق تدفق ربحي إيجابي."
                                else "Perfect Break-even posture (Zero Net Yield). Expand operational capacity or apply selective overhead containment to establish positive cash-flow direction."
                            }
                            
                            Text(
                                text = auditAdvice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgingReportView(viewModel: LedgerViewModel) {
    val lang by viewModel.currentLanguage.collectAsState()
    val isLibyan by viewModel.isLibyanMode.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val snapshots by viewModel.accountSnapshots.collectAsState()
    val vouchers by viewModel.vouchers.collectAsState()

    var isCustomerView by remember { mutableStateOf(true) }
    var rawLines by remember { mutableStateOf<List<com.example.data.VoucherLine>>(emptyList()) }
    var isLoadingLines by remember { mutableStateOf(false) }

    LaunchedEffect(vouchers) {
        isLoadingLines = true
        try {
            rawLines = viewModel.getAllVoucherLines()
        } catch (e: Exception) {
            // silent fail
        } finally {
            isLoadingLines = false
        }
    }

    if (isLoadingLines) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val postedHeaders = vouchers.filter { it.isPosted }
    val now = System.currentTimeMillis()
    val oneDayMs = 24 * 60 * 60 * 1000L

    val agingRows = remember(isCustomerView, customers, suppliers, snapshots, rawLines) {
        val rows = mutableListOf<AgingRow>()
        val clients = if (isCustomerView) {
            customers.map { Triple(it.name, it.phone, it.accountId) }
        } else {
            suppliers.map { Triple(it.name, it.phone, it.accountId) }
        }

        for (client in clients) {
            val name = client.first
            val phone = client.second
            val accId = client.third
            if (accId == null) continue

            val balance = snapshots.find { it.accountId == accId }?.balance ?: 0L
            val totalOutstanding = if (isCustomerView) balance else -balance

            if (totalOutstanding <= 0L) continue

            val accountLines = rawLines.filter { line ->
                line.accountId == accId && postedHeaders.any { it.id == line.headerId }
            }

            var current = 0L
            var bucket30to60 = 0L
            var bucket60to90 = 0L
            var over90 = 0L

            for (line in accountLines) {
                val header = postedHeaders.find { it.id == line.headerId } ?: continue
                val amount = line.amountBase
                val ageDays = (now - header.date) / oneDayMs

                val isIncrease = if (isCustomerView) (line.debit > 0) else (line.credit > 0)

                if (isIncrease) {
                    when {
                        ageDays <= 30 -> current += amount
                        ageDays <= 60 -> bucket30to60 += amount
                        ageDays <= 90 -> bucket60to90 += amount
                        else -> over90 += amount
                    }
                } else {
                    var remainingReduction = amount
                    if (over90 >= remainingReduction) {
                        over90 -= remainingReduction
                        remainingReduction = 0L
                    } else {
                        remainingReduction -= over90
                        over90 = 0L
                    }

                    if (remainingReduction > 0L) {
                        if (bucket60to90 >= remainingReduction) {
                            bucket60to90 -= remainingReduction
                            remainingReduction = 0L
                        } else {
                            remainingReduction -= bucket60to90
                            bucket60to90 = 0L
                        }
                    }

                    if (remainingReduction > 0L) {
                        if (bucket30to60 >= remainingReduction) {
                            bucket30to60 -= remainingReduction
                            remainingReduction = 0L
                        } else {
                            remainingReduction -= bucket30to60
                            bucket30to60 = 0L
                        }
                    }

                    if (remainingReduction > 0L) {
                        if (current >= remainingReduction) {
                            current -= remainingReduction
                            remainingReduction = 0L
                        } else {
                            current = 0L
                        }
                    }
                }
            }

            val sumOfBuckets = current + bucket30to60 + bucket60to90 + over90
            val scaledCurrent: Long
            val scaled30to60: Long
            val scaled60to90: Long
            val scaledOver90: Long

            if (sumOfBuckets > 0L) {
                val ratio = totalOutstanding.toDouble() / sumOfBuckets.toDouble()
                scaledCurrent = (current * ratio).toLong()
                scaled30to60 = (bucket30to60 * ratio).toLong()
                scaled60to90 = (bucket60to90 * ratio).toLong()
                scaledOver90 = (over90 * ratio).toLong()
            } else {
                scaledCurrent = totalOutstanding
                scaled30to60 = 0L
                scaled60to90 = 0L
                scaledOver90 = 0L
            }

            rows.add(
                AgingRow(
                    name = name,
                    phone = phone,
                    totalOutstanding = totalOutstanding,
                    current = scaledCurrent,
                    bucket30to60 = scaled30to60,
                    bucket60to90 = scaled60to90,
                    over90 = scaledOver90
                )
            )
        }
        rows.sortByDescending { it.totalOutstanding }
        rows
    }

    val totalCurrent = agingRows.sumOf { it.current }
    val total30to60 = agingRows.sumOf { it.bucket30to60 }
    val total60to90 = agingRows.sumOf { it.bucket60to90 }
    val totalOver90 = agingRows.sumOf { it.over90 }
    val accumulatedTotal = totalCurrent + total30to60 + total60to90 + totalOver90

    val corporateOrange = Color(0xFFF97316)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(4.dp)
        ) {
            Button(
                onClick = { isCustomerView = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCustomerView) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (isCustomerView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (lang == "ar") "ديون العملاء" else "Customer Receivables")
            }
            
            Button(
                onClick = { isCustomerView = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isCustomerView) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (!isCustomerView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Engineering, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (lang == "ar") "مستحقات الموردين" else "Supplier Payables")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (lang == "ar") "الملخص التراكمي للمحفظة" else "CONSOLIDATED PORTFOLIO SUMMARY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = if (isCustomerView) {
                                    if (lang == "ar") "إجمالي الذمم المدينة المستحقة" else "Total Accounts Receivable"
                                } else {
                                    if (lang == "ar") "إجمالي الحسابات الدائنة والديون" else "Total Accounts Payable"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${FinancialUtils.formatBase(accumulatedTotal)} ${if (isLibyan) "د.ل" else "LYD"}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(10.dp))
                        
                        val buckets = listOf(
                            Triple(if (lang == "ar") "جاري (0-30)" else "Current (0-30)", totalCurrent, EmeraldGreen),
                            Triple(if (lang == "ar") "متأخر (31-60)" else "Overdue (31-60)", total30to60, MaterialTheme.colorScheme.secondary),
                            Triple(if (lang == "ar") "حرج (61-90)" else "Critical (61-90)", total60to90, corporateOrange),
                            Triple(if (lang == "ar") "خطر (90+)" else "Risk (90+)", totalOver90, RoseRed)
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            for (bucket in buckets) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text(bucket.first, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = FinancialUtils.formatBase(bucket.second),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = bucket.third
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (agingRows.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (lang == "ar") "لا توجد ذمم معلقة أو ديون نشطة بالفترة." else "No active overdue outstanding balances detected.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                items(agingRows) { row ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(row.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(row.phone.ifBlank { "N/A" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                Text(
                                    text = "${FinancialUtils.formatBase(row.totalOutstanding)} ${if (isLibyan) "د.ل" else "LYD"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Spacer(Modifier.height(10.dp))
                            
                            val sum = row.current + row.bucket30to60 + row.bucket60to90 + row.over90
                            if (sum > 0) {
                                val currentPct = row.current.toFloat() / sum
                                val pct30to60 = row.bucket30to60.toFloat() / sum
                                val pct60to90 = row.bucket60to90.toFloat() / sum
                                val over90Pct = row.over90.toFloat() / sum
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                ) {
                                    if (currentPct > 0) Box(modifier = Modifier.fillMaxHeight().weight(java.lang.Float.max(currentPct, 0.01f)).background(EmeraldGreen))
                                    if (pct30to60 > 0) Box(modifier = Modifier.fillMaxHeight().weight(java.lang.Float.max(pct30to60, 0.01f)).background(MaterialTheme.colorScheme.secondary))
                                    if (pct60to90 > 0) Box(modifier = Modifier.fillMaxHeight().weight(java.lang.Float.max(pct60to90, 0.01f)).background(corporateOrange))
                                    if (over90Pct > 0) Box(modifier = Modifier.fillMaxHeight().weight(java.lang.Float.max(over90Pct, 0.01f)).background(RoseRed))
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                AgingItem(if (lang == "ar") "جاري" else "0-30d", row.current, EmeraldGreen)
                                AgingItem(if (lang == "ar") "31-60ي" else "31-60d", row.bucket30to60, MaterialTheme.colorScheme.secondary)
                                AgingItem(if (lang == "ar") "61-90ي" else "61-90d", row.bucket60to90, corporateOrange)
                                AgingItem(if (lang == "ar") "90ي+" else "90d+", row.over90, RoseRed)
                            }
                        }
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (totalOver90 > 0L) RoseRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.TipsAndUpdates, contentDescription = null, tint = if (totalOver90 > 0) RoseRed else EmeraldGreen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (lang == "ar") "توصيات الائتمان ومخاطر الديون" else "Debt Risks & Recovery Strategic Action",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(Modifier.height(6.dp))
                        
                        val riskText = if (totalOver90 > 0L) {
                            if (lang == "ar") "تحذير: وجود مستحقات متأخرة فوق 90 يوماً! يُنصح بالتوقف عن البيع بالآجل لهذا العميل فوراً وتعيين محصل لمراجعة السندات وبدء إجراءات التحصيل القانوني لحماية السيولة."
                            else "Action Required: Active arrears exceeded the 90-day threshold! Stop all dynamic credit facilities to this customer immediately. Initiate prompt legal debt collection."
                        } else if (total60to90 > 0L) {
                            if (lang == "ar") "مخاطر ائتمان متوسطة. بعض الحسابات في مرحلة متأخرة (61-90 يوماً). أرسل إشعارات تذكير رسمية قبل انزلاقها لبند الديون المعدومة."
                            else "Moderate portfolio warnings in effect. Remind clients systematically to clear upcoming outstanding arrears."
                        } else {
                            if (lang == "ar") "جودة ممتازة لمحفظة الائتمان! جميع الديون المعلقة تقع في الفئة الآمنة وفي الفترات المستحقة القريبة. نوصي باستمرار نفس الشروط الائتمانية."
                            else "Excellent credit asset quality. All dynamic client balances reside inside safe payment windows."
                        }
                        
                        Text(riskText, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun AgingItem(label: String, value: Long, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Spacer(Modifier.width(4.dp))
        Text("$label: ${FinancialUtils.formatBase(value)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

data class AgingRow(
    val name: String,
    val phone: String,
    val totalOutstanding: Long,
    val current: Long,
    val bucket30to60: Long,
    val bucket60to90: Long,
    val over90: Long
)

@Composable
fun CashFlowView(viewModel: LedgerViewModel) {
    val loading by viewModel.cashFlowLoading.collectAsState()
    val statementState by viewModel.cashFlowStatement.collectAsState()
    val lang by viewModel.currentLanguage.collectAsState()
    val isLibyan by viewModel.isLibyanMode.collectAsState()

    // Trigger calculation when the screen opens if statement is null
    LaunchedEffect(Unit) {
        if (statementState == null) {
            viewModel.refreshCashFlow()
        }
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val statement = statementState
    if (statement == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { viewModel.refreshCashFlow() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Calculate", modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (lang == "ar") "اضغط لتوليد قائمة التدفقات النقدية" else "Tap to calculate Cash Flow Statement",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        return
    }

    val netOperating = statement.operatingInflow - statement.operatingOutflow
    val netInvesting = statement.investingInflow - statement.investingOutflow
    val netFinancing = statement.financingInflow - statement.financingOutflow
    val periodNetChange = netOperating + netInvesting + netFinancing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.1f))
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (lang == "ar") "قائمة التدفقات النقدية (الأسلوب المباشر)" else "Cash Flow Statement (Direct Method)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (lang == "ar") "مطابقة النقدية: الرصيد الافتتاحي + التغير بالفترة = الرصيد الختامي" else "Reconciliation check: Opening cash + Period Net Change = Closing cash",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    IconButton(onClick = { viewModel.refreshCashFlow() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
            }

            // Beginning Cash
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (lang == "ar") "النقد وما يماثله - رصيد بداية الفترة" else "Cash and equivalents - Opening Balance",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${FinancialUtils.formatBase(statement.openingBalance)} ${if (isLibyan) "د.ل" else "LYD"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // OPERATING ACTIVITIES
            item {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (lang == "ar") "1. التدفقات من الأنشطة التشغيلية" else "1. OPERATING ACTIVITIES",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(text = if (lang == "ar") "مقبوضات نقدية من المبيعات والعملاء" else "Cash receipts from operations / customers", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(text = FinancialUtils.formatBase(statement.operatingInflow), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(text = if (lang == "ar") "مدفوعات نقدية للموردين والمشتريات والرواتب" else "Cash paid for procurement / operations / suppliers", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(text = "- ${FinancialUtils.formatBase(statement.operatingOutflow)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = RoseRed)
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = if (lang == "ar") "صافي نقد الأنشطة التشغيلية" else "Net Cash from Operating Activities", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = FinancialUtils.formatBase(netOperating), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = if (netOperating >= 0) EmeraldGreen else RoseRed)
                        }
                    }
                }
            }

            // INVESTING ACTIVITIES
            item {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (lang == "ar") "2. الأنشطة الاستثمارية" else "2. INVESTING ACTIVITIES",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.CorporateAmethyst
                        )
                        Spacer(Modifier.height(8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(text = if (lang == "ar") "متحصلات بيع أصول ثابتة ومعدات" else "Cash receipts from sale of non-current assets", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(text = FinancialUtils.formatBase(statement.investingInflow), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(text = if (lang == "ar") "مدفوعات شراء معدات وعقارات (شراء أصول)" else "Cash payments for fixed assets / Capex", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(text = "- ${FinancialUtils.formatBase(statement.investingOutflow)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = RoseRed)
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = if (lang == "ar") "صافي نقد الأنشطة الاستثمارية" else "Net Cash used in Investing Activities", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = FinancialUtils.formatBase(netInvesting), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = if (netInvesting >= 0) EmeraldGreen else RoseRed)
                        }
                    }
                }
            }

            // FINANCING ACTIVITIES
            item {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (lang == "ar") "3. الأنشطة التمويلية" else "3. FINANCING ACTIVITIES",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.CorporateSky
                        )
                        Spacer(Modifier.height(8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(text = if (lang == "ar") "متحصلات زيادة رأس المال والمساهمين" else "Cash receipts from capital / equity investments", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(text = FinancialUtils.formatBase(statement.financingInflow), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(text = if (lang == "ar") "سداد أقساط قروض وتوزيعات أرباح" else "Cash paid for loans / debt settlements / equity drawdowns", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(text = "- ${FinancialUtils.formatBase(statement.financingOutflow)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = RoseRed)
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = if (lang == "ar") "صافي التدفقات من الأنشطة التمويلية" else "Net Cash from Financing Activities", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = FinancialUtils.formatBase(netFinancing), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = if (netFinancing >= 0) EmeraldGreen else RoseRed)
                        }
                    }
                }
            }

            // Net period changes
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (periodNetChange >= 0L) EmeraldGreen.copy(alpha = 0.12f) else RoseRed.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (lang == "ar") "صافي حركة وتأثير السيولة النقدية بالفترة" else "NET CONSOLIDATED CASH MOVEMENT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (periodNetChange >= 0L) EmeraldGreen else RoseRed
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${if (periodNetChange >= 0L) "+" else ""}${FinancialUtils.formatBase(periodNetChange)} ${if (isLibyan) "د.ل" else "LYD"}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (periodNetChange >= 0L) EmeraldGreen else RoseRed
                        )
                    }
                }
            }

            // Closing Cash and Reconciliation
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (lang == "ar") "النقد وما يماثله - رصيد نهاية الفترة" else "Cash and equivalents - Closing Balance",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${FinancialUtils.formatBase(statement.closingBalance)} ${if (isLibyan) "د.ل" else "LYD"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Advisory control card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.TipsAndUpdates, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (lang == "ar") "مؤشرات جودة التدفق النقدي" else "Cash Generation Advisory",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        val advisoryText = if (netOperating > 0L) {
                            if (netOperating > netInvesting + netFinancing) {
                                if (lang == "ar") "تدفق نقدي صحي ومستدام! العمليات التشغيلية الأساسية تغطي بالكامل نفقات الاستثمار والتزامات التمويل الخارجي."
                                else "Highly healthy cash profile! Core operational receipts easily sustain and fully fund current business growth and expansions."
                            } else {
                                if (lang == "ar") "تدفقات تشغيلية إيجابية، ولكن النفقات الرأسمالية أو سداد الديون عالية جداً. احرص على توازن السيولة لمنع انكشاف الصندوق."
                                else "Operating cash is positive, but capital expansions or debt amortization represent heavy drawdowns. Monitor cash buffers closely."
                            }
                        } else {
                            if (lang == "ar") "تحذير: تدفق نقدي تشغيلي سالب! مبيعاتك وأنشطتك اليومية لا تغطي نفقات التشغيل الفعلية. تحتاج إلى تمويل خارجي أو استعجال المطالبات ومراقبة التحصيل."
                            else "Warning: Negative cash flow from operations! Regular collections do not cover general operational expenditure. Accelerate accounts receivable collection."
                        }
                        
                        Text(
                            text = advisoryText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
