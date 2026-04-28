package com.example.unimarket.data.repository

import android.util.Log
import com.example.unimarket.domain.model.AppNotification
import com.example.unimarket.domain.repository.NotificationRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseNotificationRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : NotificationRepository {

    override suspend fun getNotifications(limit: Int): Result<List<AppNotification>> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
        val safeLimit = limit.coerceIn(1, 100)
        val perSourceLimit = safeLimit * 2

        val userScopedResult = runCatching {
            firestore.collection("users")
                .document(userId)
                .collection("notifications")
                .limit(perSourceLimit.toLong())
                .get()
                .await()
        }

        val globalResult = runCatching {
            firestore.collection("notifications")
                .whereEqualTo("receiverId", userId)
                .limit(perSourceLimit.toLong())
                .get()
                .await()
        }

        userScopedResult.exceptionOrNull()?.let { error ->
            Log.w(TAG, "Failed to load user-scoped notifications for userId=$userId", error)
        }
        globalResult.exceptionOrNull()?.let { error ->
            Log.w(TAG, "Failed to load global notifications for userId=$userId", error)
        }

        if (userScopedResult.isFailure && globalResult.isFailure) {
            val userError = userScopedResult.exceptionOrNull()
            val globalError = globalResult.exceptionOrNull()
            val fallbackMessage = globalError?.message ?: userError?.message ?: "Failed to load notifications"
            return Result.failure(Exception(fallbackMessage))
        }

        val merged = (userScopedResult.getOrNull().safeDocuments() + globalResult.getOrNull().safeDocuments())
            .mapNotNull { document -> mapNotification(document) }
            .groupBy { it.id }
            .mapNotNull { (_, items) -> items.maxByOrNull { it.createdAt } }
            .sortedByDescending { it.createdAt }
            .take(safeLimit)

        Log.d(
            TAG,
            "Loaded notifications userId=$userId userScoped=${userScopedResult.getOrNull().safeDocuments().size} " +
                "global=${globalResult.getOrNull().safeDocuments().size} merged=${merged.size}"
        )

        return Result.success(merged)
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
        if (notificationId.isBlank()) return Result.failure(Exception("Notification id is required"))

        val userScopedResult = runCatching {
            firestore.collection("users")
                .document(userId)
                .collection("notifications")
                .document(notificationId)
                .delete()
                .await()
        }

        val globalResult = runCatching {
            firestore.collection("notifications")
                .document(notificationId)
                .delete()
                .await()
        }

        if (userScopedResult.isSuccess || globalResult.isSuccess) {
            return Result.success(Unit)
        }

        val error = userScopedResult.exceptionOrNull() ?: globalResult.exceptionOrNull()
        return Result.failure(Exception(error?.message ?: "Failed to delete notification"))
    }

    private fun mapNotification(document: DocumentSnapshot): AppNotification? {
        val title = document.getString("title")
            ?.trim()
            .orEmpty()
            .ifBlank { "UniMarket" }
        val body = document.getString("body")
            ?.trim()
            .orEmpty()
            .ifBlank {
                document.getString("message")
                    ?.trim()
                    .orEmpty()
                    .ifBlank { document.getString("content")?.trim().orEmpty() }
            }
        val createdAt = document.get("createdAt").toMillis()
            .takeIf { it > 0L }
            ?: document.get("sentAt").toMillis()
                .takeIf { it > 0L }
            ?: document.get("timestamp").toMillis()
                .takeIf { it > 0L }
            ?: 0L

        return AppNotification(
            id = document.id,
            title = title,
            body = body,
            createdAt = createdAt,
            isRead = document.getBoolean("isRead") ?: document.getBoolean("read") ?: false
        )
    }
}

private fun QuerySnapshot?.safeDocuments(): List<DocumentSnapshot> {
    return this?.documents ?: emptyList()
}

private const val TAG = "NotifRepository"

private fun Any?.toMillis(): Long {
    return when (this) {
        is Timestamp -> this.toDate().time
        is Number -> this.toLong()
        is String -> this.toLongOrNull() ?: 0L
        else -> 0L
    }
}
