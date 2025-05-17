package com.aubank.loanapp.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

@Composable
fun GetCurrentLocation(
    acceptableLastLocationAgeMillis: Long = 15 * 60 * 1000L, // 15 minutes default
    onLocationResult: (Location?) -> Unit,
    locationRequestTimeoutMillis: Long = 10 * 1000 // 10 seconds default){}
) {
    val context = LocalContext.current
    val locationState = remember { mutableStateOf<Location?>(null) }
    val isPermissionGrantedState = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val isFetchingLocation = remember { mutableStateOf(false) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            isPermissionGrantedState.value = isGranted
        }
    )

    // Function to get the last known location
    suspend fun getLastKnownLocation(): Location? = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        } else {
            null
        }
    }

    // Function to request a single current location update
    suspend fun requestSingleLocation(): Location? = suspendCancellableCoroutine { continuation ->
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationListener = object : android.location.LocationListener {
            override fun onLocationChanged(location: Location) {
                continuation.resume(location)
                locationManager.removeUpdates(this)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, null)

            // Set a timeout
            val scope = CoroutineScope(Dispatchers.Default)
            val timeoutJob = scope.launch {
                delay(locationRequestTimeoutMillis)
                if (continuation.isActive) {
                    continuation.resume(null) // Resume with null if timeout occurs
                    locationManager.removeUpdates(locationListener)
                }
            }

            continuation.invokeOnCancellation {
                locationManager.removeUpdates(locationListener)
                timeoutJob.cancel()
            }
        } catch (e: SecurityException) {
            continuation.resume(null)
        } catch (e: IllegalArgumentException) {
            continuation.resume(null)
        }
    }

    // Observe permission state and fetch location
    LaunchedEffect(key1 = isPermissionGrantedState.value) {
        if (isPermissionGrantedState.value) {
            isFetchingLocation.value = true
            val lastLocation = getLastKnownLocation()
            if (lastLocation != null && (System.currentTimeMillis() - lastLocation.time <= acceptableLastLocationAgeMillis)) {
                locationState.value = lastLocation
                onLocationResult(lastLocation)
            } else {
                // Last known location is old or null, request a fresh one
                val freshLocation = requestSingleLocation()
                locationState.value = freshLocation
                onLocationResult(freshLocation)
            }
            isFetchingLocation.value = false
        } else {
            locationState.value = null
            onLocationResult(null)
        }
    }

    // Request permission if not already granted
    LaunchedEffect(key1 = true) {
        if (!isPermissionGrantedState.value) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }
}
