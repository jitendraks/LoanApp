package com.aubank.loanapp.api

import android.util.Log
import com.aubank.loanapp.data.ApprovalRequest
import com.aubank.loanapp.data.AttendanceRequest
import com.aubank.loanapp.data.EmployeeIdRequest
import com.aubank.loanapp.data.FeedbackData
import com.aubank.loanapp.data.FeedbackDataRequest
import com.aubank.loanapp.data.FetchAttendanceResponse
import com.aubank.loanapp.data.LoginRequest
import com.aubank.loanapp.data.LoginResponse
import com.aubank.loanapp.data.MasterData
import com.aubank.loanapp.data.PendingApp
import com.aubank.loanapp.data.PendingApprovalFeedbackData
import com.aubank.loanapp.data.TrackingRequest
import kotlinx.coroutines.CompletableDeferred
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class UserRepository {
    suspend fun login(loginRequest: LoginRequest): Result<LoginResponse> {
        return try {
            val response = RetrofitInstance.api.login(loginRequest)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePassword(loginRequest: LoginRequest): Result<Boolean> {
        return try {
            val response = RetrofitInstance.api.changePassword(loginRequest)
            Log.e("dddddd", "UserRepository: updatePassword: response = " + response.isSuccessful)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAttendance(attendanceRequest: AttendanceRequest): Result<Boolean> {
        return try {
            val response = RetrofitInstance.api.employeeAttendance(attendanceRequest)
            Log.e("dddddd", "UserRepository: updatePassword: response = " + response.isSuccessful)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAttendance(attendanceRequest: EmployeeIdRequest): Result<FetchAttendanceResponse> {
        return try {
            val response = RetrofitInstance.api.fetchAttendance(attendanceRequest)
            Log.e("dddddd", "UserRepository: fetchAttendance: response = " + response.isSuccessful)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Fetch presence api failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun trackEmployee(trackingRequest: TrackingRequest): Result<Boolean> {
        return try {
            val response = RetrofitInstance.api.trackEmployee(trackingRequest)
            Log.e("dddddd", "UserRepository: updatePassword: response = " + response.isSuccessful)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAssignedApps(employeeIdRequest: EmployeeIdRequest): Result<List<PendingApp>> {
        return try {
            val response = RetrofitInstance.api.getPendingApplications(employeeIdRequest)
            Log.e(
                "dddddd",
                "UserRepository: fetchAssignedApps: response = " + response.isSuccessful
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Fetch presence api failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchLastFeedbackData(feedbackDataRequest: FeedbackDataRequest): Result<PendingApprovalFeedbackData> {
        return try {
            val response = RetrofitInstance.api.getLastFeedback(feedbackDataRequest)
            Log.e(
                "dddddd",
                "UserRepository: fetchAssignedApps: response = " + response.isSuccessful
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Fetch presence api failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchPendingAprovalApps(employeeIdRequest: EmployeeIdRequest): Result<List<PendingApprovalFeedbackData>> {
        return try {
            val response = RetrofitInstance.api.getPendingApprovals(employeeIdRequest)
            Log.e(
                "dddddd",
                "UserRepository: fetchAssignedApps: response = " + response.isSuccessful
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Fetch presence api failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchMasterData(): Result<MasterData> {
        return try {
            val response = RetrofitInstance.api.getMasterData()
            Log.e("dddddd", "UserRepository: fetchMasterData: response = " + response.isSuccessful)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Fetch master data api failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approveFeedback(approvalRequest: ApprovalRequest): Result<Boolean> {
        return try {
            val response = RetrofitInstance.api.approveFeedback(approvalRequest)
            Log.e("dddddd", "UserRepository: approveFeedback: response = " + response.isSuccessful)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitFeedbackData(feedbackData: FeedbackData): Result<Boolean> {
        return try {
            val imageParts = feedbackData.images.map {
                val file = File(it)
                val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("images", it, requestBody)
            }

            val call = RetrofitInstance.api.submitFeedback(
                images = imageParts,
                EmployeeId = createIntBodyPart(feedbackData.EmployeeId),
                VisitDate = createStringBodyPart(feedbackData.VisitDate),
                LoanNo = createStringBodyPart(feedbackData.LoanNo),
                VisitType = createStringBodyPart(feedbackData.VisitType),
                LoanDetailId = createIntBodyPart(feedbackData.LoanDetailId),
                VisitDoneId = createIntBodyPart(feedbackData.VisitDoneId),
                RelationId = createIntBodyPart(feedbackData.RelationId),
                TypeOfLoanId = createIntBodyPart(feedbackData.TypeOfLoanId),
                VehicleStatusId = createIntBodyPart(feedbackData.VehicleStatusId),
                NameWithMeet = createStringBodyPart(feedbackData.NameWithMeet),
                ContactNoWithMeet = createStringBodyPart(feedbackData.ContactNoWithMeet),
                BorrowerLivingCurrentAddress = createBoolBodyPart(feedbackData.BorrowerLivingCurrentAddress),
                NewAddressOfBorrower = createStringBodyPart(feedbackData.NewAddressOfBorrower),
                LandMark = createStringBodyPart(feedbackData.LandMark),
                CurrentContactNoOfBorrower = createStringBodyPart(feedbackData.CurrentContactNoOfBorrower),
                BorrowerJobAddress = createStringBodyPart(feedbackData.BorrowerJobAddress),
                JobId = createIntBodyPart(feedbackData.JobId),
                FinConditionId = createIntBodyPart(feedbackData.FinConditionId),
                IncomeId = createIntBodyPart(feedbackData.IncomeId),
                LitigationId = createIntBodyPart(feedbackData.LitigationId),
                NewLitigationId = createIntBodyPart(feedbackData.NewLitigationId),
                WorkableNonWorkable = createBoolBodyPart(feedbackData.WorkableNonWorkable),
                ReasonforWorkable = createStringBodyPart(feedbackData.ReasonforWorkable),
                NewLitigationReuired = createStringBodyPart(feedbackData.NewLitigationReuired),
                AnySettlementProposal = createStringBodyPart(feedbackData.AnySettlementProposal),
                Latitude = createStringBodyPart(feedbackData.Latitude),
                Longitude = createStringBodyPart(feedbackData.Longitude),
            )

            val deferredResult = CompletableDeferred<Result<Boolean>>()

            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.isSuccessful) {
                        // Handle successful response
                        deferredResult.complete(Result.success(true))
                    } else {
                        val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                        deferredResult.completeExceptionally(Exception("Feedback submission failed: $errorMessage"))
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    // Handle network or other errors
                    deferredResult.completeExceptionally(t)
                }
            })

            return deferredResult.await()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createStringBodyPart(part: String): RequestBody {
        return part.toRequestBody("text/plain".toMediaTypeOrNull())
    }

    fun createBoolBodyPart(part: Boolean): RequestBody {
        return createStringBodyPart(part.toString())
    }

    fun createIntBodyPart(part: Int): RequestBody {
        return createStringBodyPart(part.toString())
    }
}