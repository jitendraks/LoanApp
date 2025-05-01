package com.aubank.loanapp.viewmodel

sealed class NavigationEvent {
    object NavigateToHome : NavigationEvent()
    object NavigateBack : NavigationEvent()
    object NavigateToChangePassword : NavigationEvent()
    object NavigateToEmployeeList : NavigationEvent()
    object NavigateToAssignedApps : NavigationEvent()
    object NavigateToViewTarget : NavigationEvent()
    object NavigateToCaptureActivity : NavigationEvent()
}