package com.k.hosken.navipulse.receiver

import android.Manifest
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.k.hosken.navipulse.service.TrackingService
import com.k.hosken.navipulse.util.PermissionUtils

class BluetoothReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                // Auto-start service when the connected device looks like a car head unit,
                // and only if location permission was already granted - starting a foreground
                // location service without it throws a SecurityException with no Activity
                // here to recover.
                if (PermissionUtils.hasLocationPermission(context) && isCarAudioDevice(context, intent)) {
                    val startIntent = Intent(context, TrackingService::class.java).apply {
                        action = "ACTION_START"
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(startIntent)
                    } else {
                        context.startService(startIntent)
                    }
                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                // Only stop for the car head unit disconnecting - otherwise an unrelated
                // accessory (earbuds, a watch) dropping mid-trip would cut tracking short.
                if (isCarAudioDevice(context, intent)) {
                    val stopIntent = Intent(context, TrackingService::class.java).apply {
                        action = "ACTION_STOP"
                    }
                    context.startService(stopIntent)
                }
            }
        }
    }

    /**
     * Restricts auto-start to actual car head units (car audio / hands-free profile),
     * so connecting headphones, a watch, or any other accessory at home doesn't log a
     * bogus trip.
     */
    private fun isCarAudioDevice(context: Context, intent: Intent): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

        val deviceClass = device?.bluetoothClass?.deviceClass
        return deviceClass == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO ||
            deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE
    }
}
