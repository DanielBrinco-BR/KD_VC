package com.projects.android.kd_vc.room

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.projects.android.kd_vc.PhoneApplication
import com.projects.android.kd_vc.retrofit.PhoneDataInfo
import com.projects.android.kd_vc.retrofit.RestApiManager
import com.projects.android.kd_vc.utils.Encryption
import com.projects.android.kd_vc.utils.appendLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PhoneRepository(private val phoneDao: PhoneDao) {
    private var TAG = "KadeVc"

    val allPhones: Flow<List<Phone>> = phoneDao.getAlphabetizedPhones()

    val lastPhoneData: Flow<PhoneData> = phoneDao.findLastPhoneDataWithFlow()

    fun findByPhoneNumber(phoneNumber: String): LiveData<Phone> {
        return phoneDao.findByPhoneNumber(phoneNumber)
    }

    fun findByEncryptedPhoneNumber(encryptedPhoneNumber: String): Phone {
        return phoneDao.findByEncryptedPhoneNumber(encryptedPhoneNumber)
    }

    fun findLastPhoneData(): LiveData<PhoneData> {
        return phoneDao.findLastPhoneData()
    }

    fun findLastPhoneDataByNumber(phoneNumber: String): LiveData<PhoneData> {
        return phoneDao.findLastPhoneDataByNumber(phoneNumber)
    }

    fun findListPhoneDataByDate(date: String, phoneNumber: String): LiveData<List<PhoneData>> {
        return phoneDao.findListPhoneDataByDate(date, phoneNumber)
    }

    fun findByDate(date: String, phoneNumber: String): LiveData<List<PhoneData>> {
        return phoneDao.findByDate(date, phoneNumber)
    }

    fun getPhoneDataByNumber(phoneNumber: String): LiveData<List<PhoneData>> {
        return phoneDao.getPhoneDataByNumber(phoneNumber)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun getMyPhoneDataList(): List<MyPhoneData> {
        return phoneDao.getMyPhoneDataList()
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun getAllActivePhones(): List<Phone> {
        return phoneDao.getAllActivePhones()
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun countPhoneDataByNumber(phoneNumber: String): Int {
        return phoneDao.countPhoneDataByNumber(phoneNumber)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insert(phone: Phone) {
        phoneDao.insert(phone)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insertNewPhoneData(phoneData: PhoneData) {
        phoneDao.insertNewPhoneData(phoneData)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insertNewMyPhoneData(myPhoneData: MyPhoneData) {
        phoneDao.insertNewMyPhoneData(myPhoneData)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun updatePhoneImage(imageUri: String, phoneNumber: String) {
        phoneDao.updatePhoneImage(imageUri, phoneNumber)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun updatePhone(oldPhoneNumber: String, phone: Phone) {
        phoneDao.updatePhone(oldPhoneNumber, phone.phoneNumber, phone.alias, phone.imageUri)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun updateLastPhoneInfo(phoneNumber: String, date: String, time: String) {
        phoneDao.updateLastPhoneInfo(phoneNumber, date, time)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun deletePhone(phone: Phone) {
        phoneDao.deletePhone(phone)
    }

    fun deleteMyPhoneData(myPhoneData: MyPhoneData) {
        phoneDao.deleteMyPhoneData(myPhoneData)
    }

    fun sendUpdatesToServer(context: Context) {
        Log.i(TAG, "PhoneRepository.sendUpdatesFromServer()")
        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        //val database = PhoneRoomDatabase.getDatabase(context, applicationScope)
        val phoneApplication = PhoneApplication()

        applicationScope.launch {
            try {
                val listMyPhoneData = phoneApplication.repository.getMyPhoneDataList()

                for (myPhoneData in listMyPhoneData) {
                    val phoneDataInfo = PhoneDataInfo(
                        id = null,
                        number = myPhoneData.number,
                        data = myPhoneData.data
                    )
                    postDataToServer(myPhoneData, phoneDataInfo, context)
                }

            } catch(e: Exception) {
                Log.e("KadeVc", "PhoneRepository.sendUpdatesFromServer() - POST to Heroku - Exception: ${e.message}")
                appendLog("KadeVc - PhoneRepository.sendUpdatesFromServer() - POST to Heroku - Exception: ${e.message}", context)
            }
        }
    }

    fun checkUpdatesFromServer(context: Context) {
        Log.i(TAG, "PhoneRepository.checkUpdatesFromServer()")
        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val phoneApplication = PhoneApplication()

        applicationScope.launch {
            try {
                val listPhones = phoneApplication.repository.getAllActivePhones()

                val apiManager = RestApiManager()

                for (phone in listPhones) {
                    Log.i("KadeVc", "PhoneRepository.checkUpdatesFromServer() - GET from Heroku - phoneNumber: ${phone.phoneNumber}")
                    Log.i("KadeVc", "PhoneRepository.checkUpdatesFromServer() - GET from Heroku - encryptedPhoneNumber: ${phone.encryptedPhoneNumber}")
                    apiManager.getPhoneData(phone.encryptedPhoneNumber, context)
                }
            } catch(e: Exception) {
                Log.e("KadeVc", "PhoneRepository.checkUpdatesFromServer() - GET from Heroku - Exception: ${e.message}")
                appendLog("KadeVc - PhoneRepository.checkUpdatesFromServer() - GET from Heroku - Exception: ${e.message}", context)
            }
        }
    }

    fun postDataToServer(myPhoneData: MyPhoneData, phoneDataInfo: PhoneDataInfo, context: Context) {
        Log.i(TAG, "PhoneRepository.postDataToServer()")
        val phoneApplication = PhoneApplication()
        val apiManager = RestApiManager()
        val applicationScope = CoroutineScope(SupervisorJob())

        apiManager.addPhoneData(phoneDataInfo) {
            if (it?.id != null) {
                val id = it.id
                val number = it.number
                val data = Encryption.AESEncyption.decrypt(it.data)

                val response = "$id \n$number \n$data"

                // Delete myPhoneData after send to server:
                applicationScope.launch {
                    try {
                        phoneApplication.repository.deleteMyPhoneData(myPhoneData)
                    } catch(e: Exception) {
                        Log.e("KadeVc", "PhoneRepository.postData() - Exception: ${e.message}")
                        appendLog("KadeVc - PhoneRepository.postData() - Exception: ${e.message}", context)
                    }
                }

                Log.i("KadeVc", "PhoneRepository.postData() - POST Response: \n $response")
                appendLog("KdVc - PhoneRepository.postData - POST Response: \n $response", context)
            } else {
                Log.e("KadeVc", "PhoneRepository.postData() - Error on POST method;")
                appendLog("KdVc - PhoneRepository.postData - POST Response: Error on POST method", context)
            }
        }
    }
}