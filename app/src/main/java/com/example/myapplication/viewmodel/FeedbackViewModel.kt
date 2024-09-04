package com.example.myapplication.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.api.UserRepository

class FeedbackViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    fun navigateBack() {
        _navigationEvent.value = NavigationEvent.NavigateBack
    }

    fun capturePicture() {
        _navigationEvent.value = NavigationEvent.NavigateToCaptureActivity
    }
}