package com.example.myapplication.api

import com.example.myapplication.data.AttendanceRequest
import com.example.myapplication.data.EmployeeIdRequest
import com.example.myapplication.data.FetchAttendanceResponse
import com.example.myapplication.data.LoginRequest
import com.example.myapplication.data.LoginResponse
import com.example.myapplication.data.PendingApp
import com.example.myapplication.data.TrackingRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

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
}