package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.api.UserRepository
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.viewmodel.ChangePasswordViewModel
import com.example.myapplication.viewmodel.LoginViewModel
import com.example.myapplication.viewmodel.NavigationEvent

class ChangePasswordActivity : ComponentActivity() {
    private val viewModel: ChangePasswordViewModel = ChangePasswordViewModel(UserRepository())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ChangePasswordScreen(viewModel = viewModel)
            }
        }
        viewModel.emailAddress = intent.getStringExtra("EMAIL_ADDRESS").toString()
        viewModel.navigationEvent.observe(this)
        { event ->
            when (event) {
                NavigationEvent.NavigateBack -> {
                    finish()
                }

                else -> {}
            }
        }

        viewModel.apiState.observe(this) {
                event ->
            Log.e("dddddd", "ChangePasswordActivity: onCreate: apiState: observe: event = " + event)

            when (event) {
                is ChangePasswordViewModel.ApiState.Success -> {
                    viewModel.isLoading = false
                    viewModel.navigateBack()
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
    }
}

@Preview(showBackground = true)
@Composable
fun Greeting(modifier: Modifier = Modifier) {
    MyApplicationTheme {
        ChangePasswordScreen(viewModel = ChangePasswordViewModel(UserRepository()))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(viewModel: ChangePasswordViewModel,
                         modifier: Modifier = Modifier) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Change Password") },
            colors = topAppBarColors(
                containerColor = Color.Blue, // Set your desired background color here
                titleContentColor = Color.White,
            ),
            navigationIcon = {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )
    }, content = {
            innerPadding ->
        ChangePasswordForm(
            modifier = Modifier.padding(innerPadding),
            viewModel
        )
    })
}

@Composable
fun ChangePasswordForm(modifier: Modifier = Modifier, viewModel: ChangePasswordViewModel) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = viewModel.newPassword,
                onValueChange = { viewModel.onNewPasswordChanged(it) },
                label = { Text("New Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = viewModel.errorMessage != null,
                supportingText = {
                    if (viewModel.errorMessage != null) {
                        Text(
                            text = viewModel.errorMessage!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.updatePassword() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update")
            }
        }
        if (viewModel.isLoading) {
            ApiProgressBar(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun ApiProgressBar(modifier: Modifier = Modifier) {
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