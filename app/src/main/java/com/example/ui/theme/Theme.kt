package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class ThemeStyle {
    CLASSIC_SKY,
    EMERALD_GOLD,
    COSMIC_AMETHYST,
    WARM_SAHARA
}

private val DarkColorScheme = darkColorScheme(
    primary = CorporateSky,
    onPrimary = Color.White,
    secondary = PrimarySlate,
    onSecondary = OnDarkText,
    tertiary = EmeraldGreen,
    background = DarkSlate,
    onBackground = OnDarkText,
    surface = MidnightBlue,
    onSurface = OnDarkText,
    error = RoseRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = CorporateSky,
    onPrimary = Color.White,
    secondary = PrimarySlate,
    onSecondary = OnLightText,
    tertiary = EmeraldGreen,
    background = LightSlate,
    onBackground = OnLightText,
    surface = CardLight,
    onSurface = OnLightText,
    error = RoseRed,
    onError = Color.White
)

fun getDynamicColorScheme(darkTheme: Boolean, style: ThemeStyle): androidx.compose.material3.ColorScheme {
    return if (darkTheme) {
        when (style) {
            ThemeStyle.CLASSIC_SKY -> darkColorScheme(
                primary = Color(0xFF0284C7),
                onPrimary = Color.White,
                secondary = Color(0xFF334155),
                onSecondary = Color(0xFFF1F5F9),
                tertiary = Color(0xFF059669),
                background = Color(0xFF0F172A),
                onBackground = Color(0xFFF1F5F9),
                surface = Color(0xFF1E293B),
                onSurface = Color(0xFFF1F5F9),
                error = Color(0xFFDC2626),
                onError = Color.White
            )
            ThemeStyle.EMERALD_GOLD -> darkColorScheme(
                primary = Color(0xFF10B981),
                onPrimary = Color.Black,
                secondary = Color(0xFF111827),
                onSecondary = Color(0xFFF9FAFB),
                tertiary = Color(0xFFF59E0B),
                background = Color(0xFF061512),
                onBackground = Color(0xFFECFDF5),
                surface = Color(0xFF0E251F),
                onSurface = Color(0xFFECFDF5),
                error = Color(0xFFEF4444),
                onError = Color.White
            )
            ThemeStyle.COSMIC_AMETHYST -> darkColorScheme(
                primary = Color(0xFF8B5CF6),
                onPrimary = Color.White,
                secondary = Color(0xFF1E1B4B),
                onSecondary = Color(0xFFFDF4FF),
                tertiary = Color(0xFFEC4899),
                background = Color(0xFF0F0B1E),
                onBackground = Color(0xFFFDF4FF),
                surface = Color(0xFF17122D),
                onSurface = Color(0xFFFDF4FF),
                error = Color(0xFFF43F5E),
                onError = Color.White
            )
            ThemeStyle.WARM_SAHARA -> darkColorScheme(
                primary = Color(0xFFF59E0B),
                onPrimary = Color.Black,
                secondary = Color(0xFF0D9488),
                onSecondary = Color(0xFFFFFBEB),
                tertiary = Color(0xFF10B981),
                background = Color(0xFF1C1304),
                onBackground = Color(0xFFFFFBEB),
                surface = Color(0xFF2C1E0A),
                onSurface = Color(0xFFFFFBEB),
                error = Color(0xFFEF4444),
                onError = Color.White
            )
        }
    } else {
        when (style) {
            ThemeStyle.CLASSIC_SKY -> lightColorScheme(
                primary = Color(0xFF0284C7),
                onPrimary = Color.White,
                secondary = Color(0xFF334155),
                onSecondary = Color(0xFF0F172A),
                tertiary = Color(0xFF059669),
                background = Color(0xFFF8FAFC),
                onBackground = Color(0xFF0F172A),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF0F172A),
                error = Color(0xFFDC2626),
                onError = Color.White
            )
            ThemeStyle.EMERALD_GOLD -> lightColorScheme(
                primary = Color(0xFF059669),
                onPrimary = Color.White,
                secondary = Color(0xFFD97706),
                onSecondary = Color(0xFF064E3B),
                tertiary = Color(0xFF10B981),
                background = Color(0xFFF0FDF4),
                onBackground = Color(0xFF064E3B),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF064E3B),
                error = Color(0xFFDC2626),
                onError = Color.White
            )
            ThemeStyle.COSMIC_AMETHYST -> lightColorScheme(
                primary = Color(0xFF7C3AED),
                onPrimary = Color.White,
                secondary = Color(0xFFDB2777),
                onSecondary = Color(0xFF3B0764),
                tertiary = Color(0xFFEC4899),
                background = Color(0xFFFAF5FF),
                onBackground = Color(0xFF3B0764),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF3B0764),
                error = Color(0xFFDC2626),
                onError = Color.White
            )
            ThemeStyle.WARM_SAHARA -> lightColorScheme(
                primary = Color(0xFFD97706),
                onPrimary = Color.White,
                secondary = Color(0xFF14B8A6),
                onSecondary = Color(0xFF1C1304),
                tertiary = Color(0xFF10B981),
                background = Color(0xFFFFFBEB),
                onBackground = Color(0xFF1C1304),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF1C1304),
                error = Color(0xFFDC2626),
                onError = Color.White
            )
        }
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    style: ThemeStyle = ThemeStyle.CLASSIC_SKY,
    content: @Composable () -> Unit
) {
    val colorScheme = getDynamicColorScheme(darkTheme, style)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
