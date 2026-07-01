package com.example.service

import android.content.Context
import android.util.Log
import com.example.model.Expense
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ExpenseService(private val context: Context) {
    private val tag = "ExpenseService"
    private var firestore: FirebaseFirestore? = null

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
                Log.d(tag, "Firebase Firestore successfully initialized for ExpenseService.")
            } else {
                Log.w(tag, "FirebaseApp is empty. Operating in offline-only mode for ExpenseService.")
                isFirebaseAvailable = false
            }
        } catch (e: Exception) {
            Log.e(tag, "Firebase initialization failed for ExpenseService: ${e.message}")
            isFirebaseAvailable = false
        }
    }

    // Realtime flow of all expenses for the user
    fun getExpensesFlow(userId: String): Flow<List<Expense>> = callbackFlow {
        if (userId.isBlank() || !isFirebaseAvailable) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val db = firestore
        if (db == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val subscription = db.collection("users")
            .document(userId)
            .collection("expenses")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Expenses snapshot listen failed", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            Expense.fromMap(doc.data ?: emptyMap()).copy(id = doc.id, userId = userId)
                        } catch (e: Exception) {
                            Log.e(tag, "Error parsing expense document", e)
                            null
                        }
                    }
                    trySend(list)
                }
            }

        awaitClose {
            subscription.remove()
        }
    }

    suspend fun addExpense(userId: String, expense: Expense): ExpenseResult {
        if (userId.isBlank()) return ExpenseResult.Error("User is not authenticated.")
        if (!isFirebaseAvailable) return ExpenseResult.Error("Cloud services are currently unavailable.")

        val expenseWithUser = expense.copy(userId = userId)
        try {
            val db = firestore ?: return ExpenseResult.Error("Firestore is unavailable.")
            db.collection("users")
                .document(userId)
                .collection("expenses")
                .document(expenseWithUser.id)
                .set(expenseWithUser.toMap())
                .await()
            return ExpenseResult.Success(expenseWithUser)
        } catch (e: Exception) {
            Log.e(tag, "Failed to add expense", e)
            return ExpenseResult.Error("Failed to save expense: ${e.localizedMessage}")
        }
    }

    suspend fun updateExpense(userId: String, expense: Expense): ExpenseResult {
        if (userId.isBlank()) return ExpenseResult.Error("User is not authenticated.")
        if (!isFirebaseAvailable) return ExpenseResult.Error("Cloud services are currently unavailable.")

        val updated = expense.copy(userId = userId)
        try {
            val db = firestore ?: return ExpenseResult.Error("Firestore is unavailable.")
            db.collection("users")
                .document(userId)
                .collection("expenses")
                .document(updated.id)
                .set(updated.toMap())
                .await()
            return ExpenseResult.Success(updated)
        } catch (e: Exception) {
            Log.e(tag, "Failed to update expense", e)
            return ExpenseResult.Error("Failed to update expense: ${e.localizedMessage}")
        }
    }

    suspend fun deleteExpense(userId: String, expenseId: String): ExpenseResult {
        if (userId.isBlank()) return ExpenseResult.Error("User is not authenticated.")
        if (!isFirebaseAvailable) return ExpenseResult.Error("Cloud services are currently unavailable.")

        try {
            val db = firestore ?: return ExpenseResult.Error("Firestore is unavailable.")
            db.collection("users")
                .document(userId)
                .collection("expenses")
                .document(expenseId)
                .delete()
                .await()
            return ExpenseResult.Success(Expense(id = expenseId))
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete expense", e)
            return ExpenseResult.Error("Failed to delete expense: ${e.localizedMessage}")
        }
    }
}

sealed class ExpenseResult {
    data class Success(val expense: Expense) : ExpenseResult()
    data class Error(val message: String) : ExpenseResult()
}
