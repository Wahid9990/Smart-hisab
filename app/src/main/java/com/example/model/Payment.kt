package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val customerId: String = "",
    val amount: Double = 0.0,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "customerId" to customerId,
            "amount" to amount,
            "note" to note,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): Payment {
            return Payment(
                id = map["id"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                customerId = map["customerId"] as? String ?: "",
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                note = map["note"] as? String,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
