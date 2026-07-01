package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.AppDatabase
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.Product
import com.example.service.BillingResult
import com.example.service.BillingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class BillingViewModel(application: Application) : AndroidViewModel(application) {
    private val billingService = BillingService(application)
    private val productDao = AppDatabase.getDatabase(application).productDao()

    private val _userId = MutableStateFlow("")
    val userId: StateFlow<String> = _userId.asStateFlow()

    // Observable Flow of active products for selection in billing
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val products: StateFlow<List<Product>> = _userId
        .flatMapLatest { uid ->
            if (uid.isBlank()) {
                flowOf(emptyList())
            } else {
                productDao.getProductsFlow(uid)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Cart items state
    private val _cartItems = MutableStateFlow<List<InvoiceItem>>(emptyList())
    val cartItems: StateFlow<List<InvoiceItem>> = _cartItems.asStateFlow()

    // Invoice values
    private val _customerId = MutableStateFlow<String?>(null)
    val customerId: StateFlow<String?> = _customerId.asStateFlow()

    private val _customerName = MutableStateFlow("")
    val customerName: StateFlow<String> = _customerName.asStateFlow()

    private val _discountAmount = MutableStateFlow(0.0)
    val discountAmount: StateFlow<Double> = _discountAmount.asStateFlow()

    private val _taxPercent = MutableStateFlow(0.0) // percentage
    val taxPercent: StateFlow<Double> = _taxPercent.asStateFlow()

    private val _paidAmount = MutableStateFlow(0.0)
    val paidAmount: StateFlow<Double> = _paidAmount.asStateFlow()

    // UI and Saving state
    private val _billingUiState = MutableStateFlow<BillingUiState>(BillingUiState.Idle)
    val billingUiState: StateFlow<BillingUiState> = _billingUiState.asStateFlow()

    // Saved invoice for preview screen
    private val _savedInvoice = MutableStateFlow<Invoice?>(null)
    val savedInvoice: StateFlow<Invoice?> = _savedInvoice.asStateFlow()

    fun initialize(uid: String) {
        if (uid.isNotBlank() && _userId.value != uid) {
            _userId.value = uid
            resetBillingState()
        }
    }

    fun setCustomerId(id: String?) {
        _customerId.value = id
    }

    fun setCustomerName(name: String) {
        _customerName.value = name
    }

    fun setDiscount(amount: Double) {
        _discountAmount.value = amount
    }

    fun setTaxPercent(percent: Double) {
        _taxPercent.value = percent
    }

    fun setPaidAmount(amount: Double) {
        _paidAmount.value = amount
    }

    // Add or increment a product in cart
    fun addProductToCart(product: Product, quantityToAdd: Int = 1) {
        val currentCart = _cartItems.value.toMutableList()
        val existingIndex = currentCart.indexOfFirst { it.productId == product.id }

        val newQuantity = if (existingIndex != -1) {
            currentCart[existingIndex].quantity + quantityToAdd
        } else {
            quantityToAdd
        }

        // Prevent quantity greater than available stock
        if (newQuantity > product.stockQuantity) {
            _billingUiState.value = BillingUiState.Error("Cannot add more. Available stock: ${product.stockQuantity}")
            return
        }

        val lineTotal = newQuantity * product.salePrice
        val lineProfit = lineTotal - (newQuantity * product.purchasePrice)

        val updatedItem = InvoiceItem(
            productId = product.id,
            productName = product.name,
            purchasePrice = product.purchasePrice,
            salePrice = product.salePrice,
            quantity = newQuantity,
            lineTotal = lineTotal,
            profit = lineProfit
        )

        if (existingIndex != -1) {
            currentCart[existingIndex] = updatedItem
        } else {
            currentCart.add(updatedItem)
        }

        _cartItems.value = currentCart
    }

    // Update quantity of an item directly
    fun updateItemQuantity(item: InvoiceItem, newQuantity: Int, availableStock: Int) {
        if (newQuantity <= 0) {
            removeItemFromCart(item)
            return
        }

        if (newQuantity > availableStock) {
            _billingUiState.value = BillingUiState.Error("Cannot increase quantity. Available stock is $availableStock.")
            return
        }

        val currentCart = _cartItems.value.toMutableList()
        val index = currentCart.indexOfFirst { it.productId == item.productId }
        if (index != -1) {
            val lineTotal = newQuantity * item.salePrice
            val lineProfit = lineTotal - (newQuantity * item.purchasePrice)
            currentCart[index] = item.copy(
                quantity = newQuantity,
                lineTotal = lineTotal,
                profit = lineProfit
            )
            _cartItems.value = currentCart
        }
    }

    fun removeItemFromCart(item: InvoiceItem) {
        _cartItems.value = _cartItems.value.filter { it.productId != item.productId }
    }

    fun resetBillingState() {
        _cartItems.value = emptyList()
        _customerId.value = null
        _customerName.value = ""
        _discountAmount.value = 0.0
        _taxPercent.value = 0.0
        _paidAmount.value = 0.0
        _savedInvoice.value = null
        _billingUiState.value = BillingUiState.Idle
    }

    fun clearUiState() {
        if (_billingUiState.value is BillingUiState.Success || _billingUiState.value is BillingUiState.Error) {
            _billingUiState.value = BillingUiState.Idle
        }
    }

    fun saveInvoice() {
        val uid = _userId.value
        if (uid.isBlank()) {
            _billingUiState.value = BillingUiState.Error("User is not authenticated.")
            return
        }

        val items = _cartItems.value
        if (items.isEmpty()) {
            _billingUiState.value = BillingUiState.Error("Cannot save an empty invoice.")
            return
        }

        val paidAmt = _paidAmount.value
        if (paidAmt < 0.0) {
            _billingUiState.value = BillingUiState.Error("Paid amount cannot be negative.")
            return
        }

        viewModelScope.launch {
            _billingUiState.value = BillingUiState.Loading

            // Calculate amounts
            val subtotal = items.sumOf { it.lineTotal }
            val discount = _discountAmount.value
            val taxPercentVal = _taxPercent.value
            val taxAmount = (subtotal - discount) * (taxPercentVal / 100.0)
            val totalAmount = maxOf(0.0, subtotal - discount + taxAmount)
            val remainingAmount = maxOf(0.0, totalAmount - paidAmt)

            // Select payment status automatically
            val paymentStatus = when {
                paidAmt >= totalAmount -> "paid"
                paidAmt > 0.0 -> "partial"
                else -> "unpaid"
            }

            // Generate invoice number securely
            val invoiceNumber = billingService.getNextInvoiceNumber(uid)
            val invoiceId = UUID.randomUUID().toString()

            val invoice = Invoice(
                id = invoiceId,
                invoiceNumber = invoiceNumber,
                customerId = _customerId.value,
                customerName = _customerName.value.trim().takeIf { it.isNotEmpty() },
                items = items,
                subtotal = subtotal,
                discountAmount = discount,
                taxAmount = taxAmount,
                totalAmount = totalAmount,
                paidAmount = paidAmt,
                remainingAmount = remainingAmount,
                paymentStatus = paymentStatus,
                createdAt = System.currentTimeMillis(),
                createdBy = uid
            )

            when (val result = billingService.saveInvoice(uid, invoice)) {
                is BillingResult.Success -> {
                    _savedInvoice.value = result.invoice
                    _billingUiState.value = BillingUiState.Success(
                        if (result.isSynced) "Invoice saved and synced successfully!" else "Invoice saved locally (sync pending)."
                    )
                }
                is BillingResult.Error -> {
                    _billingUiState.value = BillingUiState.Error(result.message)
                }
            }
        }
    }
}

sealed interface BillingUiState {
    object Idle : BillingUiState
    object Loading : BillingUiState
    data class Success(val message: String) : BillingUiState
    data class Error(val message: String) : BillingUiState
}
