package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.app.Activity
import android.content.Intent
import java.io.File
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.Currency
import com.example.data.FiscalYear
import com.example.ui.Localization
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.ui.theme.ThemeStyle
import com.example.ui.viewmodel.LedgerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val fiscalYears by viewModel.fiscalYears.collectAsStateWithLifecycle()
    val fullLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val currencies by viewModel.currencies.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val localBackups by viewModel.localBackups.collectAsStateWithLifecycle()

    var simulatedUserIsAdmin by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.refreshLocalBackups(context)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupToUri(context, uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackupFromUri(context, uri) {
                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent)
                if (context is Activity) {
                    context.finish()
                }
            }
        }
    }

    var selectedBackupForAction by remember { mutableStateOf<File?>(null) }
    var showCreateFyDialog by remember { mutableStateOf(false) }

    // Currency Form State
    var currencyCode by remember { mutableStateOf("") }
    var currencyName by remember { mutableStateOf("") }
    var currencyDecimals by remember { mutableStateOf("2") }
    var activeTab by remember { mutableStateOf(0) }

    val direction = Localization.getLayoutDirection(lang)

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
        ) {
            // Screen Header Content
            Text(
                text = Localization.translate(Localization.Key.COMPLIANCE_SETTINGS, lang),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (lang == "ar") "قم بضبط لغة واجهة المستخدم، وإضافة العملات الأجنبية، وقفل الفترات الحسابية." else "Configure system locales, add foreign currencies, and control locked fiscal periods.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Dynamic User Role Selector Board
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (lang == "ar") "محاكاة صلاحيات المستخدم (Role-based UI)" else "User Authorization Simulation (Role-based UI)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (simulatedUserIsAdmin) EmeraldGreen.copy(alpha = 0.15f)
                                    else Color(0xFFFFB300).copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (simulatedUserIsAdmin) {
                                    if (lang == "ar") "مدير النظام" else "Senior Admin"
                                } else {
                                    if (lang == "ar") "محاسب مبتدئ" else "Junior Accountant"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (simulatedUserIsAdmin) EmeraldGreen else Color(0xFFD84315)
                            )
                        }
                    }

                    Text(
                        text = if (lang == "ar") {
                            "اختر دورًا تاليًا لرؤية آلية إخفاء وتأمين الإعدادات الحساسة (مثل تكوين العملة، قفل الدورة الحسابية، وحذف/استرجاع قواعد البيانات) بنسق يحمل طابع حذر آمن."
                        } else {
                            "Select a role to see how sensitive operations are dynamically secured. Selecting 'Junior Accountant' locks database controls and currency setup, rendering them into custom compliance alert states."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { simulatedUserIsAdmin = true },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (simulatedUserIsAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                if (lang == "ar") "مدير النظام" else "Senior Admin",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { simulatedUserIsAdmin = false },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!simulatedUserIsAdmin) Color(0xFFF4511E) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!simulatedUserIsAdmin) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(
                                if (lang == "ar") "محاسب مبتدئ" else "Junior Acc",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Category Navigation Pills for perfect organization
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf(
                    Triple(0, if (lang == "ar") "المظهر" else "Theme", Icons.Filled.Palette),
                    Triple(1, if (lang == "ar") "العملات والصرف" else "FX & Rates", Icons.Filled.MonetizationOn),
                    Triple(2, if (lang == "ar") "الدورات والنسخ" else "Data & Periods", Icons.Filled.Backup),
                    Triple(3, if (lang == "ar") "التدقيق" else "Audit Log", Icons.Filled.FactCheck)
                )

                tabs.forEach { (index, title, icon) ->
                    val isSelected = activeTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { activeTab = index }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Crossfade transitions for smooth tab switching
            Crossfade(
                targetState = activeTab,
                modifier = Modifier.weight(1f),
                label = "settings_tabs_fade"
            ) { currentTab ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    when (currentTab) {
                        0 -> {
                            // TAB 0: INTERFACE & APPEARANCE
                            // Language Card
                            item {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("language_card"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Translate, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = Localization.translate(Localization.Key.LANGUAGE, lang),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            // Arabic Option
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (lang == "ar") MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                                    .border(1.dp, if (lang == "ar") MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                    .clickable { viewModel.setLanguage("ar") }
                                                    .padding(12.dp)
                                                    .testTag("lang_toggle_ar"),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                RadioButton(selected = lang == "ar", onClick = { viewModel.setLanguage("ar") })
                                                Spacer(Modifier.width(8.dp))
                                                Text("العربية (RTL)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            }

                                            // English Option
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (lang == "en") MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                                    .border(1.dp, if (lang == "en") MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                    .clickable { viewModel.setLanguage("en") }
                                                    .padding(12.dp)
                                                    .testTag("lang_toggle_en"),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                RadioButton(selected = lang == "en", onClick = { viewModel.setLanguage("en") })
                                                Spacer(Modifier.width(8.dp))
                                                Text("English (LTR)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }
                                }
                            }

                            // Accounting Mode Toggle Card
                            item {
                                val isLibyan by viewModel.isLibyanMode.collectAsStateWithLifecycle()
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("accounting_mode_card"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.AccountBalance, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = Localization.translate(Localization.Key.ACCOUNTING_MODE, lang),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Text(
                                            text = Localization.translate(Localization.Key.LIBYAN_MODE_DESC, lang),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            // Libyan Mode Option
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isLibyan) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                                    .border(1.dp, if (isLibyan) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                    .clickable { viewModel.setLibyanMode(true) }
                                                    .padding(12.dp)
                                                    .testTag("mode_toggle_libyan"),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                RadioButton(selected = isLibyan, onClick = { viewModel.setLibyanMode(true) })
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = if (lang == "ar") "د.ل (الليبي)" else "LYD Style",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }

                                            // Normal Mode Option
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (!isLibyan) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                                    .border(1.dp, if (!isLibyan) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                    .clickable { viewModel.setLibyanMode(false) }
                                                    .padding(12.dp)
                                                    .testTag("mode_toggle_normal"),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                RadioButton(selected = !isLibyan, onClick = { viewModel.setLibyanMode(false) })
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = if (lang == "ar") "العالمي (العادي)" else "Normal Style",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Theme Mode Card
                            item {
                                val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("theme_mode_card"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Contrast, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = Localization.translate(Localization.Key.THEME_MODE, lang),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Text(
                                            text = Localization.translate(Localization.Key.THEME_DESC, lang),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            // Dark Mode Option
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                                    .border(1.dp, if (isDark) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                    .clickable { viewModel.setDarkMode(true) }
                                                    .padding(12.dp)
                                                    .testTag("theme_toggle_dark"),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                RadioButton(selected = isDark, onClick = { viewModel.setDarkMode(true) })
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = Localization.translate(Localization.Key.DARK_MODE, lang),
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }

                                            // Light Mode Option
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (!isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                                    .border(1.dp, if (!isDark) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                    .clickable { viewModel.setDarkMode(false) }
                                                    .padding(12.dp)
                                                    .testTag("theme_toggle_light"),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                RadioButton(selected = !isDark, onClick = { viewModel.setDarkMode(false) })
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = Localization.translate(Localization.Key.LIGHT_MODE, lang),
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Accent Palette Card
                            item {
                                val currentStyle by viewModel.currentThemeStyle.collectAsStateWithLifecycle()
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("theme_style_card"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Palette, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = if (lang == "ar") "لوحة الألوان ونسق الهوية البصرية" else "Accent Palette & Visual Theme",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Text(
                                            text = if (lang == "ar") "اختر الطابع اللوني لبيئة العمل لتناسب هويتك التجارية وتفضيلاتك البصرية الحسابية." else "Select the primary visual identity scheme to match your brand guidelines and workstation styling preferences.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )

                                        val styles = listOf(
                                            Triple(ThemeStyle.CLASSIC_SKY, if (lang == "ar") "الأزرق الكلاسيكي" else "Classic Sky", Color(0xFF0284C7)),
                                            Triple(ThemeStyle.EMERALD_GOLD, if (lang == "ar") "الأخضر الذهبي" else "Emerald Gold", Color(0xFF0D9488)),
                                            Triple(ThemeStyle.COSMIC_AMETHYST, if (lang == "ar") "البنفسج الكوني" else "Cosmic Purple", Color(0xFF8B5CF6)),
                                            Triple(ThemeStyle.WARM_SAHARA, if (lang == "ar") "نسيم الصحراء (تراثي)" else "Sahara Breeze (Native)", Color(0xFFF59E0B))
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            styles.forEach { (style, name, accentColor) ->
                                                val isSelected = currentStyle == style
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                                        .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                        .clickable { viewModel.setThemeStyle(style) }
                                                        .padding(8.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(accentColor)
                                                    )
                                                    Spacer(Modifier.height(4.dp))
                                                    Text(
                                                        text = name,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // TAB 1: CURRENCY & EXCHANGE SIMULATOR
                            // Currencies Settings Card
                            item {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("currencies_card"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.MonetizationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = Localization.translate(Localization.Key.ADD_CURRENCY, lang),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        if (!simulatedUserIsAdmin) {
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = Color(0xFFFFF3E0)
                                                ),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                                                    Spacer(Modifier.width(10.dp))
                                                    Text(
                                                        text = if (lang == "ar") {
                                                            "تكوين العملات وأسعار الصرف مقيد. عذرًا، لا تملك صلاحية مدير النظام حاليًا لتعديل هذا الجزء السيادي."
                                                        } else {
                                                            "Currency parameters are read-only. Your current Junior Accountant profile does not permit adding new master currencies."
                                                        },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF5D4037),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }

                                        // Form Fields
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = currencyCode,
                                                onValueChange = { if (it.length <= 4) currencyCode = it },
                                                label = { Text(Localization.translate(Localization.Key.CURRENCY_CODE, lang)) },
                                                modifier = Modifier.weight(1f).testTag("curr_input_code"),
                                                singleLine = true,
                                                enabled = simulatedUserIsAdmin
                                            )

                                            OutlinedTextField(
                                                value = currencyName,
                                                onValueChange = { currencyName = it },
                                                label = { Text(Localization.translate(Localization.Key.NAME, lang)) },
                                                modifier = Modifier.weight(1.5f).testTag("curr_input_name"),
                                                singleLine = true,
                                                enabled = simulatedUserIsAdmin
                                            )

                                            OutlinedTextField(
                                                value = currencyDecimals,
                                                onValueChange = { currencyDecimals = it },
                                                label = { Text(Localization.translate(Localization.Key.DECIMAL_PLACES, lang)) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f).testTag("curr_input_decimals"),
                                                singleLine = true,
                                                enabled = simulatedUserIsAdmin
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                val decs = currencyDecimals.toIntOrNull() ?: 2
                                                viewModel.addCurrency(currencyCode, currencyName, decs)
                                                currencyCode = ""
                                                currencyName = ""
                                                currencyDecimals = "2"
                                            },
                                            enabled = simulatedUserIsAdmin && currencyCode.isNotBlank() && currencyName.isNotBlank(),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.align(Alignment.End).testTag("curr_btn_add")
                                        ) {
                                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(Localization.translate(Localization.Key.ADD, lang))
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), thickness = 0.5.dp)

                                        // Currencies view list
                                        Text(
                                            text = Localization.translate(Localization.Key.CURRENCY_OVERVIEW, lang),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray
                                        )

                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            currencies.forEach { curr ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                                        .padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(curr.code, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                                                        }
                                                        Spacer(Modifier.width(10.dp))
                                                        Text(curr.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                                    }

                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        if (curr.isBase) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(EmeraldGreen.copy(alpha = 0.15f))
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(Localization.translate(Localization.Key.BASE_CURRENCY, lang), color = EmeraldGreen, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                        Text(
                                                            text = "Scale: ${curr.decimalPlaces}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // FX Exchange & Parallel market simulator
                            item {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("exchange_calculator_card"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                ) {
                                    var conversionAmount by remember { mutableStateOf("100") }
                                    var fromLYD by remember { mutableStateOf(false) }
                                    var useParallelRate by remember { mutableStateOf(false) }

                                    val rate = if (useParallelRate) 7.15 else 4.82
                                    val amountVal = conversionAmount.toDoubleOrNull() ?: 0.0
                                    val result = if (fromLYD) {
                                        amountVal / rate
                                    } else {
                                        amountVal * rate
                                    }

                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Calculate, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = if (lang == "ar") "محرر ومحاكي أسعار الصرف (رسمي وموازي)" else "FX Rate Converter & Simulator",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Text(
                                            text = if (lang == "ar") "محاكي عملي للتحويل الفوري بين الدينار الليبي (د.ل) والدولار الأمريكي بأسعار مصرف ليبيا المركزي الرسمية أو أسعار السوق الموازي بشارع الصريم وميدان الشهداء."
                                                   else "Simulate instant FX translation between Libyan Dinar (LYD) and US Dollars (USD) according to official central bank (CBL) caps or active commercial parallel markets.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )

                                        OutlinedTextField(
                                            value = conversionAmount,
                                            onValueChange = { conversionAmount = it },
                                            label = { Text(if (lang == "ar") "المقدار / المبلغ المراد تحويله" else "Transaction Amount to Convert") },
                                            leadingIcon = { Icon(Icons.Filled.Money, null, tint = MaterialTheme.colorScheme.primary) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = { fromLYD = !fromLYD },
                                                modifier = Modifier.weight(1.5f),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                                    contentColor = MaterialTheme.colorScheme.primary
                                                )
                                            ) {
                                                Icon(Icons.Filled.SwapHoriz, null)
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = if (fromLYD) {
                                                        if (lang == "ar") "من د.ل إلى دولار ($)" else "LYD ➔ USD ($)"
                                                    } else {
                                                        if (lang == "ar") "من دولار ($) إلى د.ل" else "USD ($) ➔ LYD"
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            FilterChip(
                                                selected = useParallelRate,
                                                onClick = { useParallelRate = !useParallelRate },
                                                label = {
                                                    Text(
                                                        text = if (lang == "ar") "السوق الموازي" else "Parallel Market",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                },
                                                leadingIcon = {
                                                    if (useParallelRate) {
                                                        Icon(Icons.Filled.Check, null, modifier = Modifier.size(14.dp))
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                                .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                .padding(12.dp)
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = if (lang == "ar") "سعر الصرف النشط:" else "Active Exchange Rate:",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                    Text(
                                                        text = "1 USD = ${java.text.DecimalFormat("#.##").format(rate)} LYD",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = if (lang == "ar") "التقدير الإجمالي الناتج:" else "Calculated Translation Yield:",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = if (fromLYD) {
                                                            java.text.DecimalFormat("$#,##0.00").format(result)
                                                        } else {
                                                            java.text.DecimalFormat("#,##0.000").format(result) + " LYD"
                                                        },
                                                        fontWeight = FontWeight.ExtraBold,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = EmeraldGreen
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // TAB 2: FISCAL PERIODS & DATA BACKUPS
                            // Periods Manager Card
                            item {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("fiscal_years_card"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = Localization.translate(Localization.Key.FISCAL_PERIODS, lang),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            TextButton(
                                                onClick = { if (simulatedUserIsAdmin) showCreateFyDialog = true },
                                                modifier = Modifier.testTag("btn_add_fy"),
                                                enabled = simulatedUserIsAdmin
                                            ) {
                                                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(if (lang == "ar") "إضافة فترة" else "Add Period")
                                            }
                                        }

                                        if (fiscalYears.isEmpty()) {
                                            Text("No periods found.", style = MaterialTheme.typography.bodyMedium)
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                fiscalYears.forEach { fy ->
                                                    FiscalYearPeriodRow(
                                                        fy = fy,
                                                        onToggleLock = { viewModel.toggleFiscalYearLock(fy.id, fy.isLocked) },
                                                        lang = lang,
                                                        enabled = simulatedUserIsAdmin
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // DB Backups & Cloud Card
                            item {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("backup_restore_card"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Backup, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = if (lang == "ar") "النسخ الاحتياطي واستعادة البيانات" else "Backup & Data Recovery",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        if (!simulatedUserIsAdmin) {
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = Color(0xFFFFEBEE)
                                                ),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF9A9A)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Filled.Dangerous, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(22.dp))
                                                    Spacer(Modifier.width(10.dp))
                                                    Text(
                                                        text = if (lang == "ar") {
                                                            "⚠️ تنبيه حماية النظام: الصلاحيات الحالية مقيدة بنمط القراءة فقط للمحاسبين المبتدئين. عمليات حذف قواعد البيانات أو استعادة البيانات الاحتياطية تتطلب صلاحيات مدير نظام لمنع التلاعب وتخريب السجلات الحساسة للشركة."
                                                        } else {
                                                            "⚠️ System Safeguard: Database recovery operations are restricted to Senior Admins under active ERP policy."
                                                        },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF37474F),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = if (lang == "ar") "قم بحفظ بياناتك المالية في مساحة آمنة داخل تطبيق الجهاز لتجنب فقد السجلات، أو قم بتصدير واستيراد قواعد بياناتك كملفات خارجية في أي وقت." 
                                                   else "Secure your ledgers locally on this device, or export/import database snapshots to prevent data loss or migrate bookkeeping logs.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )

                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.createBackup(context) },
                                                modifier = Modifier.fillMaxWidth().testTag("btn_create_local_backup"),
                                                shape = RoundedCornerShape(8.dp),
                                                enabled = simulatedUserIsAdmin
                                            ) {
                                                Icon(Icons.Filled.CloudUpload, null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text(if (lang == "ar") "إنشاء نسخة احتياطية محلية فورية" else "Create Instant Local Backup")
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = { exportLauncher.launch("ledger_backup_${System.currentTimeMillis()}.db") },
                                                    modifier = Modifier.weight(1f).testTag("btn_export_backup_saf"),
                                                    shape = RoundedCornerShape(8.dp),
                                                    enabled = simulatedUserIsAdmin
                                                ) {
                                                    Icon(Icons.Filled.Launch, null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(if (lang == "ar") "تصدير كملف خارجي" else "Export File")
                                                }

                                                OutlinedButton(
                                                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                                                    modifier = Modifier.weight(1f).testTag("btn_import_backup_saf"),
                                                    shape = RoundedCornerShape(8.dp),
                                                    enabled = simulatedUserIsAdmin
                                                ) {
                                                    Icon(Icons.Filled.FolderOpen, null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(if (lang == "ar") "استيراد ملف خارجي" else "Import File")
                                                }
                                            }
                                        }

                                        if (localBackups.isNotEmpty()) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), thickness = 0.5.dp)
                                            Text(
                                                text = if (lang == "ar") "نسخك الاحتياطية المخزنة محلياً:" else "Your stored local backups:",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Gray
                                            )

                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                localBackups.forEach { file ->
                                                    val sizeInKB = file.length() / 1024
                                                    val sizeStr = if (sizeInKB > 1024) String.format("%.1f MB", sizeInKB / 1024.0) else "$sizeInKB KB"
                                                    
                                                    val dateStr = try {
                                                        val parts = file.name.removePrefix("ledger_backup_").removeSuffix(".db").split("_")
                                                        if (parts.size >= 2) {
                                                            val d = SimpleDateFormat("yyyyMMdd", Locale.US).parse(parts[0]) ?: throw Exception("Invalid date")
                                                            val t = SimpleDateFormat("HHmmss", Locale.US).parse(parts[1]) ?: throw Exception("Invalid time")
                                                            val formattedD = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(d)
                                                            val formattedT = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(t)
                                                            "$formattedD @ $formattedT"
                                                        } else {
                                                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(file.lastModified()))
                                                        }
                                                    } catch (e: Exception) {
                                                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(file.lastModified()))
                                                    }

                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                                            .clickable(enabled = simulatedUserIsAdmin) { selectedBackupForAction = file }
                                                            .padding(10.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(36.dp)
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(Icons.Filled.SettingsBackupRestore, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                            }
                                                            Spacer(Modifier.width(10.dp))
                                                            Column {
                                                                Text(dateStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                                Text(file.name, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                            }
                                                        }
                                                        Text(sizeStr, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        3 -> {
                            // TAB 3: TRACE AUDIT RECORDS
                            item {
                                Text(
                                    text = Localization.translate(Localization.Key.LOG_TRAIL_IFRS, lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }

                            if (fullLogs.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No compliance audit records.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            } else {
                                items(fullLogs) { log ->
                                    AuditLogRow(log = log)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Fiscal period dialogue
        if (showCreateFyDialog) {
            CreateFiscalYearDialog(
                onDismiss = { showCreateFyDialog = false },
                onSave = { name, start, end ->
                    viewModel.addFiscalYear(name, start, end)
                    showCreateFyDialog = false
                },
                lang = lang
            )
        }

        // Local Backup Actions Confirmation Dialog
        selectedBackupForAction?.let { file ->
            AlertDialog(
                onDismissRequest = { selectedBackupForAction = null },
                title = {
                    Text(
                        text = if (lang == "ar") "إدارة الملف الاحتياطي" else "Manage Backup File",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (lang == "ar") "الاسم: ${file.name}" else "Name: ${file.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (lang == "ar") 
                                "الرجاء تأكيد الإجراء المطلوب. استعادة البيانات ستستبدل قاعدة البيانات الحالية بكاملها وتقوم بإعادة تشغيل التطبيق لتطبيق التغييرات." 
                                else "Please choose an action. Restoring will overwrite all active ledger registers and reboot the system to commit database changes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            colors = ButtonDefaults.textButtonColors(contentColor = RoseRed),
                            onClick = {
                                viewModel.deleteBackup(context, file)
                                selectedBackupForAction = null
                            }
                        ) {
                            Icon(Icons.Filled.Delete, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (lang == "ar") "حذف" else "Delete")
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { selectedBackupForAction = null }) {
                                Text(Localization.translate(Localization.Key.CANCEL, lang))
                            }

                            Button(
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                                onClick = {
                                    viewModel.restoreBackup(context, file) {
                                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                        context.startActivity(intent)
                                        if (context is Activity) {
                                            context.finish()
                                        }
                                    }
                                    selectedBackupForAction = null
                                }
                            ) {
                                Icon(Icons.Filled.SettingsBackupRestore, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (lang == "ar") "استعادة" else "Restore")
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun FiscalYearPeriodRow(
    fy: FiscalYear,
    onToggleLock: () -> Unit,
    lang: String,
    enabled: Boolean = true
) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val startStr = remember(fy.startDate) { formatter.format(Date(fy.startDate)) }
    val endStr = remember(fy.endDate) { formatter.format(Date(fy.endDate)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fy.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$startStr  to  $endStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (fy.isLocked) RoseRed.copy(alpha = 0.15f)
                    else EmeraldGreen.copy(alpha = 0.15f)
                )
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (fy.isLocked) Localization.translate(Localization.Key.LOCKED, lang) else Localization.translate(Localization.Key.ACTIVE, lang),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (fy.isLocked) RoseRed else EmeraldGreen
            )
        }

        IconButton(
            onClick = onToggleLock,
            modifier = Modifier.testTag("toggle_fiscal_lock_${fy.name}"),
            enabled = enabled
        ) {
            Icon(
                imageVector = if (fy.isLocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                contentDescription = null,
                tint = if (fy.isLocked) EmeraldGreen else RoseRed
            )
        }
    }
}

@Composable
fun CreateFiscalYearDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, start: Long, end: Long) -> Unit,
    lang: String
) {
    var name by remember { mutableStateOf("") }
    var startDay by remember { mutableStateOf("2026-01-01") }
    var endDay by remember { mutableStateOf("2026-12-31") }

    val formatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (lang == "ar") "إضافة دورة مالية" else "Add Fiscal Period",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (lang == "ar") "اسم الدورة المالية (مثال: FY 2026)" else "Fiscal Period Name (e.g. FY 2026)") },
                    modifier = Modifier.fillMaxWidth().testTag("add_fy_name_input")
                )

                OutlinedTextField(
                    value = startDay,
                    onValueChange = { startDay = it },
                    label = { Text(Localization.translate(Localization.Key.START_OF_YEAR, lang)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = endDay,
                    onValueChange = { endDay = it },
                    label = { Text(Localization.translate(Localization.Key.END_OF_YEAR, lang)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(Localization.translate(Localization.Key.CANCEL, lang))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                val sLong = formatter.parse(startDay)?.time ?: System.currentTimeMillis()
                                val eLong = formatter.parse(endDay)?.time ?: System.currentTimeMillis()
                                onSave(name, sLong, eLong)
                            } catch (e: Exception) {
                                onSave(name, System.currentTimeMillis(), System.currentTimeMillis() + 31536000000L)
                            }
                        },
                        enabled = name.isNotBlank() && startDay.isNotBlank() && endDay.isNotBlank(),
                        modifier = Modifier.testTag("save_fy_button")
                    ) {
                        Text(Localization.translate(Localization.Key.ADD, lang))
                    }
                }
            }
        }
    }
}
