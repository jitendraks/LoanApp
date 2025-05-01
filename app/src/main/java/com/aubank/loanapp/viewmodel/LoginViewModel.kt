package com.aubank.loanapp.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aubank.loanapp.api.UserRepository
import com.aubank.loanapp.data.Constants
import com.aubank.loanapp.data.LoginRequest
import com.aubank.loanapp.data.LoginResponse
import com.aubank.loanapp.utils.loadString
import com.aubank.loanapp.utils.saveString
import kotlinx.coroutines.launch

class LoginViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent
    val loginState = MutableLiveData<LoginState>()

    /*var username by mutableStateOf("david.john@aubank.in")
    var password by mutableStateOf("22376")*/
    /*var username by mutableStateOf("abhishek.raghuwanshi3@aubank.in")
    var password by mutableStateOf("71811")*/
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    private val _userData = MutableLiveData<LoginResponse>()
    val userData: LiveData<LoginResponse> get() = _userData

    fun onAppear(context: Context) {
        username = context.loadString(Constants.PREF_NAME_USERNAME) ?: ""
    }

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

    fun login(context: Context) {
        // Login logic
        isLoading = true
        viewModelScope.launch {
            loginState.value = LoginState.Loading
            val result = userRepository.login(LoginRequest(username, password))
            isLoading = false
            loginState.value = if (result.isSuccess) {
                context.saveString(Constants.PREF_NAME_USERNAME, username)
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
