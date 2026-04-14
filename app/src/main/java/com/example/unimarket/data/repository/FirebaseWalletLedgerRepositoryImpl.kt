package com.example.unimarket.data.repository

import com.example.unimarket.domain.model.WalletLedgerEntry
import com.example.unimarket.domain.repository.WalletLedgerRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseWalletLedgerRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : WalletLedgerRepository {

    override suspend fun getRecent(limit: Int): Result<List<WalletLedgerEntry>> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("No user logged in"))
        val safeLimit = limit.coerceIn(1, 100)

        return runCatching {
            firestore.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .collection(WALLET_TRANSACTIONS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(safeLimit.toLong())
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val data = doc.data.orEmpty()
                    val type = (data["type"] as? String).orEmpty().trim().uppercase()
                    val status = (data["status"] as? String).orEmpty().trim().uppercase()
                    val amount = (data["amount"] as? Number)?.toDouble() ?: 0.0
                    if (type.isBlank() || amount <= 0.0) return@mapNotNull null

                    WalletLedgerEntry(
                        id = doc.id,
                        type = type,
                        status = status.ifBlank { "PENDING" },
                        amount = amount,
                        title = (data["title"] as? String).orEmpty().trim(),
                        createdAt = data["createdAt"].toMillis(),
                        updatedAt = data["updatedAt"].toMillis()
                    )
                }
        }
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val WALLET_TRANSACTIONS_COLLECTION = "walletTransactions"
    }
}

private fun Any?.toMillis(): Long {
    return when (this) {
        is Timestamp -> this.toDate().time
        is Number -> this.toLong()
        else -> 0L
    }
}

