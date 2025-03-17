package com.bg.locationtracker.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bg.locationtracker.MainActivity
import com.bg.locationtracker.R
import com.bg.locationtracker.repository.LocationRepository
import com.bg.locationtracker.util.Constants
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LocationService : Service() {
    private val TAG = "LocationService"
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRepository: LocationRepository
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Location service created")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRepository = LocationRepository(applicationContext)
        sharedPreferences = getSharedPreferences(Constants.PREF_FILE_NAME, Context.MODE_PRIVATE)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    Log.d(TAG, "Location update received: ${location.latitude}, ${location.longitude}")
                    sendLocationData(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: Starting location service")

        // Start as foreground service
        startForeground(Constants.NOTIFICATION_ID, createNotification())

        // Save tracking state
        sharedPreferences.edit().putBoolean(Constants.PREF_TRACKING_ACTIVE, true).apply()

        // Start location updates
        startLocationUpdates()

        // If service is killed, restart it
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Location service destroyed")
        stopLocationUpdates()
        serviceJob.cancel()

        // Update tracking state
        sharedPreferences.edit().putBoolean(Constants.PREF_TRACKING_ACTIVE, false).apply()
    }

    private fun startLocationUpdates() {
        try {
            // Create location request
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                Constants.LOCATION_UPDATE_INTERVAL
            )
                .setMinUpdateIntervalMillis(Constants.FASTEST_LOCATION_INTERVAL)
                .setMaxUpdateDelayMillis(Constants.LOCATION_UPDATE_INTERVAL)
                .build()

            // Request location updates
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            Log.d(TAG, "startLocationUpdates: Location updates requested")
        } catch (e: SecurityException) {
            Log.e(TAG, "startLocationUpdates: Permission denied", e)
        }
    }

    private fun stopLocationUpdates() {
        Log.d(TAG, "stopLocationUpdates: Stopping location updates")
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun sendLocationData(location: Location) {
        serviceScope.launch {
            val success = locationRepository.sendLocationToServer(
                location.latitude,
                location.longitude,
                location.accuracy
            )

            if (success) {
                Log.d(TAG, "Location sent to server successfully")
            } else {
                Log.e(TAG, "Failed to send location to server")
            }
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Location Tracking Active")
            .setContentText("Your location is being tracked")
            .setSmallIcon(R.drawable.ic_location)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    companion object {
        fun startService(context: Context) {
            val startIntent = Intent(context, LocationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent)
            } else {
                context.startService(startIntent)
            }
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, LocationService::class.java)
            context.stopService(stopIntent)
        }
    }
}