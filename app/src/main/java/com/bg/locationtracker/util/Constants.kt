package com.bg.locationtracker.util

object Constants {
    // API constants
    const val BASE_URL = "https://your-api-endpoint.com/"
    const val LOCATION_ENDPOINT = "api/location"

    // Service constants
    const val LOCATION_SERVICE_ID = 175
    const val NOTIFICATION_CHANNEL_ID = "location_tracker_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Location Tracking"
    const val NOTIFICATION_ID = 176

    // Location request constants
    const val LOCATION_UPDATE_INTERVAL = 5000L  // 5 seconds
    const val FASTEST_LOCATION_INTERVAL = 3000L  // 3 seconds
    const val LOCATION_ACCURACY = 100f  // meters

    // Worker tag
    const val LOCATION_WORKER_TAG = "LocationWorkerTag"
    const val LOCATION_WORKER_NAME = "LocationWorker"

    // Preferences
    const val PREF_FILE_NAME = "location_tracker_prefs"
    const val PREF_TRACKING_ACTIVE = "tracking_active"
}