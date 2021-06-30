package com.projects.android.kd_vc.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.projects.android.kd_vc.utils.Actions
import com.projects.android.kd_vc.utils.appendLog
import com.projects.android.kd_vc.services.EndlessService
import java.text.SimpleDateFormat
import java.util.*

class StartReceiver : BroadcastReceiver() {
    private val TAG = "KadeVC"

    override fun onReceive(context: Context, intent: Intent) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR"))
        val currentDate = sdf.format(Date())

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Intent(context.applicationContext, EndlessService::class.java).also {
                it.action = Actions.RESTART.name
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.d(TAG,"StartReceiver.onReceive() - Starting the service in >=26 Mode - Date: $currentDate")
                    appendLog("$currentDate KdVc - StartReceiver.onReceive() - Starting the service in >=26 Mode", context.applicationContext)
                    context.applicationContext.startForegroundService(it)
                } else {
                    Log.d(TAG, "StartReceiver.onReceive() - Starting the service in < 26 Mode - Date: $currentDate")
                    appendLog("$currentDate - KdVc - StartReceiver.onReceive() - Starting the service in < 26 Mode", context.applicationContext)
                    context.applicationContext.startService(it)
                }
            }
        }
    }
}