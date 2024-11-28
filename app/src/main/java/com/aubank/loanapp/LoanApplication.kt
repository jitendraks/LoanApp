package com.aubank.loanapp

import android.app.Application
import com.aubank.loanapp.data.MasterData
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

class LoanApplication : Application() {

    private var masterData: MasterData? = null

    override fun onCreate() {
        super.onCreate();

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize Crashlytics
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
    }

    fun setMasterData(masterData: MasterData) {
        this.masterData = masterData
    }

    fun getMasterData() : MasterData? {
        return masterData
    }
}