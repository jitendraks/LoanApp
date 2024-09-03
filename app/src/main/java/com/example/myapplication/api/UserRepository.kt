package com.example.myapplication.api

import android.util.Log
import com.example.myapplication.data.AttendanceRequest
import com.example.myapplication.data.EmployeeIdRequest
import com.example.myapplication.data.FetchAttendanceResponse
import com.example.myapplication.data.LoginRequest
import com.example.myapplication.data.LoginResponse
import com.example.myapplication.data.PendingApp
import com.example.myapplication.data.TrackingRequest

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

    suspend fun fetchAssignedApps(employeeIdRequest: EmployeeIdRequest): Result<Array<PendingApp>> {
        return try {
            val response = RetrofitInstance.api.getPendingApplications(employeeIdRequest)
            Log.e("dddddd", "UserRepository: fetchAssignedApps: response = " + response.isSuccessful)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Fetch presence api failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}