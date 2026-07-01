package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String? = null,
    val address: String? = null,
    val totalCredit: Double = 0.0,
    val totalPaid: Double = 0.0,
    val balance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "name" to name,
            "phone" to phone,
            "address" to address,
            "totalCredit" to totalCredit,
            "totalPaid" to totalPaid,
            "balance" to balance,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): Customer {
            return Customer(
                id = map["id"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                name = map["name"] as? String ?: "",
                phone = map["phone"] as? String,
                address = map["address"] as? String,
                totalCredit = (map["totalCredit"] as? Number)?.toDouble() ?: 0.0,
                totalPaid = (map["totalPaid"] as? Number)?.toDouble() ?: 0.0,
                balance = (map["balance"] as? Number)?.toDouble() ?: 0.0,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
