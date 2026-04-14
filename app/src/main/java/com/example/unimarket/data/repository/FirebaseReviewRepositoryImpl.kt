package com.example.unimarket.data.repository

import com.example.unimarket.domain.model.Order
import com.example.unimarket.domain.model.OrderStatus
import com.example.unimarket.domain.model.Review
import com.example.unimarket.domain.repository.ReviewRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseReviewRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ReviewRepository {

    override suspend fun getBuyerReviews(): Result<List<Review>> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("No user logged in"))

        return try {
            val snapshot = firestore.collection(REVIEWS_COLLECTION)
                .whereEqualTo("buyerId", userId)
                .get()
                .await()

            Result.success(snapshot.documents.mapNotNull { document ->
                val rating = document.getLong("rating")?.toInt() ?: return@mapNotNull null
                Review(
                    orderId = document.id,
                    buyerId = document.getString("buyerId").orEmpty(),
                    sellerId = document.getString("sellerId").orEmpty(),
                    productId = document.getString("productId").orEmpty(),
                    productName = document.getString("productName").orEmpty(),
                    rating = rating,
                    comment = document.getString("comment").orEmpty(),
                    createdAt = document.getLong("createdAt") ?: 0L
                )
            })
        } catch (e: Exception) {
            Result.failure(e.toReviewError())
        }
    }

    override suspend fun getSellerReviews(sellerId: String): Result<List<Review>> {
        if (sellerId.isBlank()) return Result.success(emptyList())

        return try {
            val snapshot = firestore.collection(REVIEWS_COLLECTION)
                .whereEqualTo("sellerId", sellerId)
                .get()
                .await()

            val reviews = snapshot.documents.mapNotNull { document ->
                val rating = document.getLong("rating")?.toInt() ?: return@mapNotNull null
                Review(
                    orderId = document.id,
                    buyerId = document.getString("buyerId").orEmpty(),
                    sellerId = document.getString("sellerId").orEmpty(),
                    productId = document.getString("productId").orEmpty(),
                    productName = document.getString("productName").orEmpty(),
                    rating = rating,
                    comment = document.getString("comment").orEmpty(),
                    createdAt = document.getLong("createdAt") ?: 0L
                )
            }.sortedByDescending { it.createdAt }

            Result.success(reviews)
        } catch (e: Exception) {
            Result.failure(e.toReviewError())
        }
    }

    override suspend fun submitReview(order: Order, rating: Int, comment: String): Result<Review> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("No user logged in"))
        if (currentUser.uid != order.buyerId) {
            return Result.failure(Exception("You can only review your own order"))
        }
        if (order.status != OrderStatus.DELIVERED) {
            return Result.failure(Exception("You can only review delivered orders"))
        }
        if (rating !in 1..5) {
            return Result.failure(Exception("Please select a rating from 1 to 5"))
        }

        val trimmedComment = comment.trim()
        val createdAt = System.currentTimeMillis()
        val review = Review(
            orderId = order.id,
            buyerId = order.buyerId,
            sellerId = order.sellerId,
            productId = order.productId,
            productName = order.productName,
            rating = rating,
            comment = trimmedComment,
            createdAt = createdAt
        )

        return try {
            firestore.runTransaction { transaction ->
                val reviewRef = firestore.collection(REVIEWS_COLLECTION).document(order.id)
                val sellerRef = firestore.collection(USERS_COLLECTION).document(order.sellerId)

                val existingReview = transaction.get(reviewRef)
                if (existingReview.exists()) {
                    throw IllegalStateException("You already reviewed this order")
                }

                val sellerSnapshot = transaction.get(sellerRef)
                val currentRatingCount = sellerSnapshot.getLong("ratingCount")?.toInt() ?: 0
                val currentAverageRating = sellerSnapshot.getDouble("averageRating") ?: 0.0
                val newRatingCount = currentRatingCount + 1
                val newAverageRating =
                    ((currentAverageRating * currentRatingCount) + rating) / newRatingCount

                transaction.set(
                    reviewRef,
                    mapOf(
                        "buyerId" to review.buyerId,
                        "sellerId" to review.sellerId,
                        "productId" to review.productId,
                        "productName" to review.productName,
                        "rating" to review.rating,
                        "comment" to review.comment,
                        "createdAt" to review.createdAt
                    )
                )
                transaction.set(
                    sellerRef,
                    mapOf(
                        "averageRating" to newAverageRating,
                        "ratingCount" to newRatingCount
                    ),
                    SetOptions.merge()
                )
            }.await()

            Result.success(review)
        } catch (e: Exception) {
            Result.failure(e.toReviewError())
        }
    }

    private fun Exception.toReviewError(): Exception {
        if (this is FirebaseFirestoreException &&
            code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        ) {
            return Exception(
                "Rating is blocked by Firestore permissions. Add rules for reviews and seller rating updates in Firebase Console."
            )
        }
        return this
    }

    private companion object {
        const val REVIEWS_COLLECTION = "reviews"
        const val USERS_COLLECTION = "users"
    }
}
