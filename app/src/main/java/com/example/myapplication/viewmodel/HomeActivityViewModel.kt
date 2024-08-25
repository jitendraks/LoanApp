package com.example.myapplication.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class HomeActivityViewModel {
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    fun navigateToChangePasswordActivity() {
        _navigationEvent.value = NavigationEvent.NavigateToChangePassword
    }

    fun navigateBack() {
        _navigationEvent.value = NavigationEvent.NavigateBack
    }
}