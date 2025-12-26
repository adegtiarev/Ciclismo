package arg.adegtiarev.ciclismo.util

fun formatDistance(metres: Double): String {
    return if (metres < 1000) {
        "${metres.toInt()} m"
    } else {
        "%.2f km".format(metres / 1000)
    }
}

fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}