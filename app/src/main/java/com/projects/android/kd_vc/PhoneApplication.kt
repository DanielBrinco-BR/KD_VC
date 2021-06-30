package com.projects.android.kd_vc

import android.app.Application
import com.projects.android.kd_vc.room.PhoneRepository
import com.projects.android.kd_vc.room.PhoneRoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class PhoneApplication : Application() {
    // No need to cancel this scope as it'll be torn down with the process
    val applicationScope = CoroutineScope(SupervisorJob())

    // Using by lazy so the database and the repository are only created when they're needed
    // rather than when the application starts
    val database by lazy { PhoneRoomDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { PhoneRepository(database.phoneDao()) }
}