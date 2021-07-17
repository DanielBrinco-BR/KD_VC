package com.projects.android.kd_vc.retrofit

import android.content.Context
import android.util.Log
import com.projects.android.kd_vc.PhoneApplication
import com.projects.android.kd_vc.room.PhoneData
import com.projects.android.kd_vc.utils.Encryption.AESEncyption.decrypt
import com.projects.android.kd_vc.utils.appendLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RestApiManager {
    private val TAG = "KadeVc"

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun addPhoneData(phoneData: PhoneDataInfo, onResult: (PhoneDataInfo?) -> Unit){
        val retrofit = ServiceBuilder.buildService(RestApi::class.java)
        retrofit.addPhoneData(phoneData).enqueue(
            object : Callback<PhoneDataInfo> {
                override fun onFailure(call: Call<PhoneDataInfo>, t: Throwable) {
                    onResult(null)
                }
                override fun onResponse(call: Call<PhoneDataInfo>, response: Response<PhoneDataInfo>) {
                    val addedUser = response.body()
                    onResult(addedUser)
                }
            }
        )
    }

    fun getPhoneData(phoneNumber: String, context: Context) {
        val retrofit = Retrofit.Builder().baseUrl("https://kd-vc-api.herokuapp.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val jsonPlaceHolderApi = retrofit.create(
            JsonPlaceHolderApi::class.java
        )

        val listCall: Call<List<PhoneDataInfo>> = jsonPlaceHolderApi.listPhoneData(phoneNumber)

        listCall.enqueue(object : Callback<List<PhoneDataInfo>> {
            override fun onResponse(call: Call<List<PhoneDataInfo>>, response: Response<List<PhoneDataInfo>>) {
                if (!response.isSuccessful) {
                    Log.i(TAG, "RestApiManager.getPhoneData() - Response Code " + response.code())
                    appendLog("RestApiManager.getPhoneData() - Response Code ${response.code()}", context)
                    return
                }

                val phoneDataInfo: List<PhoneDataInfo> = response.body()!!
                val phoneApplication = PhoneApplication()

                for (dataInfo in phoneDataInfo) {
                    val dataDecrypted = decrypt(dataInfo.data)
                    val data = dataDecrypted.filter { !it.isWhitespace() }
                    val dataArray = data.split(",")

                    val latitude = dataArray[0]
                    val longitude =  dataArray[1]
                    val accuracy =  dataArray[2]
                    val date = dataArray[3]
                    val time = dataArray[4]
                    val batteryLevel = dataArray[5]
                    val wifiSSID = dataArray[6]
                    val hasInternet = dataArray[7]
                    val networkType = dataArray[8]

                    applicationScope.launch {
                        val phone = phoneApplication.repository.findByEncryptedPhoneNumber(dataInfo.number)

                        Log.i(TAG, "RestApiManager.getPhoneData() - Callback onResponse: ${phone.phoneNumber}, $latitude, $longitude, $accuracy, $date, $time, $batteryLevel," +
                                "$wifiSSID, $hasInternet, $networkType")
                        appendLog("KdVc - RestApiManager.getPhoneData() - Callback onResponse: ${phone.phoneNumber}, $latitude, $longitude, $accuracy, $date, $time, " +
                                "$batteryLevel, $wifiSSID, $hasInternet, $networkType", context)

                        val phoneData = PhoneData(phone.phoneNumber, latitude, longitude, accuracy, date,
                            time, batteryLevel, wifiSSID, hasInternet, networkType)
                        phoneApplication.repository.insertNewPhoneData(phoneData)
                    }
                }
            }

            override fun onFailure(call: Call<List<PhoneDataInfo>>, t: Throwable) {
                Log.e(TAG, "RestApiManager.getPhoneData() - Callback onFailure: ${t.message}!!!!!!!!!")
                Log.e(TAG, "RestApiManager.getPhoneData() - Callback onFailure: \n ${t.stackTraceToString()}!!!!!!!!!")
                appendLog("KdVc - RestApiManager.getPhoneData() - Callback onFailure: ${t.message}!!!!!!!!!!", context)
            }
        })
    }
}