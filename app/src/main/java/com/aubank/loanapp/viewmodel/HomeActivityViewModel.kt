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
import com.aubank.loanapp.data.MasterData
import com.aubank.loanapp.data.PendingApp
import com.aubank.loanapp.data.PendingApprovalFeedbackData
import com.aubank.loanapp.data.VisitDone
import kotlinx.coroutines.launch

class HomeActivityViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    var isLoading by mutableStateOf(false)
    val fetchMasterDataApiState = MutableLiveData<FetchMasterDataApiState>()

    fun navigateToChangePasswordActivity() {
        _navigationEvent.value = NavigationEvent.NavigateToChangePassword
    }

    fun navigateBack() {
        _navigationEvent.value = NavigationEvent.NavigateBack
    }

    fun fetchMasterData() {
        isLoading = true
        viewModelScope.launch {
            fetchMasterDataApiState.value = FetchMasterDataApiState.Loading
            val result = userRepository.fetchMasterData()
            isLoading = false
            fetchMasterDataApiState.value = if (result.isSuccess) {
                FetchMasterDataApiState.Success(result.getOrThrow())
            } else {
                FetchMasterDataApiState.Error(result.exceptionOrNull())
            }
        }
    }

    sealed class FetchMasterDataApiState {
        data object Loading : FetchMasterDataApiState()
        data class Success(val masterData: MasterData) : FetchMasterDataApiState()
        data class Error(val exception: Throwable?) : FetchMasterDataApiState()
    }
}