package com.areen.fixmystreet.services

import com.areen.fixmystreet.models.Case
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("/uploadImage")
    suspend fun uploadImage(
        @Body report: PotholeReport
    ): Response<PotholeResponse>
    @GET("getAllCases")
    fun getAllCases(): Call<List<Case>>
}

data class PotholeReport(
    val address: String,
    val latitude: String,
    val longitude: String,
    val submittedBy: String,
    val image: String?
)

data class PotholeResponse(
    val message: String,
    val potholeId: String
)