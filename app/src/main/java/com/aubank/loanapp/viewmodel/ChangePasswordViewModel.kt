package com.aubank.loanapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aubank.loanapp.api.UserRepository
import com.aubank.loanapp.data.LoginRequest
import kotlinx.coroutines.launch

class ChangePasswordViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    val apiState = MutableLiveData<ApiState>()

    var newPassword by mutableStateOf("")
    var emailAddress by mutableStateOf("")

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun onNewPasswordChanged(newPassword: String) {
        this.newPassword = newPassword
    }

    fun navigateToHomeActivity() {
        _navigationEvent.value = NavigationEvent.NavigateToHome
    }

    fun navigateBack() {
        _navigationEvent.value = NavigationEvent.NavigateBack
    }

    fun updatePassword() {
        // change password logic
        isLoading = true
        viewModelScope.launch {
            apiState.value = ApiState.Loading
            val result = userRepository.updatePassword(LoginRequest(emailAddress, newPassword))
            isLoading = false
            apiState.value = if (result.isSuccess) {
                ApiState.Success(result.getOrThrow())
            } else
                ApiState.Error(result.exceptionOrNull())
        }
    }

    private fun cancelUpdatePassword() {
        navigateBack()
    }
}