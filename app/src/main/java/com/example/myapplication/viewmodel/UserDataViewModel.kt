package com.example.myapplication.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.data.LoginResponse

class UserDataViewModel : ViewModel() {
    var userData = MutableLiveData<LoginResponse>()
}