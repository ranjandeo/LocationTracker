package com.bg.locationtracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.bg.locationtracker.service.LocationService
import com.bg.locationtracker.util.Constants
import com.bg.locationtracker.worker.LocationWorker

class BootReceiver : BroadcastReceiver() {
    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "onReceive: Device boot completed")

            val sharedPreferences: SharedPreferences = context.getSharedPreferences(
                Constants.PREF_FILE_NAME, Context.MODE_PRIVATE
            )

            val isTrackingActive = sharedPreferences.getBoolean(Constants.PREF_TRACKING_ACTIVE, false)

            if (isTrackingActive) {
                Log.d(TAG, "onReceive: Tracking was active before reboot, restarting")
                LocationService.startService(context)
            }

            // Start periodic worker to ensure service stays running
            LocationWorker.startPeriodicWorker(context)
        }
    }
}