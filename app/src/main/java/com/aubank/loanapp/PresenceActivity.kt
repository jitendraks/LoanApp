package com.aubank.loanapp

import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aubank.loanapp.api.UserRepository
import com.aubank.loanapp.components.ApiProgressBar
import com.aubank.loanapp.components.GetCurrentLocation
import com.aubank.loanapp.data.Constants
import com.aubank.loanapp.data.LoginResponse
import com.aubank.loanapp.ui.theme.MyApplicationTheme
import com.aubank.loanapp.viewmodel.ApiState
import com.aubank.loanapp.viewmodel.NavigationEvent
import com.aubank.loanapp.viewmodel.PresenceViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.Locale

enum class PresenceViewType {
    TYPE_LOGIN,
    TYPE_LOGOUT
}

val presenceViewModel: PresenceViewModel = PresenceViewModel(UserRepository())

class PresenceActivity : ComponentActivity() {

    private lateinit var userData: LoginResponse

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userData = intent.getParcelableExtra(Constants.USER_DATA)!!
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PresenceScreen(
                    userData,
                    modifier = Modifier.fillMaxWidth(),
                    startEmployeeTracking = {
                        startEmployeeTracking(this, userData)
                    },
                    stopEmployeeTracking = {
                        stopEmployeeTracking()
                    }
                )
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
                    presenceViewModel.isLoading = false
                    Log.d("PresenceActivity", "attendanceApiState is null")
                    return@observe
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            presenceViewModel.resetApiResponseState()
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

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresenceScreen(userData: LoginResponse,
                           modifier: Modifier,
                           startEmployeeTracking : () -> Unit,
                           stopEmployeeTracking : () -> Unit) {
    val isLocationLoading = remember { mutableStateOf(false) }
    val isStartTime = remember { mutableStateOf(true) }
    val shouldGetLocation = remember { mutableStateOf(false) }
    val context: Context = LocalContext.current

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
            if (presenceViewModel.isLoading || isLocationLoading.value) {
                ApiProgressBar(modifier = Modifier.align(Alignment.Center))
            }

            key(shouldGetLocation.value) {
                if (shouldGetLocation.value) {
                    isLocationLoading.value = true
                    GetCurrentLocation(
                        acceptableLastLocationAgeMillis = 15 * 60 * 1000L,
                        onLocationResult = { location ->
                            if (location != null) {
                                getAddressFromLatLng(
                                    context = context,
                                    location = location,
                                    onAddressResolved = { address ->
                                        shouldGetLocation.value = false
                                        isLocationLoading.value = false
                                        if (isStartTime.value) {
                                            presenceViewModel.setInLocation(location, address)
                                        } else {
                                            presenceViewModel.setOutLocation(location, address)
                                        }
                                    },
                                )
                            } else {
                                isLocationLoading.value = false
                            }
                        },
                        locationRequestTimeoutMillis = 10 * 1000
                    )
                }
            }
        }
    })

    presenceViewModel.fetchAttendanceApiState.observe(LocalLifecycleOwner.current) { event ->
        if (event == null) {
            Log.d("PresenceActivity", "fetchAttendanceApiState is null")
            presenceViewModel.isLoading = false
            shouldGetLocation.value = false
            return@observe
        }

        when (event) {
            is PresenceViewModel.FetchAttendanceState.Success -> {
                presenceViewModel.isLoading = false
                val presenceResponse = event.attendanceResponse
                presenceViewModel.presenceResponse.value = presenceResponse

                try {
                    if (TextUtils.isEmpty(presenceResponse.startTime)) {
                        shouldGetLocation.value = true
                        isStartTime.value = true

                    } else if (TextUtils.isEmpty(presenceResponse.endTime)) {
                        shouldGetLocation.value = true
                        isStartTime.value = false
                        startEmployeeTracking.invoke()
                    } else {
                        shouldGetLocation.value = false
                        isStartTime.value = true
                        stopEmployeeTracking.invoke()
                    }
                } catch (e: Exception) {
                    Log.e("PresenceActivity", "Exception during location handling", e)
                    shouldGetLocation.value = false
                    presenceViewModel.isLoading = false
                    presenceViewModel.errorMessage = "Failed due to internal error"
                }
            }

            is PresenceViewModel.FetchAttendanceState.Error -> {
                shouldGetLocation.value = true
                presenceViewModel.isLoading = false
                presenceViewModel.errorMessage = "Fetch presence failed"
            }

            PresenceViewModel.FetchAttendanceState.Loading -> {
                presenceViewModel.isLoading = true
            }
        }
    }
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


private fun getAddressFromLatLng(context: Context, location: Location, onAddressResolved: (String) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            if (!Geocoder.isPresent()) {
                withContext(Dispatchers.Main) {
                    onAddressResolved("Geocoder service not available")
                }
                return@launch
            }

            val address = withTimeoutOrNull(10 * 1000) { // 3-second timeout
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                addresses?.firstOrNull()?.getAddressLine(0)
            } ?: "Timeout: Unable to get address"

            withContext(Dispatchers.Main) {
                onAddressResolved(address)
            }
        } catch (e: IOException) {
            Log.e("Geocoder", "Geocoding failed", e)
            withContext(Dispatchers.Main) {
                onAddressResolved("Error: Could not retrieve address")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GreetingPreview() {
    MyApplicationTheme {
        PresenceScreen(LoginResponse(
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
        ), modifier = Modifier.fillMaxSize(), { }, { })
    }
}