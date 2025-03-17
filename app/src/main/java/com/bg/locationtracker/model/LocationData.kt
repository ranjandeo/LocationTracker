package com.bg.locationtracker.model

import java.util.Date

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceId: String
)

data class LocationResponse(
    val success: Boolean,
    val message: String
)