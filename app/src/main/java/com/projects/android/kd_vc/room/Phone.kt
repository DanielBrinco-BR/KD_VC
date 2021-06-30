package com.projects.android.kd_vc.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phone_table")
class Phone(
    @PrimaryKey @ColumnInfo(name = "phone_number") var phoneNumber: String,
    @ColumnInfo(name = "alias") val alias: String,
    @ColumnInfo(name = "imageUri") val imageUri: String,
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "time") val time: String,
    @ColumnInfo(name = "encrypted_phone_number") val encryptedPhoneNumber: String
)