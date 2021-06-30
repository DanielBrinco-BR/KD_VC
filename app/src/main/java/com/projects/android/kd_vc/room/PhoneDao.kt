package com.projects.android.kd_vc.room

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PhoneDao {
    @Query("SELECT count(*) FROM phone_table")
    fun countPhones(): Int

    @Query("SELECT count(*) FROM phone_data WHERE phone_number LIKE :phoneNumber")
    fun countPhoneDataByNumber(phoneNumber: String): Int

    @Query("SELECT count(*) FROM phone_data")
    fun countPhoneData(): Int

    @Query("SELECT * FROM phone_table ORDER BY alias ASC")
    fun getAlphabetizedPhones(): Flow<List<Phone>>

    @Query("SELECT * FROM phone_data ORDER BY id DESC LIMIT 1")
    fun findLastPhoneDataWithFlow() : Flow<PhoneData>

    @Query("SELECT * FROM phone_table")
    fun getAllPhones(): Flow<List<Phone>>

    @Query("SELECT * FROM phone_table")
    fun getAllActivePhones(): List<Phone>

    @Query("SELECT * FROM phone_data WHERE phone_number LIKE :phoneNumber")
    fun getPhoneDataByNumber(phoneNumber: String): LiveData<List<PhoneData>>

    @Query("SELECT * FROM phone_table WHERE phone_number LIKE :phoneNumber LIMIT 1")
    fun findByPhoneNumber(phoneNumber: String): LiveData<Phone>

    @Query("SELECT * FROM phone_table WHERE encrypted_phone_number LIKE :encryptedPhoneNumber LIMIT 1")
    fun findByEncryptedPhoneNumber(encryptedPhoneNumber: String): Phone

    @Query("SELECT * FROM phone_data WHERE phone_number LIKE :phoneNumber")
    fun findAllByPhoneNumber(phoneNumber: String): List<PhoneData>

    @Query("SELECT * FROM phone_data WHERE date LIKE :date AND phone_number LIKE :phoneNumber")
    fun findByDate(date: String, phoneNumber: String): LiveData<List<PhoneData>>

    @Query("SELECT * FROM phone_data WHERE date LIKE :date AND phone_number LIKE :phoneNumber")
    fun findPhoneDataByDate(date: String, phoneNumber: String): List<PhoneData>

    @Query("SELECT * FROM phone_data WHERE date LIKE :date AND phone_number LIKE :phoneNumber")
    fun findListPhoneDataByDate(date: String, phoneNumber: String): LiveData<List<PhoneData>>

    @Query("SELECT * FROM phone_data WHERE phone_number LIKE :phoneNumber ORDER BY id DESC LIMIT 1")
    fun findLastPhoneDataByNumber(phoneNumber: String): LiveData<PhoneData>

    @Query("SELECT * FROM phone_data ORDER BY id DESC LIMIT 1")
    fun findLastPhoneData(): LiveData<PhoneData>

    @Query("SELECT * FROM phone_data WHERE date LIKE :date AND time LIKE :time LIMIT 1")
    fun getPhoneDataByDateAndTime(date: String, time: String): PhoneData

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(phone: Phone)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNewPhoneData(phoneData: PhoneData)

    @Query("UPDATE phone_table SET imageUri = :imageUri WHERE phone_number = :phoneNumber")
    suspend fun updatePhoneImage(imageUri: String, phoneNumber: String)

    @Query("UPDATE phone_table SET phone_number = :phoneNumber, alias = :alias, imageUri = :imageUri WHERE phone_number = :oldPhoneNumber")
    suspend fun updatePhone(oldPhoneNumber: String, phoneNumber: String, alias: String, imageUri: String)

    @Query("UPDATE phone_table SET date = :date, time = :time WHERE phone_number = :phoneNumber")
    suspend fun updateLastPhoneInfo(phoneNumber: String, date: String, time: String)

    @Delete
    fun deletePhone(phone: Phone)

    @Query("DELETE FROM phone_table")
    suspend fun deleteAll()
}