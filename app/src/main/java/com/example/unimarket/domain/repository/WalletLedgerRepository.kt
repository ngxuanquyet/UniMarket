package com.example.unimarket.domain.repository

import com.example.unimarket.domain.model.WalletLedgerEntry

interface WalletLedgerRepository {
    suspend fun getRecent(limit: Int = 50): Result<List<WalletLedgerEntry>>
}

