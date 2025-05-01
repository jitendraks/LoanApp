package com.aubank.loanapp.utils

import android.content.Context
import com.aubank.loanapp.data.Constants

fun Context.saveString(key: String, value: String) {
    getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE).edit().putString(key, value).apply()
}

fun Context.loadString(key: String): String? {
    return getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE).getString(key, null)
}
