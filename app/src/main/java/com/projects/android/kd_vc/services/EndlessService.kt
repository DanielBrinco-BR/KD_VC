package com.projects.android.kd_vc.services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.projects.android.kd_vc.*
import com.projects.android.kd_vc.R
import com.projects.android.kd_vc.activities.MainActivity
import com.projects.android.kd_vc.room.MyPhoneData
import com.projects.android.kd_vc.utils.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class EndlessService : Service() {
    private val TAG = "KadeVc"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        val formatedDate = SimpleDateFormat("dd-MM-yyyy", Locale("pt", "BR")).format(Date())
        val formatedTime = SimpleDateFormat("HH:mm:ss", Locale("pt", "BR")).format(Date())
        Log.d(TAG, "EndlessService.onCreate()")
        appendLog("$formatedDate $formatedTime EndlessService.onCreate()", this)

        if (!isLocationEnabled()) {
            Toast.makeText(this, "Ative a localização!", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setLocationUpdates()

        val notification = createNotification()
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val formatedDate = SimpleDateFormat("dd-MM-yyyy", Locale("pt", "BR")).format(Date())
        val formatedTime = SimpleDateFormat("HH:mm:ss", Locale("pt", "BR")).format(Date())
        Log.d(TAG,"EndlessService.onStartCommand() executed with startId: $startId")
        appendLog("$formatedDate $formatedTime EndlessService.onStartCommand() - executed with startId: $startId", this)

        startLocationUpdates()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "EndlessService.onDestroy() - The service has been destroyed")
        Toast.makeText(this, "Rastreador Desativado!", Toast.LENGTH_SHORT).show()
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent = Intent(applicationContext, EndlessService::class.java).also {
            it.setPackage(packageName)
        };
        val restartServicePendingIntent: PendingIntent = PendingIntent.getService(this, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
        applicationContext.getSystemService(Context.ALARM_SERVICE);
        val alarmService: AlarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager;
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent);
    }

    private fun isLocationEnabled(): Boolean {
        Log.d(TAG, "EndlessService.isLocationEnabled()")
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun startService() {
        if (isServiceStarted) {
            Log.i(TAG, "KdVc - EndlessService.startService() - isServiceStarted = true - return")
            appendLog("KdVc - EndlessService.startService() - isServiceStarted = true - return", this)
            return
        }

        Log.d(TAG,"EndlessService.startService() - Starting the foreground service task")
        appendLog("KdVc - EndlessService.startService() - Starting the foreground service task", this)
        Toast.makeText(this, "Rastreador Ativado!", Toast.LENGTH_SHORT).show()
        isServiceStarted = true
        setServiceState(this, ServiceState.STARTED)

        // We need this lock so our service gets not affected by Doze Mode
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EndlessService::lock").apply {
                    acquire()
                }
            }

        startLocationUpdates()
    }

    private fun stopService() {
        Log.d(TAG,"EndlessService.stopService() - Stopping the foreground service")
        Toast.makeText(this, "Service stopping", Toast.LENGTH_SHORT).show()
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            Log.d(TAG,"EndlessService.stopService() - Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
        setServiceState(this, ServiceState.STOPPED)
        stopLocationUpdates()
    }

    private fun restartService() {
        setServiceState(this, ServiceState.STOPPED)

        Log.d(TAG,"EndlessService.restartService() - isServiceStarted: $isServiceStarted")
        appendLog("KdVc - EndlessService.restartService() - isServiceStarted: $isServiceStarted\"", this)
        startService()
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "ENDLESS SERVICE CHANNEL"

        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                notificationChannelId,
                "KD_VC?",
                NotificationManager.IMPORTANCE_HIGH
            ).let {
                it.description = "Rastreador Ativado"
                it.enableLights(true)
                it.lightColor = Color.RED
                it.enableVibration(true)
                it.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                it
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, 0)
        }

        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
            this,
            notificationChannelId
        ) else Notification.Builder(this)

        // Substituindo .setSmallIcon(R.mipmap.user_location_foreground) por .setLargerIcon:
        val iconBitmap = BitmapFactory.decodeResource(resources, R.mipmap.user_location_foreground)

        return builder
            .setContentTitle("KD_VC?")
            .setContentText("Rastreador Ativado")
            .setContentIntent(pendingIntent)
            .setLargeIcon(iconBitmap)
            .setTicker("Ticker text")
            .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
            .build()
    }

    private fun setLocationUpdates() {
        locationRequest = LocationRequest()
        locationRequest.interval = 60000 //60000 * 1 = 1 minute
        locationRequest.fastestInterval = 60000 //60000 * 1 = 1 minute
        locationRequest.smallestDisplacement = 5f // 5 meters
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return

                if (locationResult.locations.isNotEmpty()) {
                    // get latest location
                    val location =
                        locationResult.lastLocation

                    // use your location object
                    // get latitude , longitude and other info from this
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val accuracy = location.accuracy

                    var batteryLevel = ""
                    var wifiSsId = ""
                    var hasInternet = ""
                    var networkType = ""

                    try {
                        batteryLevel = getBatteryLevel(this@EndlessService)
                        wifiSsId = getWiFiSSID(this@EndlessService)
                        hasInternet = isNetworkAvailable(this@EndlessService).toString()

                        val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
                        if (ActivityCompat.checkSelfPermission(
                                this@EndlessService, Manifest.permission.READ_PHONE_STATE)
                                    != PackageManager.PERMISSION_GRANTED) {
                            return
                        }
                        networkType = networkTypeClass(telephonyManager.networkType)

                    } catch(e: Exception) {
                        Log.i(TAG, "EndlessService.setLocationUpdates().locationCallback.onLocationResult() - Exception: $e.message \n e.stackTraceToString()")
                    }

                    Log.d(TAG, "EndlessService.setLocationUpdates().locationCallback.onLocationResult() - $latitude, $longitude, $accuracy, Extras: $batteryLevel, $wifiSsId, $hasInternet, $networkType")
                    appendLog("KD_VC? - EndlessService.setLocationUpdates().locationCallback.onLocationResult() - $latitude, $longitude, $accuracy, Extras: $batteryLevel, $wifiSsId, $hasInternet, $networkType", applicationContext)

                    if(accuracy <= 20.0) {
                        Log.d(TAG, "EndlessService.setLocationUpdates.locationCallback.onLocationResult() - accuracy <= 20.0 - Call saveMyPhoneData() and/or sendSMS()")
                        saveMyPhoneData(location.latitude.toString(), location.longitude.toString(), location.accuracy.toString(), batteryLevel, wifiSsId, hasInternet, networkType)

                        // Check if SMS send mode is active:
                        val smsState = getSmsState(this@EndlessService)
                        if(smsState == SmsState.ACTIVATED) {
                            Log.d(TAG, "EndlessService.setLocationUpdates.locationCallback.onLocationResult() - SmsState = $smsState - Call sendSMS()")
                            sendSMS(location.latitude.toString(), location.longitude.toString(), location.accuracy.toString(), batteryLevel, wifiSsId, hasInternet, networkType)
                        }
                        // Stop Location Updates to save battery:
                        stopLocationUpdates()
                    }
                }
            }
        }
    }

    private fun startLocationUpdates() {
        val formatedDate = SimpleDateFormat("dd-MM-yyyy", Locale("pt", "BR")).format(Date())
        val formatedTime = SimpleDateFormat("HH:mm:ss", Locale("pt", "BR")).format(Date())
        Log.d(TAG, "EndlessService.startLocationUpdates()")
        appendLog("$formatedDate $formatedTime EndlessService.startLocationUpdates()", this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) { return }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null /* Looper */
        )
    }

    // stop location updates
    private fun stopLocationUpdates() {
        val formatedDate = SimpleDateFormat("dd-MM-yyyy", Locale("pt", "BR")).format(Date())
        val formatedTime = SimpleDateFormat("HH:mm:ss", Locale("pt", "BR")).format(Date())
        Log.d(TAG, "EndlessService.stopLocationUpdates()")
        appendLog("$formatedDate $formatedTime EndlessService.stopLocationUpdates()", this)

        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun saveMyPhoneData(latitude: String, longitude: String, accuracy: String, batteryLevel: String,
                                wifiSsId: String, hasInternet: String, networkType: String) {
        // Get date and time:
        val formatedDate = SimpleDateFormat("dd-MM-yyyy", Locale("pt", "BR")).format(Date())
        val formatedTime = SimpleDateFormat("HH:mm:ss", Locale("pt", "BR")).format(Date())

        val number = getPhoneNumber(this)
        val data = "$latitude, $longitude, $accuracy, $formatedDate, $formatedTime, " +
                       "$batteryLevel, $wifiSsId, $hasInternet, $networkType"

        if(number != null) {
            Log.i(TAG, "EndlessService.saveMyPhoneData() - phone number != null: $number")
            appendLog("KadeVc - EndlessService.saveMyPhoneData() - phone number != null: $number", this)

            val encryptedData= Encryption.AESEncyption.encrypt(data)
            val myPhoneData = MyPhoneData(number, encryptedData)
            val phoneApplication = PhoneApplication()

            applicationScope.launch {
                phoneApplication.repository.insertNewMyPhoneData(myPhoneData)
            }

        } else {
            Log.i(TAG, "EndlessService.saveMyPhoneData() - phone number == null - Check SharedPreferences!!")
            appendLog("KadeVc - EndlessService.saveMyPhoneData() - phone number == null - Check SharedPreferences!!", this)
        }
    }

    fun sendSMS(latitude: String, longitude: String, accuracy: String, batteryLevel: String,
                wifiSsId: String, hasInternet: String, networkType: String) {
        // Get phone list and send SMS to everyone:
        applicationScope.launch {
            val phoneApplication = PhoneApplication()
            val listPhones = phoneApplication.repository.getAllActivePhones()

            // Get date and time:
            val formatedDate = SimpleDateFormat("dd-MM-yyyy", Locale("pt", "BR")).format(Date())
            val formatedTime = SimpleDateFormat("HH:mm:ss", Locale("pt", "BR")).format(Date())

            Log.d(TAG, "EndlessService.sendSMS() - $latitude, $longitude, $accuracy, $formatedDate, $formatedTime")
            appendLog("KD_VC? - EndlessService.sendSMS() - $latitude, $longitude, $accuracy, $formatedDate, $formatedTime", applicationContext)

            val smsManager = SmsManager.getDefault() as SmsManager

            // Send SMS to all phones registered:
            for(phone in listPhones) {
                Log.d(TAG, "EndlessService.sendSMS() - for listPhones phoneNumber: ${phone.phoneNumber}")
                smsManager.sendTextMessage(phone.phoneNumber, null,
                    "WhereAreYou, $latitude, $longitude, $accuracy, $formatedDate, $formatedTime, " +
                            "$batteryLevel, $wifiSsId, $hasInternet, $networkType", null, null)
            }
        }
    }
}