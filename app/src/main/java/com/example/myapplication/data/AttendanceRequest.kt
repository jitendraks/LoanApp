package com.example.myapplication.data

data class AttendanceRequest (
    val attendanceTime: String,
    val logDate: String,
    val appVersion: String,
    val latLong: String,
    val mode: String,
    val location: String,
    val emailAddress: String,
    val attendanceFrom: String,
    val employeeId: Int = 0
)
