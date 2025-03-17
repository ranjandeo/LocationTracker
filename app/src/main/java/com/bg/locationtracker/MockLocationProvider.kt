package com.bg.locationtracker

import android.content.Context
import android.location.Location
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient

/**
 * A utility class for mocking location updates during testing
 */
class MockLocationProvider(private val context: Context) {
    private val TAG = "MockLocationProvider"
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val settingsClient: SettingsClient = LocationServices.getSettingsClient(context)
    
    /**
     * Set a mock location to be returned when location is requested
     */
    fun setMockLocation(latitude: Double, longitude: Double, accuracy: Float = 3.0f) {
        try {
            // Create a mock location
            val mockLocation = Location("mock-provider").apply {
                this.latitude = latitude
                this.longitude = longitude
                this.accuracy = accuracy
                this.time = System.currentTimeMillis()
                
                // Set elapsed real-time nanos for API level 17+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    this.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                }
            }
            
            // Enable mock mode
            fusedLocationClient.setMockMode(true)
                .addOnSuccessListener {
                    Log.d(TAG, "Mock mode enabled successfully")
                    
                    // Set the mock location
                    fusedLocationClient.setMockLocation(mockLocation)
                        .addOnSuccessListener {
                            Log.d(TAG, "Mock location set: $latitude, $longitude")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to set mock location", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to enable mock mode", e)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing permission to set mock location", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting mock location", e)
        }
    }

    /**
     * Disable mock mode
     */
    fun disableMockMode() {
        try {
            fusedLocationClient.setMockMode(false)
                .addOnSuccessListener {
                    Log.d(TAG, "Mock mode disabled")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to disable mock mode", e)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing permission to disable mock mode", e)
        }
    }
    
    /**
     * Check if location services are enabled
     */
    fun checkLocationSettings(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(
                com.google.android.gms.location.LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY, 5000
                ).build()
            )
        
        settingsClient.checkLocationSettings(builder.build())
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}