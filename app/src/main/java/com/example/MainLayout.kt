package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LedgerViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(viewModel: LedgerViewModel) {
    val isSeeding by viewModel.isSeeding.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()

    val orgNameAr by viewModel.orgNameAr.collectAsStateWithLifecycle()
    val orgNameEn by viewModel.orgNameEn.collectAsStateWithLifecycle()
    val userDescAr by viewModel.userDescAr.collectAsStateWithLifecycle()
    val userDescEn by viewModel.userDescEn.collectAsStateWithLifecycle()
    val logoConfig by viewModel.logoConfig.collectAsStateWithLifecycle()
    val logoImageUri by viewModel.logoImageUri.collectAsStateWithLifecycle()

    val currentOrgName = if (lang == "ar") orgNameAr else orgNameEn
    val currentUserDesc = if (lang == "ar") userDescAr else userDescEn

    var activeTab by remember { mutableStateOf(0) } // 0 = Dashboard, 1 = CoA, 2 = Customers, 3 = Suppliers, 4 = Cash Boxes, 5 = Banks, 6 = Vouchers, 7 = Statement, 8 = Reports, 9 = Settings...
    var showEditor by remember { mutableStateOf(false) }
    var isVouchersGroupExpanded by remember { mutableStateOf(false) }
    var isReportsGroupExpanded by remember { mutableStateOf(false) }

    var activeToast by remember { mutableStateOf<CustomToast?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var showExitDialog by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = true) {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            icon = { Icon(Icons.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = {
                Text(
                    text = if (lang == "ar") "تأكيد الخروج" else "Confirm Exit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (lang == "ar") "هل تريد الخروج من التطبيق فعلاً؟" else "Are you sure you want to exit the application?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        val activity = (context as? android.app.Activity)
                        activity?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (lang == "ar") "خروج" else "Exit")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showExitDialog = false }
                ) {
                    Text(if (lang == "ar") "إلغاء" else "Cancel")
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.navigateToTabFlow.collect { tab ->
            activeTab = tab
        }
    }

    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            val translated = com.example.ui.Localization.getLocalizedNotification(msg, lang)
            val type = when {
                msg.contains("failed", true) || msg.contains("error", true) || msg.contains("فشل", true) || msg.contains("غير", true) || msg.contains("مطلوب", true) -> ToastType.ERROR
                msg.contains("success", true) || msg.contains("created", true) || msg.contains("saved", true) || msg.contains("تم", true) || msg.contains("نجاح", true) || msg.contains("سجل", true) || msg.contains("مضافة", true) -> ToastType.SUCCESS
                else -> ToastType.INFO
            }
            activeToast = CustomToast(translated, type)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(activeToast) {
        if (activeToast != null) {
            delay(3500)
            activeToast = null
        }
    }

    val layoutDirection = com.example.ui.Localization.getLayoutDirection(lang)

    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isSeeding -> {
                    // Seed Loader screen: Elevated splash screen
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp)
                        ) {
                            AppLogo(
                                logoConfig = logoConfig,
                                logoImageUri = logoImageUri,
                                size = 100.dp,
                                clipShape = RoundedCornerShape(24.dp),
                                textStyle = MaterialTheme.typography.displayMedium.copy(fontSize = 42.sp)
                            )
                            
                            Spacer(Modifier.height(28.dp))
                            
                            Text(
                                text = currentOrgName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            
                            Spacer(Modifier.height(8.dp))
                            
                            Text(
                                text = currentUserDesc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            
                            Spacer(Modifier.height(56.dp))
                            
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(32.dp)
                            )
                            
                            Spacer(Modifier.height(16.dp))
                            
                            Text(
                                text = com.example.ui.Localization.translate(com.example.ui.Localization.Key.INITIALIZING, lang),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = com.example.ui.Localization.translate(com.example.ui.Localization.Key.SEEDING_PROP, lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
                showEditor -> {
                    // Immersive Full Screen Voucher Editor
                    VoucherEditorScreen(
                        viewModel = viewModel,
                        onNavigateBack = { showEditor = false }
                    )
                }
                else -> {
                    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                    val isWideScreen = configuration.screenWidthDp >= 750

                    if (isWideScreen) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Sidebar Container
                            Surface(
                                modifier = Modifier
                                    .width(310.dp)
                                    .fillMaxHeight(),
                                tonalElevation = 1.dp,
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                            ) {
                                DrawerNavigationMenu(
                                    lang = lang,
                                    activeTab = activeTab,
                                    isVouchersGroupExpanded = isVouchersGroupExpanded,
                                    onVouchersGroupExpandedChange = { isVouchersGroupExpanded = it },
                                    isReportsGroupExpanded = isReportsGroupExpanded,
                                    onReportsGroupExpandedChange = { isReportsGroupExpanded = it },
                                    orgName = currentOrgName,
                                    userDesc = currentUserDesc,
                                    logoConfig = logoConfig,
                                    logoImageUri = logoImageUri,
                                    onTabSelected = { index ->
                                        activeTab = index
                                    }
                                )
                            }

                            VerticalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                thickness = 1.dp
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                MainScaffoldContainer(
                                    activeTab = activeTab,
                                    lang = lang,
                                    isWideScreen = true,
                                    onMenuClick = {},
                                    onLangClick = {
                                        val nextLang = if (lang == "ar") "en" else "ar"
                                        viewModel.setLanguage(nextLang)
                                    },
                                    onTabChange = { activeTab = it },
                                    onNavigateToEditor = { showEditor = true },
                                    viewModel = viewModel
                                )
                            }
                        }
                    } else {
                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            gesturesEnabled = true,
                            drawerContent = {
                                ModalDrawerSheet(
                                    modifier = Modifier
                                        .width(300.dp)
                                        .fillMaxHeight()
                                ) {
                                    DrawerNavigationMenu(
                                        lang = lang,
                                        activeTab = activeTab,
                                        isVouchersGroupExpanded = isVouchersGroupExpanded,
                                        onVouchersGroupExpandedChange = { isVouchersGroupExpanded = it },
                                        isReportsGroupExpanded = isReportsGroupExpanded,
                                        onReportsGroupExpandedChange = { isReportsGroupExpanded = it },
                                        orgName = currentOrgName,
                                        userDesc = currentUserDesc,
                                        logoConfig = logoConfig,
                                        logoImageUri = logoImageUri,
                                        onTabSelected = { index ->
                                            activeTab = index
                                            scope.launch { drawerState.close() }
                                        }
                                    )
                                }
                            }
                        ) {
                            MainScaffoldContainer(
                                activeTab = activeTab,
                                lang = lang,
                                isWideScreen = false,
                                onMenuClick = { scope.launch { drawerState.open() } },
                                onLangClick = {
                                    val nextLang = if (lang == "ar") "en" else "ar"
                                    viewModel.setLanguage(nextLang)
                                },
                                onTabChange = { activeTab = it },
                                onNavigateToEditor = { showEditor = true },
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }

            // Slide toast panel
            AnimatedVisibility(
                visible = activeToast != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
                    .zIndex(9999f)
            ) {
                activeToast?.let { toast ->
                    ToastPill(toast = toast, onDismiss = { activeToast = null }, lang = lang)
                }
            }
        }
    }
}

enum class ToastType {
    SUCCESS, ERROR, INFO
}

data class CustomToast(
    val message: String,
    val type: ToastType = ToastType.INFO
)

@Composable
fun ToastPill(
    toast: CustomToast,
    onDismiss: () -> Unit,
    lang: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .shadow(16.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xEE1E1E24)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = when (toast.type) {
                ToastType.SUCCESS -> Color(0xFF4CAF50).copy(alpha = 0.5f)
                ToastType.ERROR -> Color(0xFFF44336).copy(alpha = 0.5f)
                ToastType.INFO -> Color(0xFF2196F3).copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (toast.type) {
                    ToastType.SUCCESS -> Icons.Filled.CheckCircle
                    ToastType.ERROR -> Icons.Filled.Error
                    ToastType.INFO -> Icons.Filled.Info
                },
                contentDescription = null,
                tint = when (toast.type) {
                    ToastType.SUCCESS -> Color(0xFF4CAF50)
                    ToastType.ERROR -> Color(0xFFF44336)
                    ToastType.INFO -> Color(0xFF2196F3)
                },
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.width(12.dp))

            Text(
                text = toast.message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun AppLogo(
    logoConfig: String,
    logoImageUri: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 46.dp,
    clipShape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleLarge
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(clipShape)
            .background(GoldAccent.copy(alpha = 0.15f))
            .border(1.dp, GoldAccent.copy(alpha = 0.3f), clipShape),
        contentAlignment = Alignment.Center
    ) {
        if (logoImageUri.isNotEmpty()) {
            coil.compose.AsyncImage(
                model = logoImageUri,
                contentDescription = "App Logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else if (logoConfig.isNotEmpty()) {
            Text(
                text = logoConfig,
                style = textStyle
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_yamama_logo),
                contentDescription = "Yamama 1 Logo",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun DrawerNavigationMenu(
    lang: String,
    activeTab: Int,
    isVouchersGroupExpanded: Boolean,
    onVouchersGroupExpandedChange: (Boolean) -> Unit,
    isReportsGroupExpanded: Boolean,
    onReportsGroupExpandedChange: (Boolean) -> Unit,
    orgName: String,
    userDesc: String,
    logoConfig: String,
    logoImageUri: String,
    onTabSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(30.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AppLogo(
                    logoConfig = logoConfig,
                    logoImageUri = logoImageUri,
                    size = 46.dp,
                    clipShape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.titleLarge
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (orgName.length >= 2) orgName.take(2).uppercase() else "LE",
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = orgName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = userDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        Spacer(Modifier.height(12.dp))

        var accountsExpanded by remember { mutableStateOf(activeTab in listOf(1, 4, 5)) }
        var partiesExpanded by remember { mutableStateOf(activeTab in listOf(2, 3)) }
        var vouchersExpanded by remember { mutableStateOf(isVouchersGroupExpanded || activeTab in listOf(10, 11, 12, 6)) }
        var reportsExpanded by remember { mutableStateOf(isReportsGroupExpanded || activeTab in listOf(7, 15, 16, 17)) }

        // Dashboard
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.SpaceDashboard, contentDescription = null, tint = if (activeTab == 0) GoldAccent else MaterialTheme.colorScheme.onSurface) },
            label = { Text(if (lang == "ar") "لوحة التحكم الرئيسية" else "Dashboard Hub", fontWeight = FontWeight.Bold, color = if (activeTab == 0) GoldAccent else MaterialTheme.colorScheme.onSurface) },
            selected = activeTab == 0,
            onClick = { onTabSelected(0) },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = GoldAccent.copy(alpha = 0.1f),
                unselectedContainerColor = Color.Transparent
            ),
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 2.dp)
                .testTag("nav_drawer_tab_0")
        )

        Spacer(Modifier.height(4.dp))

        // Measurements
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.SquareFoot, contentDescription = null, tint = if (activeTab == 20) GoldAccent else MaterialTheme.colorScheme.onSurface) },
            label = { Text(if (lang == "ar") "التمتير والمقاسات الفنية" else "Sizing & Measurements", fontWeight = FontWeight.Bold, color = if (activeTab == 20) GoldAccent else MaterialTheme.colorScheme.onSurface) },
            selected = activeTab == 20,
            onClick = { onTabSelected(20) },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = GoldAccent.copy(alpha = 0.1f),
                unselectedContainerColor = Color.Transparent
            ),
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 2.dp)
                .testTag("nav_drawer_tab_20")
        )

        Spacer(Modifier.height(4.dp))

        // Invoices
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = if (activeTab == 22) GoldAccent else MaterialTheme.colorScheme.onSurface) },
            label = { Text(if (lang == "ar") "الفواتير والمشتريات المدمجة" else "Invoices & Billing Hub", fontWeight = FontWeight.Bold, color = if (activeTab == 22) GoldAccent else MaterialTheme.colorScheme.onSurface) },
            selected = activeTab == 22,
            onClick = { onTabSelected(22) },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = GoldAccent.copy(alpha = 0.1f),
                unselectedContainerColor = Color.Transparent
            ),
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 2.dp)
                .testTag("nav_drawer_tab_22")
        )

        Spacer(Modifier.height(4.dp))

        // Inventory Console
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Warehouse, contentDescription = null, tint = if (activeTab == 23) GoldAccent else MaterialTheme.colorScheme.onSurface) },
            label = { Text(if (lang == "ar") "إدارة المخازن والأصناف" else "Warehouses & Stock Hub", fontWeight = FontWeight.Bold, color = if (activeTab == 23) GoldAccent else MaterialTheme.colorScheme.onSurface) },
            selected = activeTab == 23,
            onClick = { onTabSelected(23) },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = GoldAccent.copy(alpha = 0.1f),
                unselectedContainerColor = Color.Transparent
            ),
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 2.dp)
                .testTag("nav_drawer_tab_23")
        )

        Spacer(Modifier.height(4.dp))

        // Accounts Submenu
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = if (accountsExpanded) GoldAccent else MaterialTheme.colorScheme.onSurface) },
            label = { Text(if (lang == "ar") "الحسابات والخزائن المالية" else "Accounts, Cash & Banks", fontWeight = FontWeight.Bold, color = if (accountsExpanded) GoldAccent else MaterialTheme.colorScheme.onSurface) },
            selected = false,
            onClick = { accountsExpanded = !accountsExpanded },
            badge = {
                Icon(
                    if (accountsExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (accountsExpanded) GoldAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = Color.Transparent,
                unselectedContainerColor = Color.Transparent
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
        )

        AnimatedVisibility(
            visible = accountsExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                val accountsSubItems = listOf(
                    Triple(1, Icons.Filled.AccountTree, if (lang == "ar") "دليل شجرة الحسابات" else "Chart of Accounts"),
                    Triple(4, Icons.Filled.Payments, if (lang == "ar") "صناديق المال والخزائن" else "Cash Boxes Registry"),
                    Triple(5, Icons.Filled.AccountBalance, if (lang == "ar") "الحسابات والمصارف البنكية" else "Bank Accounts")
                )
                accountsSubItems.forEach { (index, icon, label) ->
                    val isSelected = activeTab == index
                    NavigationDrawerItem(
                        icon = { Icon(icon, contentDescription = null, tint = if (isSelected) GoldAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) },
                        label = { Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) GoldAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)) },
                        selected = isSelected,
                        onClick = { onTabSelected(index) },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = GoldAccent.copy(alpha = 0.08f),
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .padding(start = 28.dp, end = 12.dp, top = 2.dp, bottom = 2.dp)
                            .testTag("nav_drawer_tab_$index")
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Parties Submenu
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.People, contentDescription = null, tint = if (partiesExpanded) GoldAccent else MaterialTheme.colorScheme.onSurface) },
            label = { Text(if (lang == "ar") "إدارة العلاقات والجهات" else "Parties & Contacts", fontWeight = FontWeight.Bold, color = if (partiesExpanded) GoldAccent else MaterialTheme.colorScheme.onSurface) },
            selected = false,
            onClick = { partiesExpanded = !partiesExpanded },
            badge = {
                Icon(
                    if (partiesExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (partiesExpanded) GoldAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = Color.Transparent,
                unselectedContainerColor = Color.Transparent
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
        )

        AnimatedVisibility(
            visible = partiesExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                val partiesSubItems = listOf(
                    Triple(2, Icons.Filled.Person, if (lang == "ar") "كشوفات العملاء والزبائن" else "Customers Register"),
                    Triple(3, Icons.Filled.Business, if (lang == "ar") "كشوفات الموردين والشركات" else "Suppliers Register")
                )
                partiesSubItems.forEach { (index, icon, label) ->
                    val isSelected = activeTab == index
                    NavigationDrawerItem(
                        icon = { Icon(icon, contentDescription = null, tint = if (isSelected) GoldAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) },
                        label = { Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) GoldAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)) },
                        selected = isSelected,
                        onClick = { onTabSelected(index) },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = GoldAccent.copy(alpha = 0.08f),
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .padding(start = 28.dp, end = 12.dp, top = 2.dp, bottom = 2.dp)
                            .testTag("nav_drawer_tab_$index")
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Vouchers Submenu
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Assignment, contentDescription = null, tint = if (vouchersExpanded) GoldAccent else MaterialTheme.colorScheme.onSurface) },
            label = { Text(if (lang == "ar") "سندات الحسابات والقيود" else "Vouchers & Daily Journals", fontWeight = FontWeight.Bold, color = if (vouchersExpanded) GoldAccent else MaterialTheme.colorScheme.onSurface) },
            selected = false,
            onClick = { 
                vouchersExpanded = !vouchersExpanded
                onVouchersGroupExpandedChange(vouchersExpanded)
            },
            badge = {
                Icon(
                    if (vouchersExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (vouchersExpanded) GoldAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = Color.Transparent,
                unselectedContainerColor = Color.Transparent
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
        )

        AnimatedVisibility(
            visible = vouchersExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                val voucherSubItems = listOf(
                    Triple(10, Icons.Filled.ArrowDownward, if (lang == "ar") "سند قبض مالي جديد" else "New Receipt Voucher"),
                    Triple(11, Icons.Filled.ArrowUpward, if (lang == "ar") "سند دفع وصرف نقدي" else "New Payment Voucher"),
                    Triple(12, Icons.Filled.CompareArrows, if (lang == "ar") "قيد معالجة وتسوية يومية" else "Daily Journal Entry")
                )
                voucherSubItems.forEach { (index, icon, label) ->
                    val isSelected = activeTab == index
                    val iconTint = when (index) {
                        10 -> com.example.ui.theme.EmeraldGreen
                        11 -> com.example.ui.theme.RoseRed
                        else -> if (isSelected) GoldAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    }
                    NavigationDrawerItem(
                        icon = { Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp)) },
                        label = { Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) GoldAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)) },
                        selected = isSelected,
                        onClick = { onTabSelected(index) },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = GoldAccent.copy(alpha = 0.08f),
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .padding(start = 28.dp, end = 12.dp, top = 2.dp, bottom = 2.dp)
                            .testTag("nav_drawer_tab_$index")
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Reports Submenu
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Assessment, contentDescription = null, tint = if (reportsExpanded) GoldAccent else MaterialTheme.colorScheme.onSurface) },
            label = { Text(if (lang == "ar") "التقارير والقوائم المالية" else "Statements & Reports", fontWeight = FontWeight.Bold, color = if (reportsExpanded) GoldAccent else MaterialTheme.colorScheme.onSurface) },
            selected = false,
            onClick = { 
                reportsExpanded = !reportsExpanded
                onReportsGroupExpandedChange(reportsExpanded)
            },
            badge = {
                Icon(
                    if (reportsExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (reportsExpanded) GoldAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = Color.Transparent,
                unselectedContainerColor = Color.Transparent
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
        )

        AnimatedVisibility(
            visible = reportsExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                val reportsSubItems = listOf(
                    Triple(7, Icons.Filled.Book, if (lang == "ar") "كشف حساب تفصيلي" else "Detailed Account Statement"),
                    Triple(15, Icons.Filled.ListAlt, if (lang == "ar") "ميزان المراجعة بالأرصدة" else "Trial Balance"),
                    Triple(16, Icons.Filled.Assessment, if (lang == "ar") "الميزانية والمركز المالي" else "Balance Sheet Ledger"),
                    Triple(17, Icons.Filled.TrendingUp, if (lang == "ar") "قائمة الأرباح والخسائر والدخل" else "Income Statement"),
                    Triple(18, Icons.Filled.SwapHoriz, if (lang == "ar") "قائمة التدفقات النقدية" else "Statement of Cash Flows"),
                    Triple(19, Icons.Filled.QueryStats, if (lang == "ar") "التحليل والأداء الشهري" else "Monthly Performance"),
                    Triple(21, Icons.Filled.CardMembership, if (lang == "ar") "حساب وتقييم الزكاة الشرعية" else "Zakat Shari'ah Assessment")
                )
                reportsSubItems.forEach { (index, icon, label) ->
                    val isSelected = activeTab == index
                    NavigationDrawerItem(
                        icon = { Icon(icon, contentDescription = null, tint = if (isSelected) GoldAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) },
                        label = { Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) GoldAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)) },
                        selected = isSelected,
                        onClick = { onTabSelected(index) },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = GoldAccent.copy(alpha = 0.08f),
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .padding(start = 28.dp, end = 12.dp, top = 2.dp, bottom = 2.dp)
                            .testTag("nav_drawer_tab_$index")
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Settings
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Settings, contentDescription = null, tint = if (activeTab == 9) GoldAccent else MaterialTheme.colorScheme.onSurface) },
            label = { Text(if (lang == "ar") "إعدادات وتهيئة النظام" else "System Settings", fontWeight = FontWeight.Bold, color = if (activeTab == 9) GoldAccent else MaterialTheme.colorScheme.onSurface) },
            selected = activeTab == 9,
            onClick = { onTabSelected(9) },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = GoldAccent.copy(alpha = 0.1f),
                unselectedContainerColor = Color.Transparent
            ),
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 2.dp)
                .testTag("nav_drawer_tab_9")
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffoldContainer(
    activeTab: Int,
    lang: String,
    isWideScreen: Boolean,
    onMenuClick: () -> Unit,
    onLangClick: () -> Unit,
    onTabChange: (Int) -> Unit,
    onNavigateToEditor: () -> Unit,
    viewModel: LedgerViewModel
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var showNotificationHub by remember { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = when (activeTab) {
                            0 -> com.example.ui.Localization.translate(com.example.ui.Localization.Key.NAV_DASHBOARD, lang)
                            1 -> com.example.ui.Localization.translate(com.example.ui.Localization.Key.NAV_ACCOUNTS, lang)
                            2 -> com.example.ui.Localization.translate(com.example.ui.Localization.Key.NAV_CUSTOMERS, lang)
                            3 -> com.example.ui.Localization.translate(com.example.ui.Localization.Key.NAV_SUPPLIERS, lang)
                            4 -> com.example.ui.Localization.translate(com.example.ui.Localization.Key.NAV_CASH_BOXES, lang)
                            5 -> com.example.ui.Localization.translate(com.example.ui.Localization.Key.NAV_BANKS, lang)
                            6 -> com.example.ui.Localization.translate(com.example.ui.Localization.Key.NAV_VOUCHERS, lang)
                            7 -> com.example.ui.Localization.translate(com.example.ui.Localization.Key.VIEW_STATEMENT, lang)
                            8 -> com.example.ui.Localization.translate(com.example.ui.Localization.Key.NAV_REPORTS, lang)
                            9 -> com.example.ui.Localization.translate(com.example.ui.Localization.Key.NAV_SETTINGS, lang)
                            10 -> if (lang == "ar") "سندات القبض المالي" else "Receipt Vouchers"
                            11 -> if (lang == "ar") "سندات الصرف والدفع" else "Payment Vouchers"
                            12 -> if (lang == "ar") "قيود اليومية والتسوية" else "Journal Entries"
                            15 -> if (lang == "ar") "ميزان المراجعة بالأرصدة" else "Trial Balance"
                            16 -> if (lang == "ar") "الميزانية العمومية والمركز" else "Balance Sheet"
                            17 -> if (lang == "ar") "قائمة الدخل والأرباح" else "Income Statement"
                            18 -> if (lang == "ar") "كشف التدفقات النقدية" else "Cash Flow Statement"
                            19 -> if (lang == "ar") "تقرير أعمار الديون" else "Debt Aging Report"
                            20 -> if (lang == "ar") "التمتير والمقاسات الفنية" else "Sizing & Measurements"
                            22 -> if (lang == "ar") "الفواتير والمشتريات المدمجة" else "Invoices & Billing Hub"
                            23 -> if (lang == "ar") "إدارة المخازن والأصناف" else "Warehouse & Stock Hub"
                            else -> ""
                        },
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    if (!isWideScreen) {
                        IconButton(
                            onClick = onMenuClick,
                            modifier = Modifier.testTag("drawer_menu_button")
                        ) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = {
                    val unreadNotifs by viewModel.unreadNotifications.collectAsStateWithLifecycle()
                    val transition = rememberInfiniteTransition(label = "pulse")
                    val scale by if (unreadNotifs.isNotEmpty()) {
                        transition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = 1.15f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )
                    } else {
                        remember { mutableStateOf(1.0f) }
                    }

                    IconButton(
                        onClick = { showNotificationHub = true },
                        modifier = Modifier
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                            .testTag("notification_bell_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifs.isNotEmpty()) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text(
                                            text = unreadNotifs.size.toString(),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = if (lang == "ar") "التنبيهات والطلبات" else "Alerts & Notifications",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(onClick = onLangClick) {
                        Text(
                            text = if (lang == "ar") "EN" else "عربي",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                0 -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAccounts = { onTabChange(1) },
                    onNavigateToVouchers = { onTabChange(6) },
                    onNavigateToReports = { onTabChange(8) }
                )
                1 -> AccountsScreen(viewModel = viewModel)
                2 -> CustomersScreen(viewModel = viewModel)
                3 -> SuppliersScreen(viewModel = viewModel)
                4 -> CashBoxesScreen(viewModel = viewModel)
                5 -> BanksScreen(viewModel = viewModel)
                6 -> VouchersScreen(
                    viewModel = viewModel,
                    onNavigateToEditor = onNavigateToEditor
                )
                7 -> AccountStatementScreen(viewModel = viewModel)
                8 -> ReportsScreen(viewModel = viewModel)
                9 -> SettingsScreen(viewModel = viewModel)
                10 -> VouchersScreen(
                    viewModel = viewModel,
                    onNavigateToEditor = onNavigateToEditor,
                    forcedType = com.example.data.VoucherType.RECEIPT
                )
                11 -> VouchersScreen(
                    viewModel = viewModel,
                    onNavigateToEditor = onNavigateToEditor,
                    forcedType = com.example.data.VoucherType.PAYMENT
                )
                12 -> VouchersScreen(
                    viewModel = viewModel,
                    onNavigateToEditor = onNavigateToEditor,
                    forcedType = com.example.data.VoucherType.JOURNAL
                )
                15 -> ReportsScreen(viewModel = viewModel, forcedTab = 0)
                16 -> ReportsScreen(viewModel = viewModel, forcedTab = 1)
                17 -> ReportsScreen(viewModel = viewModel, forcedTab = 2)
                18 -> ReportsScreen(viewModel = viewModel, forcedTab = 3)
                19 -> ReportsScreen(viewModel = viewModel, forcedTab = 7)
                21 -> ReportsScreen(viewModel = viewModel, forcedTab = 8)
                20 -> MeasurementsScreen(viewModel = viewModel)
                22 -> InvoicesScreen(viewModel = viewModel)
                23 -> InventoryScreen(viewModel = viewModel)
            }
        }
        if (showNotificationHub) {
            NotificationHubDialog(
                viewModel = viewModel,
                onDismissRequest = { showNotificationHub = false }
            )
        }
    }
}
