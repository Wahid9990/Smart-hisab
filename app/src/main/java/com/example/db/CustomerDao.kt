package com.example.db

import androidx.room.*
import com.example.model.Customer
import com.example.model.Payment
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE userId = :userId ORDER BY name ASC")
    fun getCustomersFlow(userId: String): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: String): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<Customer>)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: String)

    @Query("DELETE FROM customers WHERE userId = :userId")
    suspend fun clearCustomers(userId: String)

    // Payments queries
    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getPaymentsForCustomerFlow(customerId: String): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY createdAt DESC")
    suspend fun getPaymentsForCustomerList(customerId: String): List<Payment>

    @Query("SELECT * FROM payments WHERE userId = :userId ORDER BY createdAt DESC")
    fun getPaymentsForUserFlow(userId: String): Flow<List<Payment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<Payment>)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePaymentById(id: String)

    @Query("DELETE FROM payments WHERE userId = :userId")
    suspend fun clearPayments(userId: String)
}
