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
import com.aubank.loanapp.data.ApprovalRequest
import com.aubank.loanapp.data.FeedbackData
import com.aubank.loanapp.data.FeedbackDataWithIdValue
import com.aubank.loanapp.data.FinancialCondition
import com.aubank.loanapp.data.IncomeSlab
import com.aubank.loanapp.data.Job
import com.aubank.loanapp.data.Litigation
import com.aubank.loanapp.data.LoanType
import com.aubank.loanapp.data.LoginResponse
import com.aubank.loanapp.data.MasterData
import com.aubank.loanapp.data.PendingApp
import com.aubank.loanapp.data.PendingApprovalFeedbackData
import com.aubank.loanapp.data.Relation
import com.aubank.loanapp.data.VehicleStatus
import com.aubank.loanapp.data.VisitDone
import com.aubank.loanapp.utils.DateTimeFormatter
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class FeedbackApprovalFormViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    var isLoading by mutableStateOf(false)

    var userData: LoginResponse? = null

    // LiveData holding a list of URIs
    private val _photos = MutableLiveData<ArrayList<Uri>>(ArrayList())
    val photos: LiveData<ArrayList<Uri>> = _photos
    private val photoPaths = arrayListOf<String>()
    val postFeedbackApprovalDataApiState = MutableLiveData<PostApprovalFeedbackDataApiState>()
    val approvalRequest: MutableLiveData<ApprovalRequest> = MutableLiveData(ApprovalRequest())

    var feedbackData: MutableLiveData<FeedbackDataWithIdValue> = MutableLiveData<FeedbackDataWithIdValue>(
        FeedbackDataWithIdValue()
    )
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

    fun setValuesFromMasterData(masterData: MasterData) {
        val feedbackData = feedbackData.value
        masterData.let {
            feedbackData?.let {
                feedbackData.visitDoneValue =
                    VisitDone.getVisitDoneById(masterData.visitDones, feedbackData.pendingApprovalFeedbackData!!.visitDoneId).toString()
                feedbackData.vehicleStatusValue =
                    VehicleStatus.getVehicleStatusById(masterData.vehicleStatuses, feedbackData.pendingApprovalFeedbackData!!.vehicleStatusId).toString()
                feedbackData.loanTypeValue =
                    LoanType.getLoanTypeById(masterData.loanTypes, feedbackData.pendingApprovalFeedbackData!!.typeOfLoanId).toString()
                feedbackData.relationValue =
                    Relation.getRelationById(masterData.relations, feedbackData.pendingApprovalFeedbackData!!.relationId).toString()
                feedbackData.financialConditionValue =
                    FinancialCondition.getFinancialConditionById(masterData.finConditions, feedbackData.pendingApprovalFeedbackData!!.finConditionId).toString()
                feedbackData.jobValue =
                    Job.getJobById(masterData.jobs, feedbackData.pendingApprovalFeedbackData!!.jobId).toString()
                feedbackData.litigationValue =
                    Litigation.getLitigationById(masterData.litigations, feedbackData.pendingApprovalFeedbackData!!.litigationId).toString()
                feedbackData.newLitigationValue =
                    Litigation.getLitigationById(masterData.litigations, feedbackData.pendingApprovalFeedbackData!!.newLitigationId).toString()
                feedbackData.incomeValue =
                    IncomeSlab.getIncomeSlabById(masterData.incomeSlabs, feedbackData.pendingApprovalFeedbackData!!.incomeId).toString()


            }
            this.feedbackData.value = feedbackData
        }
    }

    fun approveFeedback(approvalStatus: Boolean) {
        approvalRequest.value?.let {
            isLoading = true
            it.approverId = userData!!.employeeId.toInt()
            it.feedbackId = feedbackData.value!!.pendingApprovalFeedbackData!!.feedBackId
            it.approvedStatus = if (approvalStatus) 1 else 0
            viewModelScope.launch {
                postFeedbackApprovalDataApiState.value = PostApprovalFeedbackDataApiState.Loading
                val result = userRepository.approveFeedback(approvalRequest = approvalRequest.value!!)
                isLoading = false
                postFeedbackApprovalDataApiState.value = if (result.isSuccess) {
                    PostApprovalFeedbackDataApiState.Success(result.getOrThrow())
                } else {
                    PostApprovalFeedbackDataApiState.Error(result.exceptionOrNull())
                }
            }
        }
    }

    sealed class PostApprovalFeedbackDataApiState {
        data object Loading : PostApprovalFeedbackDataApiState()
        data class Success(val success: Boolean) : PostApprovalFeedbackDataApiState()
        data class Error(val exception: Throwable?) : PostApprovalFeedbackDataApiState()
    }
}