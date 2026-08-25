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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.k.hosken.navipulse.data.TripLog
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
    onTripClicked: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val isTracking by viewModel.isTracking.collectAsState()
    val totalDistance by viewModel.totalDistanceKm.collectAsState()
    val elapsedTime by viewModel.elapsedTimeMs.collectAsState()
    val trips by viewModel.allTrips.collectAsState()

    var showCategoryDialog by remember { mutableStateOf(false) }

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

    // Business vs Personal stats calculation
    val totalKm = trips.sumOf { it.distanceKm }
    val businessKm = trips.filter { it.isBusiness }.sumOf { it.distanceKm }
    val personalKm = totalKm - businessKm

    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Classify Trip Purpose") },
            text = { Text("Is this trip for Business or Personal use?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.stopTracking(isBusiness = true)
                    showCategoryDialog = false
                }) { Text("Business") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    viewModel.stopTracking(isBusiness = false)
                    showCategoryDialog = false
                }) { Text("Personal") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NaviPulse", fontWeight = FontWeight.Bold) },
                actions = {
                    Row(modifier = Modifier.padding(end = 8.dp)) {
                        if (trips.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { CsvExporter.exportAndShareTrips(context, trips) },
                                modifier = Modifier.padding(end = 4.dp)
                            ) { Text("CSV", fontSize = 12.sp) }
                            OutlinedButton(
                                onClick = { PdfExporter.exportAndSharePdf(context, trips) },
                                modifier = Modifier.padding(end = 4.dp)
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
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
                        Text(String.format("%.1f km", totalKm), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Business", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text(String.format("%.1f km", businessKm), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Column {
                        Text("Personal", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text(String.format("%.1f km", personalKm), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
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
                                    text = String.format("%.2f km", totalDistance),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatDuration(elapsedTime),
                                    fontSize = 18.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (isTracking) {
                                    showCategoryDialog = true
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
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (trip.isBusiness) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = if (trip.isBusiness) "Business" else "Personal",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateFormat.format(Date(trip.startTimestamp)),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = String.format("%.2f km", trip.distanceKm),
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