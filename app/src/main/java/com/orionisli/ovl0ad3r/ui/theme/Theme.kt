package com.orionisli.ovl0ad3r.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class OvlThemeType {
    Sakura,
    Beige,
    Slate,
    Cyber,
    Matcha,
    Nord
}

enum class TerminalThemeType {
    Parchment,
    Matrix,
    Solarized,
    Monokai,
    Custom
}

data class ThemePalette(
    val isDark: Boolean,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceContainer: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val outline: Color,
    val outlineVariant: Color,
    val accent: Color,
    val accentBg: Color = primaryContainer,
    val success: Color,
    val warning: Color,
    val error: Color,
    val onError: Color,
    val queueDockBackground: Color,
    val queueDockBorder: Color,
    val badgeBackground: Color,
    val badgeContent: Color
)

data class TerminalThemePalette(
    val name: String,
    val background: Color,
    val foreground: Color,
    val border: Color = Color(0x33888888),
    val cursor: Color = foreground,
    val selection: Color = background,
    val ansiBlack: Color,
    val ansiRed: Color,
    val ansiGreen: Color,
    val ansiYellow: Color,
    val ansiBlue: Color,
    val ansiMagenta: Color,
    val ansiCyan: Color,
    val ansiWhite: Color,
    val ansiBrightBlack: Color,
    val ansiBrightRed: Color,
    val ansiBrightGreen: Color,
    val ansiBrightYellow: Color,
    val ansiBrightBlue: Color,
    val ansiBrightMagenta: Color,
    val ansiBrightCyan: Color,
    val ansiBrightWhite: Color
)

val Local0vlPalette = staticCompositionLocalOf<ThemePalette> {
    error("No Local0vlPalette provided")
}

val Local0vlTerminalPalette = staticCompositionLocalOf<TerminalThemePalette> {
    error("No Local0vlTerminalPalette provided")
}

object OvlTheme {
    val colors: ThemePalette
        @Composable
        @ReadOnlyComposable
        get() = Local0vlPalette.current

    val palette: ThemePalette
        @Composable
        @ReadOnlyComposable
        get() = Local0vlPalette.current

    val terminal: TerminalThemePalette
        @Composable
        @ReadOnlyComposable
        get() = Local0vlTerminalPalette.current

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography
}

@Composable
fun Ovl0ad3rTheme(
    themeType: OvlThemeType = OvlThemeType.Sakura,
    darkTheme: Boolean = getThemePalette(themeType).isDark,
    terminalThemeType: TerminalThemeType = TerminalThemeType.Parchment,
    customTerminalPalette: TerminalThemePalette? = null,
    content: @Composable () -> Unit
) {
    val palette = getThemePalette(themeType)
    val terminalPalette = customTerminalPalette ?: getTerminalThemePalette(terminalThemeType)

    val colorScheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.primaryContainer,
            onPrimaryContainer = palette.onPrimaryContainer,
            secondary = palette.secondary,
            onSecondary = palette.onSecondary,
            background = palette.background,
            onBackground = palette.onBackground,
            surface = palette.surface,
            onSurface = palette.onSurface,
            surfaceVariant = palette.surfaceVariant,
            onSurfaceVariant = palette.onSurfaceVariant,
            outline = palette.outline,
            outlineVariant = palette.outlineVariant,
            error = palette.error,
            onError = palette.onError
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.primaryContainer,
            onPrimaryContainer = palette.onPrimaryContainer,
            secondary = palette.secondary,
            onSecondary = palette.onSecondary,
            background = palette.background,
            onBackground = palette.onBackground,
            surface = palette.surface,
            onSurface = palette.onSurface,
            surfaceVariant = palette.surfaceVariant,
            onSurfaceVariant = palette.onSurfaceVariant,
            outline = palette.outline,
            outlineVariant = palette.outlineVariant,
            error = palette.error,
            onError = palette.onError
        )
    }

    CompositionLocalProvider(
        Local0vlPalette provides palette,
        Local0vlTerminalPalette provides terminalPalette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = OvlTypography,
            content = content
        )
    }
}

@Composable
fun `0vl0ad3rTheme`(
    themeType: OvlThemeType = OvlThemeType.Sakura,
    darkTheme: Boolean = getThemePalette(themeType).isDark,
    terminalThemeType: TerminalThemeType = TerminalThemeType.Parchment,
    customTerminalPalette: TerminalThemePalette? = null,
    content: @Composable () -> Unit
) = Ovl0ad3rTheme(themeType, darkTheme, terminalThemeType, customTerminalPalette, content)
