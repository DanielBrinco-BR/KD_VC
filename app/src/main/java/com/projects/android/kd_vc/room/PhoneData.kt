package com.projects.android.kd_vc.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Data received from tracked phones
@Entity(tableName = "phone_data")
class PhoneData (
    @ColumnInfo(name = "phone_number") val phoneNumber: String,
    @ColumnInfo(name = "latitude") val latitude: String,
    @ColumnInfo(name = "longitude") val longitude: String,
    @ColumnInfo(name = "accuracy") val accuracy: String,
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "time") val time: String,
    @ColumnInfo(name = "battery_level") val batteryLevel: String,
    @ColumnInfo(name = "wifi_ssid") val wifiSSID: String,
    @ColumnInfo(name = "has-internet") val hasInternet: String,
    @ColumnInfo(name = "network_type") val networkType: String,
    @PrimaryKey(autoGenerate = true) val id: Int = 0)