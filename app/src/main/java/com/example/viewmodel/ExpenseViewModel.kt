package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.Expense
import com.example.service.ExpenseResult
import com.example.service.ExpenseService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val expenseService = ExpenseService(application)

    private val _userId = MutableStateFlow("")
    val userId: StateFlow<String> = _userId.asStateFlow()

    // Categories list as requested
    val categories = listOf(
        "Rent",
        "Electricity",
        "Salary",
        "Transport",
        "Internet",
        "Maintenance",
        "Other"
    )

    // Realtime flow of all expenses
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

    // Monthly total expenses calculation
    val monthlyTotalExpenses: StateFlow<Double> = allExpenses
        .map { list ->
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)

            list.filter { exp ->
                val expCal = Calendar.getInstance().apply { timeInMillis = exp.date }
                expCal.get(Calendar.YEAR) == year && expCal.get(Calendar.MONTH) == month
            }.sumOf { it.amount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    private val _expenseUiState = MutableStateFlow<ExpenseUiState>(ExpenseUiState.Idle)
    val expenseUiState: StateFlow<ExpenseUiState> = _expenseUiState.asStateFlow()

    fun initialize(uid: String) {
        if (uid.isNotBlank() && _userId.value != uid) {
            _userId.value = uid
        }
    }

    fun clearUiState() {
        _expenseUiState.value = ExpenseUiState.Idle
    }

    fun addExpense(category: String, amount: Double, note: String?, date: Long) {
        val uid = _userId.value
        if (uid.isBlank()) {
            _expenseUiState.value = ExpenseUiState.Error("User not authenticated.")
            return
        }
        if (category.isBlank()) {
            _expenseUiState.value = ExpenseUiState.Error("Category is required.")
            return
        }
        if (amount <= 0.0) {
            _expenseUiState.value = ExpenseUiState.Error("Amount must be greater than zero.")
            return
        }

        viewModelScope.launch {
            _expenseUiState.value = ExpenseUiState.Loading
            val expense = Expense(
                id = UUID.randomUUID().toString(),
                userId = uid,
                category = category,
                amount = amount,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                date = date,
                createdAt = System.currentTimeMillis()
            )

            when (val result = expenseService.addExpense(uid, expense)) {
                is ExpenseResult.Success -> {
                    _expenseUiState.value = ExpenseUiState.Success("Expense added successfully.")
                }
                is ExpenseResult.Error -> {
                    _expenseUiState.value = ExpenseUiState.Error(result.message)
                }
            }
        }
    }

    fun updateExpense(expense: Expense, category: String, amount: Double, note: String?, date: Long) {
        val uid = _userId.value
        if (uid.isBlank()) {
            _expenseUiState.value = ExpenseUiState.Error("User not authenticated.")
            return
        }
        if (category.isBlank()) {
            _expenseUiState.value = ExpenseUiState.Error("Category is required.")
            return
        }
        if (amount <= 0.0) {
            _expenseUiState.value = ExpenseUiState.Error("Amount must be greater than zero.")
            return
        }

        viewModelScope.launch {
            _expenseUiState.value = ExpenseUiState.Loading
            val updated = expense.copy(
                category = category,
                amount = amount,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                date = date
            )

            when (val result = expenseService.updateExpense(uid, updated)) {
                is ExpenseResult.Success -> {
                    _expenseUiState.value = ExpenseUiState.Success("Expense updated successfully.")
                }
                is ExpenseResult.Error -> {
                    _expenseUiState.value = ExpenseUiState.Error(result.message)
                }
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        val uid = _userId.value
        if (uid.isBlank()) {
            _expenseUiState.value = ExpenseUiState.Error("User not authenticated.")
            return
        }

        viewModelScope.launch {
            _expenseUiState.value = ExpenseUiState.Loading
            when (val result = expenseService.deleteExpense(uid, expenseId)) {
                is ExpenseResult.Success -> {
                    _expenseUiState.value = ExpenseUiState.Success("Expense deleted successfully.")
                }
                is ExpenseResult.Error -> {
                    _expenseUiState.value = ExpenseUiState.Error(result.message)
                }
            }
        }
    }
}

sealed interface ExpenseUiState {
    object Idle : ExpenseUiState
    object Loading : ExpenseUiState
    data class Success(val message: String) : ExpenseUiState
    data class Error(val message: String) : ExpenseUiState
}
