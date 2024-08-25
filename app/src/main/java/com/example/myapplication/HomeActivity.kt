@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myapplication

import android.app.Activity
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.api.UserRepository
import com.example.myapplication.data.LoginResponse
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.viewmodel.ChangePasswordViewModel
import com.example.myapplication.viewmodel.HomeActivityViewModel
import com.example.myapplication.viewmodel.LoginViewModel
import com.example.myapplication.viewmodel.NavigationEvent
import com.example.myapplication.viewmodel.UserDataViewModel

const val EMPLOYEE_TRACKING_JOB: Int = 1
private const val SHARED_PREFS_NAME = "my_prefs"
private const val KEY_ATTENDANCE = "attendance"
private const val REQUEST_LOCATION_PERMISSION = 100
private val viewModel: HomeActivityViewModel = HomeActivityViewModel(UserRepository())
var attendanceOn: Boolean = false
var runAfterLocationFetch: Runnable? = null
var userData: LoginResponse? = null

class HomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        userData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("USER_DATA", LoginResponse::class.java)
        } else {
            intent.getParcelableExtra("USER_DATA")
        }
        viewModel.attendanceStatus.value = getSharedPreferences(SHARED_PREFS_NAME, 0).getBoolean(KEY_ATTENDANCE, false)


        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DashboardScreen(modifier = Modifier.padding(innerPadding),
                        viewModel, this)
                }
            }
        }

        viewModel.navigationEvent.observe(this)
        { event ->
            when (event) {
                NavigationEvent.NavigateBack -> {
                    finish()
                }

                NavigationEvent.NavigateToAssignedApps -> TODO()
                NavigationEvent.NavigateToChangePassword -> {
                    val intent = Intent(this, ChangePasswordActivity::class.java)
                    intent.putExtra("EMAIL_ADDRESS", userData?.emailAddress)
                    startActivity(intent)
                }
                NavigationEvent.NavigateToEmployeeList -> TODO()
                NavigationEvent.NavigateToHome -> TODO()
                NavigationEvent.NavigateToViewTarget -> TODO()
            }
        }

        viewModel.attendanceApiState.observe(this) {
                event ->
            Log.e("dddddd", "HomeActivity: onCreate: apiState: observe: event = " + event)

            when (event) {
                is ChangePasswordViewModel.ApiState.Success -> {
                    viewModel.isLoading = false
                    attendanceOn = getSharedPreferences(SHARED_PREFS_NAME, 0).getBoolean(KEY_ATTENDANCE, false)
                    getSharedPreferences(SHARED_PREFS_NAME, 0).edit().putBoolean(KEY_ATTENDANCE, !attendanceOn).apply()
                    viewModel.attendanceStatus.value = !attendanceOn
                }
                is ChangePasswordViewModel.ApiState.Error -> {
                    viewModel.isLoading = false
                    viewModel.errorMessage = "Invalid Credentials"
                }
                ChangePasswordViewModel.ApiState.Loading -> { // Update isLoading state here
                    viewModel.isLoading = true
                }
            }
        }

        viewModel.attendanceStatus.observe(this) {
            if (it) {
                scheduleEmployeeTrackingJob(this, userData!!.trackingTime)
            } else {
                stopEmployeeTrackingJob(this)
            }

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed with location request
                    fetchLocation(context = this)
                } else {
                    // Permission denied, handle accordingly (e.g., show a message or disable location-related features)
                }
            }
        }
    }
}

fun scheduleEmployeeTrackingJob(context: Context, interval: String) {

    val mins = Integer.valueOf(interval)
    val extras = PersistableBundle()
    extras.putString("EMPLOYEE_ID", userData?.employeeId)

    val jobInfo = JobInfo.Builder(EMPLOYEE_TRACKING_JOB, ComponentName(context, TrackingService::class.java))
        .setPeriodic((mins * 60 * 1000).toLong()) // Schedule every 30 minutes
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setPersisted(true) // Keep the job scheduled even if the device reboots
        .setExtras(extras)
        .build()

    val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    jobScheduler.schedule(jobInfo)
}

fun stopEmployeeTrackingJob(context: Context) {
    val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    jobScheduler.cancel(EMPLOYEE_TRACKING_JOB)
}

fun setLocation(location: Location, context: Context) {
    viewModel.setLocation(location, context)
}

fun markPresence() {
    Log.e("dddddd", "HomeActviity: markPresence: userData != null, ${userData != null}")
    userData?.let {
        viewModel.markAttendance(
            employeeId = it.employeeId,
            email = it.emailAddress,
            mode = !attendanceOn)
    }
}

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeActivityViewModel,
    activity: Activity
) {
    val context = LocalContext.current

    val options = listOf("Employee List", "Assigned Applications",
        "Mark Attendance " + if (viewModel.attendanceStatus.value == true) "Off" else "On", "Change Password", "View Target Details")

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            TargetView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.3f),
                monthlyTarget = stringResource(
                    id = R.string.monthly_target,
                    userData?.monthlyTarget ?: 0
                ),
                yearlyTarget = stringResource(
                    id = R.string.yearly_target,
                    userData?.yearlyTarget ?: 0
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
            ) {
                items(options) { item ->
                    Box(
                        modifier = Modifier.clickable {
                            // Handle click here
                            when (item) {
                                "Employee List" -> {

                                }

                                "Assigned Applications" -> {

                                }

                                "Mark Attendance On" -> {
                                    runAfterLocationFetch = Runnable {
                                        markPresence()
                                    }
                                    if (ContextCompat.checkSelfPermission(/* context = */ context, /* permission = */
                                            android.Manifest.permission.ACCESS_FINE_LOCATION
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        // Request
                                        requestLocationPermission(activity)
                                    } else {
                                        fetchLocation(context = activity)
                                    }
                                }

                                "Mark Attendance Off" -> {
                                    runAfterLocationFetch = Runnable {
                                        markPresence()
                                    }
                                    if (ContextCompat.checkSelfPermission(/* context = */ context, /* permission = */
                                            android.Manifest.permission.ACCESS_FINE_LOCATION
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        // Request
                                        requestLocationPermission(activity)
                                    } else {
                                        fetchLocation(context = activity)
                                    }
                                }

                                "Change Password" -> {
                                    viewModel.navigateToChangePasswordActivity()
                                }

                                "View Target Details" -> {

                                }
                            }
                            println("Item clicked: $item")
                        }
                    ) {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                        HorizontalDivider(
                            color = Color.Gray,
                            thickness = 1.dp
                        )
                    }

                }
            }
            Spacer(modifier = Modifier.weight(0.1f))
            // Additional information or actions
        }
        if (viewModel.isLoading) {
            ApiProgressBar(modifier = Modifier.align(Alignment.Center))
        }
    }

}

@Composable
fun TargetView(modifier: Modifier, monthlyTarget: String, yearlyTarget: String) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = modifier.fillMaxWidth()
        ) {
            Text(
                text = "Targets",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )
            Text(
                text = monthlyTarget,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )
            Text(
                text = yearlyTarget,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun requestLocationPermission(activity: Activity) {
    if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
    } else {
        // Permission already granted
        fetchLocation(activity)
    }
}

private lateinit var locationManager: LocationManager
private lateinit var locationListener: LocationListener

fun fetchLocation(context: Activity) {
    locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
    locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.e("dddddd", "HomeActivity: fetchLocation: Got Location = ")
            Log.e("dddddd", "HomeActivity: fetchLocation:  runAfterLocationFetch != null ${runAfterLocationFetch != null}")
            // Handle the location data here
            setLocation(location, context)
            runAfterLocationFetch?.let { Handler(Looper.getMainLooper()).post(it) }
            locationManager.removeUpdates(locationListener)
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 0f, locationListener)
    }
}

enum class DashboardOptions(s: String) {
    EMPLOYEE_LIST("Employee List"),
    ASSIGNED_APPS("Assigned Applications"),
    MARK_ATTENDANCE("Mark Attendance"),
    CHANGE_PASSWORD("Change Password"),
    VIEW_TARGET("View Target Details")

}
