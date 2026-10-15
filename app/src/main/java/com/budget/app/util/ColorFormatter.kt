package com.budget.app.util

import androidx.compose.ui.graphics.Color

/** Разбор hex-цвета категории вида "#4CAF50" в [Color]. */
object ColorFormatter {

    /**
     * @return цвет или [fallback], если строка не является hex-цветом
     */
    fun parse(hex: String?, fallback: Color): Color {
        if (hex == null) return fallback

        val cleaned = hex.removePrefix("#")
        if (cleaned.length != 6 && cleaned.length != 8) return fallback

        val value = cleaned.toLongOrNull(radix = 16) ?: return fallback

        return if (cleaned.length == 6) {
            Color(value or 0xFF000000L)
        } else {
            Color(value)
        }
    }
}