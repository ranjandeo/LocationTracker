package com.bg.locationtracker

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bg.locationtracker.service.LocationService
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationServiceTest {
    private lateinit var mockLocationProvider: MockLocationProvider
    
    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        mockLocationProvider = MockLocationProvider(context)
        
        // Ensure mock mode is enabled
        mockLocationProvider.setMockMode(true)
    }
    
    @After
    fun tearDown() {
        // Disable mock mode when done
        mockLocationProvider.disableMockMode()
    }
    
    @Test
    fun testLocationUpdates() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Start the location service
        LocationService.startService(context)
        
        // Set a series of mock locations
        val testLocations = listOf(
            Triple(37.7749, -122.4194, 0),   // San Francisco
            Triple(37.7750, -122.4197, 5),   // Move slightly
            Triple(37.7752, -122.4200, 10),  // Move more
            Triple(37.7755, -122.4205, 15)   // Continue movement
        )
        
        for ((lat, lng, seconds) in testLocations) {
            // Delay to simulate passage of time
            delay(seconds * 1000L)
            
            // Set the mock location
            mockLocationProvider.setMockLocation(lat, lng)
            
            // Allow time for location to be processed
            delay(2000)
        }
        
        // Stop the service
        LocationService.stopService(context)
    }
}