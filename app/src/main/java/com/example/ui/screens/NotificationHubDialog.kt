package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Notification
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RoseRed
import com.example.ui.theme.CorporateSky
import com.example.ui.theme.CorporateAmethyst
import com.example.ui.viewmodel.LedgerViewModel

// Represents a fast-filling custom alert template for instant multi-lingual testing
private data class AlertTemplate(
    val icon: ImageVector,
    val nameAr: String,
    val nameEn: String,
    val titleAr: String,
    val titleEn: String,
    val msgAr: String,
    val msgEn: String,
    val type: String, // "ADMIN", "TEAM", "SYSTEM"
    val priority: String, // "HIGH", "MEDIUM", "LOW"
    val isInteractive: Boolean,
    val voucherNo: String
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NotificationHubDialog(
    viewModel: LedgerViewModel,
    onDismissRequest: () -> Unit
) {
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadNotifs by viewModel.unreadNotifications.collectAsStateWithLifecycle()

    var showCompose by remember { mutableStateOf(false) }

    // Selected filter: 0 = All, 1 = Admin, 2 = Team, 3 = Yamama Control
    var activeFilter by remember { mutableStateOf(0) }

    val filteredNotifications = remember(notifications, activeFilter) {
        when (activeFilter) {
            1 -> notifications.filter { it.type == "ADMIN" }
            2 -> notifications.filter { it.type == "TEAM" }
            3 -> notifications.filter { it.type == "SYSTEM" }
            else -> notifications
        }
    }

    // Interactive Broadcast Templates
    val alertTemplates = remember {
        listOf(
            AlertTemplate(
                icon = Icons.Filled.RateReview,
                nameAr = "📝 قيد رواتب معلق",
                nameEn = "📝 Payroll Review",
                titleAr = "طلب عاجل: مراجعة وترحيل قيد تسوية رواتب الربع الحالي",
                titleEn = "Urgent Request: Audit and post payroll settlement ledger",
                msgAr = "يرجى من قسم المراجعة المالية التحقق من توازن قيد رواتب الموظفين رقم JV-002 المرفق ومطابقة الحسابات العامة وترحيله فوراً.",
                msgEn = "Auditors are requested to finalize the payroll settlement ledger draft JV-002, matching cost centers with the trial balance.",
                type = "ADMIN",
                priority = "HIGH",
                isInteractive = true,
                voucherNo = "JV-002"
            ),
            AlertTemplate(
                icon = Icons.Filled.Payments,
                nameAr = "💡 تجاوز حد كشك النقدية",
                nameEn = "💡 Vault Threshold",
                titleAr = "تحذير أمان: رصيد الصندوق الرئيسي تجاوز السقف التشغيلي",
                titleEn = "Liquidity Cap Warning: Main cash vault overlimit",
                msgAr = "رصيد السيولة النقدية المتوفرة حالياً بالخزينة الرئيسية تخطى 150,000 د.ل. يرجى إيداع المبالغ الزائدة بالكامل في الحساب الجاري للمصرف للحد من مخاطر الخزانة.",
                msgEn = "Main office cash vault currently holds above 150,000 LYD. Recommend a secure corporate bank deposit to adjust localized threshold risks.",
                type = "SYSTEM",
                priority = "MEDIUM",
                isInteractive = false,
                voucherNo = ""
            ),
            AlertTemplate(
                icon = Icons.Filled.ReportProblem,
                nameAr = "⚠️ حد ائتمان العميل",
                nameEn = "⚠️ Client Limit",
                titleAr = "تحذير ائتماني: العميل شركة المدار تخطى سقف التسهيلات",
                titleEn = "Fiduciary Warning: Client 'Al-Madar' exceeded credit cap",
                msgAr = "تنبيه من وحدة التدقيق: تخطى العميل 'شركة المدار' سقف التسهيلات الائتمانية الآجلة المحددة بـ 50,000 د.ل. يرجى وقف البيع الآجل لحين تحصيل الأقساط.",
                msgEn = "Customer account 'Al-Madar' has breached their maximum safe accounts-receivable limit of 50,000 LYD. Suspend future billing in local database.",
                type = "TEAM",
                priority = "HIGH",
                isInteractive = false,
                voucherNo = ""
            ),
            AlertTemplate(
                icon = Icons.Filled.AccountBalance,
                nameAr = "🏦 مطابقة مصرفية",
                nameEn = "🏦 Bank Recon",
                titleAr = "إخطار: إتمام المعالجة والمطابقة المصرفية لكشف حساب الجمهورية",
                titleEn = "System Audit: Gumhouria Bank statement reconciled",
                msgAr = "تم بنجاح مطابقة الحركات المقيدة لدفتر اليومية مع الكشف الشهري لمصرف الجمهورية وتأكيد الرصيد الفعلي المتطابق للشركة.",
                msgEn = "Monthly reconciliation for Gumhouria Bank accounts completed with zero discrepancies detected. Ledger and statement balances are fully synchronized.",
                type = "SYSTEM",
                priority = "LOW",
                isInteractive = false,
                voucherNo = ""
            )
        )
    }

    // Composer fields
    var composeTitleAr by remember { mutableStateOf("") }
    var composeTitleEn by remember { mutableStateOf("") }
    var composeMessageAr by remember { mutableStateOf("") }
    var composeMessageEn by remember { mutableStateOf("") }
    var composeType by remember { mutableStateOf("TEAM") } // "ADMIN", "TEAM", "SYSTEM"
    var composePriority by remember { mutableStateOf("MEDIUM") } // "HIGH", "MEDIUM", "LOW"
    var composeSenderAr by remember { mutableStateOf("المراجع المالي") }
    var composeSenderEn by remember { mutableStateOf("Senior Auditor") }
    var composeReceiverAr by remember { mutableStateOf("المحاسب المساعد") }
    var composeReceiverEn by remember { mutableStateOf("Junior Accountant") }
    
    // Optional interactive voucher trigger
    var isInteractiveVoucher by remember { mutableStateOf(false) }
    var interactiveVoucherNo by remember { mutableStateOf("JV-002") }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismissRequest), // Dimmer dismiss
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 780.dp) // Professional Tablet Proportions (Strict Width Cap)
                    .fillMaxHeight(0.92f) // Beautiful floating layout sheet
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, GoldAccent.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                    .clickable(enabled = false) { /* Prevent click through */ },
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(GoldAccent.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (lang == "ar") "نظام التنبيهات والتراسل المالي" else "Financial Messaging & Alerts Center",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (lang == "ar") 
                                        "منصة التراسل الفوري والتدقيق الداخلي وبث الإعلانات الإدارية" 
                                    else "Direct team audit logs, real-time compliance directives, and alerts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier
                                .testTag("dismiss_notifications_hub")
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = if (lang == "ar") "إغلاق" else "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(Modifier.height(14.dp))

                    // Global Overview Stats Row & Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Alerts status overview pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (unreadNotifs.isNotEmpty()) RoseRed.copy(alpha = 0.15f) 
                                    else EmeraldGreen.copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (unreadNotifs.isNotEmpty()) RoseRed else EmeraldGreen)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (lang == "ar") {
                                        if (unreadNotifs.isNotEmpty()) "لديك ${unreadNotifs.size} تنبيهات معلقة" else "صندوق الوارد نظيف ومطابق"
                                    } else {
                                        if (unreadNotifs.isNotEmpty()) "${unreadNotifs.size} pending actions" else "Audit Log fully synced"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (unreadNotifs.isNotEmpty()) RoseRed else EmeraldGreen
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (notifications.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.markAllNotificationsAsRead()
                                        viewModel.showMessage("all_notifications_read")
                                    },
                                    modifier = Modifier
                                        .testTag("mark_all_read_button")
                                        .height(38.dp)
                                        .padding(end = 8.dp),
                                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DoneAll,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (lang == "ar") "قراءة الكل" else "Mark All Read",
                                        color = EmeraldGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Button(
                                onClick = { showCompose = !showCompose },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (showCompose) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .testTag("toggle_compose")
                                    .height(38.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Icon(
                                    imageVector = if (showCompose) Icons.Filled.KeyboardArrowUp else Icons.Filled.Campaign,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (showCompose) {
                                        if (lang == "ar") "إغلاق التحرير" else "Close Editor"
                                    } else {
                                        if (lang == "ar") "بث تعميم جديد" else "Broadcast Alert"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Composer Panel (Slide/Fade Animated) with One-Tap Templates
                    AnimatedVisibility(
                        visible = showCompose,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .border(1.dp, GoldAccent.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = if (lang == "ar") "بث إشعار مع جلبه من القوالب السريعة المحمولة" else "Compose Alert or Pick Pre-loaded Audit Template",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = GoldAccent
                                )

                                Spacer(Modifier.height(6.dp))

                                // Fast-Filling Templates Row (Awesome UX Enhancement!)
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    items(alertTemplates) { template ->
                                        OutlinedCard(
                                            onClick = {
                                                composeTitleAr = template.titleAr
                                                composeTitleEn = template.titleEn
                                                composeMessageAr = template.msgAr
                                                composeMessageEn = template.msgEn
                                                composeType = template.type
                                                composePriority = template.priority
                                                isInteractiveVoucher = template.isInteractive
                                                if (template.isInteractive) {
                                                    interactiveVoucherNo = template.voucherNo
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.outlinedCardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                                            ),
                                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = template.icon,
                                                    contentDescription = null,
                                                    tint = GoldAccent,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = if (lang == "ar") template.nameAr else template.nameEn,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                // Dual-language titles
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = composeTitleAr,
                                        onValueChange = { composeTitleAr = it },
                                        label = { Text(if (lang == "ar") "العنوان بالكامل" else "Full Title (AR)") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("compose_title_ar"),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    OutlinedTextField(
                                        value = composeTitleEn,
                                        onValueChange = { composeTitleEn = it },
                                        label = { Text(if (lang == "ar") "Title in English" else "Title (EN)") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("compose_title_en"),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                // Dual-language message fields
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = composeMessageAr,
                                        onValueChange = { composeMessageAr = it },
                                        label = { Text(if (lang == "ar") "الرسالة (العربية)" else "Message text (AR)") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("compose_message_ar"),
                                        maxLines = 3,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    OutlinedTextField(
                                        value = composeMessageEn,
                                        onValueChange = { composeMessageEn = it },
                                        label = { Text(if (lang == "ar") "Message text in English" else "Message text (EN)") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("compose_message_en"),
                                        maxLines = 3,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }

                                Spacer(Modifier.height(10.dp))

                                // Metadata Selectors (Type & Priority)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Segmented Category selector for alert
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (lang == "ar") "تصنيف التوجيه" else "Broadcast Channel",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Start
                                        ) {
                                            listOf(
                                                "ADMIN" to (if (lang == "ar") "إدارة" else "Admin"),
                                                "TEAM" to (if (lang == "ar") "فريق" else "Team"),
                                                "SYSTEM" to (if (lang == "ar") "نظام" else "System")
                                            ).forEach { (typeVal, typeLabel) ->
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.clickable { composeType = typeVal }
                                                ) {
                                                    RadioButton(
                                                        selected = composeType == typeVal,
                                                        onClick = { composeType = typeVal }
                                                    )
                                                    Text(typeLabel, style = MaterialTheme.typography.bodySmall)
                                                    Spacer(Modifier.width(6.dp))
                                                }
                                            }
                                        }
                                    }

                                    // Priority selector
                                    Column(modifier = Modifier.weight(1.2f)) {
                                        Text(
                                            text = if (lang == "ar") "درجة الأهمية" else "Priority",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Start
                                        ) {
                                            listOf(
                                                "HIGH" to "High 🔴",
                                                "MEDIUM" to "Med 🟡",
                                                "LOW" to "Low 🔵"
                                            ).forEach { (priVal, priLabel) ->
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.clickable { composePriority = priVal }
                                                ) {
                                                    RadioButton(
                                                        selected = composePriority == priVal,
                                                        onClick = { composePriority = priVal }
                                                    )
                                                    Text(priLabel, style = MaterialTheme.typography.bodySmall)
                                                    Spacer(Modifier.width(6.dp))
                                                }
                                            }
                                        }
                                    }
                                }

                                // Interactive Ledger Voucher Control Row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Checkbox(
                                        checked = isInteractiveVoucher,
                                        onCheckedChange = { isInteractiveVoucher = it }
                                    )
                                    Text(
                                        text = if (lang == "ar") 
                                            "ربط إجرائي بمراجعة واعتماد قيد محاسبي معلق" 
                                        else 
                                            "Link with interactive double-entry voucher action",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isInteractiveVoucher) {
                                        OutlinedTextField(
                                            value = interactiveVoucherNo,
                                            onValueChange = { interactiveVoucherNo = it },
                                            modifier = Modifier.width(100.dp),
                                            singleLine = true,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        if (composeTitleAr.isBlank() || composeTitleEn.isBlank()) {
                                            viewModel.showMessage("fields_required")
                                            return@Button
                                        }
                                        
                                        viewModel.sendNotification(
                                            titleAr = composeTitleAr,
                                            titleEn = composeTitleEn,
                                            messageAr = composeMessageAr,
                                            messageEn = composeMessageEn,
                                            type = composeType,
                                            priority = composePriority,
                                            senderAr = composeSenderAr,
                                            senderEn = composeSenderEn,
                                            receiverAr = composeReceiverAr,
                                            receiverEn = composeReceiverEn,
                                            actionType = if (isInteractiveVoucher) "APPROVE_VOUCHER" else null,
                                            actionPayload = if (isInteractiveVoucher) interactiveVoucherNo else null
                                        )

                                        // Reset fields
                                        composeTitleAr = ""
                                        composeTitleEn = ""
                                        composeMessageAr = ""
                                        composeMessageEn = ""
                                        showCompose = false
                                        viewModel.showMessage("notification_broadcasted")
                                    },
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .testTag("submit_broadcast"),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                                ) {
                                    Icon(Icons.Filled.Campaign, contentDescription = null, tint = Color.Black)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (lang == "ar") "بث وإرسال التوجيه الآن" else "Broadcast Now",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Filter Tabs Row with Alert Counters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filterLabels = listOf(
                            Pair(0, if (lang == "ar") "الجميع" else "All"),
                            Pair(1, if (lang == "ar") "الإدارة والمجلس" else "Admin"),
                            Pair(2, if (lang == "ar") "فريق العمل" else "Team"),
                            Pair(3, if (lang == "ar") "سجل الرقابة" else "Yamama")
                        )

                        filterLabels.forEach { (index, label) ->
                            val isSelected = activeFilter == index
                            
                            // Dynamic unread count in each specific category
                            val categoryCount = remember(unreadNotifs, index) {
                                when (index) {
                                    0 -> unreadNotifs.size
                                    1 -> unreadNotifs.count { it.type == "ADMIN" }
                                    2 -> unreadNotifs.count { it.type == "TEAM" }
                                    3 -> unreadNotifs.count { it.type == "SYSTEM" }
                                    else -> 0
                                }
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = { activeFilter = index },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(label, fontWeight = FontWeight.Bold)
                                        
                                        if (categoryCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(RoseRed),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = categoryCount.toString(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldAccent.copy(alpha = 0.15f),
                                    selectedLabelColor = GoldAccent
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    selectedBorderColor = GoldAccent,
                                    selectedBorderWidth = 1.dp
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Main Scrollable Notifications List
                    if (filteredNotifications.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.NotificationsOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = if (lang == "ar") "لا توجد تنبيهات معلقة حالياً" else "No pending alerts found",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = if (lang == "ar") 
                                        "صندوق التراسل المالي وطلبات المراجعة فارغ بالكامل للحساب المحدد." 
                                    else 
                                        "No new announcements, compliance breaches, or unposted vouchers exist.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("notifications_list"),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredNotifications, key = { it.id }) { item ->
                                val borderAccent = when (item.priority) {
                                    "HIGH" -> RoseRed
                                    "MEDIUM" -> GoldAccent
                                    else -> CorporateSky
                                }

                                // High contrast design card with priority colored margin bar
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (!item.isRead) {
                                                viewModel.markNotificationAsRead(item)
                                            }
                                        }
                                        .border(
                                            width = if (!item.isRead) 1.5.dp else 0.5.dp,
                                            color = if (!item.isRead) borderAccent else borderAccent.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (!item.isRead) {
                                            // Soft premium priority-sensitive gradient glow
                                            borderAccent.copy(alpha = 0.04f)
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                                    ) {
                                        // Colored priority indicator vertical line
                                        Box(
                                            modifier = Modifier
                                                .width(6.dp)
                                                .fillMaxHeight()
                                                .background(borderAccent)
                                        )

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(14.dp)
                                        ) {
                                            // Top Meta: Sender Role + Priority Tag + Timestamp + New Badge
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    // Channel icon
                                                    val iconColor = when (item.type) {
                                                        "ADMIN" -> CorporateAmethyst
                                                        "TEAM" -> CorporateSky
                                                        else -> GoldAccent
                                                    }
                                                    val channelIcon = when (item.type) {
                                                        "ADMIN" -> Icons.Filled.Business
                                                        "TEAM" -> Icons.Filled.Person
                                                        else -> Icons.Filled.Psychology
                                                    }
                                                    
                                                    Icon(
                                                        imageVector = channelIcon,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = iconColor
                                                    )

                                                    Text(
                                                        text = if (lang == "ar") item.senderAr else item.senderEn,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = iconColor
                                                    )

                                                    Text("•", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))

                                                    // Priority capsule
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(borderAccent.copy(alpha = 0.15f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = item.priority,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Black,
                                                            color = borderAccent,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                    
                                                    // "NEW" Indicator
                                                    if (!item.isRead) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(borderAccent.copy(alpha = 0.25f))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = if (lang == "ar") "جديد" else "NEW",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Black,
                                                                color = borderAccent,
                                                                fontSize = 9.sp
                                                            )
                                                        }
                                                    }
                                                }

                                                // Time stamp
                                                val minutesAgo = ((System.currentTimeMillis() - item.timestamp) / 60000).coerceAtLeast(1)
                                                val timeStr = if (minutesAgo < 60) {
                                                    if (lang == "ar") "منذ $minutesAgo د" else "${minutesAgo}m ago"
                                                } else {
                                                    val hours = minutesAgo / 60
                                                    if (lang == "ar") "منذ $hours س" else "${hours}h ago"
                                                }
                                                Text(
                                                    text = timeStr,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            }

                                            Spacer(Modifier.height(8.dp))

                                            // Content Body
                                            Text(
                                                text = if (lang == "ar") item.titleAr else item.titleEn,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = if (lang == "ar") item.messageAr else item.messageEn,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                                lineHeight = 18.sp
                                            )

                                            Spacer(Modifier.height(10.dp))

                                            // Interactive Action Controls Drawer (High Contrast)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Optional receiver metadata info tag
                                                Text(
                                                    text = "${if (lang == "ar") "الموجه إلى: " else "To: "} ${if (lang == "ar") item.receiverAr else item.receiverEn}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                                )

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (item.actionType == "APPROVE_VOUCHER" && !item.isActionHandled) {
                                                        // Audit linking action controls
                                                        OutlinedButton(
                                                            onClick = { viewModel.deleteNotification(item.id) },
                                                            modifier = Modifier.padding(end = 8.dp),
                                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Filled.DeleteOutline, 
                                                                contentDescription = null, 
                                                                tint = MaterialTheme.colorScheme.error,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(Modifier.width(4.dp))
                                                            Text(
                                                                text = if (lang == "ar") "تجاهل" else "Dismiss",
                                                                color = MaterialTheme.colorScheme.error,
                                                                fontSize = 11.sp
                                                            )
                                                        }

                                                        Button(
                                                            onClick = { viewModel.handleNotificationAction(item) },
                                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                                            shape = RoundedCornerShape(8.dp),
                                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Filled.Check, 
                                                                contentDescription = null, 
                                                                tint = Color.Black,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(Modifier.width(6.dp))
                                                            Text(
                                                                text = if (lang == "ar") 
                                                                    "اعتماد وترحيل القيد ${item.actionPayload ?: "JV"}" 
                                                                else 
                                                                    "Post Ledger ${item.actionPayload ?: "JV"}",
                                                                color = Color.Black,
                                                                fontWeight = FontWeight.Black,
                                                                fontSize = 11.sp
                                                            )
                                                        }
                                                    } else {
                                                        // Handled status or simple delete
                                                        if (item.isActionHandled) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(EmeraldGreen.copy(alpha = 0.15f))
                                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                                                    .padding(end = 8.dp)
                                                            ) {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Icon(
                                                                        Icons.Filled.Verified, 
                                                                        contentDescription = null, 
                                                                        tint = EmeraldGreen, 
                                                                        modifier = Modifier.size(14.dp)
                                                                    )
                                                                    Spacer(Modifier.width(4.dp))
                                                                    Text(
                                                                        text = if (lang == "ar") "معتمد ومُرَحَّل" else "Posted & Handled",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = EmeraldGreen,
                                                                        fontWeight = FontWeight.Bold,
                                                                        fontSize = 11.sp
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        IconButton(
                                                            onClick = { viewModel.deleteNotification(item.id) },
                                                            modifier = Modifier.size(34.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Filled.Delete,
                                                                contentDescription = "Delete",
                                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
