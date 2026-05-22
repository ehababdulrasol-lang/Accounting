package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LedgerViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: LedgerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val lang by viewModel.currentLanguage.collectAsState()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val themeStyle by viewModel.currentThemeStyle.collectAsState()
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
    val isSeeding by viewModel.isSeeding.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val lang by viewModel.currentLanguage.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 = Dashboard, 1 = CoA, 2 = Customers, 3 = Vouchers, 4 = Account Statement, 5 = Reports, 6 = Settings
    var showEditor by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            val translated = com.example.ui.Localization.getLocalizedNotification(it, lang)
            snackbarHostState.showSnackbar(translated)
            viewModel.clearMessage()
        }
    }

    val layoutDirection = com.example.ui.Localization.getLayoutDirection(lang)

    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection) {
        when {
            isSeeding -> {
                // Seed Loader screen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = com.example.ui.Localization.translate(com.example.ui.Localization.Key.INITIALIZING, lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = com.example.ui.Localization.translate(com.example.ui.Localization.Key.SEEDING_PROP, lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
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
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = true,
                    drawerContent = {
                        ModalDrawerSheet(
                            modifier = Modifier.width(300.dp)
                        ) {
                            Spacer(Modifier.height(30.dp))
                            // Drawer Header
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = com.example.ui.Localization.translate(com.example.ui.Localization.Key.APP_NAME, lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (lang == "ar") "النظام المحاسبي الذكي" else "Smart Ledger Workstation",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            Spacer(Modifier.height(12.dp))

                            // Drawer Items
                            val items = listOf(
                                Triple(0, Icons.Filled.SpaceDashboard, com.example.ui.Localization.Key.NAV_DASHBOARD),
                                Triple(1, Icons.Filled.AccountTree, com.example.ui.Localization.Key.NAV_ACCOUNTS),
                                Triple(2, Icons.Filled.People, com.example.ui.Localization.Key.NAV_CUSTOMERS),
                                Triple(3, Icons.Filled.Assignment, com.example.ui.Localization.Key.NAV_VOUCHERS),
                                Triple(4, Icons.Filled.Book, com.example.ui.Localization.Key.VIEW_STATEMENT),
                                Triple(5, Icons.Filled.Assessment, com.example.ui.Localization.Key.NAV_REPORTS),
                                Triple(6, Icons.Filled.Security, com.example.ui.Localization.Key.NAV_SETTINGS)
                            )

                            items.forEach { (index, icon, key) ->
                                NavigationDrawerItem(
                                    icon = { Icon(icon, contentDescription = null) },
                                    label = { Text(com.example.ui.Localization.translate(key, lang), fontWeight = FontWeight.SemiBold) },
                                    selected = activeTab == index,
                                    onClick = {
                                        activeTab = index
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                        .testTag("nav_drawer_tab_$index")
                                )
                            }
                        }
                    }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    Text(
                                        text = when (activeTab) {
                                            0 -> com.example.ui.Localization.translate(com.example.ui.Localization.Key.NAV_DASHBOARD, lang)
                                            1 -> com.example.ui.Localization.translate(com.example.ui.Localization.Key.NAV_ACCOUNTS, lang)
                                            2 -> com.example.ui.Localization.translate(com.example.ui.Localization.Key.NAV_CUSTOMERS, lang)
                                            3 -> com.example.ui.Localization.translate(com.example.ui.Localization.Key.NAV_VOUCHERS, lang)
                                            4 -> com.example.ui.Localization.translate(com.example.ui.Localization.Key.VIEW_STATEMENT, lang)
                                            5 -> com.example.ui.Localization.translate(com.example.ui.Localization.Key.NAV_REPORTS, lang)
                                            6 -> com.example.ui.Localization.translate(com.example.ui.Localization.Key.NAV_SETTINGS, lang)
                                            else -> ""
                                        },
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                },
                                navigationIcon = {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier.testTag("drawer_menu_button")
                                    ) {
                                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                                    }
                                },
                                actions = {
                                    IconButton(
                                        onClick = {
                                            val nextLang = if (lang == "ar") "en" else "ar"
                                            viewModel.setLanguage(nextLang)
                                        }
                                    ) {
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
                                )
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
                                    fadeIn() togetherWith fadeOut()
                                },
                                label = "TabTransition"
                            ) { tab ->
                                when (tab) {
                                    0 -> DashboardScreen(
                                        viewModel = viewModel,
                                        onNavigateToAccounts = { activeTab = 1 },
                                        onNavigateToVouchers = { activeTab = 3 },
                                        onNavigateToReports = { activeTab = 5 }
                                    )
                                    1 -> AccountsScreen(viewModel = viewModel)
                                    2 -> CustomersScreen(viewModel = viewModel)
                                    3 -> VouchersScreen(
                                        viewModel = viewModel,
                                        onNavigateToEditor = { showEditor = true }
                                    )
                                    4 -> AccountStatementScreen(viewModel = viewModel)
                                    5 -> ReportsScreen(viewModel = viewModel)
                                    6 -> SettingsScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
