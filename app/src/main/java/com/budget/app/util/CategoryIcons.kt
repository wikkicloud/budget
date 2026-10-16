package com.budget.app.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.ChildFriendly
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Сопоставление ключей иконок категорий с Material Icons.
 * Ключ — строка из `CategoryEntity.iconKey`; для встроенных категорий это legacy-эмодзи.
 */
object CategoryIcons {

    /** Вектор иконки по ключу; null — если ключ не распознан (например, legacy-эмодзи без аналога). */
    fun vector(key: String?): ImageVector? = when (key.orEmpty()) {
        "restaurant", "🍽️" -> Icons.Filled.Restaurant
        "local_cafe", "☕" -> Icons.Filled.LocalCafe
        "shopping_cart", "🛍️" -> Icons.Filled.ShoppingCart
        "directions_car", "🚗" -> Icons.Filled.DirectionsCar
        "home", "🏠" -> Icons.Filled.Home
        "favorite" -> Icons.Filled.Favorite
        "health_and_safety", "💊" -> Icons.Filled.HealthAndSafety
        "sports_esports", "🎮" -> Icons.Filled.SportsEsports
        "movie" -> Icons.Filled.Movie
        "flight" -> Icons.Filled.Flight
        "school" -> Icons.Filled.School
        "work" -> Icons.Filled.Work
        "pets" -> Icons.Filled.Pets
        "local_gas_station" -> Icons.Filled.LocalGasStation
        "build" -> Icons.Filled.Build
        "phone" -> Icons.Filled.Phone
        "wifi" -> Icons.Filled.Wifi
        "fitness_center" -> Icons.Filled.FitnessCenter
        "local_hospital" -> Icons.Filled.LocalHospital
        "local_grocery_store", "🛒" -> Icons.Filled.LocalGroceryStore
        "child_friendly" -> Icons.Filled.ChildFriendly
        "music_note" -> Icons.Filled.MusicNote
        "book" -> Icons.Filled.MenuBook
        "camera_alt" -> Icons.Filled.CameraAlt
        "cake" -> Icons.Filled.Cake
        "celebration" -> Icons.Filled.Celebration
        "payments" -> Icons.Filled.Payments
        "savings" -> Icons.Filled.Savings
        "more_horiz", "📌" -> Icons.Filled.MoreHoriz
        else -> null
    }
}

/** Вектор иконки категории; null — если ключ не распознан. */
fun categoryIconVectorOrNull(key: String?): ImageVector? = CategoryIcons.vector(key)

/** Вектор иконки категории с запасным вариантом [fallback]. */
fun categoryIconVector(key: String?, fallback: ImageVector = Icons.Filled.MoreHoriz): ImageVector =
    categoryIconVectorOrNull(key) ?: fallback

/** Набор иконок для выбора: ключ и цвет по умолчанию. */
object CategoryIconOptions {
    const val defaultKey = "restaurant"

    val options: List<Pair<String, String>> = listOf(
        "restaurant" to "#FF9800",
        "local_cafe" to "#795548",
        "shopping_cart" to "#4CAF50",
        "directions_car" to "#2196F3",
        "home" to "#9C27B0",
        "favorite" to "#F44336",
        "health_and_safety" to "#F44336",
        "sports_esports" to "#E91E63",
        "movie" to "#673AB7",
        "flight" to "#03A9F4",
        "school" to "#3F51B5",
        "work" to "#607D8B",
        "pets" to "#FF5722",
        "local_gas_station" to "#607D8B",
        "build" to "#FF9800",
        "phone" to "#00BCD4",
        "wifi" to "#03A9F4",
        "fitness_center" to "#4CAF50",
        "local_hospital" to "#F44336",
        "local_grocery_store" to "#4CAF50",
        "child_friendly" to "#E91E63",
        "music_note" to "#9C27B0",
        "book" to "#3F51B5",
        "camera_alt" to "#607D8B",
        "cake" to "#EC407A",
        "celebration" to "#AB47BC",
        "payments" to "#66BB6A",
        "savings" to "#26A69A",
        "more_horiz" to "#9E9E9E"
    )

    /** Цвет по умолчанию для ключа иконки; null — если ключ неизвестен. */
    fun colorFor(key: String?): String? =
        key?.let { k -> options.firstOrNull { it.first == k }?.second }
}
