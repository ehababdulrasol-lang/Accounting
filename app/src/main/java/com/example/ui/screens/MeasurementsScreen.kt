package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Account
import com.example.data.MeasurementHeader
import com.example.data.MeasurementLine
import com.example.ui.Localization
import com.example.ui.StaggeredItem
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RoseRed
import com.example.ui.theme.CorporateSky
import com.example.ui.viewmodel.LedgerViewModel
import com.example.util.FinancialUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementsScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isLibyan by viewModel.isLibyanMode.collectAsStateWithLifecycle()
    val measurements by viewModel.measurementHeaders.collectAsStateWithLifecycle()
    val customersList by viewModel.customers.collectAsStateWithLifecycle()
    val allAccounts by viewModel.accounts.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedHeaderForDetails by remember { mutableStateOf<MeasurementHeader?>(null) }
    var detailLines by remember { mutableStateOf<List<MeasurementLine>>(emptyList()) }
    var showNewMeasurementSheet by remember { mutableStateOf(false) }

    // Floating action click triggers "مقاس جديد" (New Measurement Sizing)
    var isEditingExisting by remember { mutableStateOf<MeasurementHeader?>(null) }

    LaunchedEffect(selectedHeaderForDetails) {
        selectedHeaderForDetails?.let { header ->
            viewModel.getMeasurementLines(header.id).collect { lines ->
                detailLines = lines
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (!showNewMeasurementSheet) {
                FloatingActionButton(
                    onClick = {
                        isEditingExisting = null
                        showNewMeasurementSheet = true
                    },
                    containerColor = GoldAccent,
                    contentColor = MaterialTheme.colorScheme.background,
                    modifier = Modifier.testTag("new_measurement_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (lang == "ar") "مقاس جديد" else "New Sizing",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Header details
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (lang == "ar") "حسابات القياس والتمتير للمقاسات" else "Measurement Registry & Sizing Ledger",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (lang == "ar") "سجل أطوال ومساحات الرخام والزجاج مع احتسابة فواتير فورية ترحيلاً للقيود المحاسبية" else "Calculate linear/square dimensions of goods, build and post measurement invoices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(16.dp))

                if (measurements.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.SquareFoot,
                                contentDescription = null,
                                tint = GoldAccent.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = if (lang == "ar") "لا توجد مستندات تمتير بعد" else "No sizing measurements on record",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        itemsIndexed(measurements) { index, header ->
                            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(header.date))
                            StaggeredItem(index = index) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            if (selectedHeaderForDetails?.id == header.id) GoldAccent.copy(alpha = 0.5f) else Color.Transparent,
                                            RoundedCornerShape(14.dp)
                                        ),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                                    ),
                                    onClick = {
                                        selectedHeaderForDetails = if (selectedHeaderForDetails?.id == header.id) null else header
                                    }
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(GoldAccent.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.SquareFoot,
                                                        contentDescription = null,
                                                        tint = GoldAccent,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Spacer(Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = header.customerName,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Text(
                                                        text = dateStr,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                    )
                                                }
                                            }

                                            // Status Badge
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (header.isPosted) EmeraldGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (header.isPosted) {
                                                        if (lang == "ar") "مرحّل ماليًا" else "Posted to Ledger"
                                                    } else {
                                                        if (lang == "ar") "مسودة تمتير" else "Sizing Draft"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (header.isPosted) EmeraldGreen else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(12.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                        Spacer(Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = if (lang == "ar") "إجمالي المقاس (م²):" else "Total Area (M²):",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                                Text(
                                                    text = String.format(Locale.getDefault(), "%.2f", header.totalMeters) + " " + (if (lang == "ar") "متر مربع" else "M²"),
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = if (lang == "ar") "قيمة الفاتورة المعتمدة:" else "Total Sizing Cost:",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                                Text(
                                                    text = "${FinancialUtils.formatBase(header.totalAmount)} ${if (isLibyan) "د.ل" else "LYD"}",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = GoldAccent
                                                )
                                            }
                                        }

                                        if (header.notes.isNotBlank()) {
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                text = "${if (lang == "ar") "ملاحظات: " else "Notes: "}${header.notes}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }

                                        // Render expanded details if selected
                                        AnimatedVisibility(
                                            visible = selectedHeaderForDetails?.id == header.id,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                                Spacer(Modifier.height(10.dp))
                                                Text(
                                                    text = if (lang == "ar") "تفاصيل بنود التمتير والمقاسات:" else "Measurement Line Detail:",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(Modifier.height(6.dp))

                                                // List detailed dimension rows
                                                detailLines.forEach { line ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1.5f)) {
                                                            Text(
                                                                text = line.itemDescription,
                                                                fontWeight = FontWeight.SemiBold,
                                                                style = MaterialTheme.typography.bodySmall
                                                            )
                                                            Text(
                                                                text = "${if (lang == "ar") "العرض: " else "W: "}${line.width}م × ${if (lang == "ar") "الارتفاع: " else "H: "}${line.height}م",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                            )
                                                        }
                                                        Text(
                                                            text = "×${line.quantity}",
                                                            fontWeight = FontWeight.Bold,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            modifier = Modifier.weight(0.5f)
                                                        )
                                                        Text(
                                                            text = "${line.totalArea} م²",
                                                            fontWeight = FontWeight.Bold,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            modifier = Modifier.weight(0.8f)
                                                        )
                                                        Text(
                                                            text = FinancialUtils.formatBase(line.totalAmount),
                                                            fontWeight = FontWeight.Bold,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                                                }

                                                Spacer(Modifier.height(14.dp))

                                                // Row Actions
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    if (!header.isPosted) {
                                                        // Post to accounting ledger
                                                        Button(
                                                            onClick = {
                                                                viewModel.postMeasurementToLedger(header)
                                                                selectedHeaderForDetails = null
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                                            modifier = Modifier.weight(1f),
                                                            shape = RoundedCornerShape(10.dp)
                                                        ) {
                                                            Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(Modifier.width(6.dp))
                                                            Text(
                                                                text = if (lang == "ar") "ترحيل للقيود" else "Post Ledger",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.sp
                                                            )
                                                        }

                                                        // Edit Sizing Button
                                                        Button(
                                                            onClick = {
                                                                isEditingExisting = header
                                                                showNewMeasurementSheet = true
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                            modifier = Modifier.weight(1f),
                                                            shape = RoundedCornerShape(10.dp)
                                                        ) {
                                                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(Modifier.width(6.dp))
                                                            Text(
                                                                text = if (lang == "ar") "تعديل" else "Edit",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 12.sp
                                                            )
                                                        }
                                                    }

                                                    // Share / Export via Whatsapp
                                                    IconButton(
                                                        onClick = {
                                                            val messageToShare = if (lang == "ar") {
                                                                "📄 فاتورة وتفاصيل المقاسات والتمتير\nالعميل: ${header.customerName}\nتاريخ المستند: $dateStr\nإجمالي الأمتار: ${header.totalMeters} م²\nالمبلغ الإجمالي: ${FinancialUtils.formatBase(header.totalAmount)} د.ل\n\nشكراً لتعاملكم معنا."
                                                            } else {
                                                                "📄 Sizing & Dimensions Invoice\nCustomer: ${header.customerName}\nDate: $dateStr\nTotal Area: ${header.totalMeters} M²\nTotal Amount: ${FinancialUtils.formatBase(header.totalAmount)} LYD\n\nThank you for business."
                                                            }
                                                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                                type = "text/plain"
                                                                putExtra(android.content.Intent.EXTRA_TEXT, messageToShare)
                                                            }
                                                            context.startActivity(android.content.Intent.createChooser(intent, "Share measurements sizing"))
                                                        },
                                                        modifier = Modifier
                                                            .background(CorporateSky.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                                            .size(42.dp)
                                                    ) {
                                                        Icon(Icons.Filled.Share, contentDescription = "Share", tint = CorporateSky)
                                                    }

                                                    // Delete Button
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.deleteMeasurement(header)
                                                            selectedHeaderForDetails = null
                                                        },
                                                        modifier = Modifier
                                                            .background(RoseRed.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                                            .size(42.dp)
                                                    ) {
                                                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = RoseRed)
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

            // Sheet / Panel: NEW MEASUREMENT SIZING FORM ("مقاس جديد")
            if (showNewMeasurementSheet) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Title bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEditingExisting != null) {
                                    if (lang == "ar") "تعديل مستند تمتير" else "Edit Sizing Document"
                                } else {
                                    if (lang == "ar") "إدخال مقاس جديد" else "Add New Measurement"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            IconButton(onClick = { showNewMeasurementSheet = false }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        Spacer(Modifier.height(12.dp))

                        // Customer Picker / Manual Name Field
                        var selectedCustomerAccount by remember { mutableStateOf<Account?>(null) }
                        var manualCustomerName by remember { mutableStateOf("") }
                        var measurementsNotes by remember { mutableStateOf("") }
                        var showCustomerDropdown by remember { mutableStateOf(false) }

                        // Form lists
                        var activeFormLines by remember { mutableStateOf<List<MeasurementLine>>(emptyList()) }

                        // Temp interactive item builder states
                        var itemDesc by remember { mutableStateOf("") }
                        var widthValStr by remember { mutableStateOf("") }
                        var heightValStr by remember { mutableStateOf("") }
                        var quantityValStr by remember { mutableStateOf("1") }
                        var priceValStr by remember { mutableStateOf("") }

                        // Initialize if editing existing
                        LaunchedEffect(isEditingExisting) {
                            isEditingExisting?.let { header ->
                                manualCustomerName = header.customerName
                                measurementsNotes = header.notes
                                selectedCustomerAccount = allAccounts.find { it.id == header.accountId }
                                
                                // Fetch lines safely
                                val fetched = viewModel.getMeasurementLinesSuspend(header.id)
                                activeFormLines = fetched
                            }
                        }

                        val calculatedMetersSum = remember(activeFormLines) {
                            activeFormLines.sumOf { it.totalArea }
                        }

                        val calculatedPriceSum = remember(activeFormLines) {
                            activeFormLines.sumOf { it.totalAmount }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Picker block
                            item {
                                Text(
                                    text = if (lang == "ar") "1. إعدادات حساب العميل المستهدف" else "1. Configure Customer Target Account",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(8.dp))

                                // Selected Account display
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                        .clickable { showCustomerDropdown = !showCustomerDropdown }
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedCustomerAccount?.let { "${it.accountCode} - ${it.name}" }
                                                ?: (if (lang == "ar") "اختر حساب العميل المستهدف..." else "Select client ledger account..."),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (selectedCustomerAccount != null) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedCustomerAccount != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                    }
                                }

                                // Dropdown lists for Customer Accounts in database
                                if (showCustomerDropdown) {
                                    val customerAccounts = remember(allAccounts) {
                                        allAccounts.filter { it.accountCode.startsWith("1103") && !it.isGroup }
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                            .shadow(8.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(modifier = Modifier.heightIn(max = 200.dp)) {
                                            customerAccounts.forEach { acc ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            selectedCustomerAccount = acc
                                                            manualCustomerName = acc.name
                                                            showCustomerDropdown = false
                                                        }
                                                        .padding(12.dp)
                                                ) {
                                                    Icon(Icons.Filled.People, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(text = "${acc.accountCode} - ${acc.name}", fontSize = 13.sp)
                                                }
                                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                            }
                                        }
                                    }
                                }
                            }

                            // Manual Customer Identifier Input
                            item {
                                OutlinedTextField(
                                    value = manualCustomerName,
                                    onValueChange = { manualCustomerName = it },
                                    label = { Text(if (lang == "ar") "اسم العميل المتنقل" else "Customer Sizing Label / Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            // Interactive Item dimension entry form
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = if (lang == "ar") "إضافة بند مقاس/منتج جديد" else "Add Measurement Sizing Row",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldAccent
                                        )
                                        Spacer(Modifier.height(10.dp))

                                        OutlinedTextField(
                                            value = itemDesc,
                                            onValueChange = { itemDesc = it },
                                            label = { Text(if (lang == "ar") "وصف البند (مثال: زجاج سيكوريت)" else "Item style description") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = true
                                        )

                                        Spacer(Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Width input
                                            OutlinedTextField(
                                                value = widthValStr,
                                                onValueChange = { widthValStr = it },
                                                label = { Text(if (lang == "ar") "العرض" else "Width (m)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                singleLine = true
                                            )

                                            // Height input
                                            OutlinedTextField(
                                                value = heightValStr,
                                                onValueChange = { heightValStr = it },
                                                label = { Text(if (lang == "ar") "الارتفاع" else "Height (m)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                singleLine = true
                                            )
                                        }

                                        Spacer(Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Quantity
                                            OutlinedTextField(
                                                value = quantityValStr,
                                                onValueChange = { quantityValStr = it },
                                                label = { Text(if (lang == "ar") "التكرار/العدد" else "Quantity") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                singleLine = true
                                            )

                                            // Price per Meter
                                            OutlinedTextField(
                                                value = priceValStr,
                                                onValueChange = { priceValStr = it },
                                                label = { Text(if (lang == "ar") "سعر المتر" else "Price per M²") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1.2f),
                                                shape = RoundedCornerShape(8.dp),
                                                singleLine = true
                                            )
                                        }

                                        Spacer(Modifier.height(14.dp))

                                        // Confirm Add Row button
                                        Button(
                                            onClick = {
                                                val w = widthValStr.toDoubleOrNull() ?: 0.0
                                                val h = heightValStr.toDoubleOrNull() ?: 0.0
                                                val q = quantityValStr.toIntOrNull() ?: 1
                                                val pr = priceValStr.toDoubleOrNull() ?: 0.0
                                                
                                                if (itemDesc.isBlank() || w <= 0.0 || h <= 0.0 || pr <= 0.0) {
                                                    // Display local error warnings
                                                    return@Button
                                                }

                                                val area = w * h * q
                                                val amt = (area * pr * 1000.0).toLong() // Corrected format scaling

                                                val newLine = MeasurementLine(
                                                    headerId = 0,
                                                    itemDescription = itemDesc.trim(),
                                                    width = w,
                                                    height = h,
                                                    quantity = q,
                                                    pricePerMeter = pr,
                                                    totalArea = area,
                                                    totalAmount = amt
                                                )

                                                activeFormLines = activeFormLines + newLine

                                                // Clean builders
                                                itemDesc = ""
                                                widthValStr = ""
                                                heightValStr = ""
                                                quantityValStr = "1"
                                                priceValStr = ""
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                                        ) {
                                            Icon(Icons.Filled.AddCircle, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = if (lang == "ar") "أضف هذه المقاسات للجدول" else "Insert Sizing Entry",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Render Added Lines table in current builder session
                            if (activeFormLines.isNotEmpty()) {
                                item {
                                    Text(
                                        text = if (lang == "ar") "2. معاينة البنود المدرجة:" else "2. Sizing Entries Table Preview:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(6.dp))

                                    activeFormLines.forEachIndexed { i, ln ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp), RoundedCornerShape(8.dp))
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1.5f)) {
                                                Text(text = ln.itemDescription, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(
                                                    text = "${ln.width}م عرض × ${ln.height}م ارتفاع × ${ln.quantity} حبات",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                                                Text(text = "${String.format(Locale.getDefault(), "%.2f", ln.totalArea)} م²", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                Text(text = "${FinancialUtils.formatBase(ln.totalAmount)} ${if (isLibyan) "د.ل" else "LYD"}", fontSize = 12.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                                            }

                                            // Delete icon
                                            IconButton(
                                                onClick = {
                                                    activeFormLines = activeFormLines.filterIndexed { index, _ -> index != i }
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Filled.RemoveCircle, contentDescription = null, tint = RoseRed)
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                    }
                                }
                            }

                            // Sizing Notes & Summary Section
                            item {
                                Text(
                                    text = if (lang == "ar") "3. الملاحظات والملخص" else "3. Sizing Note & Summary",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = measurementsNotes,
                                    onValueChange = { measurementsNotes = it },
                                    label = { Text(if (lang == "ar") "ملاحظات إضافية على التركيب/المقاس" else "Assembly or project sizing instructions") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(Modifier.height(12.dp))

                                // Sizing summary cards
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(GoldAccent.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                                        .border(2.dp, GoldAccent.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (lang == "ar") "مجموع المساحة الكلية:" else "Accumulated Total Area:",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "${String.format(Locale.getDefault(), "%.2f", calculatedMetersSum)} م²",
                                                fontWeight = FontWeight.ExtraBold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = GoldAccent
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (lang == "ar") "إجمالي الفاتورة التقريبية:" else "Estimated Total Bill:",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "${FinancialUtils.formatBase(calculatedPriceSum)} ${if (isLibyan) "د.ل" else "LYD"}",
                                                fontWeight = FontWeight.ExtraBold,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = EmeraldGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Submit buttons
                        Button(
                            onClick = {
                                if (manualCustomerName.isBlank() || activeFormLines.isEmpty() || selectedCustomerAccount == null) {
                                    // Trigger feedback
                                    return@Button
                                }

                                val headerEntity = MeasurementHeader(
                                    id = isEditingExisting?.id ?: 0L,
                                    customerName = manualCustomerName.trim(),
                                    accountId = selectedCustomerAccount!!.id,
                                    date = isEditingExisting?.date ?: System.currentTimeMillis(),
                                    notes = measurementsNotes.trim(),
                                    totalMeters = calculatedMetersSum,
                                    totalAmount = calculatedPriceSum,
                                    isPosted = isEditingExisting?.isPosted ?: false,
                                    voucherHeaderId = isEditingExisting?.voucherHeaderId
                                )

                                viewModel.saveMeasurement(headerEntity, activeFormLines) {
                                    showNewMeasurementSheet = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (lang == "ar") "حفظ مستند المقاس" else "Save Measurement Invoice",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
