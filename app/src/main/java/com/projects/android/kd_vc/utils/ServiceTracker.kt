package com.projects.android.kd_vc.utils

import android.content.Context
import android.content.SharedPreferences

enum class ServiceState {
    STARTED,
    STOPPED,
}

private const val name = "SMS_SERVICE_KEY"
private const val key = "SMS_SERVICE_STATE"
private const val devicePhoneNumber = "PHONE_NUMBER"

fun setServiceState(context: Context, state: ServiceState) {
    val sharedPrefs = getPreferences(context)
    sharedPrefs.edit().let {
        it.putString(key, state.name)
        it.apply()
    }
}

fun setPhoneNumber(context: Context, phoneNumber: String) {
    val sharedPrefs = getPreferences(context)
    sharedPrefs.edit().let {
        it.putString(devicePhoneNumber, phoneNumber)
        it.apply()
    }
}

fun getServiceState(context: Context): ServiceState? {
    val sharedPrefs = getPreferences(context)
    val value = sharedPrefs.getString(key, ServiceState.STOPPED.name)
    return value?.let { ServiceState.valueOf(it) }
}

fun getPhoneNumber(context: Context): String? {
    val sharedPrefs = getPreferences(context)
    return sharedPrefs.getString(devicePhoneNumber, "")
}

private fun getPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(name, 0)
}