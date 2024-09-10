package com.example.myapplication.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import com.example.myapplication.api.UserRepository
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.LoginRequest
import com.example.myapplication.data.LoginResponse
import kotlinx.coroutines.launch

class LoginViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent
    val loginState = MutableLiveData<LoginState>()

    /*var username by mutableStateOf("david.john@aubank.in")
    var password by mutableStateOf("22376")*/
    var username by mutableStateOf("abhishek.raghuwanshi3@aubank.in")
    var password by mutableStateOf("71811")
    /*var username by mutableStateOf("")
    var password by mutableStateOf("")*/
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    private val _userData = MutableLiveData<LoginResponse>()
    val userData: LiveData<LoginResponse> get() = _userData

    fun setUserData(user: LoginResponse) {
        _userData.value = user
    }

    fun onUsernameChanged(username: String) {
        this.username = username
        errorMessage = null
    }

    fun onPasswordChanged(password: String) {
        this.password = password
        errorMessage = null
    }

    fun navigateToHomeActivity() {
        _navigationEvent.value = NavigationEvent.NavigateToHome
    }

    fun login() {
        // Login logic
        isLoading = true
        viewModelScope.launch {
            loginState.value = LoginState.Loading
            val result = userRepository.login(LoginRequest(username, password))
            isLoading = false
            loginState.value = if (result.isSuccess) {
                LoginState.Success(result.getOrThrow())
            } else
                LoginState.Error(result.exceptionOrNull())
            }
    }

    sealed class LoginState {
        object Loading : LoginState()
        data class Success(val loginResponse: LoginResponse) : LoginState()
        data class Error(val exception: Throwable?) : LoginState()
    }
}
