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
    val maxSpeedKmhSinceLastFuelUp: Double = 0.0,
    /**
     * Wall-clock time the log was actually created, distinct from [dateRefuelled] (which the
     * user picks via a date-only picker and may backdate). Used to sort the log so a newly
     * added fuel-up always lands at the top, regardless of the chosen refuel date.
     */
    val createdAt: Long = 0L
)

/**
 * Trips logged between the previous fuel-up (if any) and [asOf] — i.e. on the tank being
 * replaced. [asOf] should be a real wall-clock instant (e.g. [FuelLog.createdAt]), not the
 * user-editable, date-only [FuelLog.dateRefuelled] — using a date-only value as the cutoff would
 * exclude same-day trips that happened after midnight but before the fuel-up was logged.
 */
fun tripsSinceLastFuelUp(trips: List<TripLog>, fuelLogs: List<FuelLog>, asOf: Long): List<TripLog> {
    val previousFuelUpDate = fuelLogs
        .filter { it.createdAt < asOf }
        .maxOfOrNull { it.createdAt } ?: 0L
    return trips.filter { it.startTimestamp in (previousFuelUpDate + 1)..asOf }
}

/**
 * Sums trip distances (km) logged between the previous fuel-up (if any) and [asOf] —
 * i.e. how far was travelled on the tank being replaced.
 */
fun distanceKmSinceLastFuelUp(trips: List<TripLog>, fuelLogs: List<FuelLog>, asOf: Long): Double {
    return tripsSinceLastFuelUp(trips, fuelLogs, asOf).sumOf { it.distanceKm }
}

/**
 * Sums trip moving time (ms) logged between the previous fuel-up (if any) and [asOf] —
 * i.e. how long the engine ran on the tank being replaced.
 */
fun movingTimeMsSinceLastFuelUp(trips: List<TripLog>, fuelLogs: List<FuelLog>, asOf: Long): Long {
    return tripsSinceLastFuelUp(trips, fuelLogs, asOf).sumOf { it.movingTimeMs }
}

/**
 * Average speed (km/h) across trips logged on the tank being replaced, counting only time the
 * vessel was underway (above the minimum-moving-speed threshold) — not idle/stationary time.
 */
fun avgSpeedKmhSinceLastFuelUp(trips: List<TripLog>, fuelLogs: List<FuelLog>, asOf: Long): Double {
    val relevantTrips = tripsSinceLastFuelUp(trips, fuelLogs, asOf)
    val movingTimeHours = relevantTrips.sumOf { it.movingTimeMs } / 3_600_000.0
    return if (movingTimeHours > 0) relevantTrips.sumOf { it.distanceKm } / movingTimeHours else 0.0
}

/** Highest trip top speed (km/h) recorded on the tank being replaced. */
fun maxSpeedKmhSinceLastFuelUp(trips: List<TripLog>, fuelLogs: List<FuelLog>, asOf: Long): Double {
    return tripsSinceLastFuelUp(trips, fuelLogs, asOf).maxOfOrNull { it.maxSpeedKmh } ?: 0.0
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
