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

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    onPrimary = Color(0xFF003822),
    primaryContainer = CavalryGreenLight,
    onPrimaryContainer = Color(0xFFD1E8D5),
    secondary = Gold80,
    onSecondary = Color(0xFF432C00),
    secondaryContainer = Color(0xFF5F4100),
    onSecondaryContainer = Color(0xFFFFDEA8),
    tertiary = CavalryHadirCyan,
    background = MilitaryDarkBackground,
    surface = MilitaryDarkSurface,
    surfaceVariant = MilitaryDarkCard,
    onBackground = MilitaryDarkText,
    onSurface = MilitaryDarkText
)

private val LightColorScheme = lightColorScheme(
    primary = CavalryGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4EDDA),
    onPrimaryContainer = Color(0xFF0B291A),
    secondary = CavalryGold,
    onSecondary = Color.White,
    secondaryContainer = CavalryGoldContainer,
    onSecondaryContainer = Color(0xFF451A03),
    tertiary = CavalryHadirCyan,
    background = Color(0xFFF3F6F4),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8EDE9),
    onBackground = Color(0xFF191C1A),
    onSurface = Color(0xFF191C1A)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep distinctive military branding
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
