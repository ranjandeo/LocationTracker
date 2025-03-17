package com.bg.locationtracker

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.bg.locationtracker.service.LocationService
import com.bg.locationtracker.ui.theme.LocationGreen
import com.bg.locationtracker.ui.theme.LocationRed
import com.bg.locationtracker.ui.theme.LocationTrackerTheme
import com.bg.locationtracker.util.Constants
import com.bg.locationtracker.util.PermissionUtils
import com.bg.locationtracker.worker.LocationWorker

class MainActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences(Constants.PREF_FILE_NAME, Context.MODE_PRIVATE)

        // Initialize the worker to ensure location service stays running
        LocationWorker.startPeriodicWorker(this)

        setContent {
            LocationTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LocationTrackerScreen(sharedPreferences)
                }
            }
        }
    }
}

@Composable
fun LocationTrackerScreen(sharedPreferences: SharedPreferences) {
    var isTrackingEnabled by remember {
        mutableStateOf(
            sharedPreferences.getBoolean(Constants.PREF_TRACKING_ACTIVE, false)
        )
    }

    var hasAllPermissions by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Permission launcher for background location
    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAllPermissions = isGranted
        if (!isGranted) {
            showPermissionRationale = true
        }
    }

    // Permission launcher for regular permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }

        if (allGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10+, request background location separately
                backgroundPermissionLauncher.launch(PermissionUtils.getBackgroundLocationPermission())
            } else {
                hasAllPermissions = true
            }
        } else {
            showPermissionRationale = true
        }
    }

    // Check permissions on lifecycle events
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAllPermissions = PermissionUtils.hasLocationPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Location Tracker",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Status indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            if (isTrackingEnabled && hasAllPermissions) LocationGreen else LocationRed,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isTrackingEnabled && hasAllPermissions) {
                            "Location Tracking Active"
                        } else if (!hasAllPermissions) {
                            "Location Permissions Required"
                        } else {
                            "Location Tracking Inactive"
                        },
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Toggle switch
                if (hasAllPermissions) {
                    TrackingToggle(
                        isEnabled = isTrackingEnabled,
                        onToggleChanged = { isEnabled ->
                            isTrackingEnabled = isEnabled
                            sharedPreferences.edit()
                                .putBoolean(Constants.PREF_TRACKING_ACTIVE, isEnabled)
                                .apply()

                            if (isEnabled) {
                                LocationService.startService(context)
                            } else {
                                LocationService.stopService(context)
                            }
                        }
                    )

                    // Add the mock location button here
                    Spacer(modifier = Modifier.height(16.dp))
                    MockLocationButton(context)

                } else {
                    Button(
                        onClick = {
                            if (showPermissionRationale && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                // Open app settings if we need to show rationale
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                val uri = Uri.fromParts("package", context.packageName, null)
                                intent.data = uri
                                context.startActivity(intent)
                            } else {
                                // Request permissions normally
                                permissionLauncher.launch(PermissionUtils.getRequiredPermissions())
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (showPermissionRationale) {
                                "Open Settings to Grant Permissions"
                            } else {
                                "Grant Location Permissions"
                            }
                        )
                    }

                    if (showPermissionRationale) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Location permissions are required for this app to function properly.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrackingToggle(isEnabled: Boolean, onToggleChanged: (Boolean) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggleChanged
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isEnabled) "Stop Tracking" else "Start Tracking",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Location updates will be sent every 5 seconds",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MockLocationButton(context: Context) {
    // Only show in debug builds
    if (com.bg.locationtracker.BuildConfig.DEBUG) {
        // Only show in debug builds
        if (BuildConfig.DEBUG) {
            val mockLocations = listOf(
                Pair(37.4220, -122.0841), // Google HQ
                Pair(40.7128, -74.0060),  // New York
                Pair(51.5074, -0.1278),   // London
                Pair(35.6762, 139.6503),  // Tokyo
                Pair(-33.8688, 151.2093)  // Sydney
            )

            var currentMockIndex by remember { mutableStateOf(0) }
            val mockLocationProvider = remember { MockLocationProvider(context) }

            Column(
                modifier = Modifier.padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Debug Tools",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        // Get next location in the list
                        val (lat, lng) = mockLocations[currentMockIndex]
                        mockLocationProvider.setMockLocation(lat, lng)

                        // Move to next mock location
                        currentMockIndex = (currentMockIndex + 1) % mockLocations.size
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("Test With Mock Location")
                }

                Text(
                    text = "Current mock: ${mockLocations[currentMockIndex].first}, ${mockLocations[currentMockIndex].second}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}