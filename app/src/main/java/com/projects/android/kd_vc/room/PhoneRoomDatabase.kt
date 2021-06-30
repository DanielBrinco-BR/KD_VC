package com.projects.android.kd_vc.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Annotates class to be a Room Database with a table (entity) of the Phone class
@Database(entities = arrayOf(Phone::class, PhoneData::class), version = 1, exportSchema = false)
abstract class PhoneRoomDatabase : RoomDatabase() {
    private val TAG = "KadeVc"

    abstract fun phoneDao(): PhoneDao

    private class PhoneDatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
        private val TAG = "KadeVc"

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    val phoneDao = database.phoneDao()

                    /*
                    if(phoneDao.countPhoneData() == 0) {
                        Log.i(TAG, "PhoneRoomDatabase.PhoneDatabaseCallback.onCreate() - countPhoneData() == 0")
                        val data = PhoneData(
                            "12991516295", "0.0", "0.0",
                            "0.0", "01-01-1900", "00:00:00")

                        phoneDao.insertNewPhoneData(data)
                    }

                    if(phoneDao.countPhones() == 0) {
                        Log.i("KD_VC?", "PhoneRoomDatabase.PhoneDatabaseCallback.onCreate() - countPhones() == 0")
                        val imageUri = R.mipmap.default_image_round
                        val phone1 = Phone("+5512991516295", "Eu", imageUri.toString())
                        val phone2 = Phone("+5512991270763", "Sandra", imageUri.toString())
                        phoneDao.insert(phone1)
                        phoneDao.insert(phone2)
                    }
                    */
                }
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: PhoneRoomDatabase? = null

        fun getDatabase(
            context: Context,
            scope: CoroutineScope
        ): PhoneRoomDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PhoneRoomDatabase::class.java,
                    "phone_database"
                )
                    .addCallback(PhoneDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}
