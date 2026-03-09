package com.example.unimarket.data.repository

import com.example.unimarket.domain.model.Category
import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class FakeProductRepositoryImpl @Inject constructor() : ProductRepository {
    override fun getCategories(): Flow<List<Category>> {
        return flowOf(
            listOf(
                Category("1", "All Items"),
                Category("2", "Electronics"),
                Category("3", "Textbooks"),
                Category("4", "Furniture")
            )
        )
    }

    override fun getRecommendedProducts(): Flow<List<Product>> {
        return flowOf(
            listOf(
                Product(
                    id = "1",
                    name = "Apple Watch Series 5 - Space Gray",
                    price = 120.00,
                    imageUrls = listOf("https://picsum.photos/seed/watch/200/200"),
                    categoryId = "2",
                    condition = "Used",
                    sellerName = "Senior",
                    rating = 4.8,
                    location = "Campus North",
                    timeAgo = "1 hour ago",
                    isFavorite = true,
                    isNegotiable = false,
                    userId = "fakeUser1"
                ),
                Product(
                    id = "2",
                    name = "Calculus: Early Transcendentals 8th Edition",
                    price = 45.00,
                    imageUrls = listOf("https://picsum.photos/seed/book/200/200"),
                    categoryId = "3",
                    condition = "Used",
                    sellerName = "Junior",
                    rating = 4.9,
                    location = "Library",
                    timeAgo = "5 hours ago",
                    isFavorite = false,
                    isNegotiable = true,
                    userId = "fakeUser2"
                ),
                Product(
                    id = "3",
                    name = "Minimalist LED Desk Lamp",
                    price = 15.00,
                    imageUrls = listOf("https://picsum.photos/seed/lamp2/200/200"),
                    categoryId = "4",
                    condition = "Used",
                    sellerName = "Sophomore",
                    rating = 5.0,
                    location = "Dorm A",
                    timeAgo = "1 day ago",
                    isFavorite = true,
                    isNegotiable = false,
                    userId = "fakeUser3"
                ),
                Product(
                    id = "4",
                    name = "Sony Noise Cancelling Headphones",
                    price = 80.00,
                    imageUrls = listOf("https://picsum.photos/seed/headphones/200/200"),
                    categoryId = "2",
                    condition = "Used",
                    sellerName = "Freshman",
                    rating = 4.2,
                    location = "Student Center",
                    timeAgo = "2d ago",
                    isFavorite = false,
                    isNegotiable = true,
                    userId = "fakeUser4"
                )
            )
        )
    }

    override suspend fun addProduct(product: Product): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun deleteProduct(productId: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun updateProduct(product: Product): Result<Unit> {
        return Result.success(Unit)
    }
}
