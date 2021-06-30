package com.projects.android.kd_vc.services

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.SupplicantState
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.projects.android.kd_vc.utils.appendLog
import java.text.SimpleDateFormat
import java.util.*


class GpsService : Service(), LocationListener {
    private val TAG = "KadeVc"
    private val ONE_MINUTE = 60000L
    private val FIVE_MINUTES = 300000L
    private val TEN_MINUTES = 600000L
    private val FIFTEEN_MINUTES = 900000L
    private lateinit var pendingIntent: PendingIntent
    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 2

    override fun onCreate() {
        super.onCreate()

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR"))
        val currentDate = sdf.format(Date())

        Log.i(TAG, "GpsService.onCreate()")
        appendLog(currentDate + " GpsService.onCreate()", this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR"))
        val currentDate = sdf.format(Date())

        Log.i(TAG, "GpsService.onStartCommand()")
        appendLog(currentDate + " GpsService.onStartCommand()", this)

        try {
            getLocation()
        } catch(e: Exception) {
            Log.e(TAG, "GpsService.onStartCommand() - Exception: $e.message \n ${e.stackTraceToString()}")
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    private fun getLocation() {
        Log.i(TAG, "GpsService.getLocation()")

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR"))
            val currentDate = sdf.format(Date())
            appendLog("$currentDate - GpsService.getLocation() - PERMISSION_NOT_GRANTED!!!", this)
            return
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, ONE_MINUTE, 5f, this)
    }
    override fun onLocationChanged(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude
        val accuracy = location.accuracy

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR"))
        val currentDate = sdf.format(Date())

        Log.d(TAG, "GpsService.onLocationChanged() - $latitude, $longitude, $accuracy")
        appendLog("$currentDate - GpsService.onLocationChanged() - $latitude, $longitude, $accuracy", applicationContext)

        if(accuracy <= 20.0) {
            Log.d(TAG, "GpsService.onLocationChanged() - accuracy <= 20.0 - Call sendSMS()")
            sendSMS(location.latitude.toString(), location.longitude.toString(), location.accuracy.toString())
        }
    }

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    fun sendSMS(latitude: String, longitude: String, accuracy: String) {
        // Get date and time:
        val formattedDate = SimpleDateFormat("dd-MM-yyyy", Locale("pt", "BR")).format(Date())
        val formattedTime = SimpleDateFormat("HH:mm:ss", Locale("pt", "BR")).format(Date())
        val batteryLevel = getBatteryLevel()
        val hasInternet = isNetworkAvailable(this)
        val wifiSSID = getWiFiSSID()


        Log.d(TAG, "GpsService.sendSMS() - $latitude, $longitude, $accuracy, $formattedDate, $formattedTime, $batteryLevel")
        appendLog("$formattedDate $formattedTime - GpsService.sendSMS() - $latitude, $longitude, $accuracy, $formattedDate, $formattedTime, $batteryLevel, $hasInternet, $wifiSSID", applicationContext)

        val smsManager = SmsManager.getDefault() as SmsManager
        smsManager.sendTextMessage("+5512991516295", null, "WhereAreYou, $latitude, $longitude, $accuracy, $formattedDate, $formattedTime, $batteryLevel, $hasInternet, $wifiSSID", null, null)
        //smsManager.sendTextMessage("+5512991270763", null, "WhereAreYou, $latitude, $longitude, $accuracy, $formattedDate, $formattedTime", null, null)
    }

    fun getBatteryLevel(): String {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            this.registerReceiver(null, ifilter)
        }

        val batteryPct: Float? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        }

        return "$batteryPct%"
    }

    fun getWiFiSSID(): String {
        var ssid = ""
        val wifiManager = getApplicationContext().getSystemService(WIFI_SERVICE) as WifiManager
        val wifiInfo: WifiInfo

        wifiInfo = wifiManager.connectionInfo
        if (wifiInfo.supplicantState == SupplicantState.COMPLETED) {
            ssid = wifiInfo.ssid
        }
        return ssid
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

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "GpsService.onDestroy()")
        locationManager.removeUpdates(this)
    }
}