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
import androidx.compose.ui.graphics.Color
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

    // Selected filter: 0 = All, 1 = Admin, 2 = Team, 3 = Yamama Copilot
    var activeFilter by remember { mutableStateOf(0) }

    val filteredNotifications = remember(notifications, activeFilter) {
        when (activeFilter) {
            1 -> notifications.filter { it.type == "ADMIN" }
            2 -> notifications.filter { it.type == "TEAM" }
            3 -> notifications.filter { it.type == "SYSTEM" }
            else -> notifications
        }
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
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, GoldAccent.copy(alpha = 0.25f), RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.testTag("dismiss_notifications_hub")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = if (lang == "ar") "إغلاق" else "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (lang == "ar") "نظام التنبيهات والتراسل المالي" else "Financial Messaging & Alerts Center",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (lang == "ar") 
                                    "صندوق التراسل الفوري والطلبات الإدارية المتبادلة" 
                                else 
                                    "Direct corporate announcements and cross-user audit logs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Top Action Buttons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                viewModel.markAllNotificationsAsRead()
                                viewModel.showMessage("all_notifications_read")
                            },
                            modifier = Modifier.testTag("mark_all_read_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DoneAll,
                                contentDescription = if (lang == "ar") "قراءة الكل" else "Mark all read",
                                tint = EmeraldGreen
                            )
                        }
                        
                        Button(
                            onClick = { showCompose = !showCompose },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showCompose) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("toggle_compose")
                        ) {
                            Icon(
                                imageVector = if (showCompose) Icons.Filled.KeyboardArrowUp else Icons.Filled.EditNote,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (showCompose) {
                                    if (lang == "ar") "إلغاء التحرير" else "Cancel Compose"
                                } else {
                                    if (lang == "ar") "إنشاء تعميم" else "Compose Alert"
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(Modifier.height(12.dp))

                // Composer Panel (Slide/Fade Animated)
                AnimatedVisibility(
                    visible = showCompose,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .border(1.dp, GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = if (lang == "ar") "تحرير وبث إشعار جديد لفريق العمل" else "Compose & Broadcast Custom Alert",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                            Spacer(Modifier.height(12.dp))

                            // Dual-language titles
                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = composeTitleAr,
                                    onValueChange = { composeTitleAr = it },
                                    label = { Text("العنوان بالعربية (Ar)") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("compose_title_ar"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                OutlinedTextField(
                                    value = composeTitleEn,
                                    onValueChange = { composeTitleEn = it },
                                    label = { Text("Title in English (En)") },
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
                                        label = { Text("مضمون التنبيه بالعربية") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("compose_message_ar"),
                                        maxLines = 3,
                                        shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                OutlinedTextField(
                                        value = composeMessageEn,
                                        onValueChange = { composeMessageEn = it },
                                        label = { Text("Message Body in English") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("compose_message_en"),
                                        maxLines = 3,
                                        shape = RoundedCornerShape(8.dp)
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            // Metadata Selectors (Type, Priority, and Roles)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Type Pick
                                Column(modifier = Modifier.width(150.dp)) {
                                    Text(
                                        text = if (lang == "ar") "نوع المرسل" else "Category Type",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = composeType == "ADMIN",
                                            onClick = { composeType = "ADMIN" }
                                        )
                                        Text("Admin", style = MaterialTheme.typography.bodySmall)
                                        Spacer(Modifier.width(4.dp))
                                        RadioButton(
                                            selected = composeType == "TEAM",
                                            onClick = { composeType = "TEAM" }
                                        )
                                        Text("Team", style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                // Priority Pick
                                Column(modifier = Modifier.width(220.dp)) {
                                    Text(
                                        text = if (lang == "ar") "مستوى الأهمية والتحذير" else "Alert Priority",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = composePriority == "HIGH",
                                            onClick = { composePriority = "HIGH" }
                                        )
                                        Text("High 🔴", style = MaterialTheme.typography.bodySmall)
                                        Spacer(Modifier.width(4.dp))
                                        RadioButton(
                                            selected = composePriority == "MEDIUM",
                                            onClick = { composePriority = "MEDIUM" }
                                        )
                                        Text("Med 🟡", style = MaterialTheme.typography.bodySmall)
                                        Spacer(Modifier.width(4.dp))
                                        RadioButton(
                                            selected = composePriority == "LOW",
                                            onClick = { composePriority = "LOW" }
                                        )
                                        Text("Low 🔵", style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                // Roles Select Mock
                                Column(modifier = Modifier.width(160.dp)) {
                                    Text(
                                        text = if (lang == "ar") "رول المرسل" else "Sender Role",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = composeSenderEn == "Senior Auditor",
                                            onClick = { 
                                                composeSenderEn = "Senior Auditor"
                                                composeSenderAr = "المراجع المالي الرئيسي"
                                            }
                                        )
                                        Text("Auditor", style = MaterialTheme.typography.bodySmall)
                                        Spacer(Modifier.width(4.dp))
                                        RadioButton(
                                            selected = composeSenderEn == "General Manager",
                                            onClick = { 
                                                composeSenderEn = "General Manager"
                                                composeSenderAr = "المدير العام لليمامة 1"
                                            }
                                        )
                                        Text("GM", style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                // Toggle Interactive Payroll Approval Action
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Checkbox(
                                        checked = isInteractiveVoucher,
                                        onCheckedChange = { isInteractiveVoucher = it }
                                    )
                                    Text(
                                        text = if (lang == "ar") 
                                            "ربط إجرائي بقيد محاسبة (مثل JV-002)" 
                                        else 
                                            "Link interactive voucher control (JV-002)",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (isInteractiveVoucher) {
                                        Spacer(Modifier.width(8.dp))
                                        OutlinedTextField(
                                            value = interactiveVoucherNo,
                                            onValueChange = { interactiveVoucherNo = it },
                                            modifier = Modifier.width(100.dp),
                                            singleLine = true,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

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
                                Icon(Icons.Filled.Campaign, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (lang == "ar") "بث وإرسال التنبيه الآن" else "Broadcast Alert Now",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Filter Tabs Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterLabels = listOf(
                        Pair(0, if (lang == "ar") "الجميع (${notifications.size})" else "All (${notifications.size})"),
                        Pair(1, if (lang == "ar") "الإدارة والمجلس" else "Administration"),
                        Pair(2, if (lang == "ar") "فريق العمل والطلبات" else "Team Requests"),
                        Pair(3, if (lang == "ar") "سجل الرقابة الذكي" else "Yamama Control")
                    )

                    filterLabels.forEach { (index, label) ->
                        val isSelected = activeFilter == index
                        FilterChip(
                            selected = isSelected,
                            onClick = { activeFilter = index },
                            label = { Text(label, fontWeight = FontWeight.Bold) },
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

                Spacer(Modifier.height(16.dp))

                // Render list
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
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = if (lang == "ar") "لا توجد تنبيهات معلقة حالياً" else "No pending alerts found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (lang == "ar") 
                                    "صندوق التراسي المالي وطلبات المراجعة فارغ بالكامل." 
                                else 
                                    "No new announcements or pending vouchers are queued.",
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredNotifications) { item ->
                            val borderAccent = when (item.priority) {
                                "HIGH" -> RoseRed
                                "MEDIUM" -> GoldAccent
                                else -> CorporateSky
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (!item.isRead) {
                                            viewModel.markNotificationAsRead(item)
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = if (!item.isRead) 2.dp else 1.dp,
                                    color = if (!item.isRead) borderAccent else borderAccent.copy(alpha = 0.3f)
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (!item.isRead) 
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                                    else 
                                        MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    // Row Header: Priority Badge + Sender Role + Title
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val iconColor = when (item.type) {
                                                "ADMIN" -> CorporateAmethyst
                                                "TEAM" -> CorporateSky
                                                else -> GoldAccent
                                            }
                                            val icon = when (item.type) {
                                                "ADMIN" -> Icons.Filled.Business
                                                "TEAM" -> Icons.Filled.Person
                                                else -> Icons.Filled.Psychology
                                            }
                                            
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = iconColor
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = if (lang == "ar") item.senderAr else item.senderEn,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = iconColor
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "•",
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            // Status Tag / Priority
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(borderAccent.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = item.priority,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = borderAccent
                                                )
                                            }
                                        }

                                        // Time description or Red Dot Indicator
                                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                            )
                                            if (!item.isRead) {
                                                Spacer(Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(borderAccent)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    // Notification Body Content
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
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    // Action buttons row (Interactive Voucher Approvals)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (item.actionType == "APPROVE_VOUCHER" && !item.isActionHandled) {
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.deleteNotification(item.id)
                                                },
                                                modifier = Modifier.padding(end = 8.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                            ) {
                                                Icon(
                                                    Icons.Filled.DeleteOutline, 
                                                    contentDescription = null, 
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = if (lang == "ar") "تجاهل" else "Dismiss",
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.handleNotificationAction(item)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = if (lang == "ar") 
                                                        "مراجعة واعتماد القيد ${item.actionPayload ?: "JV"}" 
                                                    else 
                                                        "Approve & Post Ledger ${item.actionPayload ?: "JV"}",
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else {
                                            // Handle standard simple notification controls
                                            if (item.isActionHandled) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(EmeraldGreen.copy(alpha = 0.15f))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                        .padding(end = 8.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text(
                                                            text = if (lang == "ar") "تم الاعتماد والتسوية" else "Handled & Posted",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = EmeraldGreen,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }

                                            IconButton(
                                                onClick = { viewModel.deleteNotification(item.id) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
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
