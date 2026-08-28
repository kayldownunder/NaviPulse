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

enum class DistanceUnit(val label: String) { KM("km"), NM("NM") }
enum class SpeedUnit(val label: String) { KMH("km/hr"), NM("NM/hr") }

private const val KM_TO_NM = 0.539957
private const val NM_TO_KM = 1.852

/** Converts a distance in kilometers to this unit. */
fun DistanceUnit.fromKm(km: Double): Double = when (this) {
    DistanceUnit.KM -> km
    DistanceUnit.NM -> km * KM_TO_NM
}

/** Formats a distance given in kilometers into this unit, e.g. "12.34 NM". */
fun DistanceUnit.formatKm(km: Double, decimals: Int = 2): String =
    "%.${decimals}f %s".format(fromKm(km), label)

/** Converts a speed in km/hr to this unit. */
fun SpeedUnit.fromKmh(kmh: Double): Double = when (this) {
    SpeedUnit.KMH -> kmh
    SpeedUnit.NM -> kmh / NM_TO_KM
}

/** Formats a speed given in km/hr into this unit, e.g. "12.34 NM/hr". */
fun SpeedUnit.formatKmh(kmh: Double, decimals: Int = 2): String =
    "%.${decimals}f %s".format(fromKmh(kmh), label)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SCREEN_ON = booleanPreferencesKey("screen_on")
        val DISTANCE_UNIT = stringPreferencesKey("distance_unit")
        val SPEED_UNIT = stringPreferencesKey("speed_unit")
    }

    val screenOnEnabled: Flow<Boolean> = context.settingsDataStore.data
        .map { it[Keys.SCREEN_ON] ?: false }

    val distanceUnit: Flow<DistanceUnit> = context.settingsDataStore.data
        .map { it.toEnum(Keys.DISTANCE_UNIT, DistanceUnit.KM) }

    val speedUnit: Flow<SpeedUnit> = context.settingsDataStore.data
        .map { it.toEnum(Keys.SPEED_UNIT, SpeedUnit.KMH) }

    suspend fun setScreenOnEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SCREEN_ON] = enabled }
    }

    suspend fun setDistanceUnit(unit: DistanceUnit) {
        context.settingsDataStore.edit { it[Keys.DISTANCE_UNIT] = unit.name }
    }

    suspend fun setSpeedUnit(unit: SpeedUnit) {
        context.settingsDataStore.edit { it[Keys.SPEED_UNIT] = unit.name }
    }

    private inline fun <reified T : Enum<T>> Preferences.toEnum(
        key: Preferences.Key<String>,
        default: T
    ): T = this[key]?.let { stored ->
        enumValues<T>().firstOrNull { it.name == stored }
    } ?: default
}
