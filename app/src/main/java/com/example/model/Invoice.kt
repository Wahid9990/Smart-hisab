package com.example.model

data class Invoice(
    val id: String = "",
    val invoiceNumber: String = "",
    val customerId: String? = null,
    val customerName: String? = null,
    val items: List<InvoiceItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxAmount: Double? = 0.0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val paymentStatus: String = "unpaid", // paid / partial / unpaid
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "" // user uid
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "invoiceNumber" to invoiceNumber,
            "customerId" to customerId,
            "customerName" to customerName,
            "items" to items.map { it.toMap() },
            "subtotal" to subtotal,
            "discountAmount" to discountAmount,
            "taxAmount" to taxAmount,
            "totalAmount" to totalAmount,
            "paidAmount" to paidAmount,
            "remainingAmount" to remainingAmount,
            "paymentStatus" to paymentStatus,
            "createdAt" to createdAt,
            "createdBy" to createdBy
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): Invoice {
            @Suppress("UNCHECKED_CAST")
            val itemsList = (map["items"] as? List<Map<String, Any?>>)?.map {
                InvoiceItem.fromMap(it)
            } ?: emptyList()

            return Invoice(
                id = map["id"] as? String ?: "",
                invoiceNumber = map["invoiceNumber"] as? String ?: "",
                customerId = map["customerId"] as? String,
                customerName = map["customerName"] as? String,
                items = itemsList,
                subtotal = (map["subtotal"] as? Number)?.toDouble() ?: 0.0,
                discountAmount = (map["discountAmount"] as? Number)?.toDouble() ?: 0.0,
                taxAmount = (map["taxAmount"] as? Number)?.toDouble(),
                totalAmount = (map["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                paidAmount = (map["paidAmount"] as? Number)?.toDouble() ?: 0.0,
                remainingAmount = (map["remainingAmount"] as? Number)?.toDouble() ?: 0.0,
                paymentStatus = map["paymentStatus"] as? String ?: "unpaid",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                createdBy = map["createdBy"] as? String ?: ""
            )
        }
    }
}

data class InvoiceItem(
    val productId: String = "",
    val productName: String = "",
    val purchasePrice: Double = 0.0,
    val salePrice: Double = 0.0,
    val quantity: Int = 0,
    val lineTotal: Double = 0.0,
    val profit: Double = 0.0
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "productId" to productId,
            "productName" to productName,
            "purchasePrice" to purchasePrice,
            "salePrice" to salePrice,
            "quantity" to quantity,
            "lineTotal" to lineTotal,
            "profit" to profit
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): InvoiceItem {
            return InvoiceItem(
                productId = map["productId"] as? String ?: "",
                productName = map["productName"] as? String ?: "",
                purchasePrice = (map["purchasePrice"] as? Number)?.toDouble() ?: 0.0,
                salePrice = (map["salePrice"] as? Number)?.toDouble() ?: 0.0,
                quantity = (map["quantity"] as? Number)?.toInt() ?: 0,
                lineTotal = (map["lineTotal"] as? Number)?.toDouble() ?: 0.0,
                profit = (map["profit"] as? Number)?.toDouble() ?: 0.0
            )
        }
    }
}
