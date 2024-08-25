package com.example.myapplication.data

data class LoginResponse(
    val userId: String,
    val employeeId: String,
    val name: String,
    val emailAddress: String,
    val roleName: String,
    val trackingTime: String,
    val monthlyTarget: Int,
    val yearlyTarget: Int
)
