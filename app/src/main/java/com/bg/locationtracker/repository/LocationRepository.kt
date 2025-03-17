package com.bg.locationtracker.repository

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.bg.locationtracker.api.LocationApiService
import com.bg.locationtracker.model.LocationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationRepository(private val context: Context) {
    private val TAG = "LocationRepository"
    private val apiService = LocationApiService.create()

    suspend fun sendLocationToServer(latitude: Double, longitude: Double, accuracy: Float): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val deviceId = getDeviceId()
                val locationData = LocationData(latitude, longitude, accuracy, deviceId = deviceId)

                val response = apiService.sendLocationData(locationData)

                if (response.isSuccessful) {
                    Log.d(TAG, "Location sent successfully: $latitude, $longitude")
                    true
                } else {
                    Log.e(TAG, "Failed to send location: ${response.code()} - ${response.message()}")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending location", e)
                false
            }
        }
    }

    private fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }
}