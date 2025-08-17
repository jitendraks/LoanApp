package com.aubank.loanapp.data

data class PresenceUiState(
    val isLoading: Boolean = false,
    val startAddress: String = "",
    val startTime: String = "",
    val endAddress: String = "",
    val endTime: String = "",
    val errorMessage: String? = null
)
