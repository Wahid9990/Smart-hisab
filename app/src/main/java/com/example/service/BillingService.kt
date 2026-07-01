package com.example.service

import android.content.Context
import android.util.Log
import com.example.db.AppDatabase
import com.example.model.Invoice
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class BillingService(private val context: Context) {
    private val tag = "BillingService"
    private var firestore: FirebaseFirestore? = null
    private val productDao = AppDatabase.getDatabase(context).productDao()
    private val customerDao = AppDatabase.getDatabase(context).customerDao()

    var isFirebaseAvailable: Boolean = false
        private set

    init {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (initEx: Exception) {
                    Log.e(tag, "Manual FirebaseApp.initializeApp failed: ${initEx.message}")
                }
            }
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                firestore = FirebaseFirestore.getInstance()
                isFirebaseAvailable = true
                Log.d(tag, "Firebase Firestore successfully initialized for BillingService.")
            } else {
                Log.w(tag, "FirebaseApp is empty. Operating in offline-only mode for BillingService.")
                isFirebaseAvailable = false
            }
        } catch (e: Exception) {
            Log.e(tag, "Firebase initialization failed for BillingService: ${e.message}")
            isFirebaseAvailable = false
        }
    }

    // Get the next invoice number based on current count / latest invoice in Firestore
    suspend fun getNextInvoiceNumber(userId: String): String {
        if (!isFirebaseAvailable || userId.isBlank()) {
            val randomSuffix = (100000..999999).random()
            return "SH-$randomSuffix"
        }
        return try {
            val db = firestore ?: return "SH-${(100000..999999).random()}"
            val query = db.collection("users")
                .document(userId)
                .collection("invoices")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            if (query.isEmpty) {
                "SH-000001"
            } else {
                val doc = query.documents.firstOrNull()
                val lastNumStr = doc?.getString("invoiceNumber") ?: "SH-000000"
                val lastNum = lastNumStr.substringAfter("SH-").toIntOrNull() ?: 0
                val nextNum = lastNum + 1
                String.format("SH-%06d", nextNum)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch latest invoice number, generating offline-style", e)
            "SH-${(100000..999999).random()}"
        }
    }

    // Process and save the invoice
    suspend fun saveInvoice(userId: String, invoice: Invoice): BillingResult {
        if (userId.isBlank()) {
            return BillingResult.Error("User is not authenticated.")
        }

        // 1. Validate and Reduce stock in local DB
        try {
            for (item in invoice.items) {
                val localProduct = productDao.getProductById(item.productId)
                    ?: return BillingResult.Error("Product ${item.productName} not found in local stock.")
                
                if (localProduct.stockQuantity < item.quantity) {
                    return BillingResult.Error("Insufficient stock for ${item.productName}. Available: ${localProduct.stockQuantity}")
                }
                
                val updatedProduct = localProduct.copy(
                    stockQuantity = localProduct.stockQuantity - item.quantity,
                    updatedAt = System.currentTimeMillis()
                )
                productDao.insertProduct(updatedProduct)
            }
        } catch (e: Exception) {
            Log.e(tag, "Local stock reduction failed", e)
            return BillingResult.Error("Failed to adjust stock locally: ${e.localizedMessage}")
        }

        // 1.5 Update Customer balance locally
        var updatedCustomer: com.example.model.Customer? = null
        if (!invoice.customerId.isNullOrBlank()) {
            try {
                val customer = customerDao.getCustomerById(invoice.customerId)
                if (customer != null) {
                    val newTotalCredit = customer.totalCredit + invoice.totalAmount
                    val newTotalPaid = customer.totalPaid + invoice.paidAmount
                    val newBalance = maxOf(0.0, newTotalCredit - newTotalPaid)
                    updatedCustomer = customer.copy(
                        totalCredit = newTotalCredit,
                        totalPaid = newTotalPaid,
                        balance = newBalance,
                        updatedAt = System.currentTimeMillis()
                    )
                    customerDao.insertCustomer(updatedCustomer)
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to update customer balance locally", e)
            }
        }

        // 2. Save Invoice and update stocks in Firestore via a batch write
        if (isFirebaseAvailable) {
            try {
                val db = firestore ?: return BillingResult.Error("Firestore is unavailable.")
                val batch = db.batch()

                // Invoice Document Reference
                val invoiceRef = db.collection("users")
                    .document(userId)
                    .collection("invoices")
                    .document(invoice.id)

                batch.set(invoiceRef, invoice.toMap())

                // Product Stock updates in Firestore
                for (item in invoice.items) {
                    val productRef = db.collection("users")
                        .document(userId)
                        .collection("products")
                        .document(item.productId)
                    
                    batch.update(productRef, "stockQuantity", com.google.firebase.firestore.FieldValue.increment(-item.quantity.toLong()))
                    batch.update(productRef, "updatedAt", System.currentTimeMillis())
                }

                // Customer balance updates in Firestore
                if (updatedCustomer != null) {
                    val customerRef = db.collection("users")
                        .document(userId)
                        .collection("customers")
                        .document(updatedCustomer.id)
                    batch.set(customerRef, updatedCustomer.toMap())
                }

                batch.commit().await()
                Log.d(tag, "Invoice, customer balance and stock reduction saved successfully in cloud.")
            } catch (e: Exception) {
                Log.e(tag, "Cloud sync failed for invoice creation. Invoice preserved locally in stock decrease.", e)
                return BillingResult.Success(invoice, isSynced = false)
            }
        }

        return BillingResult.Success(invoice, isSynced = isFirebaseAvailable)
    }
}

sealed class BillingResult {
    data class Success(val invoice: Invoice, val isSynced: Boolean) : BillingResult()
    data class Error(val message: String) : BillingResult()
}
