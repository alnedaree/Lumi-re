package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.model.AppLanguage
import com.example.model.LumiereThemeMode

@Immutable
data class LumiereExtendedColors(
    val glassSurface: Color,
    val goldAccent: Color,
    val goldMuted: Color,
    val hairlineBorder: Color,
    val activeBeatGlow: Color,
    val stageOverlayBottom: Color,
    val stageOverlayTop: Color,
    val isDark: Boolean
)

val LocalLumiereColors = staticCompositionLocalOf {
    LumiereExtendedColors(
        glassSurface = ObsidianGlass,
        goldAccent = AntiqueGold,
        goldMuted = DeepBronze,
        hairlineBorder = ObsidianBorder,
        activeBeatGlow = Color(0x26D4AF37),
        stageOverlayBottom = ObsidianVoid,
        stageOverlayTop = Color(0x990B0D12),
        isDark = true
    )
}

private val ObsidianColorScheme = darkColorScheme(
    primary = AntiqueGold,
    onPrimary = ObsidianVoid,
    primaryContainer = Color(0xFF2B2412),
    onPrimaryContainer = ChampagneGold,
    secondary = ChampagneGold,
    onSecondary = ObsidianVoid,
    secondaryContainer = ObsidianElevated,
    onSecondaryContainer = SoftIvory,
    tertiary = Color(0xFF5FB49C),
    onTertiary = ObsidianVoid,
    background = ObsidianVoid,
    onBackground = SoftIvory,
    surface = ObsidianSurface,
    onSurface = SoftIvory,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = MutedIvory,
    outline = ObsidianBorder,
    error = CrimsonError,
    onError = ObsidianVoid
)

private val RoyalParchmentColorScheme = lightColorScheme(
    primary = RoyalBurgundy,
    onPrimary = Color(0xFFFDF8EE),
    primaryContainer = Color(0xFFE7D3C5),
    onPrimaryContainer = RoyalBurgundy,
    secondary = BurnishedGold,
    onSecondary = Color(0xFFFDF8EE),
    secondaryContainer = ParchmentElevated,
    onSecondaryContainer = EspressoInk,
    tertiary = Color(0xFF2F5D50),
    onTertiary = Color(0xFFFDF8EE),
    background = ParchmentBg,
    onBackground = EspressoInk,
    surface = ParchmentSurface,
    onSurface = EspressoInk,
    surfaceVariant = ParchmentElevated,
    onSurfaceVariant = SepiaInk,
    outline = ParchmentBorder,
    error = Color(0xFF9E2A2B),
    onError = Color.White
)

private val DaylightEditorialColorScheme = lightColorScheme(
    primary = EditorialBronze,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF2E8D2),
    onPrimaryContainer = Color(0xFF4A3408),
    secondary = EditorialNavy,
    onSecondary = Color.White,
    secondaryContainer = EditorialElevated,
    onSecondaryContainer = CharcoalInk,
    tertiary = Color(0xFF2E6B5E),
    onTertiary = Color.White,
    background = EditorialBg,
    onBackground = CharcoalInk,
    surface = EditorialSurface,
    onSurface = CharcoalInk,
    surfaceVariant = EditorialElevated,
    onSurfaceVariant = SlateMuted,
    outline = EditorialBorder,
    error = Color(0xFFB3261E),
    onError = Color.White
)

@Composable
fun LumiereTheme(
    themeMode: LumiereThemeMode = LumiereThemeMode.OBSIDIAN_CINEMA,
    language: AppLanguage = AppLanguage.EN,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        LumiereThemeMode.OBSIDIAN_CINEMA -> ObsidianColorScheme
        LumiereThemeMode.ROYAL_PARCHMENT -> RoyalParchmentColorScheme
        LumiereThemeMode.DAYLIGHT_EDITORIAL -> DaylightEditorialColorScheme
    }

    val extendedColors = when (themeMode) {
        LumiereThemeMode.OBSIDIAN_CINEMA -> LumiereExtendedColors(
            glassSurface = ObsidianGlass,
            goldAccent = AntiqueGold,
            goldMuted = DeepBronze,
            hairlineBorder = ObsidianBorder,
            activeBeatGlow = Color(0x28D4AF37),
            stageOverlayBottom = ObsidianVoid,
            stageOverlayTop = Color(0x990B0D12),
            isDark = true
        )
        LumiereThemeMode.ROYAL_PARCHMENT -> LumiereExtendedColors(
            glassSurface = ParchmentGlass,
            goldAccent = BurnishedGold,
            goldMuted = RoyalBurgundy,
            hairlineBorder = ParchmentBorder,
            activeBeatGlow = Color(0x227A1F2B),
            stageOverlayBottom = ParchmentBg,
            stageOverlayTop = Color(0x70241B14),
            isDark = false
        )
        LumiereThemeMode.DAYLIGHT_EDITORIAL -> LumiereExtendedColors(
            glassSurface = EditorialGlass,
            goldAccent = EditorialBronze,
            goldMuted = EditorialNavy,
            hairlineBorder = EditorialBorder,
            activeBeatGlow = Color(0x1F9A721E),
            stageOverlayBottom = EditorialBg,
            stageOverlayTop = Color(0x66171A21),
            isDark = false
        )
    }

    val layoutDirection = if (language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalLumiereColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = lumiereTypography(language),
            content = content
        )
    }
}
