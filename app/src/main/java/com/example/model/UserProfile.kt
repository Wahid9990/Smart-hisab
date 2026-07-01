package com.example.model

data class UserProfile(
    val uid: String = "",
    val shopName: String = "",
    val ownerName: String = "",
    val email: String = "",
    val phone: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val currency: String = "Rs",
    val language: String = "en"
) {
    // Convert to Map for Firestore
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "shopName" to shopName,
            "ownerName" to ownerName,
            "email" to email,
            "phone" to phone,
            "createdAt" to createdAt,
            "currency" to currency,
            "language" to language
        )
    }

    companion object {
        // Build from Firestore Map safely
        fun fromMap(map: Map<String, Any?>): UserProfile {
            return UserProfile(
                uid = map["uid"] as? String ?: "",
                shopName = map["shopName"] as? String ?: "",
                ownerName = map["ownerName"] as? String ?: "",
                email = map["email"] as? String ?: "",
                phone = map["phone"] as? String,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                currency = map["currency"] as? String ?: "Rs",
                language = map["language"] as? String ?: "en"
            )
        }
    }
}
