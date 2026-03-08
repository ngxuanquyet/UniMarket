package com.example.unimarket.data.api

import com.example.unimarket.data.api.model.ImageResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ImageApiService {

    @Multipart
    @POST("upload")
    suspend fun uploadImage(
        @Query("key") key: String,
        @Part image: MultipartBody.Part
    ): Response<ImageResponse>

}