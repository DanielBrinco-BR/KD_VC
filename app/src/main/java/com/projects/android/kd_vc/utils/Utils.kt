package com.projects.android.kd_vc.utils

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.SupplicantState
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.telephony.TelephonyManager
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

fun appendLog(text: String?, context: Context) {
    val logFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "kd_vc_log.html")
    if (!logFile.exists()) {
        try {
            logFile.createNewFile()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }
    try {
        //BufferedWriter for performance, true to set append to file flag
        val buf = BufferedWriter(FileWriter(logFile, true))
        buf.append(text)
        buf.newLine()
        buf.close()
    } catch (e: IOException) {
        // TODO Auto-generated catch block
        e.printStackTrace()
    }
}

fun getBatteryLevel(context: Context): String {
    val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
        context.registerReceiver(null, ifilter)
    }

    val batteryPct: Float? = batteryStatus?.let { intent ->
        val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        level * 100 / scale.toFloat()
    }

    return "$batteryPct%"
}

fun getWiFiSSID(context: Context): String {
    var ssid = ""
    val wifiManager = context.getApplicationContext().getSystemService(Service.WIFI_SERVICE) as WifiManager
    val wifiInfo: WifiInfo

    wifiInfo = wifiManager.connectionInfo
    if (wifiInfo.supplicantState == SupplicantState.COMPLETED) {
        ssid = wifiInfo.ssid.toString()
    }
    return ssid.substring(1, ssid.length - 1);
}

fun isNetworkAvailable(context: Context?): Boolean {
    if (context == null) return false
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    return true
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    return true
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    return true
                }
            }
        }
    } else {
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
            return true
        }
    }
    return false
}

fun isLocationEnabled(context: Context): Boolean {
    val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    // return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
    // LocationManager.NETWORK_PROVIDER)
}

/** Usage: `networkTypeClass(telephonyManager.networkType)` */
fun networkTypeClass(networkType: Int): String {
    when (networkType) {
        TelephonyManager.NETWORK_TYPE_GPRS,
        TelephonyManager.NETWORK_TYPE_EDGE,
        TelephonyManager.NETWORK_TYPE_CDMA,
        TelephonyManager.NETWORK_TYPE_1xRTT,
        TelephonyManager.NETWORK_TYPE_IDEN,
        TelephonyManager.NETWORK_TYPE_GSM
        -> return "2G"
        TelephonyManager.NETWORK_TYPE_UMTS,
        TelephonyManager.NETWORK_TYPE_EVDO_0,
        TelephonyManager.NETWORK_TYPE_EVDO_A,
        TelephonyManager.NETWORK_TYPE_HSDPA,
        TelephonyManager.NETWORK_TYPE_HSUPA,
        TelephonyManager.NETWORK_TYPE_HSPA,
        TelephonyManager.NETWORK_TYPE_EVDO_B,
        TelephonyManager.NETWORK_TYPE_EHRPD,
        TelephonyManager.NETWORK_TYPE_HSPAP,
        TelephonyManager.NETWORK_TYPE_TD_SCDMA
        -> return "3G"
        TelephonyManager.NETWORK_TYPE_LTE
        -> return "4G"
        TelephonyManager.NETWORK_TYPE_NR
        -> return "5G"
        else -> return "Unknown"
    }
}

fun removeSymbolsFromString (s: String): String {
    var answer = s
    val re = Regex("[^A-Za-z0-9 ]")
    answer = re.replace(answer, "")
    return answer
}