package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

enum class ThemeStyle {
    CLASSIC_SKY,
    EMERALD_GOLD,
    COSMIC_AMETHYST,
    WARM_SAHARA
}

// Define the updated Financial Dashboard scheme
private val DarkFinancialScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = DeepNavy,
    secondary = SurfaceDark,
    onSecondary = CleanWhite,
    background = DeepNavy,
    onBackground = CleanWhite,
    surface = SurfaceDark,
    onSurface = CleanWhite,
    error = ErrorRed,
    onError = CleanWhite
)

private val LightFinancialScheme = lightColorScheme(
    primary = GoldAccent,
    onPrimary = DeepNavy,
    secondary = SurfaceLight,
    onSecondary = DeepNavy,
    background = CleanWhite,
    onBackground = DeepNavy,
    surface = SurfaceLight,
    onSurface = DeepNavy,
    error = ErrorRed,
    onError = CleanWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    style: ThemeStyle = ThemeStyle.CLASSIC_SKY,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkFinancialScheme else LightFinancialScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
