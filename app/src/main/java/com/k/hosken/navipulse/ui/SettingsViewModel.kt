package com.k.hosken.navipulse.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.k.hosken.navipulse.data.AppDatabase
import com.k.hosken.navipulse.data.AppFont
import com.k.hosken.navipulse.data.AppTextColor
import com.k.hosken.navipulse.data.AppTextSize
import com.k.hosken.navipulse.data.DistanceUnit
import com.k.hosken.navipulse.data.SettingsRepository
import com.k.hosken.navipulse.data.SpeedUnit
import com.k.hosken.navipulse.util.BackgroundImageManager
import com.k.hosken.navipulse.util.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)
    private val db = AppDatabase.getDatabase(application)

    val screenOnEnabled: StateFlow<Boolean> = repository.screenOnEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val distanceUnit: StateFlow<DistanceUnit> = repository.distanceUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DistanceUnit.KM)

    val speedUnit: StateFlow<SpeedUnit> = repository.speedUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SpeedUnit.KMH)

    val backgroundImagePath: StateFlow<String?> = repository.backgroundImagePath
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val appFont: StateFlow<AppFont> = repository.appFont
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppFont.ROBOTO)

    val textColor: StateFlow<AppTextColor> = repository.textColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTextColor.DEFAULT)

    val textSize: StateFlow<AppTextSize> = repository.textSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTextSize.DEFAULT)

    val titleTextSize: StateFlow<AppTextSize> = repository.titleTextSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTextSize.DEFAULT)

    val timeZoneId: StateFlow<String> = repository.timeZoneId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), java.util.TimeZone.getDefault().id)

    fun setScreenOnEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setScreenOnEnabled(enabled) }
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        viewModelScope.launch { repository.setDistanceUnit(unit) }
    }

    fun setSpeedUnit(unit: SpeedUnit) {
        viewModelScope.launch { repository.setSpeedUnit(unit) }
    }

    fun setAppFont(font: AppFont) {
        viewModelScope.launch { repository.setAppFont(font) }
    }

    fun setTextColor(color: AppTextColor) {
        viewModelScope.launch { repository.setTextColor(color) }
    }

    fun setTextSize(size: AppTextSize) {
        viewModelScope.launch { repository.setTextSize(size) }
    }

    fun setTitleTextSize(size: AppTextSize) {
        viewModelScope.launch { repository.setTitleTextSize(size) }
    }

    fun setTimeZoneId(id: String) {
        viewModelScope.launch { repository.setTimeZoneId(id) }
    }

    fun setBackgroundImage(uri: Uri, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                BackgroundImageManager.saveResizedBackground(getApplication(), uri)
            }
            if (savedPath != null) {
                repository.setBackgroundImagePath(savedPath)
            }
            onResult(savedPath != null)
        }
    }

    fun resetBackgroundImage() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                BackgroundImageManager.deleteBackground(getApplication())
            }
            repository.setBackgroundImagePath(null)
        }
    }

    fun exportBackup(uri: Uri, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                val trips = db.tripDao().getAllTrips().first()
                withContext(Dispatchers.IO) {
                    BackupManager.exportBackup(getApplication(), uri, trips)
                }
                trips.size
            }
            onResult(result)
        }
    }

    fun importBackup(uri: Uri, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                val trips = withContext(Dispatchers.IO) {
                    BackupManager.importBackup(getApplication(), uri)
                }
                trips.forEach { db.tripDao().insertTrip(it) }
                trips.size
            }
            onResult(result)
        }
    }
}
