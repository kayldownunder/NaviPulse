package com.k.hosken.navipulse.util

import android.content.Context
import android.net.Uri
import com.k.hosken.navipulse.data.TripLog
import org.json.JSONArray
import org.json.JSONObject

/**
 * Exports/imports the full trip history as a JSON file the user picks via the system file
 * picker (Storage Access Framework), so it can be saved anywhere - Google Drive, local
 * storage, etc. - and restored later on a new or reset device.
 */
object BackupManager {

    private const val BACKUP_VERSION = 1

    fun exportBackup(context: Context, uri: Uri, trips: List<TripLog>) {
        val tripsArray = JSONArray()
        for (trip in trips) {
            tripsArray.put(
                JSONObject().apply {
                    put("startTimestamp", trip.startTimestamp)
                    put("endTimestamp", trip.endTimestamp)
                    put("distanceKm", trip.distanceKm)
                    put("durationMs", trip.durationMs)
                    put("avgSpeedKmh", trip.avgSpeedKmh)
                    put("maxSpeedKmh", trip.maxSpeedKmh)
                    put("movingTimeMs", trip.movingTimeMs)
                    put("routePointsCsv", trip.routePointsCsv)
                    put("startAddress", trip.startAddress)
                    put("endAddress", trip.endAddress)
                    put("notes", trip.notes)
                }
            )
        }
        val root = JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("trips", tripsArray)
        }

        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(root.toString(2).toByteArray())
        } ?: throw IllegalStateException("Could not open the selected file for writing")
    }

    /** Returns the trips found in the backup file, each with id=0 so Room assigns fresh ids on insert. */
    fun importBackup(context: Context, uri: Uri): List<TripLog> {
        val body = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
            ?: throw IllegalStateException("Could not open the selected file for reading")

        val root = JSONObject(body)
        val tripsArray = root.getJSONArray("trips")

        return buildList {
            for (i in 0 until tripsArray.length()) {
                val obj = tripsArray.getJSONObject(i)
                add(
                    TripLog(
                        id = 0,
                        startTimestamp = obj.getLong("startTimestamp"),
                        endTimestamp = obj.getLong("endTimestamp"),
                        distanceKm = obj.getDouble("distanceKm"),
                        durationMs = obj.getLong("durationMs"),
                        avgSpeedKmh = obj.optDouble("avgSpeedKmh", 0.0),
                        maxSpeedKmh = obj.optDouble("maxSpeedKmh", 0.0),
                        movingTimeMs = obj.optLong("movingTimeMs", 0L),
                        routePointsCsv = obj.optString("routePointsCsv", ""),
                        startAddress = obj.optString("startAddress", "Unknown Location"),
                        endAddress = obj.optString("endAddress", "Unknown Location"),
                        notes = obj.optString("notes", "")
                    )
                )
            }
        }
    }
}
