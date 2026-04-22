package com.example.unimarket.data.api.model

data class SendOtpRequestDto(
    val phoneNumber: String
)

data class SendOtpResponseDto(
    val success: Boolean,
    val sid: String?,
    val status: String?,
    val to: String?
)

data class VerifyOtpRequestDto(
    val phoneNumber: String,
    val code: String
)

data class VerifyOtpResponseDto(
    val success: Boolean,
    val status: String?,
    val to: String?
)
