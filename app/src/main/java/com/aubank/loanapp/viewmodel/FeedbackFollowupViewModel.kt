package com.aubank.loanapp.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aubank.loanapp.api.UserRepository
import com.aubank.loanapp.data.FeedbackData
import com.aubank.loanapp.data.LoginResponse
import com.aubank.loanapp.data.MasterData
import com.aubank.loanapp.data.PendingApp
import com.aubank.loanapp.data.PendingApprovalFeedbackData
import com.aubank.loanapp.utils.DateTimeFormatter
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class FeedbackFollowupViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    var isLoading by mutableStateOf(false)

    var userData: LoginResponse? = null
    lateinit var approvalFeedbackData: PendingApprovalFeedbackData;
    // LiveData holding a list of URIs
    private val _photos = MutableLiveData<ArrayList<Uri>>(ArrayList())
    val photos: LiveData<ArrayList<Uri>> = _photos
    private val photoPaths = arrayListOf<String>()
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


    sealed class PostFeedbackDataApiState {
        data object Loading : PostFeedbackDataApiState()
        data class Success(val success: Boolean) : PostFeedbackDataApiState()
        data class Error(val exception: Throwable?) : PostFeedbackDataApiState()
    }
}