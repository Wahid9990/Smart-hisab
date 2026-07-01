package com.example.model

data class Expense(
    val id: String = "",
    val userId: String = "",
    val category: String = "",
    val amount: Double = 0.0,
    val note: String? = null,
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "category" to category,
            "amount" to amount,
            "note" to note,
            "date" to date,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): Expense {
            return Expense(
                id = map["id"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                category = map["category"] as? String ?: "",
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                note = map["note"] as? String,
                date = (map["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
