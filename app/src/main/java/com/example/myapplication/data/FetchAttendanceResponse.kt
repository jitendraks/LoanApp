package com.example.myapplication.data

import android.os.Parcelable

import kotlinx.parcelize.Parcelize

@Parcelize
data class FetchAttendanceResponse(
    val attendanceDate: String,
    val startTime: String,
    val startAddress: String,
    val endTime: String,
    val endAddress: String,
    val dutyTime: Double
) : Parcelable