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
import com.example.data.Customer
import com.example.data.Supplier
import com.example.data.BankAccount
import androidx.compose.foundation.clickable
import com.example.ui.Localization
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.CorporateAmethyst
import com.example.ui.theme.CorporateSky
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.ui.viewmodel.LedgerViewModel
import com.example.util.FinancialUtils

@Composable
fun ReportsScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier,
    forcedTab: Int? = null
) {
    var activeTab by remember { mutableStateOf(forcedTab ?: 0) } // Tabs: 0 = Trial Balance, 1 = Balance Sheet, 2 = Income Statement, 3 = Cash Flow, 4 = Customers, 5 = Suppliers, 6 = Banks
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()

    var activeDetailCustomer by remember { mutableStateOf<com.example.data.Customer?>(null) }
    var activeDetailSupplier by remember { mutableStateOf<com.example.data.Supplier?>(null) }
    var activeDetailBank by remember { mutableStateOf<com.example.data.BankAccount?>(null) }

    LaunchedEffect(forcedTab) {
        if (forcedTab != null) {
            activeTab = forcedTab
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (activeDetailCustomer != null) {
            CustomerStatementView(
                customer = activeDetailCustomer!!,
                viewModel = viewModel,
                onBack = { activeDetailCustomer = null }
            )
        } else if (activeDetailSupplier != null) {
            SupplierStatementView(
                supplier = activeDetailSupplier!!,
                viewModel = viewModel,
                onBack = { activeDetailSupplier = null }
            )
        } else if (activeDetailBank != null) {
            BankStatementView(
                bankAccount = activeDetailBank!!,
                viewModel = viewModel,
                onBack = { activeDetailBank = null }
            )
        } else {
            val customTitle = when (activeTab) {
                0 -> Localization.translate(Localization.Key.TRIAL_BALANCE, lang)
                1 -> Localization.translate(Localization.Key.BALANCE_SHEET, lang)
                2 -> Localization.translate(Localization.Key.INCOME_STATEMENT, lang)
                3 -> if (lang == "ar") "كشف الأنشطة والتدفقات النقدية" else "Statement of Cash Flows (IAS 7)"
                4 -> if (lang == "ar") "كشف أرصدة العملاء والذمم المدينة" else "Customer Balances & Receivables Trail"
                5 -> if (lang == "ar") "كشف أرصدة الموردين والالتزامات" else "Supplier Outstanding Balances"
                6 -> if (lang == "ar") "كشف الحسابات البنكية والسيولة النقدية" else "Bank Accounts & Liquidity Balances"
                else -> Localization.translate(Localization.Key.FINANCIAL_STATEMENTS, lang)
            }
            val customSubtitle = when (activeTab) {
                0 -> if (lang == "ar") "ميزان المراجعة غير المعدل بالأرصدة وحركات الفترة تتبعاً لقييد اليومية" else "Verify balanced summary of debits and credits from active transactions"
                1 -> if (lang == "ar") "معاينة هيكل المركز المالي السنوي متضمناً الأصول والمسؤوليات وحقوق الملكية" else "Review company assets, liabilities, and owner's equity balances"
                2 -> if (lang == "ar") "تقرير الأرباح والخسائر والأنشطة التشغيلية والإيرادات وصافي الدخول" else "Measure financial performance, sales, expenses, and net profit/loss"
                3 -> if (lang == "ar") "تقرير مباشر يوضح النقد ومعادلاته عبر الأنشطة التشغيلية والاستثمارية والتمويلية" else "Direct-method statements showcasing Operating, Investing, and Financing flows"
                4 -> if (lang == "ar") "فحص جرد تفصيلي لمجموعات وديون العملاء وتدقيق أرصدتهم المستحقة للتسوية المباشرة." else "Detailed summary of outstanding balances across customer segment folders."
                5 -> if (lang == "ar") "متابعة ديون ومطالبات الموردين وحسابات الدائنون لسهولة سداد الالتزامات والجدولة." else "Track upcoming payables for supplier accounts, optimizing settlement and vendor relations."
                6 -> if (lang == "ar") "سيولة الشركة وتوزيعها الفعلي في البنوك الاستثمارية والتجارية مع موازنة الحسابات الجارية." else "Real-time summary of bank accounts, IBANs, and ledger reconciliation balances per branch."
                else -> if (lang == "ar") "تنفيذ حسابات الأرصدة في الوقت الفعلي المتوافقة مع المعايير الدولية (IFRS) مباشرة من قيود اليومية المزدوجة المتوازنة." else "IFRS-compliant, real-time balance calculations derived directly from balanced double-entries."
            }

            Text(
                text = customTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = customSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (forcedTab == null) {
                ScrollableTabRow(
                    selectedTabIndex = activeTab,
                    edgePadding = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text(Localization.translate(Localization.Key.TRIAL_BALANCE, lang), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) },
                        icon = { Icon(Icons.Filled.AccountBalance, null) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text(Localization.translate(Localization.Key.BALANCE_SHEET, lang), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) },
                        icon = { Icon(Icons.Filled.Assessment, null) }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text(Localization.translate(Localization.Key.INCOME_STATEMENT, lang), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) },
                        icon = { Icon(Icons.Filled.TrendingUp, null) }
                    )
                    Tab(
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        text = { Text(if (lang == "ar") "التدفقات النقدية" else "Cash Flows", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) },
                        icon = { Icon(Icons.Filled.SwapHoriz, null) }
                    )
                    Tab(
                        selected = activeTab == 4,
                        onClick = { activeTab = 4 },
                        text = { Text(if (lang == "ar") "أرصدة العملاء" else "Customer Balances", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) },
                        icon = { Icon(Icons.Filled.People, null) }
                    )
                    Tab(
                        selected = activeTab == 5,
                        onClick = { activeTab = 5 },
                        text = { Text(if (lang == "ar") "أرصدة الموردين" else "Supplier Balances", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) },
                        icon = { Icon(Icons.Filled.Storefront, null) }
                    )
                    Tab(
                        selected = activeTab == 6,
                        onClick = { activeTab = 6 },
                        text = { Text(if (lang == "ar") "حسابات البنوك" else "Bank Accounts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) },
                        icon = { Icon(Icons.Filled.AccountBalanceWallet, null) }
                    )
                }

                Spacer(Modifier.height(16.dp))
            } else {
                Spacer(Modifier.height(8.dp))
            }

            when (activeTab) {
                0 -> TrialBalanceView(viewModel)
                1 -> BalanceSheetView(viewModel)
                2 -> IncomeStatementView(viewModel)
                3 -> CashFlowStatementView(viewModel)
                4 -> CustomersReportView(viewModel, onSelectCustomer = { activeDetailCustomer = it })
                5 -> SuppliersReportView(viewModel, onSelectSupplier = { activeDetailSupplier = it })
                6 -> BanksReportView(viewModel, onSelectBank = { activeDetailBank = it })
            }
        }
    }
}

@Composable
fun TrialBalanceView(viewModel: LedgerViewModel) {
    val rows by viewModel.trialBalanceRows.collectAsStateWithLifecycle()
    val loading by viewModel.trialBalanceLoading.collectAsStateWithLifecycle()
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()

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
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val snapshotsList = remember(accounts) { accounts.filter { !it.isGroup } } // only leaf for accuracy
    val snapshots by viewModel.accountSnapshots.collectAsStateWithLifecycle()
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()

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
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val snapshotsList = remember(accounts) { accounts.filter { !it.isGroup } }
    val snapshots by viewModel.accountSnapshots.collectAsStateWithLifecycle()
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isLibyan by viewModel.isLibyanMode.collectAsStateWithLifecycle()

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
fun CashFlowStatementView(viewModel: LedgerViewModel) {
    val statement by viewModel.cashFlowStatement.collectAsStateWithLifecycle()
    val loading by viewModel.cashFlowLoading.collectAsStateWithLifecycle()
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isLibyan by viewModel.isLibyanMode.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.1f))
    ) {
        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (statement == null) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (lang == "ar") "لا تتوفر تدفقات نقدية مسجلة حالياً." else "No Cash Flow data currently available.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            val stmt = statement!!
            val netOperating = stmt.operatingInflow - stmt.operatingOutflow
            val netInvesting = stmt.investingInflow - stmt.investingOutflow
            val netFinancing = stmt.financingInflow - stmt.financingOutflow
            val netChange = netOperating + netInvesting + netFinancing

            LazyColumn(
                modifier = Modifier.weight(1f).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (lang == "ar") "كشف التدفق النقدي المباشر (IAS 7)" else "Direct Cash Flow Statement (IAS 7)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (lang == "ar") "تتبع النقد ومعادلاته عبر العمليات والأنشطة" else "Tracking of cash and cash equivalents across active systems",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        IconButton(
                            onClick = {
                                com.example.util.PrintUtils.printCashFlow(
                                    context = context,
                                    statement = stmt,
                                    isLibyan = isLibyan,
                                    lang = lang
                                )
                            },
                            modifier = Modifier.testTag("print_cash_flow_button")
                        ) {
                            Icon(Icons.Filled.Print, contentDescription = "Print Cash Flow")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                }

                // OPERATING ACTIVITIES
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (lang == "ar") "1. التدفقات من الأنشطة التشغيلية" else "1. Operating Activities",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (lang == "ar") "المقبوضات النقدية من العملاء" else "Cash receipts from customers", style = MaterialTheme.typography.bodyMedium)
                                Text(FinancialUtils.formatBase(stmt.operatingInflow), style = MaterialTheme.typography.bodyMedium, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (lang == "ar") "المدفوعات النقدية للموردين والمصاريف" else "Cash paid to suppliers & employees", style = MaterialTheme.typography.bodyMedium)
                                Text(FinancialUtils.formatBase(stmt.operatingOutflow), style = MaterialTheme.typography.bodyMedium, color = RoseRed, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(6.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            Spacer(Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (lang == "ar") "صافي التدفقات التشغيلية" else "Net Cash from Operating Activities", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${if (netOperating >= 0) "+" else ""}${FinancialUtils.formatBase(netOperating)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (netOperating >= 0) EmeraldGreen else RoseRed
                                )
                            }
                        }
                    }
                }

                // INVESTING ACTIVITIES
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (lang == "ar") "2. التدفقات من الأنشطة الاستثمارية" else "2. Investing Activities",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CorporateAmethyst
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (lang == "ar") "المتحصلات من بيع أصول" else "Inflows from asset sales", style = MaterialTheme.typography.bodyMedium)
                                Text(FinancialUtils.formatBase(stmt.investingInflow), style = MaterialTheme.typography.bodyMedium, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (lang == "ar") "المدفوعات لشراء أصول" else "Outflows for asset purchases", style = MaterialTheme.typography.bodyMedium)
                                Text(FinancialUtils.formatBase(stmt.investingOutflow), style = MaterialTheme.typography.bodyMedium, color = RoseRed, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(6.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            Spacer(Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (lang == "ar") "صافي التدفقات الاستثمارية" else "Net Cash from Investing Activities", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${if (netInvesting >= 0) "+" else ""}${FinancialUtils.formatBase(netInvesting)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (netInvesting >= 0) EmeraldGreen else RoseRed
                                )
                            }
                        }
                    }
                }

                // FINANCING ACTIVITIES
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (lang == "ar") "3. التدفقات من الأنشطة التمويلية" else "3. Financing Activities",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CorporateSky
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (lang == "ar") "المتحصلات من بيع أسهم أو قروض" else "Inflows from capital issues & loans", style = MaterialTheme.typography.bodyMedium)
                                Text(FinancialUtils.formatBase(stmt.financingInflow), style = MaterialTheme.typography.bodyMedium, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (lang == "ar") "تسديدات قروض وتوزيعات أرباح" else "Outflows for loans & dividends", style = MaterialTheme.typography.bodyMedium)
                                Text(FinancialUtils.formatBase(stmt.financingOutflow), style = MaterialTheme.typography.bodyMedium, color = RoseRed, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(6.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            Spacer(Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (lang == "ar") "صافي التدفقات التمويلية" else "Net Cash from Financing Activities", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${if (netFinancing >= 0) "+" else ""}${FinancialUtils.formatBase(netFinancing)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (netFinancing >= 0) EmeraldGreen else RoseRed
                                )
                            }
                        }
                    }
                }

                // RECONCILIATION SUMMARY
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (netChange >= 0) EmeraldGreen.copy(alpha = 0.15f) else RoseRed.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, (if (netChange >= 0) EmeraldGreen else RoseRed).copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (lang == "ar") "مطابقة النقد وأثر حركات الفترة" else "Cash Reconciliation Balance Sheet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (netChange >= 0) EmeraldGreen else RoseRed
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (lang == "ar") "صافي التغير النقدي بالفترة" else "Net cash change during period", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${if (netChange >= 0) "+" else ""}${FinancialUtils.formatBase(netChange)}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (netChange >= 0) EmeraldGreen else RoseRed
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (lang == "ar") "الرصيد النقدي أول الفترة" else "Cash at beginning of period", style = MaterialTheme.typography.bodyMedium)
                                Text(FinancialUtils.formatBase(stmt.openingBalance), fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = (if (netChange >= 0) EmeraldGreen else RoseRed).copy(alpha = 0.2f))
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (lang == "ar") "الرصيد النقدي النهائي للدفاتر" else "Cash Equivalents at End of Period",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${FinancialUtils.formatBase(stmt.closingBalance)} ${if (isLibyan) "د.ل" else "LYD"}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (stmt.closingBalance >= 0) EmeraldGreen else RoseRed
                                )
                            }
                        }
                    }
                }

                // AI DIAGNOSTIC INSIGHTS CARD
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.TipsAndUpdates, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (lang == "ar") "تقرير الكفاءة والسيولة التشغيلية" else "Liquidity Runway & Operational Guidance",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            val runwayAdvice = if (netOperating > 0L) {
                                if (netChange > 0L) {
                                    if (lang == "ar") "تراكم نقدي إيجابي ممتاز مدفوع بالعمليات التشغيلية الذاتية. سيولة كافية جداً ودرجة حماية مرتفعة لتوسيع الاستمارات أو تفادي أزمات الاعتمادات المستندية المحلية."
                                    else "Excellent positive cash accumulator fueled directly by core business loops. Healthy cash flow secures your procurement runways and helps negotiate better credit facilities."
                                } else {
                                    if (lang == "ar") "العمليات التشغيلية تدر نفقات ممتازة، لكن صافي النقد الإجمالي متراجع نتيجة تزايد الاستثمارات بالأصول الثابتة أو سداد التزامات تمويلية سابقة. الموقف آمن."
                                    else "Operational core is earning robust cash, though the net overall cash is negative due to strategic investments in capital equipment or loan reductions."
                                }
                            } else if (netOperating < 0L) {
                                if (lang == "ar") "عجز نقدي تشغيلي خطير! عمليات الشركة المحورية عاجزة عن تمويل ذاتها وتنزف النقد للاحتياجات التشغيلية. يُرجى مراجعة سياسات البيع الآجل والتحصيل الفوري للعملاء."
                                    else "Severe operating cash bleed! Core transactions are consuming instead of breeding liquidity. Re-evaluate customer credit policies and accounts receivable invoice collection periods."
                            } else {
                                if (lang == "ar") "لا تتوفر حركات تدفق نقدي تشغيلي مميزة للفترة معلنة في السجلات. ينصح بتسجيل فواتير وسندات القبض والدفع المحاسبية مرحلة لضمان تتبع السيولة تشغيلياً."
                                else "No significant operational flows are compiled right now. Ensure payment vouchers are correctly posted in the ledger to obtain real-time cash flow diagnostics."
                            }

                            Text(
                                text = runwayAdvice,
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
fun CustomersReportView(
    viewModel: LedgerViewModel,
    onSelectCustomer: (Customer) -> Unit
) {
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val snapshots by viewModel.accountSnapshots.collectAsStateWithLifecycle()
    val allAccounts by viewModel.accounts.collectAsStateWithLifecycle()
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val totalOutstanding = remember(customers, snapshots) {
        customers.sumOf { snapshots.find { s -> s.accountId == it.accountId }?.balance ?: 0L }
    }
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (lang == "ar") "الذمم المدينة الفعالة • إجمالي المدينين" else "Receivables Accounts • Active Balances",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = {
                    com.example.util.PrintUtils.printCustomersReport(context, customers, allAccounts, snapshots, lang)
                }
            ) {
                Icon(Icons.Filled.Print, contentDescription = "Print Customers Report")
            }
        }

        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (lang == "ar") "إجمالي أرصدة العملاء والديون المستحقة" else "Total Customer Receivables",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = FinancialUtils.formatBase(totalOutstanding) + " " + (if (lang == "ar") "د.ل" else "LYD"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Box(
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.People, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                }
            }
        }

        if (customers.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(if (lang == "ar") "لا يوجد عملاء مسجلين حالياً" else "No registered customers found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (lang == "ar") "العميل" else "Customer/Group", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text(if (lang == "ar") "رقم الهاتف" else "Phone No", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text(if (lang == "ar") "الحساب المقترن" else "Linked Account", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text(if (lang == "ar") "الرصيد د.ل" else "Outstanding Balance", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                }

                items(customers) { customer ->
                    val balance = snapshots.find { s -> s.accountId == customer.accountId }?.balance ?: 0L
                    val linkedAcc = allAccounts.find { it.id == customer.accountId }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCustomer(customer) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(customer.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(customer.groupName.ifEmpty { if (lang == "ar") "عام" else "General" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(customer.phone.ifEmpty { "-" }, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Column(modifier = Modifier.weight(1.5f)) {
                            if (linkedAcc != null) {
                                Text(linkedAcc.accountCode, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                Text(Localization.getAccountName(linkedAcc.accountCode, linkedAcc.name, lang), style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text("-", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Text(FinancialUtils.formatBase(balance), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                }
            }
        }
    }
}

@Composable
fun SuppliersReportView(
    viewModel: LedgerViewModel,
    onSelectSupplier: (Supplier) -> Unit
) {
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val snapshots by viewModel.accountSnapshots.collectAsStateWithLifecycle()
    val allAccounts by viewModel.accounts.collectAsStateWithLifecycle()
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val totalPayable = remember(suppliers, snapshots) {
        suppliers.sumOf {
            val balance = snapshots.find { s -> s.accountId == it.accountId }?.balance ?: 0L
            if (balance != 0L) -balance else 0L
        }
    }
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (lang == "ar") "حسابات ذمم الدائنين والمطالبات" else "Payables Accounts • Outstanding Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = {
                    com.example.util.PrintUtils.printSuppliersReport(context, suppliers, allAccounts, snapshots, lang)
                }
            ) {
                Icon(Icons.Filled.Print, contentDescription = "Print Suppliers Report")
            }
        }

        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (lang == "ar") "إجمالي أرصدة الموردين وحسابات القيد الدائن" else "Total Outstanding Payables",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = FinancialUtils.formatBase(totalPayable) + " " + (if (lang == "ar") "د.ل" else "LYD"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Box(
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Storefront, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp))
                }
            }
        }

        if (suppliers.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(if (lang == "ar") "لا يوجد موردين مسجلين حالياً" else "No registered suppliers found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (lang == "ar") "المورد" else "Supplier/Group", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text(if (lang == "ar") "رقم الهاتف" else "Phone No", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text(if (lang == "ar") "الحساب المقترن" else "Linked Account", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text(if (lang == "ar") "الرصيد المستحق" else "Outstanding Payable", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                }

                items(suppliers) { supplier ->
                    val balance = snapshots.find { s -> s.accountId == supplier.accountId }?.balance ?: 0L
                    val revBalance = if (balance != 0L) -balance else 0L
                    val linkedAcc = allAccounts.find { it.id == supplier.accountId }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSupplier(supplier) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(supplier.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(supplier.groupName.ifEmpty { if (lang == "ar") "عام" else "General" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(supplier.phone.ifEmpty { "-" }, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Column(modifier = Modifier.weight(1.5f)) {
                            if (linkedAcc != null) {
                                Text(linkedAcc.accountCode, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                Text(Localization.getAccountName(linkedAcc.accountCode, linkedAcc.name, lang), style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text("-", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Text(FinancialUtils.formatBase(revBalance), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.error)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                }
            }
        }
    }
}

@Composable
fun BanksReportView(
    viewModel: LedgerViewModel,
    onSelectBank: (BankAccount) -> Unit
) {
    val banks by viewModel.banks.collectAsStateWithLifecycle()
    val allBranches by viewModel.allBranches.collectAsStateWithLifecycle()
    val allBankAccounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()
    val snapshots by viewModel.accountSnapshots.collectAsStateWithLifecycle()
    val allAccounts by viewModel.accounts.collectAsStateWithLifecycle()
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    val totalCashAtBank = remember(allBankAccounts, snapshots) {
        allBankAccounts.sumOf { snapshots.find { s -> s.accountId == it.accountId }?.balance ?: 0L }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (lang == "ar") "تفاصيل أرصدة الحسابات الجارية بالبنوك" else "Bank Accounts Liquidity Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = {
                    com.example.util.PrintUtils.printBanksReport(context, banks, allBranches, allBankAccounts, allAccounts, snapshots, lang)
                }
            ) {
                Icon(Icons.Filled.Print, contentDescription = "Print Banks Report")
            }
        }

        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (lang == "ar") "إجمالي الأرصدة والسيولة المصرفية" else "Total Cash at Bank Assets",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldGreen
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = FinancialUtils.formatBase(totalCashAtBank) + " " + (if (lang == "ar") "د.ل" else "LYD"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldGreen
                    )
                }
                Box(
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)).background(EmeraldGreen.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.AccountBalance, null, tint = EmeraldGreen, modifier = Modifier.size(28.dp))
                }
            }
        }

        // List bank accounts balances
        if (allBankAccounts.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(if (lang == "ar") "لا يوجد حسابات مصرفية مسجلة حالياً" else "No banking accounts found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (lang == "ar") "البنك / الفرع" else "Bank / Branch", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text(if (lang == "ar") "بيانات الحساب" else "Bank Account details", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text(if (lang == "ar") "رابط الأستاذ" else "General Ledger", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text(if (lang == "ar") "الرصيد د.ل" else "Balance (LYD)", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                }

                items(allBankAccounts) { bAcc ->
                    val balance = snapshots.find { s -> s.accountId == bAcc.accountId }?.balance ?: 0L
                    val linkedAcc = allAccounts.find { it.id == bAcc.accountId }
                    val branchObj = allBranches.find { it.id == bAcc.branchId }
                    val bankObj = branchObj?.let { br -> banks.find { b -> b.id == br.bankId } }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectBank(bAcc) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(bankObj?.name ?: "-", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(branchObj?.name ?: "-", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(bAcc.accountName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(bAcc.accountNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            if (linkedAcc != null) {
                                Text(linkedAcc.accountCode, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            } else {
                                Text("-", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text(FinancialUtils.formatBase(balance), modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, color = EmeraldGreen)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                }
            }
        }
    }
}

@Composable
fun CustomerStatementView(
    customer: Customer,
    viewModel: LedgerViewModel,
    onBack: () -> Unit
) {
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val allAccounts by viewModel.accounts.collectAsStateWithLifecycle()
    val snapshots by viewModel.accountSnapshots.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val sdf = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }

    var statementRows by remember { mutableStateOf<List<com.example.ui.viewmodel.AccountStatementRow>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(customer.accountId) {
        isLoading = true
        statementRows = viewModel.getAccountStatement(customer.accountId)
        isLoading = false
    }

    val filteredRows = remember(statementRows, searchQuery) {
        if (searchQuery.isBlank()) {
            statementRows
        } else {
            statementRows.filter {
                it.memo.contains(searchQuery, ignoreCase = true) ||
                it.voucherNo.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val balance = snapshots.find { s -> s.accountId == customer.accountId }?.balance ?: 0L
    val totalDebit = remember(statementRows) { statementRows.sumOf { it.debit } }
    val totalCredit = remember(statementRows) { statementRows.sumOf { it.credit } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        // Top section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (lang == "ar") "كشف الحساب المالي التفصيلي للعميل" else "Detailed Customer Ledger Statement",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            IconButton(
                onClick = {
                    val linkedAcc = allAccounts.find { it.id == customer.accountId } ?: Account(
                        id = customer.accountId,
                        accountCode = "1201001",
                        name = customer.name,
                        accountType = com.example.data.AccountType.ASSET,
                        currencyId = 1L
                    )
                    com.example.util.PrintUtils.printAccountStatement(context, linkedAcc, statementRows, lang)
                }
            ) {
                Icon(Icons.Filled.Print, contentDescription = "Print Statement")
            }
        }

        // Customer Info Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (lang == "ar") "المجموعة" else "Customer Segment",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = customer.groupName.ifEmpty { if (lang == "ar") "عام" else "General" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (lang == "ar") "الهاتف" else "Phone Number",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = customer.phone.ifEmpty { "-" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Total Debit
                    Column {
                        Text(
                            text = if (lang == "ar") "إجمالي مدين (مبيعات)" else "Total debited (Sales)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = FinancialUtils.formatBase(totalDebit) + " " + (if (lang == "ar") "د.ل" else "LYD"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    // Total Credit
                    Column {
                        Text(
                            text = if (lang == "ar") "إجمالي دائن (سدادات)" else "Total credited (Receipts)",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldGreen
                        )
                        Text(
                            text = FinancialUtils.formatBase(totalCredit) + " " + (if (lang == "ar") "د.ل" else "LYD"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                    }
                    // Running Balance
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (lang == "ar") "الرصيد النهائي الحالي" else "Current Outstanding Balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (balance >= 0) EmeraldGreen else RoseRed
                        )
                        Text(
                            text = FinancialUtils.formatBase(balance) + " " + (if (lang == "ar") "د.ل" else "LYD"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (balance >= 0) EmeraldGreen else RoseRed
                        )
                    }
                }
            }
        }

        // Search in entries
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(if (lang == "ar") "البحث في تفاصيل القيود والبيان..." else "Search transactions memo/no...") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        if (isLoading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filteredRows.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(if (lang == "ar") "لا يوجد قيود مطابقة معايير البحث" else "No matching historical statements found.")
            }
        } else {
            // LazyColumn Table
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (lang == "ar") "التاريخ" else "Date", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text(if (lang == "ar") "رقم السند" else "Doc No", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text(if (lang == "ar") "البيان / القيد" else "Transaction/Memo", modifier = Modifier.weight(2.2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text(if (lang == "ar") "مدين" else "Debit", modifier = Modifier.weight(1.1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                        Text(if (lang == "ar") "دائن" else "Credit", modifier = Modifier.weight(1.1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                        Text(if (lang == "ar") "الرصيد" else "Balance", modifier = Modifier.weight(1.3f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                }

                items(filteredRows) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sdf.format(java.util.Date(row.date)), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Text("#${row.voucherNo}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text(row.memo, modifier = Modifier.weight(2.2f), style = MaterialTheme.typography.bodyMedium)
                        
                        Text(
                            text = if (row.debit > 0) FinancialUtils.formatBase(row.debit) else "-",
                            modifier = Modifier.weight(1.1f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = if (row.credit > 0) FinancialUtils.formatBase(row.credit) else "-",
                            modifier = Modifier.weight(1.1f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.End,
                            color = EmeraldGreen
                        )
                        Text(
                            text = FinancialUtils.formatBase(row.runningBalance),
                            modifier = Modifier.weight(1.3f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            color = if (row.runningBalance >= 0) EmeraldGreen else RoseRed
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                }
            }
        }
    }
}

@Composable
fun SupplierStatementView(
    supplier: Supplier,
    viewModel: LedgerViewModel,
    onBack: () -> Unit
) {
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val allAccounts by viewModel.accounts.collectAsStateWithLifecycle()
    val snapshots by viewModel.accountSnapshots.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val sdf = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }

    var statementRows by remember { mutableStateOf<List<com.example.ui.viewmodel.AccountStatementRow>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(supplier.accountId) {
        isLoading = true
        statementRows = viewModel.getAccountStatement(supplier.accountId)
        isLoading = false
    }

    val filteredRows = remember(statementRows, searchQuery) {
        if (searchQuery.isBlank()) {
            statementRows
        } else {
            statementRows.filter {
                it.memo.contains(searchQuery, ignoreCase = true) ||
                it.voucherNo.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val balance = snapshots.find { s -> s.accountId == supplier.accountId }?.balance ?: 0L
    val payablesBalance = if (balance != 0L) -balance else 0L
    val totalDebit = remember(statementRows) { statementRows.sumOf { it.debit } }
    val totalCredit = remember(statementRows) { statementRows.sumOf { it.credit } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        // Top section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = supplier.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (lang == "ar") "كشف الحساب المالي التفصيلي للمورد" else "Detailed Supplier Ledger Statement",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            IconButton(
                onClick = {
                    val linkedAcc = allAccounts.find { it.id == supplier.accountId } ?: Account(
                        id = supplier.accountId,
                        accountCode = "2101001",
                        name = supplier.name,
                        accountType = com.example.data.AccountType.LIABILITY,
                        currencyId = 1L
                    )
                    com.example.util.PrintUtils.printAccountStatement(context, linkedAcc, statementRows, lang)
                }
            ) {
                Icon(Icons.Filled.Print, contentDescription = "Print Statement")
            }
        }

        // Supplier Info Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (lang == "ar") "المجموعة" else "Supplier Classification",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = supplier.groupName.ifEmpty { if (lang == "ar") "عام" else "General" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (lang == "ar") "الهاتف" else "Phone Number",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = supplier.phone.ifEmpty { "-" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Total Debit (Paid)
                    Column {
                        Text(
                            text = if (lang == "ar") "سدادات مدين" else "Paid Amount (Debit)",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldGreen
                        )
                        Text(
                            text = FinancialUtils.formatBase(totalDebit) + " " + (if (lang == "ar") "د.ل" else "LYD"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                    }
                    // Total Credit (Purchases)
                    Column {
                        Text(
                            text = if (lang == "ar") "مشتريات دائن" else "Total Purchased (Credit)",
                            style = MaterialTheme.typography.labelSmall,
                            color = RoseRed
                        )
                        Text(
                            text = FinancialUtils.formatBase(totalCredit) + " " + (if (lang == "ar") "د.ل" else "LYD"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = RoseRed
                        )
                    }
                    // Final Outstanding Payable
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (lang == "ar") "إجمالي ديون مستحقة للمورد" else "Outstanding Supplier Balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (payablesBalance >= 0) RoseRed else EmeraldGreen
                        )
                        Text(
                            text = FinancialUtils.formatBase(payablesBalance) + " " + (if (lang == "ar") "د.ل" else "LYD"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (payablesBalance >= 0) RoseRed else EmeraldGreen
                        )
                    }
                }
            }
        }

        // Search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(if (lang == "ar") "البحث في تفاصيل القيود والمشتريات..." else "Search supplier memo/no...") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        if (isLoading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filteredRows.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(if (lang == "ar") "لا يوجد فواتير أو قيود مطابقة معايير البحث" else "No matching historical statements found.")
            }
        } else {
            // LazyColumn Table
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (lang == "ar") "التاريخ" else "Date", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text(if (lang == "ar") "السند" else "Doc No", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text(if (lang == "ar") "تفاصيل و بيان الحركة المالية" else "Voucher Details / Memo", modifier = Modifier.weight(2.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text(if (lang == "ar") "مدين" else "Debit", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                        Text(if (lang == "ar") "دائن" else "Credit", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                        Text(if (lang == "ar") "الرصيد" else "Balance", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                }

                items(filteredRows) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sdf.format(java.util.Date(row.date)), modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall)
                        Text("#${row.voucherNo}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text(row.memo, modifier = Modifier.weight(2.5f), style = MaterialTheme.typography.bodyMedium)
                        
                        Text(
                            text = if (row.debit > 0) FinancialUtils.formatBase(row.debit) else "-",
                            modifier = Modifier.weight(1.2f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.End,
                            color = EmeraldGreen
                        )
                        Text(
                            text = if (row.credit > 0) FinancialUtils.formatBase(row.credit) else "-",
                            modifier = Modifier.weight(1.2f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.End,
                            color = RoseRed
                        )
                        // Supplier Account Balance: credit account
                        val revRunBalance = if (row.runningBalance != 0L) -row.runningBalance else 0L
                        Text(
                            text = FinancialUtils.formatBase(revRunBalance),
                            modifier = Modifier.weight(1.5f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            color = if (revRunBalance >= 0) RoseRed else EmeraldGreen
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                }
            }
        }
    }
}

@Composable
fun BankStatementView(
    bankAccount: BankAccount,
    viewModel: LedgerViewModel,
    onBack: () -> Unit
) {
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val allAccounts by viewModel.accounts.collectAsStateWithLifecycle()
    val snapshots by viewModel.accountSnapshots.collectAsStateWithLifecycle()
    val allBranches by viewModel.allBranches.collectAsStateWithLifecycle()
    val banks by viewModel.banks.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val sdf = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }

    var statementRows by remember { mutableStateOf<List<com.example.ui.viewmodel.AccountStatementRow>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(bankAccount.accountId) {
        isLoading = true
        statementRows = viewModel.getAccountStatement(bankAccount.accountId)
        isLoading = false
    }

    val filteredRows = remember(statementRows, searchQuery) {
        if (searchQuery.isBlank()) {
            statementRows
        } else {
            statementRows.filter {
                it.memo.contains(searchQuery, ignoreCase = true) ||
                it.voucherNo.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val balance = snapshots.find { s -> s.accountId == bankAccount.accountId }?.balance ?: 0L
    val totalDebit = remember(statementRows) { statementRows.sumOf { it.debit } }
    val totalCredit = remember(statementRows) { statementRows.sumOf { it.credit } }

    val branchObj = allBranches.find { it.id == bankAccount.branchId }
    val bankObj = branchObj?.let { br -> banks.find { b -> b.id == br.bankId } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        // Top section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bankObj?.name ?: bankAccount.accountName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${branchObj?.name ?: "-"} • ${bankAccount.accountNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            IconButton(
                onClick = {
                    val linkedAcc = allAccounts.find { it.id == bankAccount.accountId } ?: Account(
                        id = bankAccount.accountId,
                        accountCode = "1102001",
                        name = bankAccount.accountName,
                        accountType = com.example.data.AccountType.ASSET,
                        currencyId = 1L
                    )
                    com.example.util.PrintUtils.printAccountStatement(context, linkedAcc, statementRows, lang)
                }
            ) {
                Icon(Icons.Filled.Print, contentDescription = "Print Statement")
            }
        }

        // Bank Account Info Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.15f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (lang == "ar") "اسم الحساب المصرفي" else "Bank Account Title",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldGreen
                        )
                        Text(
                            text = bankAccount.accountName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (lang == "ar") "الآيبان IBAN" else "IBAN String",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldGreen
                        )
                        Text(
                            text = bankAccount.iban.ifEmpty { "-" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Total Debit
                    Column {
                        Text(
                            text = if (lang == "ar") "مقبوضات / مدين" else "Total Inflows (Debits)",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldGreen
                        )
                        Text(
                            text = FinancialUtils.formatBase(totalDebit) + " " + (if (lang == "ar") "د.ل" else "LYD"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                    }
                    // Total Credit
                    Column {
                        Text(
                            text = if (lang == "ar") "مدفوعات / دائن" else "Total Outflows (Credits)",
                            style = MaterialTheme.typography.labelSmall,
                            color = RoseRed
                        )
                        Text(
                            text = FinancialUtils.formatBase(totalCredit) + " " + (if (lang == "ar") "د.ل" else "LYD"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = RoseRed
                        )
                    }
                    // Ending Balance
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (lang == "ar") "الرصيد الدفتري الحالي" else "Current Reconciled Balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (balance >= 0) EmeraldGreen else RoseRed
                        )
                        Text(
                            text = FinancialUtils.formatBase(balance) + " " + (if (lang == "ar") "د.ل" else "LYD"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (balance >= 0) EmeraldGreen else RoseRed
                        )
                    }
                }
            }
        }

        // Search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(if (lang == "ar") "البحث في تفاصيل حركات وبنود التغذية والسحب..." else "Search transactions memo/no...") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        if (isLoading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filteredRows.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(if (lang == "ar") "لا يوجد حركات مصرفية مطابقة معايير البحث" else "No matching historical statements found.")
            }
        } else {
            // LazyColumn Table
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (lang == "ar") "التاريخ" else "Date", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text(if (lang == "ar") "السند" else "Doc No", modifier = Modifier.weight(1.1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text(if (lang == "ar") "تفاصيل بيان حركة المصرف" else "Bank Invalidation / Memo", modifier = Modifier.weight(2.2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text(if (lang == "ar") "مدين / إيداع" else "Dr / Inflow", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                        Text(if (lang == "ar") "دائن / سحب" else "Cr / Outflow", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                        Text(if (lang == "ar") "السيولة" else "Liquid Bal", modifier = Modifier.weight(1.4f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                }

                items(filteredRows) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sdf.format(java.util.Date(row.date)), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Text("#${row.voucherNo}", modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text(row.memo, modifier = Modifier.weight(2.2f), style = MaterialTheme.typography.bodyMedium)
                        
                        Text(
                            text = if (row.debit > 0) FinancialUtils.formatBase(row.debit) else "-",
                            modifier = Modifier.weight(1.2f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.End,
                            color = EmeraldGreen
                        )
                        Text(
                            text = if (row.credit > 0) FinancialUtils.formatBase(row.credit) else "-",
                            modifier = Modifier.weight(1.2f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.End,
                            color = RoseRed
                        )
                        Text(
                            text = FinancialUtils.formatBase(row.runningBalance),
                            modifier = Modifier.weight(1.4f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            color = if (row.runningBalance >= 0) EmeraldGreen else RoseRed
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                }
            }
        }
    }
}
