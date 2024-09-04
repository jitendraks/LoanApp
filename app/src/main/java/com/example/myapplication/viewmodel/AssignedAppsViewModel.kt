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
import com.example.myapplication.data.PendingApp
import kotlinx.coroutines.launch

class AssignedAppsViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    val fetchAssignedAppsApiState = MutableLiveData<FetchAssignedAppsApiState>()
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

    sealed class FetchAssignedAppsApiState {
        data object Loading : FetchAssignedAppsApiState()
        data class Success(val pendingApp: List<PendingApp>) : FetchAssignedAppsApiState()
        data class Error(val exception: Throwable?) : FetchAssignedAppsApiState()
    }
}