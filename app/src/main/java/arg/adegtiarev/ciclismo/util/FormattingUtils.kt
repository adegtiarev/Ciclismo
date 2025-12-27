package arg.adegtiarev.ciclismo.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun formatDuration(seconds: Long): String {
    val hours = TimeUnit.SECONDS.toHours(seconds)
    val minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60
    val secs = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs)
}

fun formatDistance(meters: Double): String {
    return if (meters >= 1000) {
        String.format(Locale.getDefault(), "%.2f km", meters / 1000)
    } else {
        String.format(Locale.getDefault(), "%.0f m", meters)
    }
}

fun formatDate(timestamp: Long): String {
    // Формат: 1 сентября 2025
    val sdf = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}