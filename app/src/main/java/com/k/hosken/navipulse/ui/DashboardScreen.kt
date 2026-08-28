package com.k.hosken.navipulse.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.k.hosken.navipulse.data.DistanceUnit
import com.k.hosken.navipulse.data.TripLog
import com.k.hosken.navipulse.data.formatKm
import com.k.hosken.navipulse.data.formatKmh
import com.k.hosken.navipulse.util.CsvExporter
import com.k.hosken.navipulse.util.IconSwitcher
import com.k.hosken.navipulse.util.PdfExporter
import com.k.hosken.navipulse.util.PermissionUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onTripClicked: (Long) -> Unit = {},
    onSettingsClicked: () -> Unit = {}
) {
    val context = LocalContext.current
    val isTracking by viewModel.isTracking.collectAsState()
    val totalDistance by viewModel.totalDistanceKm.collectAsState()
    val elapsedTime by viewModel.elapsedTimeMs.collectAsState()
    val currentSpeed by viewModel.currentSpeedKmh.collectAsState()
    val trips by viewModel.allTrips.collectAsState()
    val distanceUnit by viewModel.distanceUnit.collectAsState()
    val speedUnit by viewModel.speedUnit.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grantedResults ->
        if (grantedResults[android.Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            viewModel.startTracking()
        } else {
            Toast.makeText(
                context,
                "Location permission is required to track trips",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val totalKm = trips.sumOf { it.distanceKm }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp, bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("NaviPulse", fontWeight = FontWeight.Bold, fontSize = 30.sp)
                        IconButton(onClick = onSettingsClicked) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        if (trips.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { CsvExporter.exportAndShareTrips(context, trips, distanceUnit, speedUnit) }
                            ) { Text("CSV", fontSize = 12.sp) }
                            OutlinedButton(
                                onClick = { PdfExporter.exportAndSharePdf(context, trips, distanceUnit, speedUnit) }
                            ) { Text("PDF", fontSize = 12.sp) }
                        }
                        OutlinedButton(
                            onClick = {
                                IconSwitcher.toggleIcon(context)
                                Toast.makeText(
                                    context,
                                    "App icon changed — check your home screen",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) { Text("Icon", fontSize = 12.sp) }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. Stats Overview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Logged", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text(distanceUnit.formatKm(totalKm, decimals = 1), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 2. Map Container / Tracking Status Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isTracking) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = if (isTracking) " ● TRACKING ACTIVE " else " READY TO TRACK ",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        AnimatedVisibility(visible = isTracking) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(top = 12.dp)
                            ) {
                                Text(
                                    text = distanceUnit.formatKm(totalDistance),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatDuration(elapsedTime),
                                    fontSize = 18.sp,
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = speedUnit.formatKmh(currentSpeed),
                                    fontSize = 16.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (isTracking) {
                                    viewModel.stopTracking()
                                } else if (PermissionUtils.hasAllTrackingPermissions(context)) {
                                    viewModel.startTracking()
                                } else {
                                    permissionLauncher.launch(PermissionUtils.trackingPermissions)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                if (isTracking) "■ Stop & Save Trip" else "▶ Start New Trip",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. Recent Trips Header & List
            Text(
                text = "Recent Logged Trips",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (trips.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No trips recorded yet. Tap 'Start New Trip' above to test tracking!", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(trips, key = { it.id }) { trip ->
                        TripItem(
                            trip = trip,
                            distanceUnit = distanceUnit,
                            onClick = { onTripClicked(trip.id) },
                            onDelete = { viewModel.deleteTrip(trip.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TripItem(
    trip: TripLog,
    distanceUnit: DistanceUnit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateFormat.format(Date(trip.startTimestamp)),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = distanceUnit.formatKm(trip.distanceKm),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${trip.startAddress} → ${trip.endAddress}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1
                )
            }

            IconButton(onClick = onDelete) {
                Text("🗑", fontSize = 16.sp)
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    val hours = (ms / (1000 * 60 * 60))
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}