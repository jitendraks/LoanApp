package com.aubank.loanapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aubank.loanapp.api.UserRepository
import com.aubank.loanapp.data.EmployeeIdRequest
import com.aubank.loanapp.data.FeedbackDataRequest
import com.aubank.loanapp.data.PendingApp
import com.aubank.loanapp.data.PendingApprovalFeedbackData
import kotlinx.coroutines.launch

class AssignedAppsViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    val fetchAssignedAppsApiState = MutableLiveData<FetchAssignedAppsApiState>()
    val fetchLastFeedbackDataApiState = MutableLiveData<FetchLastFeedbackDataApiState>()

    var pendingApps: MutableLiveData<List<PendingApp>> = MutableLiveData(emptyList())
    var filteredResults: MutableLiveData<List<PendingApp>> = MutableLiveData(emptyList())
    var searchQuery by mutableStateOf<String>("")
    fun navigateBack() {
        _navigationEvent.value = NavigationEvent.NavigateBack
    }

    fun fetchAssignedApps(employeeId: Int) {
        // Login logic
        isLoading = true
        viewModelScope.launch {
            fetchAssignedAppsApiState.value = FetchAssignedAppsApiState.Loading
            val result = userRepository.fetchAssignedApps(EmployeeIdRequest(employeeId = employeeId))
            isLoading = false
            fetchAssignedAppsApiState.value = if (result.isSuccess) {
                pendingApps.value = result.getOrThrow()
                FetchAssignedAppsApiState.Success(result.getOrThrow())
            } else {
                FetchAssignedAppsApiState.Error(result.exceptionOrNull())
            }
        }
    }

    fun filterData() {
        val result = pendingApps.value?.filter { item ->
            // Replace with your actual field names and filtering logic
            item.borrowerName.contains(searchQuery, ignoreCase = true) ||
                    item.borrowerAddress.contains(searchQuery, ignoreCase = true) ||
                    item.caseNo.contains(searchQuery, ignoreCase = true)
        }
        filteredResults.value = result ?: emptyList()
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



    sealed class FetchAssignedAppsApiState {
        data object Loading : FetchAssignedAppsApiState()
        data class Success(val pendingApp: List<PendingApp>) : FetchAssignedAppsApiState()
        data class Error(val exception: Throwable?) : FetchAssignedAppsApiState()
    }

    sealed class FetchLastFeedbackDataApiState {
        data object Loading : FetchLastFeedbackDataApiState()
        data class Success(val feedbackData: PendingApprovalFeedbackData, val pendingApp: PendingApp, val visitDoneId: Int) : FetchLastFeedbackDataApiState()
        data class Error(val exception: Throwable?, val pendingApp: PendingApp, val visitDoneId: Int) : FetchLastFeedbackDataApiState()
    }
}