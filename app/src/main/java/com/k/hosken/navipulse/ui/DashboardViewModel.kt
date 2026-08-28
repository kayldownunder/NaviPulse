package com.k.hosken.navipulse.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.k.hosken.navipulse.data.AppDatabase
import com.k.hosken.navipulse.data.DistanceUnit
import com.k.hosken.navipulse.data.SettingsRepository
import com.k.hosken.navipulse.data.SpeedUnit
import com.k.hosken.navipulse.data.TripLog
import com.k.hosken.navipulse.service.TrackingService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    val allTrips: StateFlow<List<TripLog>> = db.tripDao().getAllTrips()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isTracking: StateFlow<Boolean> = TrackingService.isTracking
    val totalDistanceKm: StateFlow<Double> = TrackingService.totalDistanceKm
    val elapsedTimeMs: StateFlow<Long> = TrackingService.elapsedTimeMs
    val currentSpeedKmh: StateFlow<Double> = TrackingService.currentSpeedKmh
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
}