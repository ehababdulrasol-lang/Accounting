package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Account
import com.example.data.AccountType
import com.example.ui.Localization

@Composable
fun AccountSearchDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    accounts: List<Account>,
    lang: String,
    onSelect: (Account) -> Unit
) {
    if (!show) return
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, accounts) {
        accounts.filter { acc ->
            acc.accountCode.contains(query, ignoreCase = true) ||
            Localization.getAccountName(acc.accountCode, acc.name, lang).contains(query, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (lang == "ar") "البحث واختيار حساب" else "Search & Select Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(if (lang == "ar") "اكتب اسم الحساب أو الرمز..." else "Type account name or code...") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true,
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    if (filtered.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (lang == "ar") "لا توجد نتائج مطابقة" else "No matching results",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                    items(filtered.size) { idx ->
                        val acc = filtered[idx]
                        val localizedName = Localization.getAccountName(acc.accountCode, acc.name, lang)
                        val typeLabel = when (acc.accountType) {
                            AccountType.ASSET -> if (lang == "ar") "أصول" else "ASSET"
                            AccountType.LIABILITY -> if (lang == "ar") "التزامات" else "LIABILITY"
                            AccountType.EQUITY -> if (lang == "ar") "حقوق ملكية" else "EQUITY"
                            AccountType.REVENUE -> if (lang == "ar") "إيرادات" else "REVENUE"
                            AccountType.EXPENSE -> if (lang == "ar") "مصروفات" else "EXPENSE"
                        }
                        DropdownMenuItem(
                            text = { Text("${acc.accountCode} - $localizedName ($typeLabel)") },
                            onClick = {
                                onSelect(acc)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (lang == "ar") "إلغاء" else "Cancel")
            }
        }
    )
}
