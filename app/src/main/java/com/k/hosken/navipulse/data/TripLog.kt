package com.k.hosken.navipulse.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

@Entity(tableName = "trip_logs")
data class TripLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val distanceKm: Double,
    val durationMs: Long,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    /** Wall-clock time (ms) spent moving above the minimum-moving-speed threshold during this trip. */
    val movingTimeMs: Long = 0L,
    val startAddress: String = "Unknown Location",
    val endAddress: String = "Unknown Location",
    val notes: String = "",
    /** Recorded GPS track for this trip, encoded via [List.encodeRoute] - empty for trips logged before this was added. */
    val routePointsCsv: String = ""
)

/** Encodes a recorded GPS track as "lat,lng;lat,lng;..." for storage on [TripLog.routePointsCsv]. */
fun List<LatLng>.encodeRoute(): String = joinToString(";") { "${it.latitude},${it.longitude}" }

/** Decodes a route previously encoded by [encodeRoute]; malformed or blank input yields an empty list. */
fun String.decodeRoute(): List<LatLng> {
    if (isBlank()) return emptyList()
    return split(";").mapNotNull { pair ->
        val parts = pair.split(",")
        val lat = parts.getOrNull(0)?.toDoubleOrNull()
        val lng = parts.getOrNull(1)?.toDoubleOrNull()
        if (lat != null && lng != null) LatLng(lat, lng) else null
    }
}