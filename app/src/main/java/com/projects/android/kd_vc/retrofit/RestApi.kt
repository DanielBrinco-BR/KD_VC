package com.projects.android.kd_vc.retrofit

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface RestApi {
    @Headers("Content-Type: application/json")
    @POST("api/v1/phones")
    fun addPhoneData(@Body phoneData: PhoneDataInfo): Call<PhoneDataInfo>
}