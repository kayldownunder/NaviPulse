package com.k.hosken.navipulse.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.k.hosken.navipulse.data.AppDatabase
import com.k.hosken.navipulse.data.TripLog
import com.k.hosken.navipulse.util.GeocoderUtils
import com.k.hosken.navipulse.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var db: AppDatabase

    private var timerJob: Job? = null
    private var startTime = 0L

    companion object {
        private const val CHANNEL_ID = "tracking_channel"
        private const val NOTIFICATION_ID = 1

        // GPS fixes worse than this are noise, not a real position.
        private const val MIN_ACCURACY_METERS = 20f

        // Segments shorter than this between consecutive fixes are typical GPS
        // jitter while stationary (parked, idling at lights), not real movement.
        private const val MIN_MOVEMENT_METERS = 5f

        private val _isTracking = MutableStateFlow(false)
        val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

        private val _totalDistanceKm = MutableStateFlow(0.0)
        val totalDistanceKm: StateFlow<Double> = _totalDistanceKm.asStateFlow()

        private val _elapsedTimeMs = MutableStateFlow(0L)
        val elapsedTimeMs: StateFlow<Long> = _elapsedTimeMs.asStateFlow()

        private val _recordedPoints = MutableStateFlow<List<LatLng>>(emptyList())
        val recordedPoints: StateFlow<List<LatLng>> = _recordedPoints.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        db = AppDatabase.getDatabase(this)
        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    if (location.hasAccuracy() && location.accuracy > MIN_ACCURACY_METERS) {
                        return@let
                    }

                    val newPoint = LatLng(location.latitude, location.longitude)
                    val currentList = _recordedPoints.value
                    val lastPoint = currentList.lastOrNull()

                    if (lastPoint == null) {
                        _recordedPoints.value = currentList + newPoint
                        return@let
                    }

                    val results = FloatArray(1)
                    Location.distanceBetween(
                        lastPoint.latitude, lastPoint.longitude,
                        newPoint.latitude, newPoint.longitude,
                        results
                    )
                    val segmentMeters = results[0]

                    // Drop the fix entirely when it doesn't represent real movement,
                    // instead of letting stationary GPS jitter inflate the trip distance.
                    if (segmentMeters >= MIN_MOVEMENT_METERS) {
                        _totalDistanceKm.value += (segmentMeters / 1000.0)
                        _recordedPoints.value = currentList + newPoint
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_START" -> {
                startTracking()
            }
            "ACTION_STOP" -> {
                stopTrackingAndSave()
            }
        }
        return START_STICKY
    }

    private fun startTracking() {
        if (_isTracking.value) return

        if (!PermissionUtils.hasLocationPermission(this)) {
            stopSelf()
            return
        }

        _isTracking.value = true
        _totalDistanceKm.value = 0.0
        _elapsedTimeMs.value = 0L
        _recordedPoints.value = emptyList()
        startTime = System.currentTimeMillis()

        startForeground(NOTIFICATION_ID, createNotification())

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateIntervalMillis(1500)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (_isTracking.value) {
                _elapsedTimeMs.value = System.currentTimeMillis() - startTime
                delay(1000)
            }
        }
    }

    private fun stopTrackingAndSave() {
        if (!_isTracking.value) return

        fusedLocationClient.removeLocationUpdates(locationCallback)
        timerJob?.cancel()
        _isTracking.value = false

        val endLocation = recordedPoints.value.lastOrNull() ?: LatLng(0.0, 0.0)
        val startLocation = recordedPoints.value.firstOrNull() ?: LatLng(0.0, 0.0)
        val tripStartTime = startTime
        val tripDistanceKm = totalDistanceKm.value
        val tripDurationMs = elapsedTimeMs.value

        stopForeground(STOP_FOREGROUND_REMOVE)

        // Geocoder.getFromLocation blocks on network I/O; run it (and the DB write)
        // off the main thread so stopping a trip can't trigger an ANR.
        CoroutineScope(Dispatchers.IO).launch {
            val startAddress = GeocoderUtils.getAddressFromLatLng(this@TrackingService, startLocation)
            val endAddress = GeocoderUtils.getAddressFromLatLng(this@TrackingService, endLocation)

            val trip = TripLog(
                startTimestamp = tripStartTime,
                endTimestamp = System.currentTimeMillis(),
                distanceKm = tripDistanceKm,
                durationMs = tripDurationMs,
                startAddress = startAddress,
                endAddress = endAddress
            )
            db.tripDao().insertTrip(trip)

            stopSelf()
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NaviPulse")
            .setContentText("Recording trip location in background...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Trip Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}