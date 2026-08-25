package com.k.hosken.navipulse.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.k.hosken.navipulse.data.TripLog
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun exportAndSharePdf(context: Context, trips: List<TripLog>) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            textSize = 20f
            isFakeBoldText = true
            color = Color.BLACK
        }
        val subTitlePaint = Paint().apply {
            textSize = 12f
            color = Color.DKGRAY
        }
        val textPaint = Paint().apply {
            textSize = 10f
            color = Color.BLACK
        }
        val headerPaint = Paint().apply {
            textSize = 10f
            isFakeBoldText = true
            color = Color.BLACK
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        val notePaint = Paint().apply {
            textSize = 9f
            textSkewX = -0.25f
            color = Color.DKGRAY
        }

        var yPos = 40f

        // Document Title
        canvas.drawText("NaviPulse - Summary Report", 40f, yPos, titlePaint)
        yPos += 20f

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        canvas.drawText("Generated on: ${dateFormat.format(Date())}", 40f, yPos, subTitlePaint)
        yPos += 25f

        // Calculate Totals
        val totalKm = trips.sumOf { it.distanceKm }
        val totalDurationMs = trips.sumOf { it.durationMs }
        val totalSec = totalDurationMs / 1000
        val totalDurationStr = String.format("%02dh %02dm", totalSec / 3600, (totalSec % 3600) / 60)

        // Summary Card
        canvas.drawRect(40f, yPos, 555f, yPos + 40f, Paint().apply { color = Color.parseColor("#F0F0F0") })
        canvas.drawText("Total Trips: ${trips.size}", 50f, yPos + 25f, headerPaint)
        canvas.drawText("Total Distance: ${String.format(Locale.US, "%.2f km", totalKm)}", 200f, yPos + 25f, headerPaint)
        canvas.drawText("Total Time: $totalDurationStr", 400f, yPos + 25f, headerPaint)
        yPos += 60f

        // Table Header
        canvas.drawText("Start Time", 40f, yPos, headerPaint)
        canvas.drawText("Distance", 220f, yPos, headerPaint)
        canvas.drawText("Duration", 340f, yPos, headerPaint)
        canvas.drawText("Location", 440f, yPos, headerPaint)
        yPos += 10f
        canvas.drawLine(40f, yPos, 555f, yPos, linePaint)
        yPos += 20f

        // Table Rows
        for (trip in trips) {
            if (yPos > 800f) break // Simple 1-page guardrail

            val startStr = dateFormat.format(Date(trip.startTimestamp))
            val distStr = String.format(Locale.US, "%.2f km", trip.distanceKm)
            val durationSec = trip.durationMs / 1000
            val durStr = String.format("%02d:%02d", durationSec / 60, durationSec % 60)

            canvas.drawText(startStr, 40f, yPos, textPaint)
            canvas.drawText(distStr, 220f, yPos, textPaint)
            canvas.drawText(durStr, 340f, yPos, textPaint)
            canvas.drawText(trip.startAddress, 440f, yPos, textPaint)
            yPos += 20f

            if (trip.notes.isNotBlank()) {
                canvas.drawText("Note: ${trip.notes}", 40f, yPos, notePaint)
                yPos += 16f
            }
        }

        document.finishPage(page)

        // Write to File & Share
        val fileName = "NaviPulse_${System.currentTimeMillis()}.pdf"
        val exportFile = File(context.cacheDir, fileName)

        try {
            document.writeTo(FileOutputStream(exportFile))
            document.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exportFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_SUBJECT, "NaviPulse PDF Report")
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Export PDF Trip Log"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}