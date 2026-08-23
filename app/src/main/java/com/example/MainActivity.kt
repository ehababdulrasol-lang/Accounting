package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LedgerViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {

    private val viewModel: LedgerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
            val darkThemeConfig by viewModel.darkThemeConfig.collectAsStateWithLifecycle()
            val themeStyle by viewModel.currentThemeStyle.collectAsStateWithLifecycle()
            val systemInDark = androidx.compose.foundation.isSystemInDarkTheme()
            val resolvedDark = when (darkThemeConfig) {
                "light" -> false
                "dark" -> true
                else -> systemInDark
            }
            val direction = com.example.ui.Localization.getLayoutDirection(lang)
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides direction) {
                MyApplicationTheme(darkTheme = resolvedDark, style = themeStyle) {
                    MainLayout(viewModel)
                }
            }
        }
    }
}
