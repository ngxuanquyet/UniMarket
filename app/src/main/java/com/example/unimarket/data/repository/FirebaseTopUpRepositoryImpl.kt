package com.example.unimarket.data.repository

import android.util.Log
import androidx.core.text.isDigitsOnly
import com.example.unimarket.BuildConfig
import com.example.unimarket.data.api.SepayApiService
import com.example.unimarket.domain.model.TopUpPaymentCheckResult
import com.example.unimarket.domain.model.TopUpPaymentStatus
import com.example.unimarket.domain.repository.TopUpRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToLong
import javax.inject.Inject

class FirebaseTopUpRepositoryImpl @Inject constructor(
    private val sepayApiService: SepayApiService,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : TopUpRepository {

    override suspend fun checkTransferAndCreditTopUp(
        amount: Long,
        transferContent: String
    ): Result<TopUpPaymentCheckResult> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("No user logged in"))
        if (amount <= 0L) return Result.failure(Exception("Invalid top-up amount"))
        if (transferContent.isBlank()) return Result.failure(Exception("Missing transfer content"))
        if (BuildConfig.SEPAY_API_KEY.isBlank()) {
            return Result.failure(Exception("Sepay API key is not configured"))
        }

        return runCatching {
            val response = sepayApiService.getTransactions(
                authorization = "Bearer ${BuildConfig.SEPAY_API_KEY}",
                accountNumber = APP_TRANSFER_ACCOUNT_NUMBER,
                limit = 20
            )
            if (!response.isSuccessful) {
                error(response.errorMessage() ?: "Payment check failed with code ${response.code()}")
            }

            val transactions = response.body()?.transactions.orEmpty()
            Log.d(
                TAG,
                "SePay returned ${transactions.size} transaction(s), expectedAmount=$amount, expectedContent=$transferContent"
            )
            transactions.take(20).forEachIndexed { index, tx ->
                Log.d(
                    TAG,
                    "tx[$index]: id=${tx.id}, amount_in=${tx.amount_in}, amount_out=${tx.amount_out}, content=${tx.transaction_content}, code=${tx.code}, ref=${tx.reference_number}, date=${tx.transaction_date}"
                )
            }

            val expectedContent = transferContent.normalizeForPaymentMatch()
            val matchedTransaction = transactions
                .firstOrNull { transaction ->
                    val incomingAmount = transaction.amount_in.toMoneyLong()
                    if (incomingAmount <= 0L || incomingAmount != amount) return@firstOrNull false
                    val candidates = listOf(
                        transaction.transaction_content,
                        transaction.code.orEmpty(),
                        transaction.reference_number.orEmpty()
                    ).map { it.normalizeForPaymentMatch() }
                        .filter { it.isNotBlank() }

                    candidates.any { candidate ->
                        isTransferContentMatch(
                            expectedContent = expectedContent,
                            candidateContent = candidate
                        )
                    }
                }

            if (matchedTransaction == null) {
                return@runCatching TopUpPaymentCheckResult(status = TopUpPaymentStatus.PENDING)
            }

            val userRef = firestore.collection(USERS_COLLECTION).document(currentUser.uid)
            val topUpTransactionId = "topup_${transferContent.toWalletTransactionKey()}"
            val walletTransactionRef = userRef
                .collection(WALLET_TRANSACTIONS_COLLECTION)
                .document(topUpTransactionId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val existingWalletTransaction = transaction.get(walletTransactionRef)

                if (!existingWalletTransaction.exists()) {
                    if (snapshot.exists()) {
                        transaction.update(userRef, "walletBalance", FieldValue.increment(amount.toDouble()))
                    } else {
                        transaction.set(userRef, mapOf("walletBalance" to amount.toDouble()))
                    }
                    transaction.set(
                        walletTransactionRef,
                        mapOf(
                            "type" to "TOP_UP",
                            "status" to "COMPLETED",
                            "amount" to amount.toDouble(),
                            "currency" to "VND",
                            "title" to "Wallet top-up",
                            "transferContent" to transferContent,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                            "source" to "ANDROID_APP"
                        )
                    )
                }
            }.await()

            TopUpPaymentCheckResult(status = TopUpPaymentStatus.CONFIRMED)
        }
    }

    private fun String.normalizeForPaymentMatch(): String {
        val trimmed = trim().lowercase()
        if (trimmed.isBlank()) return ""
        val builder = StringBuilder(trimmed.length)
        trimmed.forEach { ch ->
            if (ch.isLetterOrDigit()) builder.append(ch)
        }
        return builder.toString().trim()
    }

    private fun String.toMoneyLong(): Long {
        val raw = trim().replace("\\s+".toRegex(), "")
        if (raw.isBlank()) return 0L
        if (raw.isDigitsOnly()) return raw.toLongOrNull() ?: 0L

        val lastComma = raw.lastIndexOf(',')
        val lastDot = raw.lastIndexOf('.')
        val normalized = when {
            lastComma >= 0 && lastDot >= 0 -> {
                // Keep the right-most separator as decimal separator.
                if (lastComma > lastDot) {
                    raw.replace(".", "").replace(',', '.')
                } else {
                    raw.replace(",", "")
                }
            }

            lastComma >= 0 -> {
                val fractionalLength = raw.length - lastComma - 1
                if (fractionalLength in 1..2) {
                    raw.replace(',', '.')
                } else {
                    raw.replace(",", "")
                }
            }

            lastDot >= 0 -> {
                val fractionalLength = raw.length - lastDot - 1
                if (fractionalLength in 1..2) {
                    raw
                } else {
                    raw.replace(".", "")
                }
            }

            else -> raw
        }

        return normalized.toDoubleOrNull()?.roundToLong() ?: 0L
    }

    private fun isTransferContentMatch(
        expectedContent: String,
        candidateContent: String
    ): Boolean {
        if (expectedContent.isBlank() || candidateContent.isBlank()) return false
        if (candidateContent.contains(expectedContent)) return true
        if (expectedContent.contains(candidateContent) && candidateContent.length >= MIN_REFERENCE_MATCH_LENGTH) {
            // Some banks truncate transfer content; accept strong prefix-style partial match.
            return true
        }
        return false
    }

    private fun retrofit2.Response<*>.errorMessage(): String? {
        val rawBody = errorBody()?.string().orEmpty()
        if (rawBody.isBlank()) return message().takeIf { it.isNotBlank() }
        return message().takeIf { it.isNotBlank() } ?: rawBody
    }

    private companion object {
        const val TAG = "TopUpSePayCheck"
        const val APP_TRANSFER_ACCOUNT_NUMBER = "0356433860"
        const val USERS_COLLECTION = "users"
        const val WALLET_TRANSACTIONS_COLLECTION = "walletTransactions"
        const val MIN_REFERENCE_MATCH_LENGTH = 12
    }
}

private fun String.toWalletTransactionKey(): String {
    val normalized = lowercase().filter { it.isLetterOrDigit() }
    return normalized.takeLast(48).ifBlank { System.currentTimeMillis().toString() }
}
