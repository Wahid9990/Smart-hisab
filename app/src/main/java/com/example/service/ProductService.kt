package com.example.service

import android.content.Context
import android.util.Log
import com.example.db.AppDatabase
import com.example.model.Product
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProductService(private val context: Context) {
    private val tag = "ProductService"
    private var firestore: FirebaseFirestore? = null
    private val productDao = AppDatabase.getDatabase(context).productDao()
    private var syncListener: ListenerRegistration? = null

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
                Log.d(tag, "Firebase Firestore successfully initialized for ProductService.")
            } else {
                Log.w(tag, "FirebaseApp is empty. Operating in offline-only mode for ProductService.")
                isFirebaseAvailable = false
            }
        } catch (e: Exception) {
            Log.e(tag, "Firebase initialization failed for ProductService: ${e.message}")
            isFirebaseAvailable = false
        }
    }

    // Get a live flow of products for the active user
    fun getProductsFlow(userId: String): Flow<List<Product>> {
        return productDao.getProductsFlow(userId)
    }

    // Add a new product to local cache and sync to Firestore
    suspend fun addProduct(userId: String, product: Product): ProductResult {
        if (userId.isBlank()) {
            return ProductResult.Error("User is not authenticated.")
        }
        val productWithUser = product.copy(userId = userId)
        try {
            productDao.insertProduct(productWithUser)
        } catch (e: Exception) {
            Log.e(tag, "Local save failed", e)
            return ProductResult.Error("Failed to save product locally: ${e.localizedMessage}")
        }

        if (isFirebaseAvailable) {
            try {
                val db = firestore ?: return ProductResult.Error("Firestore is unavailable.")
                db.collection("users")
                    .document(userId)
                    .collection("products")
                    .document(productWithUser.id)
                    .set(productWithUser.toMap())
                    .await()
            } catch (e: Exception) {
                Log.e(tag, "Cloud sync failed for addProduct", e)
                // We do not fail the function since local Room database is primary offline cache
            }
        }
        return ProductResult.Success(productWithUser)
    }

    // Update an existing product
    suspend fun updateProduct(userId: String, product: Product): ProductResult {
        if (userId.isBlank()) {
            return ProductResult.Error("User is not authenticated.")
        }
        val updatedProduct = product.copy(userId = userId, updatedAt = System.currentTimeMillis())
        try {
            productDao.insertProduct(updatedProduct)
        } catch (e: Exception) {
            Log.e(tag, "Local update failed", e)
            return ProductResult.Error("Failed to update product locally: ${e.localizedMessage}")
        }

        if (isFirebaseAvailable) {
            try {
                val db = firestore ?: return ProductResult.Error("Firestore is unavailable.")
                db.collection("users")
                    .document(userId)
                    .collection("products")
                    .document(updatedProduct.id)
                    .set(updatedProduct.toMap())
                    .await()
            } catch (e: Exception) {
                Log.e(tag, "Cloud sync failed for updateProduct", e)
            }
        }
        return ProductResult.Success(updatedProduct)
    }

    // Soft delete a product by setting isActive = false
    suspend fun softDeleteProduct(userId: String, product: Product): ProductResult {
        if (userId.isBlank()) {
            return ProductResult.Error("User is not authenticated.")
        }
        val deletedProduct = product.copy(isActive = false, updatedAt = System.currentTimeMillis())
        try {
            productDao.insertProduct(deletedProduct)
        } catch (e: Exception) {
            Log.e(tag, "Local soft delete failed", e)
            return ProductResult.Error("Failed to delete product locally: ${e.localizedMessage}")
        }

        if (isFirebaseAvailable) {
            try {
                val db = firestore ?: return ProductResult.Error("Firestore is unavailable.")
                db.collection("users")
                    .document(userId)
                    .collection("products")
                    .document(deletedProduct.id)
                    .set(deletedProduct.toMap())
                    .await()
            } catch (e: Exception) {
                Log.e(tag, "Cloud sync failed for softDeleteProduct", e)
            }
        }
        return ProductResult.Success(deletedProduct)
    }

    // Listen for changes on Firestore in real-time and mirror them to local Room DB
    fun startRealtimeSync(userId: String) {
        if (!isFirebaseAvailable || userId.isBlank()) return
        try {
            syncListener?.remove()
            val db = firestore ?: return
            syncListener = db.collection("users")
                .document(userId)
                .collection("products")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e(tag, "Firestore snapshots listen failed", error)
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        @Suppress("OPT_IN_USAGE")
                        GlobalScope.launch(Dispatchers.IO) {
                            for (docChange in snapshots.documentChanges) {
                                val doc = docChange.document
                                try {
                                    val product = Product.fromMap(doc.data).copy(id = doc.id, userId = userId)
                                    when (docChange.type) {
                                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                            productDao.insertProduct(product)
                                        }
                                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                            productDao.insertProduct(product.copy(isActive = false))
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(tag, "Error processing document change", e)
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "Error initializing realtime sync", e)
        }
    }

    // Stop Listening to Firestore changes
    fun stopRealtimeSync() {
        syncListener?.remove()
        syncListener = null
    }
}

sealed class ProductResult {
    data class Success(val product: Product) : ProductResult()
    data class Error(val message: String) : ProductResult()
}
