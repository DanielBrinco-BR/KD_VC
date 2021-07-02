package com.projects.android.kd_vc.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Annotates class to be a Room Database with a table (entity) of the Phone class
@Database(entities = arrayOf(Phone::class, PhoneData::class, MyPhoneData::class), version = 1, exportSchema = false)
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
