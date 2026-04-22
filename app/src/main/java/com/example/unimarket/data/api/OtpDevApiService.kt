package com.example.unimarket.data.api

import com.example.unimarket.data.api.model.OtpDevSendVerificationRequestDto
import com.example.unimarket.data.api.model.OtpDevSendVerificationResponseDto
import com.example.unimarket.data.api.model.OtpDevVerifyVerificationResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface OtpDevApiService {

    @Headers(
        "accept: application/json",
        "content-type: application/json"
    )
    @POST("v1/verifications")
    suspend fun sendSmsVerification(
        @Header("X-OTP-Key") apiKey: String,
        @Body body: OtpDevSendVerificationRequestDto
    ): Response<OtpDevSendVerificationResponseDto>

    @Headers("accept: application/json")
    @GET("v1/verifications")
    suspend fun verifySmsCode(
        @Header("X-OTP-Key") apiKey: String,
        @Query("code") code: String,
        @Query("phone") phone: String
    ): Response<OtpDevVerifyVerificationResponseDto>
}
