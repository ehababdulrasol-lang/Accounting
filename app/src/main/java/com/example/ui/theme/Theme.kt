package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

enum class ThemeStyle {
    CLASSIC_SKY,
    EMERALD_GOLD,
    COSMIC_AMETHYST,
    WARM_SAHARA,
    LUXURY_ONYX
}

// Define the updated Financial Dashboard schemes per Style
private val DarkClassicSky = darkColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    secondary = Color(0xFF1E293B),
    onSecondary = Color.White,
    background = Color(0xFF0F172A),
    onBackground = Color.White,
    surface = Color(0xFF1E293B),
    onSurface = Color.White,
    error = ErrorRed,
    onError = Color.White
)

private val LightClassicSky = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    secondary = Color(0xFFF1F5F9),
    onSecondary = Color(0xFF0F172A),
    background = Color.White,
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF1F5F9),
    onSurface = Color(0xFF0F172A),
    error = ErrorRed,
    onError = Color.White
)

private val DarkEmeraldGold = darkColorScheme(
    primary = Color(0xFF0D9488),
    onPrimary = Color.White,
    secondary = Color(0xFF111827),
    onSecondary = Color.White,
    background = Color(0xFF030712),
    onBackground = Color.White,
    surface = Color(0xFF1F2937),
    onSurface = Color.White,
    error = ErrorRed,
    onError = Color.White
)

private val LightEmeraldGold = lightColorScheme(
    primary = Color(0xFF0D9488),
    onPrimary = Color.White,
    secondary = Color(0xFFF3F4F6),
    onSecondary = Color(0xFF111827),
    background = Color.White,
    onBackground = Color(0xFF111827),
    surface = Color(0xFFF3F4F6),
    onSurface = Color(0xFF111827),
    error = ErrorRed,
    onError = Color.White
)

private val DarkCosmicAmethyst = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color.White,
    secondary = Color(0xFF18181B),
    onSecondary = Color.White,
    background = Color(0xFF09090B),
    onBackground = Color.White,
    surface = Color(0xFF27272A),
    onSurface = Color.White,
    error = ErrorRed,
    onError = Color.White
)

private val LightCosmicAmethyst = lightColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color.White,
    secondary = Color(0xFFF4F4F5),
    onSecondary = Color(0xFF18181B),
    background = Color.White,
    onBackground = Color(0xFF18181B),
    surface = Color(0xFFF4F4F5),
    onSurface = Color(0xFF18181B),
    error = ErrorRed,
    onError = Color.White
)

private val DarkWarmSahara = darkColorScheme(
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

private val LightWarmSahara = lightColorScheme(
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

private val DarkLuxuryOnyx = darkColorScheme(
    primary = Color(0xFFF7D16A), // Bright Luxury Champagne Gold
    onPrimary = Color(0xFF0F0F12),
    secondary = Color(0xFF16161C), // Obsidian grey
    onSecondary = Color.White,
    background = Color(0xFF0B0B0E), // Onyx deep space dark
    onBackground = Color.White,
    surface = Color(0xFF131318), // Pure slate black surface
    onSurface = Color.White,
    error = ErrorRed,
    onError = Color.White
)

private val LightLuxuryOnyx = lightColorScheme(
    primary = Color(0xFF916B19), // Antique rich gold
    onPrimary = Color.White,
    secondary = Color(0xFFFAFAFD), // Alabaster white
    onSecondary = Color(0xFF0F0F12),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0F0F12),
    surface = Color(0xFFF2F2F6), // Pearl surface
    onSurface = Color(0xFF0F0F12),
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    style: ThemeStyle = ThemeStyle.CLASSIC_SKY,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        when (style) {
            ThemeStyle.CLASSIC_SKY -> DarkClassicSky
            ThemeStyle.EMERALD_GOLD -> DarkEmeraldGold
            ThemeStyle.COSMIC_AMETHYST -> DarkCosmicAmethyst
            ThemeStyle.WARM_SAHARA -> DarkWarmSahara
            ThemeStyle.LUXURY_ONYX -> DarkLuxuryOnyx
        }
    } else {
        when (style) {
            ThemeStyle.CLASSIC_SKY -> LightClassicSky
            ThemeStyle.EMERALD_GOLD -> LightEmeraldGold
            ThemeStyle.COSMIC_AMETHYST -> LightCosmicAmethyst
            ThemeStyle.WARM_SAHARA -> LightWarmSahara
            ThemeStyle.LUXURY_ONYX -> LightLuxuryOnyx
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
