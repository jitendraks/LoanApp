package com.example.myapplication.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.api.UserRepository

class HomeActivityViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    var isLoading by mutableStateOf(false)


    fun navigateToChangePasswordActivity() {
        _navigationEvent.value = NavigationEvent.NavigateToChangePassword
    }

    fun navigateBack() {
        _navigationEvent.value = NavigationEvent.NavigateBack
    }
}