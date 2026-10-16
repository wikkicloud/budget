package com.budget.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LavenderPrimaryDark,
    onPrimary = LavenderOnPrimaryDark,
    primaryContainer = LavenderPrimaryContainerDark,
    onPrimaryContainer = LavenderOnPrimaryContainerDark,
    secondary = LavenderSecondaryDark,
    tertiary = LavenderTertiaryDark,
    error = LavenderErrorDark,
    background = LavenderBackgroundDark,
    onBackground = LavenderOnBackgroundDark,
    surface = LavenderSurfaceDark,
    onSurface = LavenderOnBackgroundDark,
    surfaceVariant = LavenderSurfaceVariantDark,
    onSurfaceVariant = LavenderOnSurfaceVariantDark,
    outline = LavenderOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = LavenderPrimary,
    onPrimary = LavenderOnPrimary,
    primaryContainer = LavenderPrimaryContainer,
    onPrimaryContainer = LavenderOnPrimaryContainer,
    secondary = LavenderSecondary,
    tertiary = LavenderTertiary,
    error = LavenderError,
    background = LavenderBackground,
    onBackground = LavenderOnBackground,
    surface = LavenderSurface,
    onSurface = LavenderOnBackground,
    surfaceVariant = LavenderSurfaceVariant,
    onSurfaceVariant = LavenderOnSurfaceVariant,
    outline = LavenderOutline
)

@Composable
fun BudgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Динамические цвета Material You отключены: приложение целиком в лавандовой гамме.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val analyticsColors = if (darkTheme) DarkAnalyticsColors else LightAnalyticsColors

    CompositionLocalProvider(LocalAnalyticsColors provides analyticsColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun analyticsColors(): AnalyticsColors = LocalAnalyticsColors.current
