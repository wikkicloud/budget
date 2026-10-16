package com.budget.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

@Immutable
data class AnalyticsColors(
    val accent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val income: Color,
    val expense: Color
)

val LightAnalyticsColors = AnalyticsColors(
    accent = Color(0xFF76558F),
    accentContainer = Color(0xFFEEDBFF),
    onAccentContainer = Color(0xFF2B123F),
    surface = Color(0xFFFFF7FF),
    surfaceVariant = Color(0xFFF2EAF5),
    onSurfaceVariant = Color(0xFF514656),
    income = Color(0xFF76558F),
    expense = Color(0xFFB56B9A)
)

val DarkAnalyticsColors = AnalyticsColors(
    accent = Color(0xFFDDB8F5),
    accentContainer = Color(0xFF563B69),
    onAccentContainer = Color(0xFFF6DDFF),
    surface = Color(0xFF19121C),
    surfaceVariant = Color(0xFF302635),
    onSurfaceVariant = Color(0xFFD4C4D8),
    income = Color(0xFFDDB8F5),
    expense = Color(0xFFE5A5CA)
)

val LocalAnalyticsColors = staticCompositionLocalOf { LightAnalyticsColors }
