package com.example.myapplication.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.api.UserRepository
import com.example.myapplication.data.FeedbackData
import com.example.myapplication.data.LoginResponse
import com.example.myapplication.data.MasterData
import com.example.myapplication.data.PendingApp
import com.example.myapplication.utils.DateTimeFormatter
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class FeedbackViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    var isLoading by mutableStateOf(false)

    var userData: LoginResponse? = null

    // LiveData holding a list of URIs
    private val _photos = MutableLiveData<ArrayList<Uri>>(ArrayList())
    val photos: LiveData<ArrayList<Uri>> = _photos
    private val photoPaths = arrayListOf<String>()
    val fetchMasterDataApiState = MutableLiveData<FetchMasterDataApiState>()
    var masterData: MutableLiveData<MasterData> = MutableLiveData()
    val postFeedbackDataApiState = MutableLiveData<PostFeedbackDataApiState>()

    val feedbackData: MutableLiveData<FeedbackData> = MutableLiveData<FeedbackData>(FeedbackData())
    // Function to add a new photo URI
    fun addPhoto(uri: Uri) {
        val currentList = _photos.value ?: ArrayList()
        val updatedList = ArrayList(currentList)  // Create a new list instance
        updatedList.add(uri)
        _photos.value = updatedList  // Update with the new list
    }

    fun addPhotoPath(currentPhotoPath: String) {
        photoPaths.add(currentPhotoPath)
    }


    fun navigateBack() {
        _navigationEvent.value = NavigationEvent.NavigateBack
    }

    fun capturePicture() {
        _navigationEvent.value = NavigationEvent.NavigateToCaptureActivity
    }

    fun fetchMasterData() {
        isLoading = true
        viewModelScope.launch {
            fetchMasterDataApiState.value = FetchMasterDataApiState.Loading
            val result = userRepository.fetchMasterData()
            isLoading = false
            fetchMasterDataApiState.value = if (result.isSuccess) {
                masterData.value = result.getOrThrow()
                FetchMasterDataApiState.Success(result.getOrThrow())
            } else {
                FetchMasterDataApiState.Error(result.exceptionOrNull())
            }
        }
    }

    fun submitFeedback(app: PendingApp) {
        setValuesFromTheLoanApp(app)
        setValuesFromTheEmployeeData()
        isLoading = true
        viewModelScope.launch {
            postFeedbackDataApiState.value = PostFeedbackDataApiState.Loading
            val result = userRepository.submitFeedbackData(feedbackData.value!!)
            isLoading = false
            postFeedbackDataApiState.value = if (result.isSuccess) {
                PostFeedbackDataApiState.Success(result.getOrThrow())
            } else {
                PostFeedbackDataApiState.Error(result.exceptionOrNull())
            }
        }
    }

    private fun setValuesFromTheEmployeeData() {
        feedbackData.value!!.EmployeeId = userData!!.employeeId.toInt()
    }

    private fun setValuesFromTheLoanApp(app: PendingApp) {
        feedbackData.value!!.FeedbackId = -1
        feedbackData.value!!.LoanDetailId = app.loanDetailId
        feedbackData.value!!.LoanNo = app.cbsLoanNo.toString()
        feedbackData.value!!.VisitDate = DateTimeFormatter.formatDate(LocalDateTime.now())
        feedbackData.value!!.ApprovedStatus = false
        feedbackData.value!!.ApproverId = -1
        feedbackData.value!!.ApproverRemark = ""
        feedbackData.value!!.CreatedDate = DateTimeFormatter.formatDate(LocalDateTime.now())
        feedbackData.value!!.images = photoPaths
    }

    sealed class FetchMasterDataApiState {
        data object Loading : FetchMasterDataApiState()
        data class Success(val masterData: MasterData) : FetchMasterDataApiState()
        data class Error(val exception: Throwable?) : FetchMasterDataApiState()
    }

    sealed class PostFeedbackDataApiState {
        data object Loading : PostFeedbackDataApiState()
        data class Success(val success: Boolean) : PostFeedbackDataApiState()
        data class Error(val exception: Throwable?) : PostFeedbackDataApiState()
    }
}