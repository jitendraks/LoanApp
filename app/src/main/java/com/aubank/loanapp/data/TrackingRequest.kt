package com.aubank.loanapp.data

data class TrackingRequest(
    val latLongTime: String,
    val logDate: String,
    val latitude: String,
    val longitude: String,
    val employeeId: Int,
    val address: String)