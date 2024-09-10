package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.api.UserRepository
import com.example.myapplication.data.Constants
import com.example.myapplication.data.LoginResponse
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.utils.DateTimeFormatter
import com.example.myapplication.viewmodel.ApiState
import com.example.myapplication.viewmodel.NavigationEvent
import com.example.myapplication.viewmodel.PresenceViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

enum class PresenceViewType {
    TYPE_LOGIN,
    TYPE_LOGOUT
}

val presenceViewModel: PresenceViewModel = PresenceViewModel(UserRepository())

class PresenceActivity : ComponentActivity() {

    private lateinit var userData: LoginResponse
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userData = intent.getParcelableExtra(Constants.USER_DATA)!!

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
                    fetchAttendanceStatus(this)
                }
                is ApiState.Error -> {
                    presenceViewModel.isLoading = false
                    presenceViewModel.errorMessage = "Invalid Credentials"
                }
                ApiState.Loading -> { // Update isLoading state here
                    presenceViewModel.isLoading = true
                }
            }
        }

        presenceViewModel.fetchAttendanceApiState.observe(this) {
                event ->
            Log.e("dddddd", "PresenceActivity: onCreate: fetchAttendanceApiState: observe: event = $event")

            when (event) {
                is PresenceViewModel.FetchAttendanceState.Success -> {
                    presenceViewModel.isLoading = false
                    presenceViewModel.presenceResponse.value = event.attendanceResponse
                    val presenceResponse = event.attendanceResponse
                    if(TextUtils.isEmpty(presenceResponse.startTime)) {
                        getLocation(this) { location: Location, address: String ->
                            presenceViewModel.isLoading = false
                            presenceViewModel.setInLocation(location, address, this)
                        }
                    } else if(TextUtils.isEmpty(presenceResponse.endTime)) {
                        // Start the location service
                        val intent = Intent(this, LocationService::class.java)
                        intent.putExtra(Constants.USER_DATA, userData)
                        startService(intent)

                        getLocation(this) { location: Location, address: String ->
                            presenceViewModel.isLoading = false
                            presenceViewModel.setOutLocation(location, address, this)
                        }
                    } else {
                        val intent = Intent(this, LocationService::class.java)
                        stopService(intent)
                        // finish()
                        // Show end duty error

                    }
                }
                is PresenceViewModel.FetchAttendanceState.Error -> {
                    presenceViewModel.isLoading = false
                    presenceViewModel.errorMessage = "Fetch presence failed"
                    getLocation(this) { location: Location, address: String ->
                        presenceViewModel.isLoading = false
                        presenceViewModel.setInLocation(location, address, this)
                    }
                }
                PresenceViewModel.FetchAttendanceState.Loading -> { // Update isLoading state here
                    presenceViewModel.isLoading = true
                }
            }
        }

        fetchAttendanceStatus(this)
    }

    private fun fetchAttendanceStatus(activity: Activity) {
        presenceViewModel.isLoading = true
        presenceViewModel.fetchAttendance(userData.employeeId)
    }

    private fun getLocation(activity: Activity, callback: (location: Location, address: String) -> Unit) {
        presenceViewModel.isLoading = true
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission(activity)
        } else {
            requestLocationUpdates(activity, callback)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates(context: Context, callback: (location: Location, address: String) -> Unit) {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // Update interval in milliseconds (adjust as needed)
            fastestInterval = 5000 // Fastest update interval in milliseconds
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, LocationCallbackImpl(context, fusedLocationClient, callback), Looper.getMainLooper())
    }

    private fun requestLocationPermission(activity: Activity) {
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION), Constants.REQUEST_LOCATION_PERMISSION)
        } else {
            // Permission already granted
            val presenceResponse = presenceViewModel.presenceResponse.value
            if (TextUtils.isEmpty(presenceResponse?.startTime)) {
                requestLocationUpdates(activity) { location: Location, address: String ->
                    presenceViewModel.setInLocation(location, address, activity)
                }
            } else if (TextUtils.isEmpty(presenceResponse?.endTime)) {
                presenceViewModel.inAddress.value = presenceResponse?.startAddress
                if (presenceResponse != null) {
                    presenceViewModel.inTime = DateTimeFormatter.parseTime(presenceResponse.startTime)
                }
                requestLocationUpdates(activity) { location: Location, address: String ->
                    presenceViewModel.setInLocation(location, address, activity)
                }
            } else {
                // Show duty already completed dialog
            }
        }
    }

    private class LocationCallbackImpl(
        val context: Context,
        val fusedLocationClient: FusedLocationProviderClient,
        val callback: (location: Location, address: String) -> Unit
    ) : LocationCallback() {

        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val location = locationResult.lastLocation
            // Handle the location data here
            if (location != null) {
                val address = presenceViewModel.getAddressFromLatLng(context, location.latitude, location.longitude) ?: ""
                callback.invoke(location, address)
            }
            fusedLocationClient.removeLocationUpdates(this)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresenceScreen(isLoading: Boolean, userData: LoginResponse, modifier: Modifier) {
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
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )
    }, content = {
            innerPadding ->
        Box(modifier = modifier.fillMaxSize()
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
                LocationProgressBar(modifier = Modifier.align(Alignment.Center))
            }
        }
    })
}

@Composable
fun PresenceView(
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



@Composable
fun LocationProgressBar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(60.dp),
            strokeWidth = 4.dp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview2() {
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
                true
        ), modifier = Modifier.fillMaxSize())
    }
}