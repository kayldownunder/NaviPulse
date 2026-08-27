package com.k.hosken.navipulse.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.k.hosken.navipulse.data.AreaUnit
import com.k.hosken.navipulse.data.DistanceUnit
import com.k.hosken.navipulse.data.ElevationUnit
import com.k.hosken.navipulse.data.SettingsRepository
import com.k.hosken.navipulse.data.SpeedUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    val screenOnEnabled: StateFlow<Boolean> = repository.screenOnEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val distanceUnit: StateFlow<DistanceUnit> = repository.distanceUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DistanceUnit.METRES)

    val speedUnit: StateFlow<SpeedUnit> = repository.speedUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SpeedUnit.KMH)

    val elevationUnit: StateFlow<ElevationUnit> = repository.elevationUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ElevationUnit.METRES)

    val areaUnit: StateFlow<AreaUnit> = repository.areaUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AreaUnit.HECTARES)

    fun setScreenOnEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setScreenOnEnabled(enabled) }
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        viewModelScope.launch { repository.setDistanceUnit(unit) }
    }

    fun setSpeedUnit(unit: SpeedUnit) {
        viewModelScope.launch { repository.setSpeedUnit(unit) }
    }

    fun setElevationUnit(unit: ElevationUnit) {
        viewModelScope.launch { repository.setElevationUnit(unit) }
    }

    fun setAreaUnit(unit: AreaUnit) {
        viewModelScope.launch { repository.setAreaUnit(unit) }
    }
}
