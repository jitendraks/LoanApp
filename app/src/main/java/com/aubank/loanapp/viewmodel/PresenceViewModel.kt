package com.aubank.loanapp.viewmodel

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aubank.loanapp.BuildConfig
import com.aubank.loanapp.api.UserRepository
import com.aubank.loanapp.data.AttendanceRequest
import com.aubank.loanapp.data.EmployeeIdRequest
import com.aubank.loanapp.data.FetchAttendanceResponse
import com.aubank.loanapp.utils.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime
import java.util.Locale

class PresenceViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    var inTime by mutableStateOf(LocalDateTime.MIN)
    var outTime by mutableStateOf(LocalDateTime.MIN)
    var inAddress = MutableLiveData("")
    var outAddress = MutableLiveData("")
    var presenceResponse: MutableLiveData<FetchAttendanceResponse> = MutableLiveData<FetchAttendanceResponse>(null)
    private var inLocation by mutableStateOf(Pair("", ""))
    private var outLocation by mutableStateOf(Pair("", ""))

    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf<String?>(null)

    val attendanceApiState = MutableLiveData<ApiState?>()
    val fetchAttendanceApiState = MutableLiveData<FetchAttendanceState?>()

    private val _address = MutableLiveData<String?>()
    val address: LiveData<String?> = _address

    fun navigateBack() {
        _navigationEvent.value = NavigationEvent.NavigateBack
    }

    fun setInLocation(location: Location, address: String) {
        inLocation = Pair(location.latitude.toString(), location.longitude.toString())
        inAddress.value = address
        inTime = LocalDateTime.now()
    }

    fun setOutLocation(location: Location, address: String) {
        outLocation = Pair(location.latitude.toString(), location.longitude.toString())
        outAddress.value = address
        outTime = LocalDateTime.now()
    }


    fun getFormattedTime(dateTime: LocalDateTime) : String {
        return if (dateTime != LocalDateTime.MIN) DateTimeFormatter.formatTime(dateTime) else ""
    }

    fun fetchAttendance(employeeId: String) {
        val employeeIdRequest = EmployeeIdRequest(
            employeeId = Integer.valueOf(employeeId)
        )
        isLoading = true
        viewModelScope.launch {
            fetchAttendanceApiState.value = FetchAttendanceState.Loading
            val result = userRepository.fetchAttendance(employeeIdRequest)
            isLoading = false
            fetchAttendanceApiState.value = if (result.isSuccess) {
                FetchAttendanceState.Success(result.getOrThrow())
            } else
                FetchAttendanceState.Error(result.exceptionOrNull())
        }
    }

    fun markAttendance(employeeId: String, email: String, mode: Boolean) {
        val appVersion = BuildConfig.VERSION_NAME
        val attendanceRequest = AttendanceRequest(
            attendanceTime = DateTimeFormatter.formatDateTime(LocalDateTime.now()),
            logDate = DateTimeFormatter.formatDate(LocalDateTime.now()),
            appVersion = appVersion,
            latLong = if (mode) "${inLocation.first},${inLocation.second}" else "${outLocation.first},${outLocation.second}",
            mode = if (mode) "start" else "end",
            location = (if (mode) inAddress.value else outAddress.value) ?: "",
            emailAddress = email,
            attendanceFrom = "Mobile",
            employeeId = Integer.valueOf(employeeId)
        )
        isLoading = true
        viewModelScope.launch {
            attendanceApiState.value = ApiState.Loading
            val result = userRepository.markAttendance(attendanceRequest)
            isLoading = false
            attendanceApiState.value = if (result.isSuccess) {
                ApiState.Success(result.getOrThrow())
            } else
                ApiState.Error(result.exceptionOrNull())
        }
    }

    sealed class FetchAttendanceState {
        data object Loading : FetchAttendanceState()
        data class Success(val attendanceResponse: FetchAttendanceResponse) : FetchAttendanceState()
        data class Error(val exception: Throwable?) : FetchAttendanceState()
    }

    fun resetApiResponseState() {
        fetchAttendanceApiState.value = null
        attendanceApiState.value = null
    }

}