package com.projects.android.kd_vc.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import android.util.Log
import androidx.annotation.RequiresApi
import com.projects.android.kd_vc.PhoneApplication
import com.projects.android.kd_vc.room.PhoneData
import com.projects.android.kd_vc.utils.appendLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


class SmsReceiver : BroadcastReceiver() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val pdu_type = "pdus"

    private val TAG = "KadeVc"

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "SmsReceiver.onReceive()")

        val phoneApplication = PhoneApplication()

        // Get the SMS message.
        val bundle = intent.extras
        val msgs: Array<SmsMessage?>
        var strMessage = ""
        var strOrigin = ""
        val format = bundle!!.getString("format")

        // Retrieve the SMS message received.
        val pdus = bundle!![pdu_type] as Array<Any>?

        if (pdus != null) {
            // Check the Android version.
            val isVersionM = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            // Fill the msgs array.
            msgs = arrayOfNulls<SmsMessage>(pdus.size)

            for (i in msgs.indices) {
                // Check Android version and use appropriate createFromPdu.
                if (isVersionM) {
                    // If Android version M or newer:
                    msgs[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray, format)
                } else {
                    // If Android version L or older:
                    msgs[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray)
                }

                try {
                    strMessage = msgs[i]?.getMessageBody().toString()
                    strOrigin = msgs[i]?.originatingAddress.toString()

                    if(strMessage.startsWith("WhereAreYou") || strMessage.startsWith("TrackerTest")) {
                        Log.d(TAG, "SmsReceiver.onReceive() - abortBroadcast()")
                        this.abortBroadcast()

                        Log.d(TAG, "SmsReceiver.onReceive() -> Message: $strMessage, Origin: $strOrigin")
                        appendLog("SmsReceiver.onReceive() -> Message: $strMessage, Origin: $strOrigin", context)

                        // Get latitute, longitude, date and time from SMS message:
                        val smsContent = strMessage.split(",")

                        val lat = smsContent[1]
                        val long =  smsContent[2]
                        val accuracy =  smsContent[3]
                        val date = smsContent[4]
                        val time = smsContent[5]
                        var batteryLevel = ""
                        var wifiSSID = ""
                        var hasInternet = ""
                        var networkType = ""

                        try {
                            batteryLevel = smsContent[6]
                            wifiSSID = smsContent[7]
                            hasInternet = smsContent[8]
                            networkType = smsContent[9]
                        } catch(e: Exception) {
                            Log.e(TAG, "SmsReceiver.onReceive() - Exception: ${e.message} \n ${e.stackTraceToString()}")
                        }

                        Log.d(TAG, "SmsReceiver.onReceive() - $strOrigin, $lat, $long, $accuracy, $date, $time, $batteryLevel, $wifiSSID, $hasInternet, $networkType")
                        appendLog("$date $time - SmsReceiver.onReceive() - $strOrigin, $lat, $long, $accuracy, $date, $time, $batteryLevel, $wifiSSID, $hasInternet, $networkType", context)

                        // Saving in Database:
                        val data = PhoneData(strOrigin, lat, long, accuracy, date, time, batteryLevel, wifiSSID, hasInternet, networkType)

                        applicationScope.launch {
                            phoneApplication.repository.insertNewPhoneData(data)
                        }

                        Log.d(TAG, "SmsReceiver.onReceive() - smsContent: $lat, $long, $date, $time")
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "SmsReceiver.onReceive() - Exception: \n ${e.printStackTrace()}")
                }

                Log.d(TAG, "SmsReceiver.onReceive(): $strMessage - $strOrigin")
                appendLog("KD_VC? - SmsReceiver.onReceive(): $strMessage, $strOrigin", context)
            }
        }
    }
}