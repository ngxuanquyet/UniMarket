package com.example.unimarket.data.repository
import com.example.unimarket.domain.model.ChatMessage
import com.example.unimarket.domain.model.ChatUser
import com.example.unimarket.domain.model.Conversation
import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.inject.Inject

class FirebaseChatRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ChatRepository {

    override fun observeConversations(currentUserId: String): Flow<List<Conversation>> = callbackFlow {
        val registration = firestore.collection(CONVERSATIONS_COLLECTION)
            .whereArrayContains("participantIds", currentUserId)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                launch {
                    val conversations = snapshot?.documents?.mapNotNull { doc ->
                        mapConversation(doc, currentUserId)
                    }.orEmpty()
                    trySend(conversations)
                }
            }

        awaitClose { registration.remove() }
    }

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> = callbackFlow {
        val registration = messagesCollection(conversationId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    mapMessage(doc, conversationId)
                }.orEmpty()
                trySend(messages)
            }

        awaitClose { registration.remove() }
    }

    override suspend fun createOrGetConversation(product: Product): Result<String> {
        return try {
            val buyer = auth.currentUser ?: return Result.failure(Exception("Please log in to chat"))
            if (buyer.uid == product.userId) {
                return Result.failure(Exception("You cannot chat with yourself"))
            }

            val conversationId = buildConversationId(product.id, buyer.uid, product.userId)
            val conversationRef = firestore.collection(CONVERSATIONS_COLLECTION).document(conversationId)

            val buyerInfo = readUserInfoOrFallback(
                userId = buyer.uid,
                fallbackName = buyer.displayName ?: "Buyer",
                fallbackAvatarUrl = buyer.authAvatarUrl().ifBlank {
                    buildAvatarFallbackUrl(buyer.displayName ?: "Buyer")
                }
            )
            val sellerInfo = readUserInfoOrFallback(
                userId = product.userId,
                fallbackName = product.sellerName.ifBlank { "Seller" },
                fallbackAvatarUrl = getAvatarUrlByUserId(product.userId).ifBlank {
                    buildAvatarFallbackUrl(product.sellerName.ifBlank { "Seller" })
                }
            )

            val createdAt = Date()
            val conversationData = mapOf(
                "productId" to product.id,
                "productName" to product.name,
                "productImageUrl" to product.imageUrls.firstOrNull().orEmpty(),
                "participantIds" to listOf(buyer.uid, product.userId),
                "participants" to mapOf(
                    buyer.uid to mapOf(
                        "name" to buyerInfo.name,
                        "avatarUrl" to buyerInfo.avatarUrl
                    ),
                    product.userId to mapOf(
                        "name" to sellerInfo.name,
                        "avatarUrl" to sellerInfo.avatarUrl
                    )
                ),
                "lastMessage" to "",
                "lastMessageAt" to createdAt,
                "lastSenderId" to "",
                "createdAt" to createdAt
            )
            conversationRef.set(conversationData, com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(conversationId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendMessage(conversationId: String, text: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Please log in to send messages"))
            val trimmedText = text.trim()
            if (trimmedText.isBlank()) {
                return Result.failure(Exception("Message cannot be empty"))
            }

            val senderInfo = readUserInfoOrFallback(
                userId = currentUser.uid,
                fallbackName = currentUser.displayName ?: "User",
                fallbackAvatarUrl = currentUser.authAvatarUrl().ifBlank {
                    buildAvatarFallbackUrl(currentUser.displayName ?: "User")
                }
            )
            val messageRef = messagesCollection(conversationId).document()
            val sentAt = Date()

            firestore.runBatch { batch ->
                batch.set(
                    messageRef,
                    mapOf(
                        "senderId" to currentUser.uid,
                        "senderName" to senderInfo.name,
                        "senderAvatarUrl" to senderInfo.avatarUrl,
                        "text" to trimmedText,
                        "createdAt" to sentAt
                    )
                )
                batch.update(
                    firestore.collection(CONVERSATIONS_COLLECTION).document(conversationId),
                    mapOf(
                        "lastMessage" to trimmedText,
                        "lastMessageAt" to sentAt,
                        "lastSenderId" to currentUser.uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun readUserInfoOrFallback(
        userId: String,
        fallbackName: String,
        fallbackAvatarUrl: String
    ): ChatUser {
        return try {
            val userDoc = firestore.collection(USERS_COLLECTION).document(userId).get().await()
            val resolvedName = userDoc.getString("name").orEmpty()
                .ifBlank { userDoc.getString("displayName").orEmpty() }
                .ifBlank { fallbackName }
            val resolvedAvatarUrl = userDoc.getString("avatarUrl").orEmpty()
                .ifBlank { fallbackAvatarUrl }
                .ifBlank { buildAvatarFallbackUrl(resolvedName) }
            ChatUser(
                id = userId,
                name = resolvedName,
                avatarUrl = resolvedAvatarUrl
            )
        } catch (_: Exception) {
            ChatUser(
                id = userId,
                name = fallbackName,
                avatarUrl = fallbackAvatarUrl.ifBlank { buildAvatarFallbackUrl(fallbackName) }
            )
        }
    }

    private suspend fun getAvatarUrlByUserId(userId: String): String {
        return try {
            val userDoc = firestore.collection(USERS_COLLECTION).document(userId).get().await()
            userDoc.getString("avatarUrl").orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    private suspend fun mapConversation(doc: DocumentSnapshot, currentUserId: String): Conversation? {
        val participantIds = (doc.get("participantIds") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
        if (participantIds.isEmpty()) return null

        val otherUserId = participantIds.firstOrNull { it != currentUserId } ?: return null
        val participants = doc.get("participants") as? Map<*, *>
        val otherUserMap = participants?.get(otherUserId) as? Map<*, *>
        val otherUserName = (otherUserMap?.get("name") as? String).orEmpty().ifBlank { "User" }
        val otherUserAvatarUrl = (otherUserMap?.get("avatarUrl") as? String).orEmpty()
        val otherUser = readUserInfoOrFallback(
            userId = otherUserId,
            fallbackName = otherUserName,
            fallbackAvatarUrl = otherUserAvatarUrl.ifBlank {
                buildAvatarFallbackUrl(otherUserName)
            }
        )

        return Conversation(
            id = doc.id,
            productId = doc.getString("productId").orEmpty(),
            productName = doc.getString("productName").orEmpty(),
            productImageUrl = doc.getString("productImageUrl").orEmpty(),
            participantIds = participantIds,
            otherUser = otherUser,
            lastMessage = doc.getString("lastMessage").orEmpty(),
            lastMessageAt = doc.getDate("lastMessageAt")?.time ?: 0L,
            lastSenderId = doc.getString("lastSenderId").orEmpty()
        )
    }

    private fun mapMessage(doc: DocumentSnapshot, conversationId: String): ChatMessage? {
        val senderId = doc.getString("senderId").orEmpty()
        if (senderId.isBlank()) return null

        return ChatMessage(
            id = doc.id,
            conversationId = conversationId,
            senderId = senderId,
            senderName = doc.getString("senderName").orEmpty(),
            senderAvatarUrl = doc.getString("senderAvatarUrl").orEmpty(),
            text = doc.getString("text").orEmpty(),
            createdAt = doc.getDate("createdAt")?.time ?: 0L
        )
    }

    private fun messagesCollection(conversationId: String) =
        firestore.collection(CONVERSATIONS_COLLECTION).document(conversationId).collection(MESSAGES_COLLECTION)

    private fun buildConversationId(productId: String, buyerId: String, sellerId: String): String {
        return "${productId}_${buyerId}_${sellerId}"
    }

    private fun buildAvatarFallbackUrl(name: String): String {
        val encodedName = URLEncoder.encode(name.ifBlank { "User" }, StandardCharsets.UTF_8.toString())
        return "https://ui-avatars.com/api/?name=$encodedName&background=random"
    }

    private fun com.google.firebase.auth.FirebaseUser.authAvatarUrl(): String {
        return photoUrl?.toString().orEmpty()
    }

    private companion object {
        const val CONVERSATIONS_COLLECTION = "conversations"
        const val MESSAGES_COLLECTION = "messages"
        const val USERS_COLLECTION = "users"
    }
}
