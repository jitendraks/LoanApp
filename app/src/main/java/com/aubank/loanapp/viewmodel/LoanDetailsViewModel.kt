package com.aubank.loanapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aubank.loanapp.api.UserRepository
import com.aubank.loanapp.data.FeedbackDataRequest
import com.aubank.loanapp.data.LoginResponse
import com.aubank.loanapp.data.PendingApp
import com.aubank.loanapp.viewmodel.AssignedAppsViewModel.FetchLastFeedbackDataApiState
import kotlinx.coroutines.launch

class LoanDetailsViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    val fetchLastFeedbackDataApiState = MutableLiveData<FetchLastFeedbackDataApiState>()

    var isLoading by mutableStateOf(false)

    var userData: LoginResponse? = null
    var loanAppDetails: PendingApp? = null

    fun navigateBack() {
        _navigationEvent.value = NavigationEvent.NavigateBack
    }

    fun fetchLastFeedbackData(pendingApp: PendingApp, visitDoneId: Int ) {
        isLoading = true
        viewModelScope.launch {
            fetchLastFeedbackDataApiState.value = FetchLastFeedbackDataApiState.Loading
            val result = userRepository.fetchLastFeedbackData(FeedbackDataRequest(pendingApp.loanDetailId, visitDoneId))
            isLoading = false
            fetchLastFeedbackDataApiState.value = if (result.isSuccess) {
                FetchLastFeedbackDataApiState.Success(result.getOrThrow(), pendingApp, visitDoneId)
            } else {
                FetchLastFeedbackDataApiState.Error(result.exceptionOrNull(), pendingApp, visitDoneId)
            }
        }
    }

}