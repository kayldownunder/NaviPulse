package com.k.hosken.navipulse.ui

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.k.hosken.navipulse.R
import com.k.hosken.navipulse.data.DistanceUnit
import com.k.hosken.navipulse.data.FuelLog
import com.k.hosken.navipulse.data.SpeedUnit
import com.k.hosken.navipulse.data.TripLog
import com.k.hosken.navipulse.data.averageLitresPerNauticalMile
import com.k.hosken.navipulse.data.distanceKmSinceLastFuelUp
import com.k.hosken.navipulse.data.formatKm
import com.k.hosken.navipulse.data.formatKmh
import com.k.hosken.navipulse.data.fromKm
import com.k.hosken.navipulse.data.litresPerNauticalMile
import com.k.hosken.navipulse.data.movingTimeMsSinceLastFuelUp
import com.k.hosken.navipulse.ui.theme.toFontFamily
import com.k.hosken.navipulse.util.PermissionUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onTripClicked: (Long) -> Unit = {},
    onSettingsClicked: () -> Unit = {},
    onExportClicked: () -> Unit = {}
) {
    val context = LocalContext.current
    val isTracking by viewModel.isTracking.collectAsState()
    val totalDistance by viewModel.totalDistanceKm.collectAsState()
    val elapsedTime by viewModel.elapsedTimeMs.collectAsState()
    val currentSpeed by viewModel.currentSpeedKmh.collectAsState()
    val currentTripMaxSpeed by viewModel.currentTripMaxSpeedKmh.collectAsState()
    val currentTripAvgSpeed by viewModel.currentTripAvgSpeedKmh.collectAsState()
    val engineRunTime by viewModel.engineRunTimeMs.collectAsState()
    val trips by viewModel.allTrips.collectAsState()
    val fuelLogs by viewModel.allFuelLogs.collectAsState()
    val distanceUnit by viewModel.distanceUnit.collectAsState()
    val speedUnit by viewModel.speedUnit.collectAsState()
    val backgroundImagePath by viewModel.backgroundImagePath.collectAsState()
    val timeZoneId by viewModel.timeZoneId.collectAsState()
    val summaryTextSize by viewModel.summaryTextSize.collectAsState()
    val titleTextSize by viewModel.titleTextSize.collectAsState()
    val valueTextSize by viewModel.valueTextSize.collectAsState()
    val appFont by viewModel.appFont.collectAsState()
    val summaryFontSize = summaryTextSize.sp.sp
    val titleFontSize = titleTextSize.sp.sp
    val valueFontSize = valueTextSize.sp.sp
    val summaryFontFamily = appFont.toFontFamily()

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

    val timelineItems = remember(trips, fuelLogs) {
        (trips.map { TimelineItem.Trip(it) } + fuelLogs.map { TimelineItem.Fuel(it) })
            .sortedByDescending { it.timestamp }
    }

    var showFuelUpDialog by rememberSaveable { mutableStateOf(false) }

    // Collapses the header image + stats summary as the trip/fuel log below is scrolled up,
    // and expands them back once the log is scrolled back to its own top. The header and the
    // log share one nested-scroll region (rather than the header living in Scaffold's
    // separate topBar slot) so the log's drag gestures actually reach this connection.
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    var headerMaxHeightPx by remember { mutableFloatStateOf(0f) }
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    // Shared by the log's nested-scroll connection below and by the scrollable modifier on
    // the header itself, so dragging directly on the header image/stats card also collapses
    // and expands it, not just dragging on the log underneath.
    fun consumeHeaderDelta(delta: Float): Float {
        val atListTop = listState.firstVisibleItemIndex == 0 &&
            listState.firstVisibleItemScrollOffset == 0
        if (delta < 0 && headerHeightPx > 0f) {
            val newHeight = (headerHeightPx + delta).coerceIn(0f, headerMaxHeightPx)
            val consumed = newHeight - headerHeightPx
            headerHeightPx = newHeight
            return consumed
        }
        if (delta > 0 && headerHeightPx < headerMaxHeightPx && atListTop) {
            val newHeight = (headerHeightPx + delta).coerceIn(0f, headerMaxHeightPx)
            val consumed = newHeight - headerHeightPx
            headerHeightPx = newHeight
            return consumed
        }
        return 0f
    }
    val headerScrollConnection = remember(listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return Offset(0f, consumeHeaderDelta(available.y))
            }
        }
    }
    val headerScrollableState = rememberScrollableState { delta -> consumeHeaderDelta(delta) }

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                    modifier = Modifier.weight(1f).height(44.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        if (isTracking) "■ Stop" else "▶ Start Trip",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                Button(
                    onClick = { showFuelUpDialog = true },
                    modifier = Modifier.weight(1f).height(44.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("⛽ Fuel Up", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onExportClicked,
                    modifier = Modifier.weight(1f).height(44.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Export", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(headerScrollConnection)
        ) {
            // Collapsible region: header image + stats summary + live tracking card. Shrinks
            // to nothing and fades out as the log below is scrolled, and grows back once the
            // log is scrolled back to its own top (see headerScrollConnection above).
            if (headerMaxHeightPx == 0f || headerHeightPx > 0.5f) {
                val collapseFraction = if (headerMaxHeightPx > 0f) {
                    1f - (headerHeightPx / headerMaxHeightPx)
                } else 0f

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scrollable(state = headerScrollableState, orientation = Orientation.Vertical)
                        .onSizeChanged { size ->
                            // Keeps tracking the natural (unclipped) content height while at
                            // rest, rather than locking in a single early measurement - the
                            // content can grow after that first frame (e.g. a Google Font
                            // finishing its download with different metrics, or the live
                            // tracking card appearing), and a stale max height would otherwise
                            // permanently clip the bottom of this block once collapsing was
                            // wired up. Only once the user starts collapsing it (headerHeightPx
                            // drops below the max) do we stop re-measuring and hold a fixed
                            // height for the shrink animation.
                            if (headerHeightPx >= headerMaxHeightPx) {
                                headerMaxHeightPx = size.height.toFloat()
                                headerHeightPx = headerMaxHeightPx
                            }
                        }
                        .let { base ->
                            if (headerHeightPx < headerMaxHeightPx) {
                                base.height(with(density) { headerHeightPx.toDp() })
                            } else base
                        }
                        .graphicsLayer {
                            val scale = 1f - collapseFraction
                            alpha = scale
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        },
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header image (edge-to-edge, no horizontal padding)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val customBackground = remember(backgroundImagePath) {
                            backgroundImagePath?.let { path -> BitmapFactory.decodeFile(path)?.asImageBitmap() }
                        }
                        if (customBackground != null) {
                            Image(
                                bitmap = customBackground,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.dashboard_background_default),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                        )
                        CompositionLocalProvider(LocalContentColor provides Color.White) {
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
                                // Keeps the header/background image the same height it was when
                                // the Start Trip/Fuel Up/Export buttons lived here, now that
                                // they've moved to the bottom bar.
                                Spacer(modifier = Modifier.height(89.dp))
                            }
                        }
                    }

                    // Fuel Up entry card - replaces the stats summary card in place while
                    // logging a fuel-up, which returns to view once it's saved or cancelled.
                    if (showFuelUpDialog) {
                        FuelUpCard(
                            trips = trips,
                            fuelLogs = fuelLogs,
                            fontSize = valueFontSize,
                            onCancel = { showFuelUpDialog = false },
                            onSave = { dateRefuelled, litres, pricePerLitre ->
                                viewModel.saveFuelLog(dateRefuelled, litres, pricePerLitre)
                                showFuelUpDialog = false
                                Toast.makeText(context, "Fuel entry saved", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    // Stats Overview Card - hidden while a trip is being recorded so the
                    // live tracking card below has the spotlight, and while logging a fuel-up.
                    AnimatedVisibility(visible = !isTracking && !showFuelUpDialog) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                SummaryStat(
                                    label = "Total Travel Distance",
                                    value = distanceUnit.formatKm(totalKm, decimals = 1),
                                    labelFontSize = summaryFontSize,
                                    valueFontSize = summaryFontSize
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                SummaryStat(
                                    label = "Total Engine Run Time",
                                    value = formatDuration(trips.sumOf { it.movingTimeMs }),
                                    labelFontSize = summaryFontSize,
                                    valueFontSize = summaryFontSize
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                SummaryStat(
                                    label = "Engine Run Time Since Refuel",
                                    value = formatDuration(
                                        movingTimeMsSinceLastFuelUp(trips, fuelLogs, System.currentTimeMillis()) +
                                            (if (isTracking) engineRunTime else 0L)
                                    ),
                                    labelFontSize = summaryFontSize,
                                    valueFontSize = summaryFontSize
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                SummaryStat(
                                    label = "Distance Travelled Since Refuel",
                                    value = distanceUnit.formatKm(
                                        distanceKmSinceLastFuelUp(trips, fuelLogs, System.currentTimeMillis())
                                    ),
                                    labelFontSize = summaryFontSize,
                                    valueFontSize = summaryFontSize
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                SummaryStat(
                                    label = "Average Fuel Economy",
                                    value = fuelLogs.averageLitresPerNauticalMile()
                                        ?.let { String.format(Locale.getDefault(), "%.2f L/NM", it) }
                                        ?: "N/A",
                                    labelFontSize = summaryFontSize,
                                    valueFontSize = summaryFontSize
                                )
                            }
                        }
                    }

                    // Live Tracking Stats (only shown while a trip is being recorded)
                    AnimatedVisibility(visible = isTracking) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Total Distance Traveled",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontFamily = summaryFontFamily,
                                            fontSize = summaryFontSize,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = distanceUnit.formatKm(totalDistance),
                                            fontFamily = summaryFontFamily,
                                            fontSize = summaryFontSize,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Top Speed",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontFamily = summaryFontFamily,
                                            fontSize = summaryFontSize,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = speedUnit.formatKmh(currentTripMaxSpeed),
                                            fontFamily = summaryFontFamily,
                                            fontSize = summaryFontSize,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Time Elapsed",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontFamily = summaryFontFamily,
                                            fontSize = summaryFontSize,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = formatDuration(elapsedTime),
                                            fontFamily = summaryFontFamily,
                                            fontSize = summaryFontSize,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Current Speed",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontFamily = summaryFontFamily,
                                            fontSize = summaryFontSize,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = speedUnit.formatKmh(currentSpeed),
                                            fontFamily = summaryFontFamily,
                                            fontSize = summaryFontSize,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Average Speed",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontFamily = summaryFontFamily,
                                            fontSize = summaryFontSize,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = speedUnit.formatKmh(currentTripAvgSpeed),
                                            fontFamily = summaryFontFamily,
                                            fontSize = summaryFontSize,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Time Underway",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontFamily = summaryFontFamily,
                                            fontSize = summaryFontSize,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = formatDuration(engineRunTime),
                                            fontFamily = summaryFontFamily,
                                            fontSize = summaryFontSize,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Recent Trips Header & List - this list is what drives the collapse above.
            // The whole section (header, labels, and values) scales with the "Recent Logged
            // Trips" Fonts setting - see titleFontSize passed into TripItem/FuelItem below.
            Text(
                text = "Recent Logged Trips",
                style = MaterialTheme.typography.titleMedium,
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )

            if (timelineItems.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No trips recorded yet. Tap 'Start New Trip' above to test tracking!", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                ) {
                    items(
                        timelineItems,
                        key = {
                            when (it) {
                                is TimelineItem.Trip -> "trip_${it.trip.id}"
                                is TimelineItem.Fuel -> "fuel_${it.fuelLog.id}"
                            }
                        }
                    ) { item ->
                        when (item) {
                            is TimelineItem.Trip -> TripItem(
                                trip = item.trip,
                                distanceUnit = distanceUnit,
                                speedUnit = speedUnit,
                                timeZoneId = timeZoneId,
                                // Both the labels and the values in this list scale together
                                // with the "Recent Logged Trips" setting.
                                titleFontSize = titleFontSize,
                                valueFontSize = titleFontSize,
                                onClick = { onTripClicked(item.trip.id) },
                                onDelete = { viewModel.deleteTrip(item.trip.id) }
                            )
                            is TimelineItem.Fuel -> FuelItem(
                                fuelLog = item.fuelLog,
                                speedUnit = speedUnit,
                                timeZoneId = timeZoneId,
                                titleFontSize = titleFontSize,
                                valueFontSize = titleFontSize,
                                onDelete = { viewModel.deleteFuelLog(item.fuelLog.id) },
                                onEdit = { dateRefuelled, litres, pricePerLitre ->
                                    viewModel.updateFuelLog(item.fuelLog.id, dateRefuelled, litres, pricePerLitre)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

}

/**
 * Inline fuel-up entry form shown in place of the stats summary card while logging a
 * fuel-up (rather than as a popup dialog), so it sits directly under the header image.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelUpCard(
    trips: List<TripLog>,
    fuelLogs: List<FuelLog>,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onCancel: () -> Unit,
    onSave: (dateRefuelled: Long, litres: Double, pricePerLitre: Double) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    var showDatePicker by remember { mutableStateOf(false) }
    var litresText by rememberSaveable { mutableStateOf("") }
    var priceText by rememberSaveable { mutableStateOf("") }

    val litres = litresText.toDoubleOrNull()
    val pricePerLitre = priceText.toDoubleOrNull()
    val totalPrice = if (litres != null && pricePerLitre != null) litres * pricePerLitre else null

    // Mirrors the cutoff saveFuelLog will actually use (the save-time instant, not the
    // date-only picker value), so this preview matches what gets persisted.
    val nauticalMilesThisFuelUp = remember(trips, fuelLogs) {
        DistanceUnit.NM.fromKm(distanceKmSinceLastFuelUp(trips, fuelLogs, System.currentTimeMillis()))
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Fuel Up",
                style = MaterialTheme.typography.titleMedium,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            OutlinedTextField(
                value = datePickerState.selectedDateMillis?.let { dateFormat.format(Date(it)) }
                    ?: dateFormat.format(Date()),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date Re-fuelled", fontSize = fontSize) },
                textStyle = LocalTextStyle.current.copy(fontSize = fontSize),
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Select date")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = litresText,
                onValueChange = { litresText = it },
                label = { Text("Litres of Unleaded", fontSize = fontSize) },
                textStyle = LocalTextStyle.current.copy(fontSize = fontSize),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("Price Per Litre", fontSize = fontSize) },
                leadingIcon = { Text("$", fontSize = fontSize) },
                textStyle = LocalTextStyle.current.copy(fontSize = fontSize),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Price",
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = fontSize,
                        color = Color.Gray
                    )
                    Text(
                        text = "$" + String.format(Locale.getDefault(), "%.2f", totalPrice ?: 0.0),
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Nautical Miles This Fuel-Up",
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = fontSize,
                        color = Color.Gray
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f NM", nauticalMilesThisFuelUp),
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Cancel", fontSize = fontSize) }
                Button(
                    enabled = litres != null && litres > 0 && pricePerLitre != null && pricePerLitre > 0,
                    onClick = {
                        val dateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                        onSave(dateMillis, litres!!, pricePerLitre!!)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Save", fontSize = fontSize) }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("OK", fontSize = fontSize) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", fontSize = fontSize) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Label stacked above its value (rather than side-by-side) so the row keeps working at
 * any selected summary text size - long labels just wrap in place instead of squeezing
 * or overlapping the value next to them.
 */
@Composable
private fun SummaryStat(
    label: String,
    value: String,
    labelFontSize: androidx.compose.ui.unit.TextUnit,
    valueFontSize: androidx.compose.ui.unit.TextUnit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontSize = labelFontSize,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = valueFontSize,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

private sealed class TimelineItem(val timestamp: Long) {
    class Trip(val trip: TripLog) : TimelineItem(trip.startTimestamp)
    class Fuel(val fuelLog: FuelLog) : TimelineItem(fuelLog.createdAt)
}

@Composable
fun FuelItem(
    fuelLog: FuelLog,
    speedUnit: SpeedUnit,
    timeZoneId: String,
    titleFontSize: androidx.compose.ui.unit.TextUnit,
    valueFontSize: androidx.compose.ui.unit.TextUnit,
    onDelete: () -> Unit,
    onEdit: (dateRefuelled: Long, litres: Double, pricePerLitre: Double) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    val dateFormat = remember(timeZoneId) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(timeZoneId)
        }
    }
    val loggedAtFormat = remember(timeZoneId) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(timeZoneId)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            val fuelEconomyText = fuelLog.litresPerNauticalMile()
                ?.let { String.format(Locale.getDefault(), "%.2f L/NM", it) }
                ?: "N/A"

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = dateFormat.format(Date(fuelLog.dateRefuelled)),
                    color = Color.White,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Logged at ${loggedAtFormat.format(Date(fuelLog.createdAt))}",
                    color = Color.Gray,
                    fontSize = titleFontSize
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Top Speed",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = titleFontSize,
                    color = Color.Gray
                )
                Text(
                    text = speedUnit.formatKmh(fuelLog.maxSpeedKmhSinceLastFuelUp),
                    fontSize = valueFontSize,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Average Speed",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = titleFontSize,
                    color = Color.Gray
                )
                Text(
                    text = speedUnit.formatKmh(fuelLog.avgSpeedKmhSinceLastFuelUp),
                    fontSize = valueFontSize,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Fuel Economy",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = titleFontSize,
                    color = Color.Gray
                )
                Text(
                    text = fuelEconomyText,
                    fontSize = valueFontSize,
                    fontWeight = FontWeight.Bold
                )

            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 84.dp, end = 4.dp)
            ) {
                Text(
                    text = "${String.format(Locale.getDefault(), "%.2f", fuelLog.litres)} L @ $${String.format(Locale.getDefault(), "%.2f", fuelLog.pricePerLitre)}/L",
                    color = Color.White,
                    fontSize = valueFontSize,
                    fontWeight = FontWeight.Bold
                )

                val nauticalMilesThisFuelUp = DistanceUnit.NM.fromKm(fuelLog.distanceKmSinceLastFuelUp)
                Text(
                    text = "${String.format(Locale.getDefault(), "%.1f", nauticalMilesThisFuelUp)} NM this fuel-up",
                    color = Color.White,
                    fontSize = valueFontSize,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "⛽ Total: $${String.format(Locale.getDefault(), "%.2f", fuelLog.totalPrice)}",
                    fontSize = valueFontSize,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = { showEditDialog = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Edit fuel log",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 4.dp, start = 4.dp)
            ) {
                Text("🗑", fontSize = 16.sp)
            }
        }
    }

    if (showEditDialog) {
        EditFuelLogDialog(
            fuelLog = fuelLog,
            onDismiss = { showEditDialog = false },
            onSave = { dateRefuelled, litres, pricePerLitre ->
                onEdit(dateRefuelled, litres, pricePerLitre)
                showEditDialog = false
            }
        )
    }
}

/** Lets the user correct the fields they entered when a fuel-up was originally logged. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditFuelLogDialog(
    fuelLog: FuelLog,
    onDismiss: () -> Unit,
    onSave: (dateRefuelled: Long, litres: Double, pricePerLitre: Double) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = fuelLog.dateRefuelled)
    var showDatePicker by remember { mutableStateOf(false) }
    var litresText by rememberSaveable { mutableStateOf(fuelLog.litres.toString()) }
    var priceText by rememberSaveable { mutableStateOf(fuelLog.pricePerLitre.toString()) }

    val litres = litresText.toDoubleOrNull()
    val pricePerLitre = priceText.toDoubleOrNull()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Edit Fuel Up",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                OutlinedTextField(
                    value = datePickerState.selectedDateMillis?.let { dateFormat.format(Date(it)) }
                        ?: dateFormat.format(Date(fuelLog.dateRefuelled)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date Re-fuelled") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Select date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = litresText,
                    onValueChange = { litresText = it },
                    label = { Text("Litres of Unleaded") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price Per Litre") },
                    leadingIcon = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Cancel") }
                    Button(
                        enabled = litres != null && litres > 0 && pricePerLitre != null && pricePerLitre > 0,
                        onClick = {
                            val dateMillis = datePickerState.selectedDateMillis ?: fuelLog.dateRefuelled
                            onSave(dateMillis, litres!!, pricePerLitre!!)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Save") }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun TripItem(
    trip: TripLog,
    distanceUnit: DistanceUnit,
    speedUnit: SpeedUnit,
    timeZoneId: String,
    titleFontSize: androidx.compose.ui.unit.TextUnit,
    valueFontSize: androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember(timeZoneId) {
        SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(timeZoneId)
        }
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Start: ${dateFormat.format(Date(trip.startTimestamp))}",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = titleFontSize,
                    color = Color.White
                )
                Text(
                    text = "Stop: ${dateFormat.format(Date(trip.endTimestamp))}",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = titleFontSize,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Total Distance Traveled",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = titleFontSize,
                    color = Color.Gray
                )
                Text(
                    text = distanceUnit.formatKm(trip.distanceKm),
                    fontSize = valueFontSize,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Average Speed",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = titleFontSize,
                    color = Color.Gray
                )
                Text(
                    text = speedUnit.formatKmh(trip.avgSpeedKmh),
                    fontSize = valueFontSize,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${trip.startAddress} → ${trip.endAddress}",
                    color = Color.White,
                    fontSize = valueFontSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 84.dp, end = 4.dp)
            ) {
                Text(
                    text = "Top Speed",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = titleFontSize,
                    color = Color.Gray
                )
                Text(
                    text = speedUnit.formatKmh(trip.maxSpeedKmh),
                    fontSize = valueFontSize,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Time Underway",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = titleFontSize,
                    color = Color.Gray
                )
                Text(
                    text = formatDuration(trip.movingTimeMs),
                    fontSize = valueFontSize,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
            ) {
                Text("🗑", fontSize = 16.sp)
            }
        }
    }
}

internal fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    val hours = (ms / (1000 * 60 * 60))
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}