package com.example.unimarket.data.api.model

data class OtpDevSendVerificationRequestDto(
    val data: OtpDevVerificationDataDto
)

data class OtpDevVerificationDataDto(
    val channel: String,
    val sender: String,
    val phone: String,
    val template: String,
    val code_length: Int
)

data class OtpDevSendVerificationResponseDto(
    val account_id: String?,
    val message_id: String?,
    val phone: String?,
    val create_date: String?,
    val expire_date: String?
)

data class OtpDevVerifyVerificationResponseDto(
    val data: List<OtpDevSendVerificationResponseDto>? = null,
    val pagination: OtpDevPaginationDto? = null
)

data class OtpDevPaginationDto(
    val number: Int? = null,
    val size: Int? = null,
    val total: Int? = null
)

data class OtpDevErrorResponseDto(
    val errors: List<OtpDevErrorItemDto>? = null
)

data class OtpDevErrorItemDto(
    val timestamp: String? = null,
    val path: String? = null,
    val method: String? = null,
    val status: Int? = null,
    val message: String? = null,
    val code: String? = null
)
