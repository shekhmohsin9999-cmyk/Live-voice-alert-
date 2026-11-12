package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SoundboxLightColorScheme = lightColorScheme(
    primary = SoundboxBlue,
    onPrimary = Color.White,
    primaryContainer = SoundboxBlueContainer,
    onPrimaryContainer = SoundboxOnBlueContainer,
    secondary = SoundboxBlueDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0EBFA),
    onSecondaryContainer = Color(0xFF093166),
    tertiary = SuccessGreen,
    onTertiary = Color.White,
    tertiaryContainer = SuccessGreenContainer,
    onTertiaryContainer = Color(0xFF064E3B),
    background = SoundboxBackground,
    onBackground = SoundboxTextPrimary,
    surface = SoundboxSurface,
    onSurface = SoundboxTextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = SoundboxTextSecondary,
    outline = SoundboxCardBorder,
    outlineVariant = Color(0xFFCBD5E1)
)

private val SoundboxDarkColorScheme = darkColorScheme(
    primary = SoundboxBlueLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF003882),
    onPrimaryContainer = Color(0xFFD3E4FD),
    secondary = Color(0xFF82B1FF),
    onSecondary = Color(0xFF00224D),
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF064E3B),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SoundboxDarkColorScheme else SoundboxLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
