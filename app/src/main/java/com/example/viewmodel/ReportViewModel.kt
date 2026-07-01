package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.Expense
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.Product
import com.example.service.ExpenseService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ReportFilterType {
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    THIS_MONTH,
    CUSTOM
}

data class ProductReportItem(
    val productId: String,
    val productName: String,
    val quantitySold: Int,
    val totalRevenue: Double,
    val totalProfit: Double
)

data class CategoryExpenseItem(
    val category: String,
    val totalAmount: Double,
    val percentage: Double
)

data class ReportDataState(
    val totalSales: Double = 0.0,
    val totalProfit: Double = 0.0,
    val totalInvoicesCount: Int = 0,
    val totalPaidAmount: Double = 0.0,
    val totalUnpaidAmount: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    
    val categoryExpenses: List<CategoryExpenseItem> = emptyList(),
    val topSellingProducts: List<ProductReportItem> = emptyList(),
    val lowSellingProducts: List<ProductReportItem> = emptyList(),
    val unpaidInvoices: List<Invoice> = emptyList(),
    val filteredInvoicesCount: Int = 0,
    val filteredExpensesCount: Int = 0
)

class ReportViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "ReportViewModel"
    private val expenseService = ExpenseService(application)
    private var invoicesListener: ListenerRegistration? = null
    val isFirebaseAvailable: Boolean = expenseService.isFirebaseAvailable

    private val _userId = MutableStateFlow("")
    val userId: StateFlow<String> = _userId.asStateFlow()

    private val _filterType = MutableStateFlow(ReportFilterType.THIS_MONTH)
    val filterType: StateFlow<ReportFilterType> = _filterType.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val customDateRange: StateFlow<Pair<Long, Long>?> = _customDateRange.asStateFlow()

    // Real-time Invoices list from Firestore
    private val _allInvoices = MutableStateFlow<List<Invoice>>(emptyList())
    val allInvoices: StateFlow<List<Invoice>> = _allInvoices.asStateFlow()

    // Real-time Expenses list using ExpenseService
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allExpenses: StateFlow<List<Expense>> = _userId
        .flatMapLatest { uid ->
            if (uid.isBlank()) {
                flowOf(emptyList())
            } else {
                expenseService.getExpensesFlow(uid)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Store product list to populate zero-sales low selling products
    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())

    // Loading/Error Indicator
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun initialize(uid: String) {
        if (uid.isNotBlank() && _userId.value != uid) {
            _userId.value = uid
            startInvoicesSync(uid)
            syncProductsList(uid)
        }
    }

    private fun startInvoicesSync(uid: String) {
        invoicesListener?.remove()
        if (!isFirebaseAvailable) {
            _isLoading.value = false
            return
        }
        try {
            _isLoading.value = true
            val db = FirebaseFirestore.getInstance()
            invoicesListener = db.collection("users")
                .document(uid)
                .collection("invoices")
                .addSnapshotListener { snapshot, error ->
                    _isLoading.value = false
                    if (error != null) {
                        Log.e(tag, "Failed to sync invoices for reports", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                Invoice.fromMap(doc.data ?: emptyMap()).copy(id = doc.id)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        _allInvoices.value = list
                    }
                }
        } catch (e: Exception) {
            _isLoading.value = false
            Log.e(tag, "Firebase firestore failed for Reports", e)
        }
    }

    private fun syncProductsList(uid: String) {
        if (!isFirebaseAvailable) return
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                db.collection("users")
                    .document(uid)
                    .collection("products")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                Product.fromMap(doc.data ?: emptyMap()).copy(id = doc.id)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        _allProducts.value = list
                    }
            } catch (e: Exception) {
                Log.e(tag, "Failed to fetch products for zero sales calculation", e)
            }
        }
    }

    fun setFilterType(type: ReportFilterType) {
        _filterType.value = type
        if (type != ReportFilterType.CUSTOM) {
            _customDateRange.value = null
        }
    }

    fun setCustomDateRange(startMs: Long, endMs: Long) {
        _customDateRange.value = Pair(startMs, endMs)
        _filterType.value = ReportFilterType.CUSTOM
    }

    // Main Report Data State (derived combine flow)
    val reportData: StateFlow<ReportDataState> = combine(
        _allInvoices,
        allExpenses,
        _allProducts,
        _filterType,
        _customDateRange
    ) { invoices, expenses, products, filter, customRange ->
        computeReportData(invoices, expenses, products, filter, customRange)
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportDataState()
    )

    private fun computeReportData(
        invoices: List<Invoice>,
        expenses: List<Expense>,
        products: List<Product>,
        filter: ReportFilterType,
        customRange: Pair<Long, Long>?
    ): ReportDataState {
        val bounds = getPeriodBounds(filter, customRange)
        val start = bounds.first
        val end = bounds.second

        // Filter invoices & expenses
        val filteredInvoices = invoices.filter { it.createdAt in start..end }
        val filteredExpenses = expenses.filter { it.date in start..end }

        // Calculations
        var totalSales = 0.0
        var totalProfit = 0.0
        val totalInvoicesCount = filteredInvoices.size
        var totalPaidAmount = 0.0
        var totalUnpaidAmount = 0.0

        val productSalesMap = mutableMapOf<String, ProductReportItem>()

        for (invoice in filteredInvoices) {
            totalSales += invoice.totalAmount
            totalPaidAmount += invoice.paidAmount
            totalUnpaidAmount += invoice.remainingAmount

            for (item in invoice.items) {
                val profit = (item.salePrice - item.purchasePrice) * item.quantity
                totalProfit += profit

                val existing = productSalesMap[item.productId]
                if (existing != null) {
                    productSalesMap[item.productId] = existing.copy(
                        quantitySold = existing.quantitySold + item.quantity,
                        totalRevenue = existing.totalRevenue + item.lineTotal,
                        totalProfit = existing.totalProfit + profit
                    )
                } else {
                    productSalesMap[item.productId] = ProductReportItem(
                        productId = item.productId,
                        productName = item.productName,
                        quantitySold = item.quantity,
                        totalRevenue = item.lineTotal,
                        totalProfit = profit
                    )
                }
            }
        }

        val totalExpenses = filteredExpenses.sumOf { it.amount }
        val netProfit = totalProfit - totalExpenses

        // Category Expenses breakdown
        val expenseCategoryMap = filteredExpenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val categoryExpenses = expenseCategoryMap.map { (cat, amt) ->
            CategoryExpenseItem(
                category = cat,
                totalAmount = amt,
                percentage = if (totalExpenses > 0) (amt / totalExpenses) * 100 else 0.0
            )
        }.sortedByDescending { it.totalAmount }

        // Unpaid invoices list
        val unpaidInvoices = filteredInvoices.filter { it.paymentStatus != "paid" && it.remainingAmount > 0.0 }
            .sortedByDescending { it.createdAt }

        // Product Rankings:
        val soldProductsList = productSalesMap.values.toList()

        // Include active products with zero sales in the product reports
        val allReportingProducts = products.map { prod ->
            val soldItem = productSalesMap[prod.id]
            soldItem ?: ProductReportItem(
                productId = prod.id,
                productName = prod.name,
                quantitySold = 0,
                totalRevenue = 0.0,
                totalProfit = 0.0
            )
        }

        val topSellingProducts = allReportingProducts.filter { it.quantitySold > 0 }
            .sortedByDescending { it.quantitySold }

        val lowSellingProducts = allReportingProducts
            .sortedBy { it.quantitySold }

        return ReportDataState(
            totalSales = totalSales,
            totalProfit = totalProfit,
            totalInvoicesCount = totalInvoicesCount,
            totalPaidAmount = totalPaidAmount,
            totalUnpaidAmount = totalUnpaidAmount,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            categoryExpenses = categoryExpenses,
            topSellingProducts = topSellingProducts,
            lowSellingProducts = lowSellingProducts,
            unpaidInvoices = unpaidInvoices,
            filteredInvoicesCount = filteredInvoices.size,
            filteredExpensesCount = filteredExpenses.size
        )
    }

    private fun getPeriodBounds(filter: ReportFilterType, customRange: Pair<Long, Long>?): Pair<Long, Long> {
        val now = Calendar.getInstance()
        val startCal = now.clone() as Calendar
        val endCal = now.clone() as Calendar

        when (filter) {
            ReportFilterType.TODAY -> {
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)

                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
            }
            ReportFilterType.YESTERDAY -> {
                startCal.add(Calendar.DAY_OF_YEAR, -1)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)

                endCal.add(Calendar.DAY_OF_YEAR, -1)
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
            }
            ReportFilterType.THIS_WEEK -> {
                startCal.set(Calendar.DAY_OF_WEEK, startCal.firstDayOfWeek)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)

                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
            }
            ReportFilterType.THIS_MONTH -> {
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)

                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
            }
            ReportFilterType.CUSTOM -> {
                if (customRange != null) {
                    return customRange
                } else {
                    // Fallback to this month
                    startCal.set(Calendar.DAY_OF_MONTH, 1)
                    startCal.set(Calendar.HOUR_OF_DAY, 0)
                    startCal.set(Calendar.MINUTE, 0)
                    startCal.set(Calendar.SECOND, 0)
                    startCal.set(Calendar.MILLISECOND, 0)

                    endCal.set(Calendar.HOUR_OF_DAY, 23)
                    endCal.set(Calendar.MINUTE, 59)
                    endCal.set(Calendar.SECOND, 59)
                    endCal.set(Calendar.MILLISECOND, 999)
                }
            }
        }
        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }

    override fun onCleared() {
        super.onCleared()
        invoicesListener?.remove()
    }
}
