package com.aubank.loanapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aubank.loanapp.api.UserRepository
import com.aubank.loanapp.data.LoginResponse
import com.aubank.loanapp.data.PendingApp

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