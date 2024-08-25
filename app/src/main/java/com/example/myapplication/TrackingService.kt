package com.example.myapplication

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.myapplication.api.ApiService
import com.example.myapplication.api.UserRepository
import com.example.myapplication.data.AttendanceRequest
import com.example.myapplication.data.TrackingRequest
import com.example.myapplication.viewmodel.ChangePasswordViewModel.ApiState
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TrackingService : JobService() {
    val TAG: String = "TrackingService"

    override fun onStartJob(params: JobParameters): Boolean {
        CoroutineScope(Dispatchers.Default).launch {
            // Get the user's location
            getLocation(baseContext) {
                location ->
                val employeeId: String = params.extras.getString("EMPLOYEE_ID")!!
                val lat = location?.latitude
                val long = location?.longitude
                val trackingRequest = TrackingRequest(
                    latLongTime = LocalDateTime.now().formatTime(),
                    logDate = LocalDateTime.now().formatDate(),
                    latitude = lat.toString(),
                    longitude = long.toString(),
                    employeeId = Integer.valueOf(employeeId)
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
        return true // Indicate that the job is still running
    }

    private fun getLocation(context: Context, callback: (location: Location) -> Unit): Location? {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // Check for location permissions
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null // Handle permission denied case
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            callback(location)
            // Process the location if available
            // (You can update the trackingRequest here or perform other actions)
        }.addOnFailureListener { exception ->
            Log.e("Location", "Failed to get location: ${exception.message}")
        }

        return null // Since we don't wait for the location here, return null
    }

    override fun onStopJob(params: JobParameters): Boolean {
        // Handle job cancellation if needed
        return false
    }

    fun LocalDateTime.formatDate(): String {
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
        return this.format(formatter)
    }

    fun LocalDateTime.formatTime(): String {
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm")
        return this.format(formatter)
    }
}
