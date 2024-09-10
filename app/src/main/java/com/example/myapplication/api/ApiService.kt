package com.example.myapplication.api

import com.example.myapplication.data.ApprovalRequest
import com.example.myapplication.data.AttendanceRequest
import com.example.myapplication.data.EmployeeIdRequest
import com.example.myapplication.data.FetchAttendanceResponse
import com.example.myapplication.data.LoginRequest
import com.example.myapplication.data.LoginResponse
import com.example.myapplication.data.MasterData
import com.example.myapplication.data.PendingApp
import com.example.myapplication.data.PendingApprovalFeedbackData
import com.example.myapplication.data.TrackingRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @POST("User/GetUserDetail")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("User/ChangePassword")
    suspend fun changePassword(@Body loginRequest: LoginRequest): Response<Void>

    @POST("Attendance/EmployeeAttendance")
    suspend fun employeeAttendance(@Body attendanceRequest: AttendanceRequest): Response<Void>

    @POST("Attendance/CheckAttendance")
    suspend fun fetchAttendance(@Body employeeIdRequest: EmployeeIdRequest): Response<FetchAttendanceResponse>

    @POST("Attendance/EmployeeTracking")
    suspend fun trackEmployee(@Body trackingRequest: TrackingRequest): Response<Unit>

    @POST("Loan/GetPendingLoanApplication")
    suspend fun getPendingApplications(@Body employeeIdRequest: EmployeeIdRequest): Response<List<PendingApp>>

    @POST("Loan/GetApplicationForApproval")
    suspend fun getPendingApprovals(@Body employeeIdRequest: EmployeeIdRequest): Response<List<PendingApprovalFeedbackData>>

    @POST("Master/getMastersData")
    suspend fun getMasterData(): Response<MasterData>

    /*@Multipart
    @POST("Loan/Feedback")
    fun submitFeedback(
        @Part images: List<MultipartBody.Part>,    // Image part
        //@Part("FeedbackId") FeedbackId: RequestBody,
        @Part("EmployeeId") EmployeeId: RequestBody,
        @Part("VisitDate") VisitDate: RequestBody,
        @Part("LoanNo") LoanNo: RequestBody,
        @Part("VisitType") VisitType: RequestBody,
        @Part("LoanDetailId") LoanDetailId: RequestBody,
        @Part("VisitDoneId") VisitDoneId: RequestBody,
        @Part("RelationId") RelationId: RequestBody,
        @Part("TypeOfLoanId") TypeOfLoanId: RequestBody,
        @Part("VehicleStatusId") VehicleStatusId: RequestBody,
        @Part("NameWithMeet") NameWithMeet: RequestBody,
        @Part("ContactNoWithMeet") ContactNoWithMeet: RequestBody,
        @Part("BorrowerLivingCurrentAddress") BorrowerLivingCurrentAddress: RequestBody,
        @Part("NewAddressOfBorrower") NewAddressOfBorrower: RequestBody,
        @Part("LandMark") LandMark: RequestBody,
        @Part("CurrentContactNoOfBorrower") CurrentContactNoOfBorrower: RequestBody,
        @Part("BorrowerJobAddress") BorrowerJobAddress: RequestBody,
        @Part("JobId") JobId: RequestBody,
        @Part("FinConditionId") FinConditionId: RequestBody,
        @Part("IncomeId") IncomeId: RequestBody,
        @Part("LitigationId") LitigationId: RequestBody,
        @Part("NewLitigationId") NewLitigationId: RequestBody,
        @Part("WorkableNonWorkable") WorkableNonWorkable: RequestBody,
        @Part("ReasonforWorkable") ReasonforWorkable: RequestBody,
        @Part("NewLitigationReuired") NewLitigationReuired: RequestBody,
        @Part("AnySettlementProposal") AnySettlementProposal: RequestBody,
        @Part("ApprovedStatus") ApprovedStatus: RequestBody,
        //@Part("ApproverId") ApproverId: RequestBody,
        // @Part("ApproverRemark") ApproverRemark: RequestBody,
        @Part("CreatedDate") CreatedDate: RequestBody,
        @Part("Latitude") Latitude: RequestBody,
        @Part("Longitude") Longitude: RequestBody
    ): Response<Void>  // Your response type
*/

    @Multipart
    @POST("Loan/Feedback")
    fun submitFeedback(
        @Part images: List<MultipartBody.Part>,    // Image part
        @Part("EmployeeId") EmployeeId: RequestBody,
        @Part("VisitDate") VisitDate: RequestBody,
        @Part("LoanNo") LoanNo: RequestBody,
        @Part("VisitType") VisitType: RequestBody,
        @Part("LoanDetailId") LoanDetailId: RequestBody,
        @Part("VisitDoneId") VisitDoneId: RequestBody,
        @Part("RelationId") RelationId: RequestBody,
        @Part("TypeOfLoanId") TypeOfLoanId: RequestBody,
        @Part("VehicleStatusId") VehicleStatusId: RequestBody,
        @Part("NameWithMeet") NameWithMeet: RequestBody,
        @Part("ContactNoWithMeet") ContactNoWithMeet: RequestBody,
        @Part("BorrowerLivingCurrentAddress") BorrowerLivingCurrentAddress: RequestBody,
        @Part("NewAddressOfBorrower") NewAddressOfBorrower: RequestBody,
        @Part("LandMark") LandMark: RequestBody,
        @Part("CurrentContactNoOfBorrower") CurrentContactNoOfBorrower: RequestBody,
        @Part("BorrowerJobAddress") BorrowerJobAddress: RequestBody,
        @Part("JobId") JobId: RequestBody,
        @Part("FinConditionId") FinConditionId: RequestBody,
        @Part("IncomeId") IncomeId: RequestBody,
        @Part("LitigationId") LitigationId: RequestBody,
        @Part("NewLitigationId") NewLitigationId: RequestBody,
        @Part("WorkableNonWorkable") WorkableNonWorkable: RequestBody,
        @Part("ReasonforWorkable") ReasonforWorkable: RequestBody,
        @Part("NewLitigationReuired") NewLitigationReuired: RequestBody,
        @Part("AnySettlementProposal") AnySettlementProposal: RequestBody,
        @Part("Latitude") Latitude: RequestBody,
        @Part("Longitude") Longitude: RequestBody
    ): Call<ResponseBody>  // Your response type

    @POST("Loan/ApproveFeedback")
    suspend fun approveFeedback(@Body approvalRequest: ApprovalRequest): Response<Void>
}