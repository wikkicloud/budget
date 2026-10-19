package com.budget.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Лавандовая палитра приложения (светлая тема).
val LavenderPrimary = Color(0xFF76558F)
val LavenderOnPrimary = Color(0xFFFFFFFF)
val LavenderPrimaryContainer = Color(0xFFEEDBFF)
val LavenderOnPrimaryContainer = Color(0xFF2B123F)
val LavenderSecondary = Color(0xFF625B71)
val LavenderTertiary = Color(0xFF7D5260)
val LavenderError = Color(0xFFB3261E)
val LavenderBackground = Color(0xFFFFF7FF)
val LavenderOnBackground = Color(0xFF1D1A21)
val LavenderSurface = Color(0xFFFFF7FF)
val LavenderOnSurfaceVariant = Color(0xFF514656)
val LavenderSurfaceVariant = Color(0xFFF2EAF5)
val LavenderOutline = Color(0xFF837D8F)

// Лавандовая палитра приложения (тёмная тема).
val LavenderPrimaryDark = Color(0xFFDDB8F5)
val LavenderOnPrimaryDark = Color(0xFF432A5A)
val LavenderPrimaryContainerDark = Color(0xFF563B69)
val LavenderOnPrimaryContainerDark = Color(0xFFF6DDFF)
val LavenderSecondaryDark = Color(0xFFCCC2DC)
val LavenderTertiaryDark = Color(0xFFEFB8C8)
val LavenderErrorDark = Color(0xFFF2B8B5)
val LavenderBackgroundDark = Color(0xFF19121C)
val LavenderOnBackgroundDark = Color(0xFFE8E0E9)
val LavenderSurfaceDark = Color(0xFF19121C)
val LavenderOnSurfaceVariantDark = Color(0xFFD4C4D8)
val LavenderSurfaceVariantDark = Color(0xFF302635)
val LavenderOutlineDark = Color(0xFF9A93A5)

/**
 * Цвета «Аналитики»: акценты и цвета диаграмм.
 * Общие цвета темы (фон, карточки, текст) берутся из `MaterialTheme.colorScheme` —
 * лавандовая гамма теперь на всех экранах.
 */
@Immutable
data class AnalyticsColors(
    val accent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val income: Color,
    val expense: Color,
    /** Цвет процентов по копилкам — отдельный сегмент столбца доходов. */
    val interest: Color
)

val LightAnalyticsColors = AnalyticsColors(
    accent = LavenderPrimary,
    accentContainer = LavenderPrimaryContainer,
    onAccentContainer = LavenderOnPrimaryContainer,
    surface = LavenderSurface,
    surfaceVariant = LavenderSurfaceVariant,
    onSurfaceVariant = LavenderOnSurfaceVariant,
    income = Color(0xFF76558F),
    expense = Color(0xFFB56B9A),
    interest = Color(0xFF9B7FBC)
)

val DarkAnalyticsColors = AnalyticsColors(
    accent = LavenderPrimaryDark,
    accentContainer = LavenderPrimaryContainerDark,
    onAccentContainer = LavenderOnPrimaryContainerDark,
    surface = LavenderSurfaceDark,
    surfaceVariant = LavenderSurfaceVariantDark,
    onSurfaceVariant = LavenderOnSurfaceVariantDark,
    income = Color(0xFFDDB8F5),
    expense = Color(0xFFE5A5CA),
    interest = Color(0xFFB39DDB)
)

val LocalAnalyticsColors = staticCompositionLocalOf { LightAnalyticsColors }
