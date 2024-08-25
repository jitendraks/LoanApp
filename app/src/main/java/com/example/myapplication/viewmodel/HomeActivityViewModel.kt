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
import com.example.myapplication.viewmodel.ChangePasswordViewModel.ApiState
import kotlinx.coroutines.launch
import okhttp3.internal.format
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeActivityViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    var isLoading by mutableStateOf(false)
    val attendanceApiState = MutableLiveData<ApiState>()
    val attendanceStatus = MutableLiveData<Boolean>(false)
    var errorMessage by mutableStateOf<String?>(null)

    private var latitude by mutableStateOf("")
    private var longitude by mutableStateOf("")
    private var address by mutableStateOf("")

    fun setLocation(location: Location, context: Context) {
        latitude = location.latitude.toString()
        longitude = location.longitude.toString()
        address = getAddressFromLatLng(context, location.latitude, location.longitude) ?: ""
    }

    fun markAttendance(employeeId: String, email: String, mode: Boolean) {
        val attendanceRequest = AttendanceRequest(
            attendanceTime = LocalDateTime.now().formatTime(),
            logDate = LocalDateTime.now().formatDate(),
            appVersion = "1.0.0.0",
            latLong = "$latitude,$longitude",
            mode = if (mode) "On" else "Off",
            location = address,
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

    private fun getAddressFromLatLng(context: Context, latitude: Double, longitude: Double): String? {
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


    fun navigateToChangePasswordActivity() {
        _navigationEvent.value = NavigationEvent.NavigateToChangePassword
    }

    fun navigateBack() {
        _navigationEvent.value = NavigationEvent.NavigateBack
    }

    fun LocalDateTime.formatDate(): String {
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
        return this.format(formatter)
    }

    fun LocalDateTime.formatTime(): String {
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm")
        return this.format(formatter)
    }
}