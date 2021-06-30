package com.projects.android.kd_vc.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.projects.android.kd_vc.services.EndlessService
import java.text.SimpleDateFormat
import java.util.*

class RestartService(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private val TAG = "KadeVC"

    override fun doWork(): Result {
        // Get date and time:
        val formatedDate = SimpleDateFormat("dd-MM-yyyy", Locale("pt", "BR")).format(Date())
        val formatedTime = SimpleDateFormat("HH:mm:ss", Locale("pt", "BR")).format(Date())

        Log.d(TAG, "RestartService.doWork() - Date: $formatedDate, Time: $formatedTime")
        appendLog("KD_VC? - RestartService.doWork() - Date: $formatedDate, Time: $formatedTime", applicationContext)

        if (getServiceState(applicationContext) == ServiceState.STOPPED) {
            Intent(applicationContext, EndlessService::class.java).also {
                it.action = Actions.START.name
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.d(TAG,"RestartService.doWork() - Starting the service in >=26 Mode - Date: $formatedDate, Time: $formatedTime")
                    appendLog("KD_VC? - RestartService.doWork() - Starting the service in >=26 Mode - Date: $formatedDate, Time: $formatedTime", applicationContext)
                    applicationContext.startForegroundService(it)
                } else {
                    Log.d(TAG, "RestartService.doWork() - Starting the service in < 26 Mode - Date: $formatedDate, Time: $formatedTime")
                    appendLog("KD_VC? - RestartService.doWork() - Starting the service in < 26 Mode - Date: $formatedDate, Time: $formatedTime", applicationContext)
                    applicationContext.startService(it)
                }
            }
        }
        return Result.success()
    }
}