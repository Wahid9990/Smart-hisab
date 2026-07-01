package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.AppDatabase
import com.example.model.Customer
import com.example.model.Payment
import com.example.service.CustomerResult
import com.example.service.CustomerService
import com.example.service.PaymentResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class CustomerViewModel(application: Application) : AndroidViewModel(application) {
    private val customerService = CustomerService(application)
    private val customerDao = AppDatabase.getDatabase(application).customerDao()

    private val _userId = MutableStateFlow("")
    val userId: StateFlow<String> = _userId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Real-time flow of all customers for the authenticated user
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allCustomers: StateFlow<List<Customer>> = _userId
        .flatMapLatest { uid ->
            if (uid.isBlank()) {
                flowOf(emptyList())
            } else {
                customerDao.getCustomersFlow(uid)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered list of customers for list screen
    val customers: StateFlow<List<Customer>> = combine(allCustomers, _searchQuery) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.name.contains(query, ignoreCase = true) || 
                (it.phone?.contains(query) ?: false)
            }
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dynamic outstanding total Udhaar/Credit across all customers
    val totalUdhaar: StateFlow<Double> = allCustomers
        .map { list -> list.sumOf { it.balance } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    // Selected customer for details screen
    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomer.asStateFlow()

    // Live Flow of payments for the selected customer
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val payments: StateFlow<List<Payment>> = _selectedCustomer
        .flatMapLatest { customer ->
            if (customer == null) {
                flowOf(emptyList())
            } else {
                customerDao.getPaymentsForCustomerFlow(customer.id)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Operation UI State
    private val _customerUiState = MutableStateFlow<CustomerUiState>(CustomerUiState.Idle)
    val customerUiState: StateFlow<CustomerUiState> = _customerUiState.asStateFlow()

    fun initialize(uid: String) {
        if (uid.isNotBlank() && _userId.value != uid) {
            _userId.value = uid
            customerService.startRealtimeSync(uid)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _customerInvoices = MutableStateFlow<List<com.example.model.Invoice>>(emptyList())
    val customerInvoices: StateFlow<List<com.example.model.Invoice>> = _customerInvoices.asStateFlow()

    fun selectCustomer(customer: Customer?) {
        _selectedCustomer.value = customer
        if (customer != null) {
            loadCustomerInvoices(customer.id)
        } else {
            _customerInvoices.value = emptyList()
        }
    }

    private fun loadCustomerInvoices(customerId: String) {
        val uid = _userId.value
        if (uid.isBlank()) return
        
        if (!customerService.isFirebaseAvailable) {
            _customerInvoices.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("users")
                    .document(uid)
                    .collection("invoices")
                    .whereEqualTo("customerId", customerId)
                    .get()
                    .addOnSuccessListener { snapshots ->
                        val list = snapshots.documents.mapNotNull { doc ->
                            try {
                                com.example.model.Invoice.fromMap(doc.data ?: emptyMap()).copy(id = doc.id)
                            } catch (e: Exception) {
                                null
                            }
                        }.sortedByDescending { it.createdAt }
                        _customerInvoices.value = list
                    }
                    .addOnFailureListener {
                        _customerInvoices.value = emptyList()
                    }
            } catch (e: Exception) {
                _customerInvoices.value = emptyList()
            }
        }
    }

    fun clearUiState() {
        _customerUiState.value = CustomerUiState.Idle
    }

    // Add or quick-add customer
    fun addCustomer(name: String, phone: String?, address: String?, onComplete: ((Customer) -> Unit)? = null) {
        val uid = _userId.value
        if (uid.isBlank()) {
            _customerUiState.value = CustomerUiState.Error("User not authenticated.")
            return
        }
        if (name.trim().isBlank()) {
            _customerUiState.value = CustomerUiState.Error("Name is required.")
            return
        }

        viewModelScope.launch {
            _customerUiState.value = CustomerUiState.Loading
            val customer = Customer(
                id = UUID.randomUUID().toString(),
                userId = uid,
                name = name.trim(),
                phone = phone?.trim()?.takeIf { it.isNotEmpty() },
                address = address?.trim()?.takeIf { it.isNotEmpty() },
                totalCredit = 0.0,
                totalPaid = 0.0,
                balance = 0.0,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            when (val result = customerService.addCustomer(uid, customer)) {
                is CustomerResult.Success -> {
                    _customerUiState.value = CustomerUiState.Success("Customer added successfully.")
                    onComplete?.invoke(result.customer)
                }
                is CustomerResult.Error -> {
                    _customerUiState.value = CustomerUiState.Error(result.message)
                }
            }
        }
    }

    // Edit/Update existing customer
    fun updateCustomer(customer: Customer, name: String, phone: String?, address: String?) {
        val uid = _userId.value
        if (uid.isBlank()) {
            _customerUiState.value = CustomerUiState.Error("User not authenticated.")
            return
        }
        if (name.trim().isBlank()) {
            _customerUiState.value = CustomerUiState.Error("Name is required.")
            return
        }

        viewModelScope.launch {
            _customerUiState.value = CustomerUiState.Loading
            val updated = customer.copy(
                name = name.trim(),
                phone = phone?.trim()?.takeIf { it.isNotEmpty() },
                address = address?.trim()?.takeIf { it.isNotEmpty() },
                updatedAt = System.currentTimeMillis()
            )

            when (val result = customerService.updateCustomer(uid, updated)) {
                is CustomerResult.Success -> {
                    _customerUiState.value = CustomerUiState.Success("Customer updated successfully.")
                    // Sync active details view
                    if (_selectedCustomer.value?.id == customer.id) {
                        _selectedCustomer.value = result.customer
                    }
                }
                is CustomerResult.Error -> {
                    _customerUiState.value = CustomerUiState.Error(result.message)
                }
            }
        }
    }

    // Add a customer payment
    fun addPayment(customerId: String, amount: Double, note: String?) {
        val uid = _userId.value
        if (uid.isBlank()) {
            _customerUiState.value = CustomerUiState.Error("User not authenticated.")
            return
        }
        if (amount <= 0.0) {
            _customerUiState.value = CustomerUiState.Error("Amount must be greater than zero.")
            return
        }

        viewModelScope.launch {
            _customerUiState.value = CustomerUiState.Loading
            val payment = Payment(
                id = UUID.randomUUID().toString(),
                userId = uid,
                customerId = customerId,
                amount = amount,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                createdAt = System.currentTimeMillis()
            )

            when (val result = customerService.addPayment(uid, payment)) {
                is PaymentResult.Success -> {
                    _customerUiState.value = CustomerUiState.Success("Payment of $amount recorded successfully.")
                    // Refresh selected customer state
                    val updatedCust = customerDao.getCustomerById(customerId)
                    if (updatedCust != null && _selectedCustomer.value?.id == customerId) {
                        _selectedCustomer.value = updatedCust
                    }
                }
                is PaymentResult.Error -> {
                    _customerUiState.value = CustomerUiState.Error(result.message)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        customerService.stopRealtimeSync()
    }
}

sealed interface CustomerUiState {
    object Idle : CustomerUiState
    object Loading : CustomerUiState
    data class Success(val message: String) : CustomerUiState
    data class Error(val message: String) : CustomerUiState
}
