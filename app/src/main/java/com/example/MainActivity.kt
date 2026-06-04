package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

class MainActivity : ComponentActivity() {

    private val viewModel: LedgerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val themeStyle by viewModel.currentThemeStyle.collectAsStateWithLifecycle()
            val direction = com.example.ui.Localization.getLayoutDirection(lang)
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides direction) {
                MyApplicationTheme(darkTheme = isDarkMode, style = themeStyle) {
                    MainLayout(viewModel)
                }
            }
        }
    }
}

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

    var activeTab by remember { mutableStateOf(0) } // 0 = Dashboard, 1 = CoA, 2 = Customers, 3 = Vouchers, 4 = Account Statement, 5 = Reports, 6 = Settings
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
                    // Seed Loader screen: Elevated dynamic splash screen
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
                            // Growing custom logo frame
                            AppLogo(
                                logoConfig = logoConfig,
                                logoImageUri = logoImageUri,
                                size = 100.dp,
                                clipShape = RoundedCornerShape(24.dp),
                                textStyle = MaterialTheme.typography.displayMedium.copy(fontSize = 42.sp)
                            )
                            
                            Spacer(Modifier.height(28.dp))
                            
                            // App/Organization Name
                            Text(
                                text = currentOrgName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            
                            Spacer(Modifier.height(8.dp))
                            
                            // User/Company tagline
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
                // Immersive full-screen Double-Entry voucher editor
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
                        // Permanent Docked Professional Sidebar
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

                        // Vertical dividing anchor line
                        VerticalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            thickness = 1.dp
                        )

                        // Main active screen content area with responsive weight layout
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
        } // Closes when {

        // High-fidelity top toast notifications
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
            containerColor = Color(0xEE1E1E24) // Charcoal black translucent profile
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
            Icon(
                imageVector = Icons.Filled.AccountBalance,
                contentDescription = null,
                tint = GoldAccent,
                modifier = Modifier.size(size * 0.5f)
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
        // Drawer Header satisfying visual guidelines
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
                // App Logo Icon reflecting the custom emoji/text logo configured
                AppLogo(
                    logoConfig = logoConfig,
                    logoImageUri = logoImageUri,
                    size = 46.dp,
                    clipShape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.titleLarge
                )

                // User Avatar
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

            // Dynamic Company name & info
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

        // Flat Main Top Navigation Items
        val topLevelItems = listOf(
            Triple(0, Icons.Filled.SpaceDashboard, com.example.ui.Localization.translate(com.example.ui.Localization.Key.NAV_DASHBOARD, lang)),
            Triple(1, Icons.Filled.AccountTree, com.example.ui.Localization.translate(com.example.ui.Localization.Key.NAV_ACCOUNTS, lang)),
            Triple(20, Icons.Filled.SquareFoot, if (lang == "ar") "التمتير والمقاسات" else "Sizing & Measurements"),
            Triple(2, Icons.Filled.People, com.example.ui.Localization.translate(com.example.ui.Localization.Key.NAV_CUSTOMERS, lang)),
            Triple(3, Icons.Filled.Storefront, com.example.ui.Localization.translate(com.example.ui.Localization.Key.NAV_SUPPLIERS, lang)),
            Triple(4, Icons.Filled.AccountBalanceWallet, com.example.ui.Localization.translate(com.example.ui.Localization.Key.NAV_CASH_BOXES, lang)),
            Triple(5, Icons.Filled.AccountBalance, com.example.ui.Localization.translate(com.example.ui.Localization.Key.NAV_BANKS, lang)),
            Triple(7, Icons.Filled.Book, com.example.ui.Localization.translate(com.example.ui.Localization.Key.VIEW_STATEMENT, lang))
        )

        // Render top level flat items
        topLevelItems.forEach { (index, icon, label) ->
            NavigationDrawerItem(
                icon = { Icon(icon, contentDescription = null, tint = if (activeTab == index) GoldAccent else MaterialTheme.colorScheme.onSurface) },
                label = { Text(label, fontWeight = FontWeight.SemiBold, color = if (activeTab == index) GoldAccent else MaterialTheme.colorScheme.onSurface) },
                selected = activeTab == index,
                onClick = { onTabSelected(index) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = GoldAccent.copy(alpha = 0.1f),
                    unselectedContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .testTag("nav_drawer_tab_$index")
            )
        }

        Spacer(Modifier.height(8.dp))

        // COLLAPSIBLE VOUCHERS GROUP
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
            label = { Text(if (lang == "ar") "سندات الحسابات والقيود" else "Voucher Registry", fontWeight = FontWeight.SemiBold) },
            selected = false,
            onClick = { onVouchersGroupExpandedChange(!isVouchersGroupExpanded) },
            badge = {
                Icon(
                    if (isVouchersGroupExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null
                )
            },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = Color.Transparent,
                unselectedContainerColor = Color.Transparent
            ),
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 2.dp)
        )

        if (isVouchersGroupExpanded) {
            val voucherSubItems = listOf(
                Triple(10, Icons.Filled.ArrowDownward, if (lang == "ar") "سند قبض مالي" else "Receipt Voucher"),
                Triple(11, Icons.Filled.ArrowUpward, if (lang == "ar") "سند دفع وصرف" else "Payment Voucher"),
                Triple(12, Icons.Filled.CompareArrows, if (lang == "ar") "قيد اليومية والتسوية" else "Journal Entry")
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

        Spacer(Modifier.height(8.dp))

        // COLLAPSIBLE REPORTS GROUP
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
            label = { Text(if (lang == "ar") "القوائم والتقارير المالية" else "Financial Reports", fontWeight = FontWeight.SemiBold) },
            selected = false,
            onClick = { onReportsGroupExpandedChange(!isReportsGroupExpanded) },
            badge = {
                Icon(
                    if (isReportsGroupExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null
                )
            },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = Color.Transparent,
                unselectedContainerColor = Color.Transparent
            ),
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 2.dp)
        )

        if (isReportsGroupExpanded) {
            val reportsSubItems = listOf(
                Triple(15, Icons.Filled.AccountBalance, if (lang == "ar") "ميزان المراجعة بالأرصدة" else "Trial Balance"),
                Triple(16, Icons.Filled.Assessment, if (lang == "ar") "الميزانية العمومية والمركز" else "Balance Sheet"),
                Triple(17, Icons.Filled.TrendingUp, if (lang == "ar") "قائمة الدخل والأرباح" else "Income Statement")
            )
            reportsSubItems.forEach { (index, icon, label) ->
                val isSelected = activeTab == index
                val iconTint = if (isSelected) GoldAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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

        Spacer(Modifier.height(8.dp))

        // Settings Item
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Security, contentDescription = null, tint = if (activeTab == 9) GoldAccent else MaterialTheme.colorScheme.onSurface) },
            label = { Text(com.example.ui.Localization.translate(com.example.ui.Localization.Key.NAV_SETTINGS, lang), fontWeight = FontWeight.SemiBold, color = if (activeTab == 9) GoldAccent else MaterialTheme.colorScheme.onSurface) },
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
                    IconButton(onClick = onLangClick) {
                        Text(
                            text = if (lang == "ar") "EN" else "عربي",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ),
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    val isForward = targetState > initialState
                    if (isForward) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "tab_switching"
            ) { tab ->
                when (tab) {
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
                    20 -> MeasurementsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
