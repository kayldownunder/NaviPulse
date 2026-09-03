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
import java.util.TimeZone

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class DistanceUnit(val label: String) { KM("km"), NM("NM") }
enum class SpeedUnit(val label: String) { KMH("km/hr"), KTS("kts") }
enum class AppFont(val label: String) {
    ROBOTO("Roboto"),
    OPEN_SANS("Open Sans"),
    LATO("Lato"),
    MONTSERRAT("Montserrat"),
    OSWALD("Oswald"),
    RALEWAY("Raleway"),
    POPPINS("Poppins"),
    MERRIWEATHER("Merriweather"),
    NUNITO("Nunito"),
    UBUNTU("Ubuntu"),
    PT_SANS("PT Sans"),
    PLAYFAIR_DISPLAY("Playfair Display"),
    INTER("Inter"),
    QUICKSAND("Quicksand"),
    DANCING_SCRIPT("Dancing Script")
}

enum class AppTextColor(val label: String) {
    DEFAULT("Default"),
    WHITE("White"),
    BLACK("Black"),
    RED("Red"),
    ORANGE("Orange"),
    YELLOW("Yellow"),
    GREEN("Green"),
    BLUE("Blue"),
    PURPLE("Purple"),
    PINK("Pink"),
    GRAY("Gray")
}

enum class AppTextSize(val sp: Int) {
    SIZE_12(12),
    SIZE_14(14),
    SIZE_15(15),
    SIZE_16(16),
    SIZE_17(17),
    SIZE_18(18),
    SIZE_20(20);

    val label: String get() = "${sp}sp"

    companion object {
        val DEFAULT = SIZE_16
    }
}

/** Same size options as [AppTextSize] - used only by the Summary Text Size setting. */
enum class AppSummaryTextSize(val sp: Int) {
    SIZE_12(12),
    SIZE_14(14),
    SIZE_15(15),
    SIZE_16(16),
    SIZE_17(17),
    SIZE_18(18),
    SIZE_20(20);

    val label: String get() = "${sp}sp"

    companion object {
        val DEFAULT = SIZE_15
    }
}

private const val KM_TO_NM = 0.539957
private const val KM_PER_NAUTICAL_MILE = 1.852

/** Converts a distance in kilometers to this unit. */
fun DistanceUnit.fromKm(km: Double): Double = when (this) {
    DistanceUnit.KM -> km
    DistanceUnit.NM -> km * KM_TO_NM
}

/** Formats a distance given in kilometers into this unit, e.g. "12.34 NM". */
fun DistanceUnit.formatKm(km: Double, decimals: Int = 2): String =
    "%.${decimals}f %s".format(fromKm(km), label)

/** Converts a speed in km/hr to knots. */
private fun kmhToKts(kmh: Double): Double = kmh / KM_PER_NAUTICAL_MILE

/** Converts a speed in km/hr to this unit. */
fun SpeedUnit.fromKmh(kmh: Double): Double = when (this) {
    SpeedUnit.KMH -> kmh
    SpeedUnit.KTS -> kmhToKts(kmh)
}

/** Formats a speed given in km/hr into this unit, e.g. "12.34 kts". */
fun SpeedUnit.formatKmh(kmh: Double, decimals: Int = 2): String =
    "%.${decimals}f %s".format(fromKmh(kmh), label)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SCREEN_ON = booleanPreferencesKey("screen_on")
        val DISTANCE_UNIT = stringPreferencesKey("distance_unit")
        val SPEED_UNIT = stringPreferencesKey("speed_unit")
        val BACKGROUND_IMAGE_PATH = stringPreferencesKey("background_image_path")
        val APP_FONT = stringPreferencesKey("app_font")
        val APP_TEXT_COLOR = stringPreferencesKey("app_text_color")
        val APP_TEXT_SIZE = stringPreferencesKey("app_text_size")
        val APP_TITLE_TEXT_SIZE = stringPreferencesKey("app_title_text_size")
        val APP_SUMMARY_TEXT_SIZE = stringPreferencesKey("app_summary_text_size")
        val TIME_ZONE_ID = stringPreferencesKey("time_zone_id")
    }

    val screenOnEnabled: Flow<Boolean> = context.settingsDataStore.data
        .map { it[Keys.SCREEN_ON] ?: false }

    val distanceUnit: Flow<DistanceUnit> = context.settingsDataStore.data
        .map { it.toEnum(Keys.DISTANCE_UNIT, DistanceUnit.KM) }

    val speedUnit: Flow<SpeedUnit> = context.settingsDataStore.data
        .map { it.toEnum(Keys.SPEED_UNIT, SpeedUnit.KMH) }

    /** Path to a custom dashboard background image saved on disk, or null to use the default. */
    val backgroundImagePath: Flow<String?> = context.settingsDataStore.data
        .map { it[Keys.BACKGROUND_IMAGE_PATH] }

    val appFont: Flow<AppFont> = context.settingsDataStore.data
        .map { it.toEnum(Keys.APP_FONT, AppFont.ROBOTO) }

    val textColor: Flow<AppTextColor> = context.settingsDataStore.data
        .map { it.toEnum(Keys.APP_TEXT_COLOR, AppTextColor.DEFAULT) }

    /** Size applied to numerical values shown on the dashboard (front page). */
    val textSize: Flow<AppTextSize> = context.settingsDataStore.data
        .map { it.toEnum(Keys.APP_TEXT_SIZE, AppTextSize.DEFAULT) }

    /** Size applied to the field titles/labels on the dashboard (front page). */
    val titleTextSize: Flow<AppTextSize> = context.settingsDataStore.data
        .map { it.toEnum(Keys.APP_TITLE_TEXT_SIZE, AppTextSize.DEFAULT) }

    /** Size applied to all text in the stats summary panel at the top of the dashboard (front page). */
    val summaryTextSize: Flow<AppSummaryTextSize> = context.settingsDataStore.data
        .map { it.toEnum(Keys.APP_SUMMARY_TEXT_SIZE, AppSummaryTextSize.DEFAULT) }

    /** IANA time zone ID used to display dates/times, e.g. "Australia/Sydney". */
    val timeZoneId: Flow<String> = context.settingsDataStore.data
        .map { it[Keys.TIME_ZONE_ID] ?: TimeZone.getDefault().id }

    suspend fun setScreenOnEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SCREEN_ON] = enabled }
    }

    suspend fun setDistanceUnit(unit: DistanceUnit) {
        context.settingsDataStore.edit { it[Keys.DISTANCE_UNIT] = unit.name }
    }

    suspend fun setSpeedUnit(unit: SpeedUnit) {
        context.settingsDataStore.edit { it[Keys.SPEED_UNIT] = unit.name }
    }

    suspend fun setBackgroundImagePath(path: String?) {
        context.settingsDataStore.edit {
            if (path != null) it[Keys.BACKGROUND_IMAGE_PATH] = path else it.remove(Keys.BACKGROUND_IMAGE_PATH)
        }
    }

    suspend fun setAppFont(font: AppFont) {
        context.settingsDataStore.edit { it[Keys.APP_FONT] = font.name }
    }

    suspend fun setTextColor(color: AppTextColor) {
        context.settingsDataStore.edit { it[Keys.APP_TEXT_COLOR] = color.name }
    }

    suspend fun setTextSize(size: AppTextSize) {
        context.settingsDataStore.edit { it[Keys.APP_TEXT_SIZE] = size.name }
    }

    suspend fun setTitleTextSize(size: AppTextSize) {
        context.settingsDataStore.edit { it[Keys.APP_TITLE_TEXT_SIZE] = size.name }
    }

    suspend fun setSummaryTextSize(size: AppSummaryTextSize) {
        context.settingsDataStore.edit { it[Keys.APP_SUMMARY_TEXT_SIZE] = size.name }
    }

    suspend fun setTimeZoneId(id: String) {
        context.settingsDataStore.edit { it[Keys.TIME_ZONE_ID] = id }
    }

    private inline fun <reified T : Enum<T>> Preferences.toEnum(
        key: Preferences.Key<String>,
        default: T
    ): T = this[key]?.let { stored ->
        enumValues<T>().firstOrNull { it.name == stored }
    } ?: default
}
