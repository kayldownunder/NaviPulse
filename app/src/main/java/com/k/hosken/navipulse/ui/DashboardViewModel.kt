package com.k.hosken.navipulse.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.k.hosken.navipulse.data.AppDatabase
import com.k.hosken.navipulse.data.AppFont
import com.k.hosken.navipulse.data.AppSummaryTextSize
import com.k.hosken.navipulse.data.AppTextSize
import com.k.hosken.navipulse.data.DistanceUnit
import com.k.hosken.navipulse.data.FuelLog
import com.k.hosken.navipulse.data.SettingsRepository
import com.k.hosken.navipulse.data.SpeedUnit
import com.k.hosken.navipulse.data.TripLog
import com.k.hosken.navipulse.data.avgSpeedKmhSinceLastFuelUp
import com.k.hosken.navipulse.data.distanceKmSinceLastFuelUp
import com.k.hosken.navipulse.data.maxSpeedKmhSinceLastFuelUp
import com.k.hosken.navipulse.service.TrackingService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val settingsRepository = SettingsRepository(application)

    val distanceUnit: StateFlow<DistanceUnit> = settingsRepository.distanceUnit
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DistanceUnit.KM
        )

    val speedUnit: StateFlow<SpeedUnit> = settingsRepository.speedUnit
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SpeedUnit.KMH
        )

    val backgroundImagePath: StateFlow<String?> = settingsRepository.backgroundImagePath
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val timeZoneId: StateFlow<String> = settingsRepository.timeZoneId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = java.util.TimeZone.getDefault().id
        )

    val summaryTextSize: StateFlow<AppSummaryTextSize> = settingsRepository.summaryTextSize
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSummaryTextSize.DEFAULT
        )

    val appFont: StateFlow<AppFont> = settingsRepository.appFont
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppFont.ROBOTO
        )

    val titleTextSize: StateFlow<AppTextSize> = settingsRepository.titleTextSize
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTextSize.DEFAULT
        )

    val valueTextSize: StateFlow<AppTextSize> = settingsRepository.textSize
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTextSize.DEFAULT
        )

    val allTrips: StateFlow<List<TripLog>> = db.tripDao().getAllTrips()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allFuelLogs: StateFlow<List<FuelLog>> = db.fuelDao().getAllFuelLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isTracking: StateFlow<Boolean> = TrackingService.isTracking
    val totalDistanceKm: StateFlow<Double> = TrackingService.totalDistanceKm
    val elapsedTimeMs: StateFlow<Long> = TrackingService.elapsedTimeMs
    val currentSpeedKmh: StateFlow<Double> = TrackingService.currentSpeedKmh
    val currentTripMaxSpeedKmh: StateFlow<Double> = TrackingService.currentTripMaxSpeedKmh
    val currentTripAvgSpeedKmh: StateFlow<Double> = TrackingService.currentTripAvgSpeedKmh
    val engineRunTimeMs: StateFlow<Long> = TrackingService.engineRunTimeMs
    val livePoints: StateFlow<List<LatLng>> = TrackingService.recordedPoints

    fun startTracking() {
        val intent = Intent(getApplication(), TrackingService::class.java).apply {
            action = "ACTION_START"
        }
        getApplication<Application>().startService(intent)
    }

    fun stopTracking() {
        val intent = Intent(getApplication(), TrackingService::class.java).apply {
            action = "ACTION_STOP"
        }
        getApplication<Application>().startService(intent)
    }

    fun deleteTrip(tripId: Long) {
        viewModelScope.launch {
            db.tripDao().deleteTripById(tripId)
        }
    }

    fun updateTrip(trip: TripLog) {
        viewModelScope.launch {
            db.tripDao().updateTrip(trip)
        }
    }

    suspend fun getTripById(tripId: Long): TripLog? {
        return db.tripDao().getTripById(tripId)
    }

    fun saveFuelLog(dateRefuelled: Long, litres: Double, pricePerLitre: Double) {
        viewModelScope.launch {
            val trips = db.tripDao().getAllTrips().first()
            val fuelLogs = db.fuelDao().getAllFuelLogs().first()
            // Stats use the actual save-time instant, not dateRefuelled (a user-editable,
            // date-only value), so same-day trips aren't excluded by a too-early cutoff.
            val createdAt = System.currentTimeMillis()
            db.fuelDao().insertFuelLog(
                FuelLog(
                    dateRefuelled = dateRefuelled,
                    litres = litres,
                    pricePerLitre = pricePerLitre,
                    totalPrice = litres * pricePerLitre,
                    distanceKmSinceLastFuelUp = distanceKmSinceLastFuelUp(trips, fuelLogs, createdAt),
                    avgSpeedKmhSinceLastFuelUp = avgSpeedKmhSinceLastFuelUp(trips, fuelLogs, createdAt),
                    maxSpeedKmhSinceLastFuelUp = maxSpeedKmhSinceLastFuelUp(trips, fuelLogs, createdAt),
                    createdAt = createdAt
                )
            )
        }
    }

    fun deleteFuelLog(fuelLogId: Long) {
        viewModelScope.launch {
            db.fuelDao().deleteFuelLogById(fuelLogId)
        }
    }
}