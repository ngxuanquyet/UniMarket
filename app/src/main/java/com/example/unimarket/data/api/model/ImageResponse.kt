package com.example.unimarket.data.api.model

import com.google.gson.annotations.SerializedName

data class ImageResponse(
    @SerializedName("data")
    val data: Data
)

data class Data(
    @SerializedName("id")
    val id: String,
    @SerializedName("url")
    val url: String
)
