package com.example.unimarket.data.repository

import com.example.unimarket.domain.model.PayoutRequestStatus
import com.example.unimarket.domain.model.SellerPaymentMethod
import com.example.unimarket.domain.repository.PayoutRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebasePayoutRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : PayoutRepository {

    override suspend fun createWithdrawalRequest(
        amount: Long,
        receiverMethod: SellerPaymentMethod
    ): Result<String> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("No user logged in"))
        if (amount <= 0L) return Result.failure(Exception("Invalid withdrawal amount"))
        if (receiverMethod.id.isBlank()) {
            return Result.failure(Exception("Missing receiver method"))
        }

        return runCatching {
            val userRef = firestore.collection(USERS_COLLECTION).document(currentUser.uid)
            val payoutRef = firestore.collection(PAYOUT_REQUESTS_COLLECTION).document()
            val walletTxRef = userRef
                .collection(WALLET_TRANSACTIONS_COLLECTION)
                .document("withdraw_${payoutRef.id}")
            val amountAsDouble = amount.toDouble()

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                if (!snapshot.exists()) {
                    throw IllegalStateException("User profile not found")
                }

                val userData = snapshot.data.orEmpty()
                val walletBalance = (userData["walletBalance"] as? Number)?.toDouble() ?: 0.0
                if (amountAsDouble > walletBalance) {
                    throw IllegalStateException("Insufficient balance")
                }

                transaction.update(userRef, "walletBalance", FieldValue.increment(-amountAsDouble))
                transaction.set(
                    payoutRef,
                    mapOf(
                        "sellerId" to currentUser.uid,
                        "requesterId" to currentUser.uid,
                        "amount" to amountAsDouble,
                        "currency" to "VND",
                        "status" to PayoutRequestStatus.PENDING.name,
                        "source" to "ANDROID_APP",
                        "type" to "WITHDRAWAL",
                        "receiverMethod" to receiverMethod.toFirestoreMap(),
                        "receiverMethodId" to receiverMethod.id,
                        "receiverMethodType" to receiverMethod.type.storageValue,
                        "receiverMethodLabel" to receiverMethod.displayTitle,
                        "receiverMethodSubtitle" to receiverMethod.shortSubtitle,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "requestedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    walletTxRef,
                    mapOf(
                        "type" to "WITHDRAW",
                        "status" to PayoutRequestStatus.PENDING.name,
                        "amount" to amountAsDouble,
                        "currency" to "VND",
                        "title" to "Wallet withdrawal",
                        "payoutRequestId" to payoutRef.id,
                        "receiverMethod" to receiverMethod.toFirestoreMap(),
                        "receiverMethodId" to receiverMethod.id,
                        "receiverMethodType" to receiverMethod.type.storageValue,
                        "receiverMethodLabel" to receiverMethod.displayTitle,
                        "receiverMethodSubtitle" to receiverMethod.shortSubtitle,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "source" to "ANDROID_APP"
                    )
                )
                payoutRef.id
            }.await()
        }
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val PAYOUT_REQUESTS_COLLECTION = "payoutRequests"
        const val WALLET_TRANSACTIONS_COLLECTION = "walletTransactions"
    }
}

private fun SellerPaymentMethod.toFirestoreMap(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "type" to type.storageValue,
        "label" to label,
        "accountName" to accountName,
        "accountNumber" to accountNumber,
        "bankCode" to bankCode,
        "bankName" to bankName,
        "phoneNumber" to phoneNumber,
        "note" to note,
        "isDefault" to isDefault
    )
}
