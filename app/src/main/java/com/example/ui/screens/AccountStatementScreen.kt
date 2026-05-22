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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.Account
import com.example.data.AccountType
import com.example.ui.Localization
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.ui.viewmodel.AccountStatementRow
import com.example.ui.viewmodel.LedgerViewModel
import com.example.util.FinancialUtils
import com.example.util.PrintUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountStatementScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val isLibyan by viewModel.isLibyanMode.collectAsState()
    val allAccounts by viewModel.accounts.collectAsState()
    
    // Filter down to only non-group accounts (leaf accounts) since those receive financial transactions, or let them pick of both!
    // Since getAccountStatement handles recursive group consolidations (WHICH IS MAJESTIC!), let's allow selecting ALL accounts!
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var statementRows by remember { mutableStateOf<List<AccountStatementRow>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    
    var dropdownExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val filteredAccounts = remember(allAccounts, searchQuery) {
        if (searchQuery.isBlank()) {
            allAccounts
        } else {
            allAccounts.filter {
                it.accountCode.contains(searchQuery, ignoreCase = true) ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                Localization.getAccountName(it.accountCode, it.name, lang).contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(selectedAccount, allAccounts) {
        selectedAccount?.let { acc ->
            loading = true
            statementRows = viewModel.getAccountStatement(acc.id)
            loading = false
        } ?: run {
            statementRows = emptyList()
        }
    }

    fun forceRefresh() {
        selectedAccount?.let { acc ->
            scope.launch {
                loading = true
                statementRows = viewModel.getAccountStatement(acc.id)
                loading = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and intro
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Localization.translate(Localization.Key.VIEW_STATEMENT, lang),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (lang == "ar") "البحث المساعد واستخراج كشوفات الأرصدة التفصيلية بنظام ترحيل دفتر الأستاذ والمطابقة." else "Search, consolidate, and print comprehensive sub-ledger transactions for any account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
            
            if (selectedAccount != null && statementRows.isNotEmpty()) {
                Button(
                    onClick = {
                        selectedAccount?.let { acc ->
                            PrintUtils.printAccountStatement(context, acc, statementRows, lang)
                        }
                    },
                    modifier = Modifier.testTag("print_statement_direct_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (lang == "ar") "طباعة كشف الحساب" else "Print Statement")
                }
            }
        }

        // Selection Cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (lang == "ar") "حدد الحساب المحاسبي المطلوب معاينته:" else "Select target accounting ledger component:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Select Box drop field
                Box(modifier = Modifier.fillMaxWidth()) {
                    val boxValueText = selectedAccount?.let {
                        "${it.accountCode} - ${Localization.getAccountName(it.accountCode, it.name, lang)} ${if (it.isGroup) (if (lang == "ar") "(حساب رئيسي مجلد)" else "(Group Account)") else ""}"
                    } ?: (if (lang == "ar") "--- اضغط هنا لاختيار حساب محاسبي ---" else "--- Click to select target account ---")

                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dropdownExpanded = true }
                            .testTag("statement_account_selector"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = boxValueText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(Icons.Filled.ArrowDropDown, null)
                        }
                    }

                    // Popup Selection Menu with Filter search box inside
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .heightIn(max = 400.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                leadingIcon = { Icon(Icons.Filled.Search, null) },
                                placeholder = { Text(if (lang == "ar") "اكتب اسم الحساب أو الرمز للتصفية..." else "Type code or account name to filter...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("statement_filter_query_input")
                            )
                        }
                        
                        DropdownMenuItem(
                            text = { Text(if (lang == "ar") "إلغاء التحديد وتصفير الكشف" else "Clear Selection") },
                            onClick = {
                                selectedAccount = null
                                dropdownExpanded = false
                            }
                        )
                        
                        HorizontalDivider()

                        filteredAccounts.forEach { acc ->
                            val isGroupPrefix = if (acc.isGroup) "📁 " else "📄 "
                            val typeLabel = when (acc.accountType) {
                                AccountType.ASSET -> if (lang == "ar") "أصول" else "ASSET"
                                AccountType.LIABILITY -> if (lang == "ar") "التزامات" else "LIABILITY"
                                AccountType.EQUITY -> if (lang == "ar") "حقوق ملكية" else "EQUITY"
                                AccountType.REVENUE -> if (lang == "ar") "إيرادات" else "REVENUE"
                                AccountType.EXPENSE -> if (lang == "ar") "مصروفات" else "EXPENSE"
                            }
                            DropdownMenuItem(
                                text = {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("$isGroupPrefix${acc.accountCode} - ${Localization.getAccountName(acc.accountCode, acc.name, lang)}")
                                        Text("($typeLabel)", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                onClick = {
                                    selectedAccount = acc
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Ledger Statement Results Table View
        if (selectedAccount == null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (lang == "ar") "يرجى اختيار الحساب المطلوب لعرض الحركات الدفترية" else "Please select an account to populate current ledger rows",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (loading) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Stats summary card
            val totalDebit = remember(statementRows) { statementRows.sumOf { it.debit } }
            val totalCredit = remember(statementRows) { statementRows.sumOf { it.credit } }
            val finalBalance = remember(statementRows) { statementRows.lastOrNull()?.runningBalance ?: 0L }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(if (lang == "ar") "إجمالي المدين (Dr)" else "Total Debit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        Text(FinancialUtils.formatBase(totalDebit), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = RoseRed.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(if (lang == "ar") "إجمالي الدائن (Cr)" else "Total Credit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        Text(FinancialUtils.formatBase(totalCredit), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RoseRed)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = (if (finalBalance >= 0) EmeraldGreen else RoseRed).copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(if (lang == "ar") "الرصيد النهائي" else "Closing Balance", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = FinancialUtils.formatBase(finalBalance),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (finalBalance >= 0) EmeraldGreen else RoseRed
                        )
                    }
                }
            }

            if (statementRows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = Localization.translate(Localization.Key.NO_STATEMENT_TXS, lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Table header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (lang == "ar") "تاريخ ومستند" else "Date / Vchr", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    Text(if (lang == "ar") "شرح البيان والملاحظات" else "Narration Memo", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    Text(if (lang == "ar") "المدين (Dr)" else "Debit (Dr)", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                    Text(if (lang == "ar") "الدائن (Cr)" else "Credit (Cr)", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                    Text(if (lang == "ar") "الرصيد د.ل" else "Bal (Base)", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                ) {
                    items(statementRows) { row ->
                        val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                        val rowDateText = sdf.format(Date(row.date))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text(rowDateText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Text(row.voucherNo, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(row.memo, modifier = Modifier.weight(2f), style = MaterialTheme.typography.bodySmall)

                            Text(if (row.debit > 0) FinancialUtils.formatBase(row.debit) else "-", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                            Text(if (row.credit > 0) FinancialUtils.formatBase(row.credit) else "-", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)

                            val balColor = if (row.runningBalance >= 0) EmeraldGreen else RoseRed
                            Text(
                                text = FinancialUtils.formatBase(row.runningBalance),
                                modifier = Modifier.weight(1.2f),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = balColor,
                                textAlign = TextAlign.End
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                    }
                }
            }
        }
    }
}
