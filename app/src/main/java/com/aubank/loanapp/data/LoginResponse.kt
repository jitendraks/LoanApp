package com.aubank.loanapp.data

import android.os.Parcel
import android.os.Parcelable

import kotlinx.parcelize.Parcelize

@Parcelize
data class LoginResponse(
    val userId: String,
    val employeeId: String,
    val name: String,
    val emailAddress: String,
    val roleName: String,
    val trackingTime: String,
    val monthlyTarget: Int,
    val yearlyTarget: Int,
    val hodStatus: Boolean,
    val monthlyAchievedTarget: Int,
    val achievedTarget: Int,
    val trackingStatus: Boolean
) : Parcelable