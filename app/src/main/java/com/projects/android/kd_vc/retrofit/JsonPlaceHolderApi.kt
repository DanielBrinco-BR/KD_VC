package com.projects.android.kd_vc.retrofit

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface JsonPlaceHolderApi {
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json",
        "Platform: android")
    @GET("api/v1/phones")
    fun listPhoneData(@Query("number") number: String): Call<List<PhoneDataInfo>>
}