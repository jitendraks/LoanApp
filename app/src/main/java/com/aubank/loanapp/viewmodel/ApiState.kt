package com.aubank.loanapp.viewmodel

sealed class ApiState {
    object Loading : ApiState()
    data class Success(val isSuccess: Boolean) : ApiState()
    data class Error(val exception: Throwable?) : ApiState()
}