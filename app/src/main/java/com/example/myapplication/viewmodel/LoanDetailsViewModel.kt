package com.example.myapplication.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.api.UserRepository
import com.example.myapplication.data.LoginResponse
import com.example.myapplication.data.PendingApp

class LoanDetailsViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    var isLoading by mutableStateOf(false)

    var userData: LoginResponse? = null
    var loanAppDetails: PendingApp? = null

    fun navigateBack() {
        _navigationEvent.value = NavigationEvent.NavigateBack
    }
}