package com.example.unimarket.data.api

import com.example.unimarket.data.api.model.SepayTransactionsResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface SepayApiService {
    @GET("userapi/transactions/list")
    suspend fun getTransactions(
        @Header("Authorization") authorization: String,
        @Query("account_number") accountNumber: String,
        @Query("limit") limit: Int = 20
    ): Response<SepayTransactionsResponseDto>
}
