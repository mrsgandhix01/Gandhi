package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HeyGandhiDarkColorScheme = darkColorScheme(
    primary = SwarajSaffron,
    onPrimary = ObsidianBg,
    primaryContainer = SwarajSaffronContainer,
    onPrimaryContainer = OnSwarajSaffronContainer,
    secondary = AshokaEmerald,
    onSecondary = ObsidianBg,
    secondaryContainer = AshokaEmeraldContainer,
    onSecondaryContainer = OnAshokaEmeraldContainer,
    tertiary = ChakraCyan,
    onTertiary = ObsidianBg,
    tertiaryContainer = ChakraCyanContainer,
    onTertiaryContainer = KhadiIvory,
    background = ObsidianBg,
    onBackground = KhadiIvory,
    surface = ObsidianSurface,
    onSurface = KhadiIvory,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = KhadiMuted,
    outline = ObsidianCardBorder,
    error = DangerCrimson
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HeyGandhiDarkColorScheme,
        typography = Typography,
        content = content
    )
}
