package com.bg.locationtracker.worker

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bg.locationtracker.service.LocationService
import com.bg.locationtracker.util.Constants
import java.util.concurrent.TimeUnit

class LocationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    private val TAG = "LocationWorker"
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        Constants.PREF_FILE_NAME, Context.MODE_PRIVATE
    )

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork: Checking location service status")

        val isTrackingActive = sharedPreferences.getBoolean(Constants.PREF_TRACKING_ACTIVE, false)

        return try {
            if (isTrackingActive) {
                Log.d(TAG, "doWork: Tracking should be active, starting service")
                LocationService.startService(applicationContext)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "doWork: Error starting location service", e)
            Result.retry()
        }
    }

    companion object {
        fun startPeriodicWorker(context: Context) {
            val periodicRequest = PeriodicWorkRequestBuilder<LocationWorker>(
                15, TimeUnit.MINUTES
            )
                .addTag(Constants.LOCATION_WORKER_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                Constants.LOCATION_WORKER_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )

            Log.d("LocationWorker", "Periodic worker scheduled")
        }
    }
}