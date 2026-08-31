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
import com.k.hosken.navipulse.data.encodeRoute
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

        // Speed readings below this are treated as "not underway" — 1.5 knots, the
        // conventional threshold below which a vessel's motion is dock drift/current
        // rather than real travel — and excluded from the moving-average speed calculation.
        private const val MIN_MOVING_SPEED_KMH = 2.778

        private val _isTracking = MutableStateFlow(false)
        val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

        private val _totalDistanceKm = MutableStateFlow(0.0)
        val totalDistanceKm: StateFlow<Double> = _totalDistanceKm.asStateFlow()

        private val _elapsedTimeMs = MutableStateFlow(0L)
        val elapsedTimeMs: StateFlow<Long> = _elapsedTimeMs.asStateFlow()

        private val _currentSpeedKmh = MutableStateFlow(0.0)
        val currentSpeedKmh: StateFlow<Double> = _currentSpeedKmh.asStateFlow()

        // Highest speed reading seen so far during the trip in progress.
        private val _currentTripMaxSpeedKmh = MutableStateFlow(0.0)
        val currentTripMaxSpeedKmh: StateFlow<Double> = _currentTripMaxSpeedKmh.asStateFlow()

        // Moving-average speed of the trip in progress, counting only time spent above
        // MIN_MOVING_SPEED_KMH (1.5 knots) — i.e. actually underway, not idling/drifting.
        private val _currentTripAvgSpeedKmh = MutableStateFlow(0.0)
        val currentTripAvgSpeedKmh: StateFlow<Double> = _currentTripAvgSpeedKmh.asStateFlow()

        // Wall-clock time the trip in progress has spent above MIN_MOVING_SPEED_KMH.
        private val _engineRunTimeMs = MutableStateFlow(0L)
        val engineRunTimeMs: StateFlow<Long> = _engineRunTimeMs.asStateFlow()

        private val _recordedPoints = MutableStateFlow<List<LatLng>>(emptyList())
        val recordedPoints: StateFlow<List<LatLng>> = _recordedPoints.asStateFlow()
    }

    // Time-weighted accumulators for the moving-average speed of the current trip.
    private var maxSpeedKmh = 0.0
    private var movingTimeMs = 0L
    private var movingSpeedTimeWeightedSum = 0.0
    private var lastSpeedSampleTimeMs = 0L

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

                    recordSpeedSample(
                        speedKmh = if (location.hasSpeed()) location.speed * 3.6 else 0.0,
                        sampleTimeMs = location.time
                    )

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

    /** Folds one GPS speed reading into the current/max/moving-average trip stats. */
    private fun recordSpeedSample(speedKmh: Double, sampleTimeMs: Long) {
        _currentSpeedKmh.value = speedKmh
        if (speedKmh > maxSpeedKmh) {
            maxSpeedKmh = speedKmh
            _currentTripMaxSpeedKmh.value = maxSpeedKmh
        }

        if (lastSpeedSampleTimeMs != 0L) {
            val dt = sampleTimeMs - lastSpeedSampleTimeMs
            if (dt > 0 && speedKmh >= MIN_MOVING_SPEED_KMH) {
                movingTimeMs += dt
                movingSpeedTimeWeightedSum += speedKmh * dt
            }
        }
        lastSpeedSampleTimeMs = sampleTimeMs

        _engineRunTimeMs.value = movingTimeMs
        _currentTripAvgSpeedKmh.value = if (movingTimeMs > 0) movingSpeedTimeWeightedSum / movingTimeMs else 0.0
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
        _currentSpeedKmh.value = 0.0
        _currentTripMaxSpeedKmh.value = 0.0
        _currentTripAvgSpeedKmh.value = 0.0
        _engineRunTimeMs.value = 0L
        _recordedPoints.value = emptyList()
        maxSpeedKmh = 0.0
        movingTimeMs = 0L
        movingSpeedTimeWeightedSum = 0.0
        lastSpeedSampleTimeMs = 0L
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
        // Average speed only over time the vessel was actually moving, not the whole trip duration.
        val tripAvgSpeedKmh = if (movingTimeMs > 0) movingSpeedTimeWeightedSum / movingTimeMs else 0.0
        val tripMaxSpeedKmh = maxSpeedKmh
        val tripMovingTimeMs = movingTimeMs
        val tripRoutePointsCsv = recordedPoints.value.encodeRoute()

        _currentSpeedKmh.value = 0.0
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
                avgSpeedKmh = tripAvgSpeedKmh,
                maxSpeedKmh = tripMaxSpeedKmh,
                movingTimeMs = tripMovingTimeMs,
                startAddress = startAddress,
                endAddress = endAddress,
                routePointsCsv = tripRoutePointsCsv
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