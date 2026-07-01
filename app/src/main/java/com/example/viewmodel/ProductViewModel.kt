package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.Product
import com.example.service.ProductResult
import com.example.service.ProductService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ProductViewModel(application: Application) : AndroidViewModel(application) {
    private val productService = ProductService(application)
    
    private val _userId = MutableStateFlow("")
    val userId: StateFlow<String> = _userId.asStateFlow()

    // Observable Flow of active products for the initialized user
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val products: StateFlow<List<Product>> = _userId
        .flatMapLatest { uid ->
            if (uid.isBlank()) {
                flowOf(emptyList())
            } else {
                productService.getProductsFlow(uid)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow<ProductUiState>(ProductUiState.Idle)
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    fun initialize(uid: String) {
        if (uid.isNotBlank() && _userId.value != uid) {
            _userId.value = uid
            productService.startRealtimeSync(uid)
        }
    }

    override fun onCleared() {
        super.onCleared()
        productService.stopRealtimeSync()
    }

    fun addProduct(
        name: String,
        category: String,
        purchasePrice: Double,
        salePrice: Double,
        stockQuantity: Int,
        lowStockLimit: Int = 5,
        barcode: String? = null
    ) {
        val uid = _userId.value
        if (uid.isBlank()) {
            _uiState.value = ProductUiState.Error("User is not authenticated.")
            return
        }

        viewModelScope.launch {
            _uiState.value = ProductUiState.Loading
            val product = Product(
                id = UUID.randomUUID().toString(),
                userId = uid,
                name = name,
                category = category,
                purchasePrice = purchasePrice,
                salePrice = salePrice,
                stockQuantity = stockQuantity,
                lowStockLimit = lowStockLimit,
                barcode = barcode,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isActive = true
            )
            when (val result = productService.addProduct(uid, product)) {
                is ProductResult.Success -> {
                    _uiState.value = ProductUiState.Success("Product added successfully.")
                }
                is ProductResult.Error -> {
                    _uiState.value = ProductUiState.Error(result.message)
                }
            }
        }
    }

    fun updateProduct(product: Product) {
        val uid = _userId.value
        if (uid.isBlank()) {
            _uiState.value = ProductUiState.Error("User is not authenticated.")
            return
        }

        viewModelScope.launch {
            _uiState.value = ProductUiState.Loading
            when (val result = productService.updateProduct(uid, product)) {
                is ProductResult.Success -> {
                    _uiState.value = ProductUiState.Success("Product updated successfully.")
                }
                is ProductResult.Error -> {
                    _uiState.value = ProductUiState.Error(result.message)
                }
            }
        }
    }

    fun deleteProduct(product: Product) {
        val uid = _userId.value
        if (uid.isBlank()) {
            _uiState.value = ProductUiState.Error("User is not authenticated.")
            return
        }

        viewModelScope.launch {
            _uiState.value = ProductUiState.Loading
            when (val result = productService.softDeleteProduct(uid, product)) {
                is ProductResult.Success -> {
                    _uiState.value = ProductUiState.Success("Product deleted successfully.")
                }
                is ProductResult.Error -> {
                    _uiState.value = ProductUiState.Error(result.message)
                }
            }
        }
    }

    fun clearUiState() {
        _uiState.value = ProductUiState.Idle
    }
}

sealed interface ProductUiState {
    object Idle : ProductUiState
    object Loading : ProductUiState
    data class Success(val message: String) : ProductUiState
    data class Error(val message: String) : ProductUiState
}
