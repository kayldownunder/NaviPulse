package com.k.hosken.navipulse.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.k.hosken.navipulse.data.TripLog
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    fun exportAndShareTrips(context: Context, trips: List<TripLog>) {
        val fileName = "NaviPulse_${System.currentTimeMillis()}.csv"
        val exportFile = File(context.cacheDir, fileName)

        try {
            val writer = FileWriter(exportFile)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            // Header row
            writer.append("ID,Start Time,End Time,Distance (km),Duration (mm:ss),Start Address,End Address,Notes\n")

            // Data rows
            for (trip in trips) {
                val startTimeStr = dateFormat.format(Date(trip.startTimestamp))
                val endTimeStr = dateFormat.format(Date(trip.endTimestamp))
                val durationSec = trip.durationMs / 1000
                val durationStr = String.format("%02d:%02d", durationSec / 60, durationSec % 60)

                writer.append("${trip.id},")
                writer.append("${csvField(startTimeStr)},")
                writer.append("${csvField(endTimeStr)},")
                writer.append("${String.format(Locale.US, "%.2f", trip.distanceKm)},")
                writer.append("${csvField(durationStr)},")
                writer.append("${csvField(trip.startAddress)},")
                writer.append("${csvField(trip.endAddress)},")
                writer.append("${csvField(trip.notes)}\n")
            }

            writer.flush()
            writer.close()

            // Share Intent via FileProvider
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exportFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "NaviPulse Export")
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Export CSV Trip Log"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Quotes a CSV field and escapes embedded quotes so it can't corrupt the row. */
    private fun csvField(value: String): String {
        return "\"${value.replace("\"", "\"\"")}\""
    }
}