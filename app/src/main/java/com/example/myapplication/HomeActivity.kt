@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myapplication

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.api.UserRepository
import com.example.myapplication.data.LoginResponse
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.viewmodel.HomeActivityViewModel
import com.example.myapplication.viewmodel.NavigationEvent

private val viewModel: HomeActivityViewModel = HomeActivityViewModel(UserRepository())

class HomeActivity : ComponentActivity() {
    private lateinit var userData: LoginResponse

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        userData = intent.getParcelableExtra("USER_DATA")!!

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DashboardScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel, userData
                    )
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
                    intent.putExtra("EMAIL_ADDRESS", userData.emailAddress)
                    startActivity(intent)
                }
                NavigationEvent.NavigateToEmployeeList -> TODO()
                NavigationEvent.NavigateToHome -> TODO()
                NavigationEvent.NavigateToViewTarget -> TODO()
            }
        }

        if (userData.trackingStatus) {
            startEmployeeTracking(this, userData)
        }
    }
}

fun startEmployeeTracking(context: Context, userData: LoginResponse) {
    val intent = Intent(context, LocationService::class.java)
    intent.putExtra("USER_DATA", userData)
    context.startService(intent)
}

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeActivityViewModel,
    userData: LoginResponse
) {
    val context = LocalContext.current

    val options = listOf("Employee List", "Assigned Applications",
        "Mark Attendance", "Change Password", "View Target Details")

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
                    userData.monthlyAchievedTarget ?: 0,
                    userData.monthlyTarget ?: 0
                ),
                yearlyTarget = stringResource(
                    id = R.string.yearly_target,
                    userData.achievedTarget ?: 0,
                    userData.yearlyTarget ?: 0
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

                                "Mark Attendance" -> {
                                    val intent = Intent(context, PresenceActivity::class.java)
                                    intent.putExtra("USER_DATA", userData)
                                    context.startActivity(intent)
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
                text = stringResource(id = R.string.monthly_target_label),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )
            Text(
                text = monthlyTarget,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.yearly_target_label),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )
            Text(
                text = yearlyTarget,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )
        }
    }
}
