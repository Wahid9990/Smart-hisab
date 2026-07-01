package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "",
    val purchasePrice: Double = 0.0,
    val salePrice: Double = 0.0,
    val stockQuantity: Int = 0,
    val lowStockLimit: Int = 5,
    val barcode: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "name" to name,
            "category" to category,
            "purchasePrice" to purchasePrice,
            "salePrice" to salePrice,
            "stockQuantity" to stockQuantity,
            "lowStockLimit" to lowStockLimit,
            "barcode" to barcode,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "isActive" to isActive
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): Product {
            return Product(
                id = map["id"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                name = map["name"] as? String ?: "",
                category = map["category"] as? String ?: "",
                purchasePrice = (map["purchasePrice"] as? Number)?.toDouble() ?: 0.0,
                salePrice = (map["salePrice"] as? Number)?.toDouble() ?: 0.0,
                stockQuantity = (map["stockQuantity"] as? Number)?.toInt() ?: 0,
                lowStockLimit = (map["lowStockLimit"] as? Number)?.toInt() ?: 5,
                barcode = map["barcode"] as? String,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isActive = map["isActive"] as? Boolean ?: true
            )
        }
    }
}
