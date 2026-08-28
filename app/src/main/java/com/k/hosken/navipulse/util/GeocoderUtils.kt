package com.k.hosken.navipulse.util

import android.content.Context
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLng
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object GeocoderUtils {

    // Free public gazetteer of named marine chart features (bays, channels, reefs,
    // harbours, straits, etc.) run by the Flanders Marine Institute (VLIZ):
    // https://www.marineregions.org/gazetteer.php?p=webservices
    private const val MARINE_GAZETTEER_URL =
        "https://www.marineregions.org/rest/getGazetteerRecordsByLatLong.json/%s/%s/"

    // A record whose charted extent is wider than this (in degrees^2) is a whole sea/ocean
    // basin, not a locally specific feature - e.g. Nickol Bay is well under 1 deg^2, while the
    // Indian Ocean is in the thousands. Above this we treat it as "no specific feature found".
    private const val MAX_SPECIFIC_MARINE_FEATURE_AREA_DEG2 = 10.0

    /**
     * Resolves a recorded position to a human-readable place name: the nearest named nautical
     * chart feature (bay, channel, reef, island, harbour, river, etc.) when the point is on or
     * near water, falling back to a street address on land. Marine names are tried first because
     * a point just off a jetty or shoreline still reverse-geocodes to the nearest street, which
     * would otherwise always win even though the vessel is actually on the water.
     */
    fun getAddressFromLatLng(context: Context, location: LatLng): String {
        lookupNearestMarineFeature(location)?.let { return it }
        reverseGeocodeLand(context, location)?.let { return it }
        return "${location.latitude}, ${location.longitude}"
    }

    /** Street-level address, or null when the point has no land address (e.g. open water). */
    private fun reverseGeocodeLand(context: Context, location: LatLng): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val address = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                ?.firstOrNull() ?: return null

            val thoroughfare = address.thoroughfare ?: address.featureName
            if (thoroughfare == null && address.locality == null) return null

            // featureName and locality are sometimes the same value (e.g. an unnamed bay whose
            // only name is the locality itself) - drop the duplicate rather than showing it twice.
            val locality = address.locality?.takeIf { it != thoroughfare }

            "${thoroughfare ?: ""}, ${locality ?: ""}".trim(',', ' ').ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Queries the Marine Regions gazetteer for named features whose charted extent contains
     * this point, and returns the smallest (most locally specific) one - e.g. a bay or channel
     * name instead of the enclosing sea or ocean.
     */
    private fun lookupNearestMarineFeature(location: LatLng): String? {
        return try {
            val url = URL(
                String.format(Locale.US, MARINE_GAZETTEER_URL, location.latitude, location.longitude)
            )
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                connectTimeout = 5000
                readTimeout = 5000
            }

            val body = try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }

            val records = JSONArray(body)
            var bestName: String? = null
            var bestArea = Double.MAX_VALUE

            for (i in 0 until records.length()) {
                val record = records.getJSONObject(i)
                val name = record.optString("preferredGazetteerName").takeIf { it.isNotBlank() } ?: continue

                val minLat = record.optDouble("minLatitude", Double.NaN)
                val maxLat = record.optDouble("maxLatitude", Double.NaN)
                val minLon = record.optDouble("minLongitude", Double.NaN)
                val maxLon = record.optDouble("maxLongitude", Double.NaN)
                // Some gazetteer sources (e.g. GeoNames) store a swapped min/max longitude for a
                // handful of records - abs() keeps that from producing a negative "area" that
                // would otherwise beat every legitimately small, correctly-bounded record below.
                val area = if (!minLat.isNaN() && !maxLat.isNaN() && !minLon.isNaN() && !maxLon.isNaN()) {
                    kotlin.math.abs(maxLat - minLat) * kotlin.math.abs(maxLon - minLon)
                } else {
                    Double.MAX_VALUE
                }

                if (area < bestArea) {
                    bestArea = area
                    bestName = name
                }
            }

            // A match that's only as specific as an entire sea/ocean isn't a useful place name;
            // let it fall through to the land address (or raw coordinates) instead.
            if (bestArea > MAX_SPECIFIC_MARINE_FEATURE_AREA_DEG2) null else bestName
        } catch (e: Exception) {
            null
        }
    }
}