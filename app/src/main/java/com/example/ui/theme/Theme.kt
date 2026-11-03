package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = Color(0xFF002029),
    primaryContainer = JarvisSurfaceElevated,
    onPrimaryContainer = JarvisCyan,
    secondary = JarvisAmberGold,
    onSecondary = Color(0xFF261900),
    secondaryContainer = Color(0xFF2B210B),
    onSecondaryContainer = JarvisAmberGold,
    tertiary = JarvisElectricBlue,
    onTertiary = Color(0xFF001E2F),
    background = JarvisObsidian,
    onBackground = JarvisTextPrimary,
    surface = JarvisDeepNavy,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisSurfaceCard,
    onSurfaceVariant = JarvisTextSecondary,
    outline = JarvisBorder,
    error = JarvisCrimson,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}
