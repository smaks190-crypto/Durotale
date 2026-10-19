package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName.lowercase(Locale.ROOT)) {
        "shopping_cart", "cart" -> Icons.Default.ShoppingCart
        "directions_car", "car", "transport" -> Icons.Default.DirectionsCar
        "restaurant", "cafe", "food" -> Icons.Default.Restaurant
        "home", "house", "housing" -> Icons.Default.Home
        "favorite", "health" -> Icons.Default.Favorite
        "movie", "entertainment" -> Icons.Default.Movie
        "shopping_bag", "shopping" -> Icons.Default.ShoppingBag
        "account_balance_wallet", "wallet", "salary" -> Icons.Default.AccountBalanceWallet
        "laptop", "freelance", "work" -> Icons.Default.Laptop
        else -> Icons.Default.MoreHoriz
    }
}

fun parseColorHex(hex: String, fallback: Color = Color(0xFF78909C)): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color(0xFF000000 or colorInt)
        } else {
            Color(colorInt)
        }
    } catch (_: Exception) {
        fallback
    }
}

fun formatCurrency(amount: Double): String {
    val formatted = String.format(Locale.US, "%,.0f", amount).replace(",", " ")
    return "$formatted ₽"
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("d MMMM, HH:mm", Locale.forLanguageTag("ru"))
    return sdf.format(Date(timestamp))
}

fun formatShortDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.ROOT)
    return sdf.format(Date(timestamp))
}

@Composable
fun CategoryIconBadge(
    iconName: String,
    colorHex: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 24.dp
) {
    val catColor = parseColorHex(colorHex)
    Box(
        modifier = modifier
            .size(size)
            .background(catColor.copy(alpha = 0.2f), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getCategoryIcon(iconName),
            contentDescription = null,
            tint = catColor,
            modifier = Modifier.size(iconSize)
        )
    }
}
