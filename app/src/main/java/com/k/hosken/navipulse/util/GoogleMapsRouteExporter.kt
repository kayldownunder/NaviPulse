package com.k.hosken.navipulse.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.google.android.gms.maps.model.LatLng
import com.k.hosken.navipulse.data.TripLog
import com.k.hosken.navipulse.data.decodeRoute
import kotlin.math.roundToInt

// Google Maps' consumer directions UI only renders a bounded number of stops reliably -
// sample the recorded track down to this many points (always keeping the first and last)
// rather than risk the deep link silently truncating or failing on a long trip.
private const val MAX_ROUTE_WAYPOINTS = 23

/**
 * Opens Google Maps with the trip's recorded track drawn as a connected route. Maps has no
 * public API to render an arbitrary GPS trace, so this approximates it via a multi-stop
 * directions deep link - expect road-snapped legs rather than an exact overlay of the track,
 * and note Maps has no marine travel mode.
 */
fun openTripInGoogleMaps(context: Context, trip: TripLog) {
    val points = trip.routePointsCsv.decodeRoute()
    if (points.size < 2) {
        Toast.makeText(context, "No recorded route for this trip", Toast.LENGTH_SHORT).show()
        return
    }

    val waypoints = sampleRoute(points, MAX_ROUTE_WAYPOINTS)
    val uri = Uri.parse(
        "https://www.google.com/maps/dir/" + waypoints.joinToString("/") { "${it.latitude},${it.longitude}" }
    )
    val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

/** Evenly samples down to [maxPoints], always keeping the first and last point. */
private fun sampleRoute(points: List<LatLng>, maxPoints: Int): List<LatLng> {
    if (points.size <= maxPoints) return points
    val step = (points.size - 1).toDouble() / (maxPoints - 1)
    return (0 until maxPoints).map { i -> points[(i * step).roundToInt()] }
}
