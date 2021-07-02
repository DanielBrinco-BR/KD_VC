package com.projects.android.kd_vc.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.projects.android.kd_vc.activities.MainActivity
import com.projects.android.kd_vc.services.EndlessService
import com.projects.android.kd_vc.utils.Actions
import com.projects.android.kd_vc.utils.ServiceState
import com.projects.android.kd_vc.utils.appendLog
import com.projects.android.kd_vc.utils.getServiceState
import java.text.SimpleDateFormat
import java.util.*

class BroadcastAlarmManger : BroadcastReceiver() {
    private val TAG = "KadeVc"

    override fun onReceive(context: Context, intent: Intent) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR"))
        val currentDate = sdf.format(Date())
        appendLog("$currentDate - BroadcastAlarmManager.onReceive()", context)
        MainActivity.registerAlarm(context)

        // Usando o EndlessService com Fused Location Provider
        if (getServiceState(context.applicationContext) == ServiceState.STOPPED) {
            Intent(context.applicationContext, EndlessService::class.java).also {
                it.action = Actions.START.name
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.d(TAG,"RestartService.doWork() - Starting the service in >=26 Mode - Date: $currentDate")
                    appendLog("KadeVc - RestartService.doWork() - Starting the service in >=26 Mode - Date: $currentDate", context.applicationContext)
                    context.applicationContext.startForegroundService(it)
                } else {
                    Log.d(TAG, "RestartService.doWork() - Starting the service in < 26 Mode - Date: $currentDate")
                    appendLog("KadeVc - RestartService.doWork() - Starting the service in < 26 Mode - Date: $currentDate", context.applicationContext)
                    context.applicationContext.startService(it)
                }
            }
        }
    }
}