package com.example.db

import androidx.room.*
import com.example.model.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE userId = :userId AND isActive = 1 ORDER BY name ASC")
    fun getProductsFlow(userId: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE userId = :userId AND isActive = 1 ORDER BY name ASC")
    suspend fun getProductsList(userId: String): List<Product>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: String)
    
    @Query("DELETE FROM products WHERE userId = :userId")
    suspend fun clearProducts(userId: String)
}
