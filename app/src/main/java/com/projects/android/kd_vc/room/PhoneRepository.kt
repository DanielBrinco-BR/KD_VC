package com.projects.android.kd_vc.room

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow

// Declares the DAO as a private property in the constructor. Pass in the DAO
// instead of the whole database, because you only need access to the DAO
class PhoneRepository(private val phoneDao: PhoneDao) {
    private var TAG = "KadeVc"
    // Room executes all queries on a separate thread.
    // Observed Flow will notify the observer when the data has changed.
    val allPhones: Flow<List<Phone>> = phoneDao.getAlphabetizedPhones()

    val lastPhoneData: Flow<PhoneData> = phoneDao.findLastPhoneDataWithFlow()

    fun findByPhoneNumber(phoneNumber: String): LiveData<Phone> {
        return phoneDao.findByPhoneNumber(phoneNumber)
    }

    fun findLastPhoneData(): LiveData<PhoneData> {
        return phoneDao.findLastPhoneData()
    }

    fun findLastPhoneDataByNumber(phoneNumber: String): LiveData<PhoneData> {
        return phoneDao.findLastPhoneDataByNumber(phoneNumber)
    }

    fun getPhoneDataByNumber(phoneNumber: String): LiveData<List<PhoneData>> {
        return phoneDao.getPhoneDataByNumber(phoneNumber)
    }

    fun findListPhoneDataByDate(date: String, phoneNumber: String): LiveData<List<PhoneData>> {
        return phoneDao.findListPhoneDataByDate(date, phoneNumber)
    }

    fun findByDate(date: String, phoneNumber: String): LiveData<List<PhoneData>> {
        return phoneDao.findByDate(date, phoneNumber)
    }

    // By default Room runs suspend queries off the main thread, therefore, we don't need to
    // implement anything else to ensure we're not doing long running database work
    // off the main thread.
    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insert(phone: Phone) {
        phoneDao.insert(phone)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insertPhoneData(phoneData: PhoneData) {
        phoneDao.insertNewPhoneData(phoneData)
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
}