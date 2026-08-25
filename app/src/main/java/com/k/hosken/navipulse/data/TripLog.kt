package com.k.hosken.navipulse.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_logs")
data class TripLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val distanceKm: Double,
    val durationMs: Long,
    val startAddress: String = "Unknown Location",
    val endAddress: String = "Unknown Location",
    val isBusiness: Boolean = true,
    val notes: String = ""
)