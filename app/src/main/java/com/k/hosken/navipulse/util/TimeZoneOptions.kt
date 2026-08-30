package com.k.hosken.navipulse.util

import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class TimeZoneOption(val id: String, val label: String)

val timeZoneOptions: List<TimeZoneOption> by lazy {
    TimeZone.getAvailableIDs()
        .filter { it.contains('/') && !it.startsWith("Etc/") }
        .distinct()
        .map { id -> TimeZoneOption(id, id.replace('_', ' ') + " (${gmtOffsetLabel(id)})") }
        .sortedBy { it.id }
}

fun gmtOffsetLabel(zoneId: String): String {
    val offsetMillis = TimeZone.getTimeZone(zoneId).rawOffset
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(offsetMillis.toLong())
    val hours = totalMinutes / 60
    val minutes = abs(totalMinutes % 60)
    val sign = if (totalMinutes >= 0) "+" else "-"
    return "GMT%s%02d:%02d".format(sign, abs(hours), minutes)
}
