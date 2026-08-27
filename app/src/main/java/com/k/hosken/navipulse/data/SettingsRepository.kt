package com.k.hosken.navipulse.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class DistanceUnit(val label: String) { METRES("Metres"), FEET("Feet") }
enum class SpeedUnit(val label: String) { KMH("km/h"), MPH("mph") }
enum class ElevationUnit(val label: String) { METRES("Metres"), FEET("Feet") }
enum class AreaUnit(val label: String) { HECTARES("Hectares"), ACRES("Acres") }

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SCREEN_ON = booleanPreferencesKey("screen_on")
        val DISTANCE_UNIT = stringPreferencesKey("distance_unit")
        val SPEED_UNIT = stringPreferencesKey("speed_unit")
        val ELEVATION_UNIT = stringPreferencesKey("elevation_unit")
        val AREA_UNIT = stringPreferencesKey("area_unit")
    }

    val screenOnEnabled: Flow<Boolean> = context.settingsDataStore.data
        .map { it[Keys.SCREEN_ON] ?: false }

    val distanceUnit: Flow<DistanceUnit> = context.settingsDataStore.data
        .map { it.toEnum(Keys.DISTANCE_UNIT, DistanceUnit.METRES) }

    val speedUnit: Flow<SpeedUnit> = context.settingsDataStore.data
        .map { it.toEnum(Keys.SPEED_UNIT, SpeedUnit.KMH) }

    val elevationUnit: Flow<ElevationUnit> = context.settingsDataStore.data
        .map { it.toEnum(Keys.ELEVATION_UNIT, ElevationUnit.METRES) }

    val areaUnit: Flow<AreaUnit> = context.settingsDataStore.data
        .map { it.toEnum(Keys.AREA_UNIT, AreaUnit.HECTARES) }

    suspend fun setScreenOnEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SCREEN_ON] = enabled }
    }

    suspend fun setDistanceUnit(unit: DistanceUnit) {
        context.settingsDataStore.edit { it[Keys.DISTANCE_UNIT] = unit.name }
    }

    suspend fun setSpeedUnit(unit: SpeedUnit) {
        context.settingsDataStore.edit { it[Keys.SPEED_UNIT] = unit.name }
    }

    suspend fun setElevationUnit(unit: ElevationUnit) {
        context.settingsDataStore.edit { it[Keys.ELEVATION_UNIT] = unit.name }
    }

    suspend fun setAreaUnit(unit: AreaUnit) {
        context.settingsDataStore.edit { it[Keys.AREA_UNIT] = unit.name }
    }

    private inline fun <reified T : Enum<T>> Preferences.toEnum(
        key: Preferences.Key<String>,
        default: T
    ): T = this[key]?.let { stored ->
        enumValues<T>().firstOrNull { it.name == stored }
    } ?: default
}
