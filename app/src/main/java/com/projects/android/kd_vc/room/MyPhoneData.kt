package com.projects.android.kd_vc.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Data from my phone pending to send to cloud
@Entity(tableName = "my_phone_data")
class MyPhoneData (
    @ColumnInfo(name = "number") val number: String,
    @ColumnInfo(name = "data") val data: String,
    @PrimaryKey(autoGenerate = true) val id: Int = 0)