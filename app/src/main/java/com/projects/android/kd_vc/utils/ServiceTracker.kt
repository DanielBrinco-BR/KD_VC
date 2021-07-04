package com.projects.android.kd_vc.utils

import android.content.Context
import android.content.SharedPreferences

enum class ServiceState {
    STARTED,
    STOPPED,
}

enum class SmsState {
    ACTIVATED,
    DEACTIVATED,
}

private const val name = "SMS_SERVICE_KEY"
private const val key = "SMS_SERVICE_STATE"
private const val devicePhoneNumber = "PHONE_NUMBER"
private const val smsState = "SMS_STATE"

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

fun setSmsState(context: Context, state: SmsState) {
    val sharedPrefs = getPreferences(context)
    sharedPrefs.edit().let {
        it.putString(smsState, state.name)
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

fun getSmsState(context: Context): SmsState? {
    val sharedPrefs = getPreferences(context)
    val value = sharedPrefs.getString(smsState, SmsState.DEACTIVATED.name)
    return value?.let { SmsState.valueOf(it) }
}

private fun getPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(name, 0)
}