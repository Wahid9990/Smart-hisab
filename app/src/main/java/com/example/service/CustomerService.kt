package com.example.service

import android.content.Context
import android.util.Log
import com.example.db.AppDatabase
import com.example.model.Customer
import com.example.model.Payment
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CustomerService(private val context: Context) {
    private val tag = "CustomerService"
    private var firestore: FirebaseFirestore? = null
    private val customerDao = AppDatabase.getDatabase(context).customerDao()
    private var customerSyncListener: ListenerRegistration? = null
    private var paymentsSyncListeners = mutableMapOf<String, ListenerRegistration>()

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
                Log.d(tag, "Firebase Firestore successfully initialized for CustomerService.")
            } else {
                Log.w(tag, "FirebaseApp is empty. Operating in offline-only mode for CustomerService.")
                isFirebaseAvailable = false
            }
        } catch (e: Exception) {
            Log.e(tag, "Firebase initialization failed for CustomerService: ${e.message}")
            isFirebaseAvailable = false
        }
    }

    // --- Customer Functions ---

    fun getCustomersFlow(userId: String): Flow<List<Customer>> {
        return customerDao.getCustomersFlow(userId)
    }

    suspend fun getCustomerById(id: String): Customer? {
        return customerDao.getCustomerById(id)
    }

    suspend fun addCustomer(userId: String, customer: Customer): CustomerResult {
        if (userId.isBlank()) return CustomerResult.Error("User is not authenticated.")
        val customerWithUser = customer.copy(userId = userId)
        
        try {
            customerDao.insertCustomer(customerWithUser)
        } catch (e: Exception) {
            Log.e(tag, "Local save failed", e)
            return CustomerResult.Error("Failed to save customer locally: ${e.localizedMessage}")
        }

        if (isFirebaseAvailable) {
            try {
                val db = firestore ?: return CustomerResult.Error("Firestore is unavailable.")
                db.collection("users")
                    .document(userId)
                    .collection("customers")
                    .document(customerWithUser.id)
                    .set(customerWithUser.toMap())
                    .await()
            } catch (e: Exception) {
                Log.e(tag, "Cloud sync failed for addCustomer", e)
            }
        }
        return CustomerResult.Success(customerWithUser)
    }

    suspend fun updateCustomer(userId: String, customer: Customer): CustomerResult {
        if (userId.isBlank()) return CustomerResult.Error("User is not authenticated.")
        val updated = customer.copy(userId = userId, updatedAt = System.currentTimeMillis())
        
        try {
            customerDao.insertCustomer(updated)
        } catch (e: Exception) {
            Log.e(tag, "Local update failed", e)
            return CustomerResult.Error("Failed to update customer locally: ${e.localizedMessage}")
        }

        if (isFirebaseAvailable) {
            try {
                val db = firestore ?: return CustomerResult.Error("Firestore is unavailable.")
                db.collection("users")
                    .document(userId)
                    .collection("customers")
                    .document(updated.id)
                    .set(updated.toMap())
                    .await()
            } catch (e: Exception) {
                Log.e(tag, "Cloud sync failed for updateCustomer", e)
            }
        }
        return CustomerResult.Success(updated)
    }

    // --- Payment Functions ---

    fun getPaymentsForCustomerFlow(customerId: String): Flow<List<Payment>> {
        return customerDao.getPaymentsForCustomerFlow(customerId)
    }

    suspend fun addPayment(userId: String, payment: Payment): PaymentResult {
        if (userId.isBlank()) return PaymentResult.Error("User is not authenticated.")
        val paymentWithUser = payment.copy(userId = userId)

        // 1. Fetch current customer
        val customer = customerDao.getCustomerById(paymentWithUser.customerId)
            ?: return PaymentResult.Error("Customer not found.")

        // Calculate new balances
        val newTotalPaid = customer.totalPaid + paymentWithUser.amount
        val newBalance = maxOf(0.0, customer.totalCredit - newTotalPaid)
        val updatedCustomer = customer.copy(
            totalPaid = newTotalPaid,
            balance = newBalance,
            updatedAt = System.currentTimeMillis()
        )

        // 2. Local database insert/update in transaction fashion
        try {
            customerDao.insertPayment(paymentWithUser)
            customerDao.insertCustomer(updatedCustomer)
        } catch (e: Exception) {
            Log.e(tag, "Local transaction failed", e)
            return PaymentResult.Error("Failed to save payment locally: ${e.localizedMessage}")
        }

        // 3. Firestore Sync using a Batch Write
        if (isFirebaseAvailable) {
            try {
                val db = firestore ?: return PaymentResult.Error("Firestore is unavailable.")
                val batch = db.batch()

                // Customer Ref
                val customerRef = db.collection("users")
                    .document(userId)
                    .collection("customers")
                    .document(customer.id)

                // Payment Ref
                val paymentRef = db.collection("users")
                    .document(userId)
                    .collection("customers")
                    .document(customer.id)
                    .collection("payments")
                    .document(paymentWithUser.id)

                batch.set(customerRef, updatedCustomer.toMap())
                batch.set(paymentRef, paymentWithUser.toMap())

                batch.commit().await()
                Log.d(tag, "Payment and customer update saved successfully in cloud.")
            } catch (e: Exception) {
                Log.e(tag, "Cloud sync failed for payment add", e)
            }
        }

        return PaymentResult.Success(paymentWithUser)
    }

    // --- Realtime Sync ---

    fun startRealtimeSync(userId: String) {
        if (!isFirebaseAvailable || userId.isBlank()) return
        try {
            customerSyncListener?.remove()
            val db = firestore ?: return

            // Sync Customers
            customerSyncListener = db.collection("users")
                .document(userId)
                .collection("customers")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e(tag, "Customers snapshots listen failed", error)
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        @Suppress("OPT_IN_USAGE")
                        GlobalScope.launch(Dispatchers.IO) {
                            for (docChange in snapshots.documentChanges) {
                                val doc = docChange.document
                                try {
                                    val customer = Customer.fromMap(doc.data).copy(id = doc.id, userId = userId)
                                    when (docChange.type) {
                                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                            customerDao.insertCustomer(customer)
                                            // Start syncing payments for this customer dynamically
                                            startPaymentsSync(userId, customer.id)
                                        }
                                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                            customerDao.deleteCustomerById(customer.id)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(tag, "Error processing customer document change", e)
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "Error initializing realtime customer sync", e)
        }
    }

    private fun startPaymentsSync(userId: String, customerId: String) {
        if (!isFirebaseAvailable || userId.isBlank() || customerId.isBlank()) return
        if (paymentsSyncListeners.containsKey(customerId)) return

        try {
            val db = firestore ?: return
            val listener = db.collection("users")
                .document(userId)
                .collection("customers")
                .document(customerId)
                .collection("payments")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e(tag, "Payments sync failed for customer $customerId", error)
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        @Suppress("OPT_IN_USAGE")
                        GlobalScope.launch(Dispatchers.IO) {
                            for (docChange in snapshots.documentChanges) {
                                val doc = docChange.document
                                try {
                                    val payment = Payment.fromMap(doc.data).copy(id = doc.id, userId = userId, customerId = customerId)
                                    when (docChange.type) {
                                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                            customerDao.insertPayment(payment)
                                        }
                                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                            customerDao.deletePaymentById(payment.id)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(tag, "Error syncing payment doc change", e)
                                }
                            }
                        }
                    }
                }
            paymentsSyncListeners[customerId] = listener
        } catch (e: Exception) {
            Log.e(tag, "Error starting payments sync", e)
        }
    }

    fun stopRealtimeSync() {
        customerSyncListener?.remove()
        customerSyncListener = null
        
        paymentsSyncListeners.values.forEach { it.remove() }
        paymentsSyncListeners.clear()
    }
}

sealed class CustomerResult {
    data class Success(val customer: Customer) : CustomerResult()
    data class Error(val message: String) : CustomerResult()
}

sealed class PaymentResult {
    data class Success(val payment: Payment) : PaymentResult()
    data class Error(val message: String) : PaymentResult()
}
