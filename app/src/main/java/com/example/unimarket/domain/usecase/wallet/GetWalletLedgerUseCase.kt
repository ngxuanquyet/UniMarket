package com.example.unimarket.domain.usecase.wallet

import com.example.unimarket.domain.model.WalletLedgerEntry
import com.example.unimarket.domain.repository.WalletLedgerRepository
import javax.inject.Inject

class GetWalletLedgerUseCase @Inject constructor(
    private val walletLedgerRepository: WalletLedgerRepository
) {
    suspend operator fun invoke(limit: Int = 50): Result<List<WalletLedgerEntry>> {
        return walletLedgerRepository.getRecent(limit)
    }
}

