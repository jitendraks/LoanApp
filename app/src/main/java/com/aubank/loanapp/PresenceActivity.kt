package com.aubank.loanapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aubank.loanapp.api.UserRepository
import com.aubank.loanapp.components.ApiProgressBar
import com.aubank.loanapp.data.Constants
import com.aubank.loanapp.data.LoginResponse
import com.aubank.loanapp.ui.theme.MyApplicationTheme
import com.aubank.loanapp.viewmodel.ApiState
import com.aubank.loanapp.viewmodel.NavigationEvent
import com.aubank.loanapp.viewmodel.PresenceViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

enum class PresenceViewType {
    TYPE_LOGIN,
    TYPE_LOGOUT
}

val presenceViewModel: PresenceViewModel = PresenceViewModel(UserRepository())

class PresenceActivity : ComponentActivity() {

    private lateinit var userData: LoginResponse
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallbackImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userData = intent.getParcelableExtra(Constants.USER_DATA)!!
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PresenceScreen(
                    presenceViewModel.isLoading,
                    userData,
                    modifier = Modifier.fillMaxWidth())
            }
        }

        presenceViewModel.navigationEvent.observe(this) {
            event ->
            when (event) {
                NavigationEvent.NavigateBack -> {
                    finish()
                }

                else -> {}
            }
        }

        presenceViewModel.attendanceApiState.observe(this) {
                event ->
            Log.e("dddddd", "PresenceActivity: onCreate: apiState: observe: event = $event")

            when (event) {
                is ApiState.Success -> {
                    presenceViewModel.isLoading = false
                    fetchAttendanceStatus()
                }
                is ApiState.Error -> {
                    presenceViewModel.isLoading = false
                    presenceViewModel.errorMessage = event.exception.toString()
                }
                ApiState.Loading -> { // Update isLoading state here
                    presenceViewModel.isLoading = true
                }

                null -> {
                    Log.d("PresenceActivity", "attendanceApiState is null")
                    return@observe
                }
            }
        }

        presenceViewModel.fetchAttendanceApiState.observe(this) { event ->
            if (event == null) {
                Log.d("PresenceActivity", "fetchAttendanceApiState is null")
                return@observe
            }

            when (event) {
                is PresenceViewModel.FetchAttendanceState.Success -> {
                    presenceViewModel.isLoading = false
                    val presenceResponse = event.attendanceResponse
                    presenceViewModel.presenceResponse.value = presenceResponse

                    try {
                        if (TextUtils.isEmpty(presenceResponse?.startTime)) {
                            getLocation(this) { location, address ->
                                presenceViewModel.isLoading = false
                                if (location != null && address != null) {
                                    presenceViewModel.setInLocation(location, address)
                                } else {
                                    presenceViewModel.errorMessage = "Could not fetch location"
                                }
                            }
                        } else if (TextUtils.isEmpty(presenceResponse?.endTime)) {
                            startEmployeeTracking(context = this, userData = userData)

                            getLocation(this) { location, address ->
                                presenceViewModel.isLoading = false
                                if (location != null && address != null) {
                                    presenceViewModel.setOutLocation(location, address)
                                } else {
                                    presenceViewModel.errorMessage = "Could not fetch location"
                                }
                            }
                        } else {
                            stopEmployeeTracking()
                        }
                    } catch (e: Exception) {
                        Log.e("PresenceActivity", "Exception during location handling", e)
                        presenceViewModel.isLoading = false
                        presenceViewModel.errorMessage = "Failed due to internal error"
                    }
                }

                is PresenceViewModel.FetchAttendanceState.Error -> {
                    presenceViewModel.isLoading = false
                    presenceViewModel.errorMessage = "Fetch presence failed"
                }

                PresenceViewModel.FetchAttendanceState.Loading -> {
                    presenceViewModel.isLoading = true
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            presenceViewModel.resetApiResponseState()
            if (::locationCallback.isInitialized) {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        } catch (e: Exception) {
            Log.e("PresenceActivity", "Failed to remove location updates", e)
        }
    }

    private fun startEmployeeTracking(context: Context, userData: LoginResponse) {
        if (!LocationService.isServiceRunning(context)) {
            val intent = Intent(context, LocationService::class.java)
            intent.putExtra(Constants.USER_DATA, userData)
            context.startService(intent)
        }
    }

    private fun stopEmployeeTracking() {
        val intent = Intent(this, LocationService::class.java)
        stopService(intent)
    }

    private fun fetchAttendanceStatus() {
        presenceViewModel.isLoading = true
        presenceViewModel.fetchAttendance(userData.employeeId)
    }

    private fun getLocation(
        activity: Activity,
        callback: (Location?, String?) -> Unit
    ) {
        presenceViewModel.isLoading = true

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                Constants.REQUEST_LOCATION_PERMISSION
            )
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            val locationResult = withTimeoutOrNull(10_000) {
                requestLocationOnce(activity)
            }

            if (locationResult != null) {
                presenceViewModel.isLoading = false
                callback(locationResult.first, locationResult.second)
            } else {
                presenceViewModel.isLoading = false
                callback(null, "Location timeout or unavailable")
            }
        }
    }

    private suspend fun requestLocationOnce(
        context: Context
    ): Pair<Location, String>? = suspendCancellableCoroutine { cont ->
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                fusedLocationClient.removeLocationUpdates(this)

                val location = locationResult.lastLocation
                if (location != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val address = try {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            addresses?.firstOrNull()?.getAddressLine(0) ?: "Address not found"
                        } catch (e: Exception) {
                            Log.e("Geocoder", "Failed to fetch address", e)
                            "Error: Could not retrieve address"
                        }
                        withContext(Dispatchers.Main) {
                            if (cont.isActive) cont.resume(Pair(location, address))
                        }
                    }
                } else {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }

        val request = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = Priority.PRIORITY_HIGH_ACCURACY
            numUpdates = 1
        }

        try {
            // Explicit permission check
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
                cont.invokeOnCancellation {
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                }
            } else {
                Log.e("Location", "Permission denied")
                cont.resume(null)
            }
        } catch (e: SecurityException) {
            Log.e("Location", "SecurityException: ${e.message}", e)
            cont.resume(null)
        }
    }

    private class LocationCallbackImpl(
        val context: Context,
        val fusedLocationClient: FusedLocationProviderClient,
        val callback: (location: Location?, address: String?) -> Unit
    ) : LocationCallback() {

        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val location = locationResult.lastLocation
            // Handle the location data here
            if (location != null) {
                getAddressAsync(context, location.latitude, location.longitude) { address ->
                    // Use the result
                    callback.invoke(location, address)
                }
            } else {
                callback.invoke(null, null)
            }
            fusedLocationClient.removeLocationUpdates(this)
        }
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
fun getAddressAsync(
    context: Context,
    lat: Double,
    lng: Double,
    callback: (String) -> Unit
) {
    val contextRef = java.lang.ref.WeakReference(context)
    CoroutineScope(Dispatchers.IO).launch {
        val currentContext = contextRef.get() // Get the current context
        if (currentContext != null) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    val address = addresses?.firstOrNull()?.getAddressLine(0) ?: ""
                    withContext(Dispatchers.Main) {
                        callback(address)
                    }
            } catch (e: IOException) {
                Log.e("Geocoder", "Failed to fetch address", e)
                withContext(Dispatchers.Main) {
                    callback("Error: Could not retrieve address") // Or a more user-friendly message
                }
            }
        } else {
            // Handle the case where the context is no longer available (e.g., Activity destroyed)
            withContext(Dispatchers.Main) {
                callback("Error: Context no longer available")
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresenceScreen(isLoading: Boolean, userData: LoginResponse, modifier: Modifier) {
    LaunchedEffect(Unit) {
        presenceViewModel.isLoading = true
        presenceViewModel.fetchAttendance(userData.employeeId)
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Set Presence") },
            colors = topAppBarColors(
                containerColor = Color.Blue, // Set your desired background color here
                titleContentColor = Color.White,
            ),
            navigationIcon = {
                IconButton(onClick = { presenceViewModel.navigateBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )
    }, content = {
            innerPadding ->
        Box(modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            Column(verticalArrangement = Arrangement.Center,
                modifier = modifier.fillMaxWidth()) {
                PresenceView(
                    modifier = modifier,
                    address = (if ( TextUtils.isEmpty(presenceViewModel.presenceResponse.value?.startTime)) presenceViewModel.inAddress.value else presenceViewModel.presenceResponse.value?.startAddress).toString(),
                    time = (if ( TextUtils.isEmpty(presenceViewModel.presenceResponse.value?.startTime)) presenceViewModel.getFormattedTime(presenceViewModel.inTime) else presenceViewModel.presenceResponse.value?.startTime).toString(),
                    presenceViewType = PresenceViewType.TYPE_LOGIN, userData)
                if (!TextUtils.isEmpty(presenceViewModel.presenceResponse.value?.startTime)) {
                    PresenceView(
                        modifier = modifier,
                        address = (if ( TextUtils.isEmpty(presenceViewModel.presenceResponse.value?.endTime)) presenceViewModel.outAddress.value else presenceViewModel.presenceResponse.value?.endAddress).toString(),
                        time = (if ( TextUtils.isEmpty(presenceViewModel.presenceResponse.value?.endTime)) presenceViewModel.getFormattedTime(presenceViewModel.outTime) else presenceViewModel.presenceResponse.value?.endTime).toString(),
                        presenceViewType = PresenceViewType.TYPE_LOGOUT, userData)
                }
            }
            if (isLoading) {
                ApiProgressBar(modifier = Modifier.align(Alignment.Center))
            }
        }
    })
}

@Composable
private fun PresenceView(
    modifier: Modifier,
    address: String,
    time: String,
    presenceViewType: PresenceViewType,
    userData: LoginResponse) {

    Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(8.dp),
            modifier = modifier.padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.Top,
                modifier = modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (presenceViewType == PresenceViewType.TYPE_LOGIN) "In Time" else "Out Time",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Left
                )
                // Divider
                HorizontalDivider(
                    thickness = 1.dp,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = {  },
                    label = { Text("Address") },
                    singleLine = false,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = {  },
                    label = { Text("Time") },
                    singleLine = false,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )

                if ((presenceViewType == PresenceViewType.TYPE_LOGIN
                            && TextUtils.isEmpty(presenceViewModel.presenceResponse.value?.startTime))
                    || (presenceViewType == PresenceViewType.TYPE_LOGOUT
                            && TextUtils.isEmpty(presenceViewModel.presenceResponse.value?.endTime))
                    ){
                    Button( onClick = { presenceViewModel.markAttendance(userData.employeeId, userData.emailAddress, presenceViewType == PresenceViewType.TYPE_LOGIN)},
                        modifier = Modifier.fillMaxWidth()) {
                        Text(text = if (presenceViewType == PresenceViewType.TYPE_LOGIN) "Start Duty" else "Stop Duty")
                    }
                }
            }
        }
}

@Preview(showBackground = true)
@Composable
private fun GreetingPreview() {
    MyApplicationTheme {
        PresenceScreen(isLoading = false,
            LoginResponse(
            "1",
                "1",
                "Jitendra Sharma",
                "jitendrasharma407@gmail.com",
                "Admin",
                "30",
                50000,
                100000,
                true,
                0,
                0,
                true,
                applicationAlloted = 0
        ), modifier = Modifier.fillMaxSize())
    }
}