package com.k.hosken.navipulse.util

import android.location.Location

object LocationUtils {
    /**
     * Calculates distance between two GPS coordinates in kilometers.
     */
    fun calculateDistanceKm(
        startLat: Double, startLng: Double,
        endLat: Double, endLng: Double
    ): Double {
        val results = FloatArray(1)
        Location.distanceBetween(startLat, startLng, endLat, endLng, results)
        return results[0] / 1000.0
    }
}