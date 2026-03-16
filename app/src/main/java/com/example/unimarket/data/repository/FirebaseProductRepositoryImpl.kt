package com.example.unimarket.data.repository

import android.util.Log
import com.example.unimarket.domain.model.Category
import com.example.unimarket.domain.model.deliveryMethodsFromStorage
import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.model.toStorageValue
import com.example.unimarket.domain.repository.ProductRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseProductRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ProductRepository {

    override fun getCategories(): Flow<List<Category>> {
        // Keeping mocked categories for consistency with UI for now
        return flowOf(
            listOf(
                Category("1", "All Items"),
                Category("2", "Electronics"),
                Category("3", "Textbooks"),
                Category("4", "Furniture")
            )
        )
    }

    override fun getRecommendedProducts(): Flow<List<Product>> = flow {
        val snapshot = firestore.collection("products")
            .get(Source.SERVER)
            .await()
        emit(snapshot.documents.mapNotNull(::mapProduct))
    }

    override suspend fun addProduct(product: Product): Result<Unit> {
        return try {
            val productMap = mapOf(
                "name" to product.name,
                "price" to product.price,
                "description" to product.description,
                "imageUrls" to product.imageUrls,
                "categoryId" to product.categoryId,
                "condition" to product.condition,
                "sellerName" to product.sellerName,
                "rating" to product.rating,
                "location" to product.location,
                "timeAgo" to product.timeAgo,
                "isFavorite" to product.isFavorite,
                "isNegotiable" to product.isNegotiable,
                "userId" to product.userId,
                "specifications" to product.specifications,
                "deliveryMethodsAvailable" to product.deliveryMethodsAvailable.map { it.toStorageValue() }
            )
            firestore.collection("products").add(productMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            firestore.collection("products").document(productId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProduct(product: Product): Result<Unit> {
        return try {
            val productMap = mapOf(
                "name" to product.name,
                "price" to product.price,
                "description" to product.description,
                "imageUrls" to product.imageUrls,
                "categoryId" to product.categoryId,
                "condition" to product.condition,
                "sellerName" to product.sellerName,
                "rating" to product.rating,
                "location" to product.location,
                "timeAgo" to product.timeAgo,
                "isFavorite" to product.isFavorite,
                "isNegotiable" to product.isNegotiable,
                "userId" to product.userId,
                "specifications" to product.specifications,
                "deliveryMethodsAvailable" to product.deliveryMethodsAvailable.map { it.toStorageValue() }
            )
            // Using set() to overwrite or create if not exists
            firestore.collection("products").document(product.id).set(productMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapProduct(doc: DocumentSnapshot): Product? {
        return try {
            Product(
                id = doc.id,
                name = doc.getString("name") ?: "",
                price = doc.getDouble("price") ?: 0.0,
                description = doc.getString("description") ?: "",
                imageUrls = (doc.get("imageUrls") as? List<String>) ?: emptyList(),
                categoryId = doc.getString("categoryId") ?: "",
                condition = doc.getString("condition") ?: "",
                sellerName = doc.getString("sellerName") ?: "",
                rating = doc.getDouble("rating") ?: 0.0,
                location = doc.getString("location") ?: "",
                timeAgo = doc.getString("timeAgo") ?: "",
                isFavorite = doc.getBoolean("isFavorite") ?: false,
                isNegotiable = doc.getBoolean("isNegotiable") ?: false,
                userId = doc.getString("userId") ?: "",
                specifications = (doc.get("specifications") as? Map<String, String>) ?: emptyMap(),
                deliveryMethodsAvailable = deliveryMethodsFromStorage(
                    (doc.get("deliveryMethodsAvailable") as? List<String>) ?: emptyList()
                )
            )
        } catch (e: Exception) {
            Log.w("FirebaseProductRepo", "Failed to map product ${doc.id}", e)
            null
        }
    }
}
