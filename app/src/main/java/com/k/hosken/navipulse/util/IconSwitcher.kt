package com.k.hosken.navipulse.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Swaps the launcher icon between the two activity-aliases declared in the manifest.
 * Exactly one alias is enabled at a time; MainActivity itself has no launcher intent-filter.
 */
object IconSwitcher {
    private const val DEFAULT_ALIAS = "com.k.hosken.navipulse.MainActivityDefault"
    private const val WIFI_ALIAS = "com.k.hosken.navipulse.MainActivityWifi"

    fun isWifiIconActive(context: Context): Boolean {
        val state = context.packageManager.getComponentEnabledSetting(
            ComponentName(context.packageName, WIFI_ALIAS)
        )
        return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

    fun toggleIcon(context: Context) {
        val useWifi = !isWifiIconActive(context)
        setAliasEnabled(context, WIFI_ALIAS, useWifi)
        setAliasEnabled(context, DEFAULT_ALIAS, !useWifi)
    }

    private fun setAliasEnabled(context: Context, alias: String, enabled: Boolean) {
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context.packageName, alias),
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
