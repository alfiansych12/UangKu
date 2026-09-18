package com.example.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formatters {
    private val indonesianLocale = Locale("id", "ID")

    fun formatRupiah(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(indonesianLocale)
        // Format e.g. Rp 1.250.000
        val formatted = format.format(amount)
        return formatted
            .replace("Rp", "Rp ")
            .replace(",00", "")
            .trim()
    }

    fun formatPercentage(ratio: Double): String {
        return "${(ratio * 100).toInt()}%"
    }

    fun formatDateShort(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM", indonesianLocale)
        return sdf.format(Date(millis))
    }

    fun formatDateFull(millis: Long): String {
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", indonesianLocale)
        return sdf.format(Date(millis))
    }

    fun formatDateSimple(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", indonesianLocale)
        return sdf.format(Date(millis))
    }

    fun formatMonthYear(month: Int, year: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, month - 1)
            set(Calendar.YEAR, year)
        }
        val sdf = SimpleDateFormat("MMMM yyyy", indonesianLocale)
        return sdf.format(cal.time)
    }

    fun parseColor(hex: String?, fallback: Color = Color(0xFF1E88E5)): Color {
        if (hex.isNullOrBlank()) return fallback
        return try {
            val cleanHex = hex.removePrefix("#")
            val colorLong = cleanHex.toLong(16)
            if (cleanHex.length == 6) {
                Color(0xFF000000 or colorLong)
            } else {
                Color(colorLong)
            }
        } catch (e: Exception) {
            fallback
        }
    }

    fun getCategoryIcon(name: String?): ImageVector {
        return when (name?.lowercase()) {
            "restaurant", "fastfood", "food", "makanan" -> Icons.Default.Restaurant
            "directions_car", "transport", "transportasi" -> Icons.Default.DirectionsCar
            "shopping_cart", "shopping", "belanja" -> Icons.Default.ShoppingCart
            "bolt", "electricity", "tagihan" -> Icons.Default.Bolt
            "movie", "entertainment", "hiburan" -> Icons.Default.Movie
            "medical_services", "health", "kesehatan" -> Icons.Default.MedicalServices
            "school", "education", "pendidikan" -> Icons.Default.School
            "payments", "salary", "gaji" -> Icons.Default.Payments
            "work", "freelance", "projek" -> Icons.Default.Work
            "trending_up", "investment", "investasi" -> Icons.AutoMirrored.Filled.TrendingUp
            "card_giftcard", "gift", "bonus" -> Icons.Default.CardGiftcard
            "savings", "tabungan" -> Icons.Default.Savings
            "shield", "darurat" -> Icons.Default.Shield
            "flight_takeoff", "travel", "liburan" -> Icons.Default.FlightTakeoff
            "laptop", "gadget" -> Icons.Default.Laptop
            "account_balance", "bank" -> Icons.Default.AccountBalance
            "phone_android", "ewallet" -> Icons.Default.PhoneAndroid
            "shopping_bag" -> Icons.Default.ShoppingBag
            else -> Icons.Default.Category
        }
    }
}
