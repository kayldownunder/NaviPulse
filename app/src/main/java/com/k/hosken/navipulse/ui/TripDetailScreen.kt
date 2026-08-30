package com.k.hosken.navipulse.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.k.hosken.navipulse.data.TripLog
import com.k.hosken.navipulse.data.formatKm
import com.k.hosken.navipulse.data.formatKmh
import com.k.hosken.navipulse.util.openTripInGoogleMaps
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    tripId: Long,
    viewModel: DashboardViewModel,
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current
    var trip by remember { mutableStateOf<TripLog?>(null) }
    var notes by remember { mutableStateOf("") }
    val distanceUnit by viewModel.distanceUnit.collectAsState()
    val speedUnit by viewModel.speedUnit.collectAsState()
    val timeZoneId by viewModel.timeZoneId.collectAsState()

    LaunchedEffect(tripId) {
        val loadedTrip = viewModel.getTripById(tripId)
        loadedTrip?.let {
            trip = it
            notes = it.notes
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Text("←", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        trip?.let { currentTrip ->
            val dateFormat = remember(timeZoneId) {
                SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone(timeZoneId)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Start: ${dateFormat.format(Date(currentTrip.startTimestamp))}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "Stop: ${dateFormat.format(Date(currentTrip.endTimestamp))}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = distanceUnit.formatKm(currentTrip.distanceKm),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Avg speed: ${speedUnit.formatKmh(currentTrip.avgSpeedKmh)}  •  " +
                                "Max speed: ${speedUnit.formatKmh(currentTrip.maxSpeedKmh)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "Time underway: ${formatDuration(currentTrip.movingTimeMs)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "Start: ${currentTrip.startAddress}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "End: ${currentTrip.endAddress}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Trip Notes / Reason") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Button(
                    onClick = { openTripInGoogleMaps(context, currentTrip) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Route on Google Maps")
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        val updatedTrip = currentTrip.copy(
                            notes = notes
                        )
                        viewModel.updateTrip(updatedTrip)
                        onBackClicked()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes")
                }
            }
        }
    }
}