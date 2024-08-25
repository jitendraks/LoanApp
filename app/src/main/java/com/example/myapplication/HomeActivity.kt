@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager

import android.location.Address
import android.location.Geocoder
import android.util.Log
import java.util.Locale

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.example.myapplication.api.UserRepository
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.viewmodel.HomeActivityViewModel
import com.example.myapplication.viewmodel.LoginViewModel
import com.example.myapplication.viewmodel.NavigationEvent

private const val SHARED_PREFS_NAME = "my_prefs"
private const val KEY_ATTENDANCE = "attendance"
private val REQUEST_LOCATION_PERMISSION = 100

class HomeActivity : ComponentActivity() {

    private val viewModel: HomeActivityViewModel = HomeActivityViewModel()
    private val loginViewModel: LoginViewModel = LoginViewModel(UserRepository())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DashboardScreen(modifier = Modifier.padding(innerPadding),
                        viewModel, loginViewModel)
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
                    intent.putExtra("EMAIL_ADDRESS", loginViewModel.userData.value?.emailAddress)
                    startActivity(intent)
                }
                NavigationEvent.NavigateToEmployeeList -> TODO()
                NavigationEvent.NavigateToHome -> TODO()
                NavigationEvent.NavigateToViewTarget -> TODO()
            }
        }

        if (ContextCompat.checkSelfPermission(/* context = */ this, /* permission = */ android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            // Request
            requestLocationPermission(this)
        } else {
            fetchLocation(context = this)
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview2() {
    MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            DashboardScreen(
                modifier = Modifier.padding(innerPadding),
                HomeActivityViewModel(),
                LoginViewModel(UserRepository())
            )
        }
    }
}

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeActivityViewModel,
    loginViewModel: LoginViewModel
) {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)

    }
    val attendanceOn = remember {
        sharedPreferences.getBoolean(KEY_ATTENDANCE, false)
    }

    val options = listOf("Employee List", "Assigned Applications",
        "Mark Attendance " + if (attendanceOn) "Off" else "On", "Change Password", "View Target Details")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TargetView(modifier = Modifier
            .fillMaxWidth()
            .weight(0.3f),
            monthlyTarget = stringResource(id = R.string.monthly_target,
                loginViewModel.userData.value?.monthlyTarget ?: 0
            ),
            yearlyTarget = stringResource(id = R.string.yearly_target,
                loginViewModel.userData.value?.yearlyTarget ?: 0
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier
            .fillMaxWidth()
            .weight(0.5f)) {
            items(options) { item ->
                Box(
                    modifier = Modifier.clickable {
                        // Handle click here
                        when(item) {
                            "Employee List" -> {

                            }
                            "Assigned Applications" -> {

                            }
                            "Mark Attendance" -> {

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
                    Text(text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp))
                    HorizontalDivider(color = Color.Gray,
                        thickness = 1.dp)
                }

            }
        }
        Spacer(modifier = Modifier.weight(0.1f))
        // Additional information or actions
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

private fun requestLocationPermission(context: Activity) {
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(context, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
    } else {
        // Permission already granted
        fetchLocation(context)
    }
}

private lateinit var locationManager: LocationManager
private lateinit var locationListener: LocationListener

fun fetchLocation(context: Activity) {
    locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
    locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Handle the location data here
            val latitude = location.latitude
            val longitude = location.longitude
            val address = getAddressFromLatLng(context, latitude, longitude)
            // ... do something with the latitude and longitude

        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, locationListener)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 0, 0f, locationListener)
        }
    }
}


fun getAddressFromLatLng(context: Activity, latitude: Double, longitude: Double): String? {
    val geocoder = Geocoder(context, Locale.getDefault())
    val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

    if (addresses != null && addresses.isNotEmpty()) {
        val address = addresses[0]
        val addressString = address.getAddressLine(0)
        Log.d("Address", addressString)
        return addressString
    } else {
        Log.e("Address", "Unable to get address from latitude and longitude")
        return null
    }
}


enum class DashboardOptions(s: String) {
    EMPLOYEE_LIST("Employee List"),
    ASSIGNED_APPS("Assigned Applications"),
    MARK_ATTENDANCE("Mark Attendance"),
    CHANGE_PASSWORD("Change Password"),
    VIEW_TARGET("View Target Details")

}
