package com.k.hosken.navipulse.util

import android.content.Context
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLng
import java.util.Locale

object GeocoderUtils {
    fun getAddressFromLatLng(context: Context, location: LatLng): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                "${address.thoroughfare ?: address.featureName ?: ""}, ${address.locality ?: ""}".trim(',', ' ')
            } else {
                "${location.latitude}, ${location.longitude}"
            }
        } catch (e: Exception) {
            "${location.latitude}, ${location.longitude}"
        }
    }
}