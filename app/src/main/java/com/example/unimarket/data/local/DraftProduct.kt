package com.example.unimarket.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.unimarket.domain.model.DeliveryMethod
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "drafts")
data class DraftProduct(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val price: Double?, // Nullable for drafts that don't have a price yet
    val imageUrls: List<String>,
    val description: String,
    val categoryId: String,
    val condition: String,
    val quantityAvailable: Int?,
    val isNegotiable: Boolean,
    val specifications: Map<String, String>,
    val deliveryMethodsAvailable: List<DeliveryMethod>,
    val lastModified: Long = System.currentTimeMillis()
)

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(value, listType) ?: emptyList()
        } catch(e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return try {
            gson.fromJson(value, mapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun fromDeliveryMethodList(value: List<DeliveryMethod>?): String {
        return gson.toJson(value ?: emptyList<DeliveryMethod>())
    }

    @TypeConverter
    fun toDeliveryMethodList(value: String): List<DeliveryMethod> {
        val listType = object : TypeToken<List<DeliveryMethod>>() {}.type
        return try {
            gson.fromJson(value, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
