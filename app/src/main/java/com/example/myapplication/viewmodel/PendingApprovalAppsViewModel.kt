package com.example.myapplication.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.api.UserRepository
import com.example.myapplication.data.EmployeeIdRequest
import com.example.myapplication.data.PendingApprovalFeedbackData
import kotlinx.coroutines.launch

class PendingApprovalAppsViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    val fetchPendingApprovalAppsApiState = MutableLiveData<FetchPendingApprovalAppsApiState>()
    var pendingApprpvals: MutableLiveData<List<PendingApprovalFeedbackData>> = MutableLiveData(emptyList())
    var filteredResults: MutableLiveData<List<PendingApprovalFeedbackData>> = MutableLiveData(emptyList())
    var searchQuery by mutableStateOf<String>("")
    fun navigateBack() {
        _navigationEvent.value = NavigationEvent.NavigateBack
    }

    fun fetchPendingApprovalApps(employeeId: Int) {
        // Login logic
        isLoading = true
        viewModelScope.launch {
            fetchPendingApprovalAppsApiState.value = FetchPendingApprovalAppsApiState.Loading
            val result = userRepository.fetchPendingAprovalApps(EmployeeIdRequest(employeeId = employeeId))
            isLoading = false
            fetchPendingApprovalAppsApiState.value = if (result.isSuccess) {
                pendingApprpvals.value = result.getOrThrow()
                FetchPendingApprovalAppsApiState.Success(result.getOrThrow())
            } else {
                FetchPendingApprovalAppsApiState.Error(result.exceptionOrNull())
            }
        }
    }

    fun filterData() {
        val result = pendingApprpvals.value?.filter { item ->
            // Replace with your actual field names and filtering logic
            item.employeeName.contains(searchQuery, ignoreCase = true) ||
                    item.loanNo.contains(searchQuery, ignoreCase = true)
        }
        filteredResults.value = result ?: emptyList()
    }

    sealed class FetchPendingApprovalAppsApiState {
        data object Loading : FetchPendingApprovalAppsApiState()
        data class Success(val pendingApprovals: List<PendingApprovalFeedbackData>) : FetchPendingApprovalAppsApiState()
        data class Error(val exception: Throwable?) : FetchPendingApprovalAppsApiState()
    }
}