package com.example.myapplication.viewmodel

import android.content.Context
import android.location.Address
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
import com.example.myapplication.api.UserRepository
import com.example.myapplication.data.AttendanceRequest
import com.example.myapplication.data.EmployeeIdRequest
import com.example.myapplication.data.FetchAttendanceResponse
import com.example.myapplication.utils.DateTimeFormatter
import kotlinx.coroutines.launch
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
    private var inLocation by mutableStateOf(Pair<String, String>("", ""))
    private var outLocation by mutableStateOf(Pair<String, String>("", ""))

    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf<String?>(null)

    val attendanceApiState = MutableLiveData<ApiState>()
    val fetchAttendanceApiState = MutableLiveData<FetchAttendanceState>()

    fun navigateBack() {
        _navigationEvent.value = NavigationEvent.NavigateBack
    }

    fun setInLocation(location: Location, address: String, context: Context) {
        inLocation = Pair(location.latitude.toString(), location.longitude.toString())
        inAddress.value = address
        inTime = LocalDateTime.now()
    }

    fun setOutLocation(location: Location, address: String, context: Context) {
        outLocation = Pair(location.latitude.toString(), location.longitude.toString())
        outAddress.value = address
        outTime = LocalDateTime.now()
    }

    public fun getAddressFromLatLng(context: Context, latitude: Double, longitude: Double): String? {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            val addressString = address.getAddressLine(0)
            Log.d("Address", addressString)
            return addressString
        } else {
            Log.e("Address", "Unable to get address from latitude and longitude")
            return null
        }
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
        val attendanceRequest = AttendanceRequest(
            attendanceTime = DateTimeFormatter.formatDateTime(LocalDateTime.now()),
            logDate = DateTimeFormatter.formatDate(LocalDateTime.now()),
            appVersion = "1.0.0.0",
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
}