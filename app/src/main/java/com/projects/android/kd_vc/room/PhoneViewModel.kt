package com.projects.android.kd_vc.room

import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PhoneViewModel(private val repository: PhoneRepository) : ViewModel() {
    private var TAG = "KadeVc"
    private lateinit var phone: Phone
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Using LiveData and caching what allPhones returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    val allPhones: LiveData<List<Phone>> = repository.allPhones.asLiveData()

    val lastPhoneData: LiveData<PhoneData> = repository.lastPhoneData.asLiveData()

    fun findByPhoneNumber(phoneNumber: String): LiveData<Phone> {
        return repository.findByPhoneNumber(phoneNumber)
    }

    fun getPhoneDataByNumber(phoneNumber: String): LiveData<List<PhoneData>> {
        return repository.getPhoneDataByNumber(phoneNumber)
    }

    fun findLastPhoneData(): LiveData<PhoneData> {
        return repository.findLastPhoneData()
    }

    fun findLastPhoneDataByNumber(phoneNumber: String): LiveData<PhoneData> {
        return repository.findLastPhoneDataByNumber(phoneNumber)
    }

    fun findListPhoneDataByDate(date: String, phoneNumber: String): LiveData<List<PhoneData>> {
        return repository.findListPhoneDataByDate(date, phoneNumber)
    }

    fun findByDate(date: String, phoneNumber: String): LiveData<List<PhoneData>> {
        return repository.findByDate(date, phoneNumber)
    }

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun insert(phone: Phone) = viewModelScope.launch {
        repository.insert(phone)
    }

    fun insertPhoneData(phoneData: PhoneData) = viewModelScope.launch {
        repository.insertPhoneData(phoneData)
    }

    fun updatePhoneImage(imageUri: String, phoneNumber: String) = viewModelScope.launch {
        Log.i(TAG, "PhoneViewModel.updatePhoneImage($imageUri, $phoneNumber)")
        repository.updatePhoneImage(imageUri, phoneNumber)
    }

    fun updatePhone(oldPhoneNumber: String, phone: Phone) = viewModelScope.launch {
        Log.i(TAG, "PhoneViewModel.updatePhone(${phone.alias}, ${phone.phoneNumber}, ${phone.imageUri})")
        repository.updatePhone(oldPhoneNumber, phone)
    }

    fun updateLastPhoneInfo(phoneNumber: String, date: String, time: String) = viewModelScope.launch {
        Log.i(TAG, "PhoneViewModel.updateLastPhoneInfo($phoneNumber, $date, $time)")
        repository.updateLastPhoneInfo(phoneNumber, date, time)
    }

    fun deletePhone(phone: Phone) = viewModelScope.launch {
        Log.i(TAG, "PhoneViewModel.deletePhone(${phone.alias}, ${phone.phoneNumber}, ${phone.imageUri})")

        /*
        GlobalScope.async {
            repository.deletePhone(phone)
        }
        */

        applicationScope.launch {
            repository.deletePhone(phone)
        }
    }
}

class PhoneViewModelFactory(private val repository: PhoneRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhoneViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PhoneViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}