package com.example.myapplication

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.example.myapplication.api.UserRepository
import com.example.myapplication.data.LoginResponse
import com.example.myapplication.data.TrackingRequest
import com.example.myapplication.utils.DateTimeFormatter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.Locale

class LocationService : Service() {
    private lateinit var userData: LoginResponse
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallBack: LocationCallbackImpl

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            userData = intent.getParcelableExtra("USER_DATA")!!
        }
        requestLocationUpdates(applicationContext)
        return START_STICKY
    }

    override fun onDestroy() {
        stopLocationUpdates()
        super.onDestroy()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallBack)
    }

    private fun checkLocationPermission(): Boolean {
        val hasPermissions = PackageManager.PERMISSION_GRANTED == checkSelfPermission(
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) && PackageManager.PERMISSION_GRANTED == checkSelfPermission(
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return hasPermissions
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates(context: Context) {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        locationCallBack = LocationCallbackImpl(context, userData)

        val trackingTime = userData.trackingTime.toInt()
        val locationRequest = LocationRequest.create().apply {
            interval =
                (trackingTime * 60 * 1000).toLong() // Update interval in milliseconds (adjust as needed)
            fastestInterval =
                (trackingTime * 60 * 1000).toLong() // Fastest update interval in milliseconds
            priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallBack, Looper.getMainLooper())
    }

    private class LocationCallbackImpl(val context: Context, val userData: LoginResponse) : LocationCallback() {
        val TAG: String = "LocationService"

        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val location = locationResult.lastLocation
            // Handle the location data here
            if (location != null) {
                val employeeId: String = userData.employeeId
                val lat = location.latitude
                val long = location.longitude
                val address = getAddressFromLatLng(context, lat, long)
                val trackingRequest = TrackingRequest(
                    latLongTime = DateTimeFormatter.formatDateTime(LocalDateTime.now()),
                    logDate = DateTimeFormatter.formatDate(LocalDateTime.now()),
                    latitude = lat.toString(),
                    longitude = long.toString(),
                    employeeId = Integer.valueOf(employeeId),
                    address = address ?: ""
                )
                CoroutineScope(Dispatchers.Default).launch {
                    val result = UserRepository().trackEmployee(trackingRequest = trackingRequest)
                    if (result.isSuccess) {
                        Log.i(TAG, "Track data sent successfully")
                    } else if (result.isFailure){
                        Log.i(TAG, "Error in sending employee tracking data")
                    }
                }
            }
        }

        private fun getAddressFromLatLng(context: Context, latitude: Double, longitude: Double): String? {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val addressString = address.getAddressLine(0)
                Log.d("Address", addressString)
                return addressString
            } else {
                Log.e("Address", "Unable to get address from latitude and longitude")
                return null
            }
        }
    }
}