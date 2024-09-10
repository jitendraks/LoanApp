package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.api.UserRepository
import com.example.myapplication.components.ApiProgressBar
import com.example.myapplication.data.Constants
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.viewmodel.LoginViewModel
import com.example.myapplication.viewmodel.NavigationEvent


class LoginActivity : ComponentActivity() {
    private val viewModel = LoginViewModel(UserRepository())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LoginScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel
                    )
                }
            }
        }

        viewModel.navigationEvent.observe(this)
        { event ->
            when (event) {
                NavigationEvent.NavigateToHome -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.putExtra(Constants.USER_DATA, viewModel.userData.value)
                    startActivity(intent)
                    finish()
                }

                else -> {}
            }
        }

        viewModel.loginState.observe(this) {
            event ->
                when (event) {
                    is LoginViewModel.LoginState.Success -> {
                        viewModel.setUserData(event.loginResponse)
                        viewModel.isLoading = false
                        viewModel.navigateToHomeActivity()
                    }
                    is LoginViewModel.LoginState.Error -> {
                        viewModel.isLoading = false
                        viewModel.errorMessage = event.exception.toString()
                    }
                    LoginViewModel.LoginState.Loading -> { // Update isLoading state here
                        viewModel.isLoading = true
                    }
                }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GreetingPreview() {
    MyApplicationTheme {
        LoginScreen(viewModel = LoginViewModel(UserRepository()))
    }
}

@Composable
private fun LoginScreen(modifier: Modifier = Modifier, viewModel: LoginViewModel) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.logo), // Replace with your logo
                    contentDescription = "Logo",
                    modifier = Modifier.wrapContentSize()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = viewModel.username,
                    onValueChange = { viewModel.onUsernameChanged(it) },
                    label = { Text("Email") },
                    singleLine = true,
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
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = viewModel.password,
                    onValueChange = { viewModel.onPasswordChanged(it) },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.login() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign In")
                }
                // Add other elements like forgot password, create account, etc.
            }
        }
        if (viewModel.isLoading) {
            ApiProgressBar(modifier = Modifier.align(Alignment.Center))
        }
    }
}