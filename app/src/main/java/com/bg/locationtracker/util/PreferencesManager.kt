package com.bg.locationtracker.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Utility class to manage SharedPreferences operations
 */
class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        Constants.PREF_FILE_NAME, Context.MODE_PRIVATE
    )

    /**
     * Check if location tracking is currently active
     */
    fun isTrackingActive(): Boolean {
        return sharedPreferences.getBoolean(Constants.PREF_TRACKING_ACTIVE, false)
    }

    /**
     * Set tracking state
     */
    fun setTrackingActive(isActive: Boolean) {
        sharedPreferences.edit {
            putBoolean(Constants.PREF_TRACKING_ACTIVE, isActive)
        }
    }

    /**
     * Save API endpoint URL
     */
    fun saveApiEndpoint(url: String) {
        sharedPreferences.edit {
            putString("api_endpoint", url)
        }
    }

    /**
     * Get API endpoint URL or default
     */
    fun getApiEndpoint(): String {
        return sharedPreferences.getString("api_endpoint", Constants.BASE_URL) ?: Constants.BASE_URL
    }

    /**
     * Save tracking interval (in milliseconds)
     */
    fun saveTrackingInterval(intervalMs: Long) {
        sharedPreferences.edit {
            putLong("tracking_interval", intervalMs)
        }
    }

    /**
     * Get tracking interval or default (5 seconds)
     */
    fun getTrackingInterval(): Long {
        return sharedPreferences.getLong("tracking_interval", Constants.LOCATION_UPDATE_INTERVAL)
    }
}