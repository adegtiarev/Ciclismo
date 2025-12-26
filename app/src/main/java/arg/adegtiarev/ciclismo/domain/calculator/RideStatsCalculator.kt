package arg.adegtiarev.ciclismo.domain.calculator

import arg.adegtiarev.ciclismo.domain.model.TrackingPoint

object RideStatsCalculator {
    // Вспомогательная функция для расчета расстояния между двумя точками
    fun calculateDistance(points: List<TrackingPoint>): Double {
        var distance = 0.0
        for (i in 0 until points.size - 1) {
            val start = points[i]
            val end = points[i + 1]

            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                start.latitude, start.longitude,
                end.latitude, end.longitude,
                results
            )
            distance += results[0]
        }
        return distance
    }

    // Логика автопаузы: если скорость меньше 0.5 м/с (1.8 км/ч)
    fun shouldAutoPause(currentSpeedMps: Float): Boolean {
        return currentSpeedMps < 0.5f
    }
}