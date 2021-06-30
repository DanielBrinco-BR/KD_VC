package com.projects.android.kd_vc.retrofit

import com.google.gson.annotations.SerializedName

data class PhoneDataInfo (
    @SerializedName("id")
    var id : Int?,
    @SerializedName("number")
    var number : String,
    @SerializedName("data")
    var data : String
)