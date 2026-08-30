package com.k.hosken.navipulse.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fuel_logs")
data class FuelLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateRefuelled: Long,
    val litres: Double,
    val pricePerLitre: Double,
    val totalPrice: Double,
    /** Distance (in km) logged since the previous fuel-up; resets to 0 with each new fuel log. */
    val distanceKmSinceLastFuelUp: Double = 0.0,
    /** Distance-weighted average speed (km/h) across trips logged on the tank being replaced. */
    val avgSpeedKmhSinceLastFuelUp: Double = 0.0,
    /** Highest trip top speed (km/h) recorded on the tank being replaced. */
    val maxSpeedKmhSinceLastFuelUp: Double = 0.0
)

/** Trips logged between the previous fuel-up (if any) and [dateRefuelled] — i.e. on the tank being replaced. */
fun tripsSinceLastFuelUp(trips: List<TripLog>, fuelLogs: List<FuelLog>, dateRefuelled: Long): List<TripLog> {
    val previousFuelUpDate = fuelLogs
        .filter { it.dateRefuelled < dateRefuelled }
        .maxOfOrNull { it.dateRefuelled } ?: 0L
    return trips.filter { it.startTimestamp in (previousFuelUpDate + 1)..dateRefuelled }
}

/**
 * Sums trip distances (km) logged between the previous fuel-up (if any) and [dateRefuelled] —
 * i.e. how far was travelled on the tank being replaced.
 */
fun distanceKmSinceLastFuelUp(trips: List<TripLog>, fuelLogs: List<FuelLog>, dateRefuelled: Long): Double {
    return tripsSinceLastFuelUp(trips, fuelLogs, dateRefuelled).sumOf { it.distanceKm }
}

/**
 * Average speed (km/h) across trips logged on the tank being replaced, counting only time the
 * vessel was underway (above the minimum-moving-speed threshold) — not idle/stationary time.
 */
fun avgSpeedKmhSinceLastFuelUp(trips: List<TripLog>, fuelLogs: List<FuelLog>, dateRefuelled: Long): Double {
    val relevantTrips = tripsSinceLastFuelUp(trips, fuelLogs, dateRefuelled)
    val movingTimeHours = relevantTrips.sumOf { it.movingTimeMs } / 3_600_000.0
    return if (movingTimeHours > 0) relevantTrips.sumOf { it.distanceKm } / movingTimeHours else 0.0
}

/** Highest trip top speed (km/h) recorded on the tank being replaced. */
fun maxSpeedKmhSinceLastFuelUp(trips: List<TripLog>, fuelLogs: List<FuelLog>, dateRefuelled: Long): Double {
    return tripsSinceLastFuelUp(trips, fuelLogs, dateRefuelled).maxOfOrNull { it.maxSpeedKmh } ?: 0.0
}

/** Fuel economy for this fuel-up, in liters used per nautical mile — null if no distance was logged. */
fun FuelLog.litresPerNauticalMile(): Double? {
    val nauticalMiles = DistanceUnit.NM.fromKm(distanceKmSinceLastFuelUp)
    return if (nauticalMiles > 0) litres / nauticalMiles else null
}

/**
 * Fuel economy across every fuel-up on record, in liters used per nautical mile — total litres
 * over total distance, not an average of each fuel-up's rate, so a single short tank doesn't skew
 * the figure as much as a plain mean would. Null when no distance has been logged yet.
 */
fun List<FuelLog>.averageLitresPerNauticalMile(): Double? {
    val totalNauticalMiles = sumOf { DistanceUnit.NM.fromKm(it.distanceKmSinceLastFuelUp) }
    return if (totalNauticalMiles > 0) sumOf { it.litres } / totalNauticalMiles else null
}
