package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localization.Loc
import com.example.model.Expense
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.ExpenseUiState
import com.example.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    authViewModel: AuthViewModel,
    expenseViewModel: ExpenseViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang by authViewModel.appLanguage.collectAsState()
    val userProfile by authViewModel.userProfile.collectAsState()
    val isRtl = Loc.isRtl(lang)
    val align = Loc.getAlign(lang)
    val context = LocalContext.current

    val expenses by expenseViewModel.allExpenses.collectAsState()
    val monthlyTotal by expenseViewModel.monthlyTotalExpenses.collectAsState()
    val uiState by expenseViewModel.expenseUiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedExpenseForEdit by remember { mutableStateOf<Expense?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    val currencySymbol = userProfile?.currency ?: "Rs."

    // Filtered list
    val filteredExpenses = remember(expenses, searchQuery, selectedCategoryFilter) {
        expenses.filter { exp ->
            val matchesSearch = searchQuery.isBlank() || 
                    (exp.note?.contains(searchQuery, ignoreCase = true) ?: false) ||
                    exp.category.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategoryFilter == "All" || exp.category == selectedCategoryFilter
            matchesSearch && matchesCategory
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is ExpenseUiState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                showAddDialog = false
                selectedExpenseForEdit = null
                expenseViewModel.clearUiState()
            }
            is ExpenseUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                expenseViewModel.clearUiState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = Loc.t(lang, "expenses"),
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("expenses_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = Loc.t(lang, "cancel"),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0C0D14))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF03DAC5),
                contentColor = Color(0xFF0C0D14),
                shape = CircleShape,
                modifier = Modifier.testTag("add_expense_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense")
            }
        },
        containerColor = Color(0xFF0C0D14)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Monthly Total Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("monthly_total_expenses_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x07FFFFFF)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Monthly Total Expenses".uppercase(),
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF90A4AE),
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$currencySymbol ${String.format(Locale.getDefault(), "%,.2f", monthlyTotal)}",
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF5350)
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF5350).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = Color(0xFFEF5350)
                        )
                    }
                }
            }

            // Search Bar & Horizontal Category Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(Loc.t(lang, "search") + " " + Loc.t(lang, "expenses") + "...", color = Color(0x66FFFFFF), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0x66FFFFFF)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF03DAC5),
                    unfocusedBorderColor = Color(0x1AFFFFFF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category filter row
            val filterOptions = remember { listOf("All") + expenseViewModel.categories }
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterOptions) { cat ->
                    val isSelected = selectedCategoryFilter == cat
                    SuggestionChip(
                        onClick = { selectedCategoryFilter = cat },
                        label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isSelected) Color(0xFF03DAC5).copy(alpha = 0.15f) else Color(0x0AFFFFFF),
                            labelColor = if (isSelected) Color(0xFF03DAC5) else Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) Color(0xFF03DAC5) else Color(0x10FFFFFF)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Expenses List
            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = Color(0x22FFFFFF),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No expenses matching current filter",
                            style = TextStyle(fontSize = 14.sp, color = Color(0xFF90A4AE))
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("expenses_lazy_column"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredExpenses, key = { it.id }) { expense ->
                        ExpenseItemRow(
                            expense = expense,
                            currencySymbol = currencySymbol,
                            isRtl = isRtl,
                            onEditClick = { selectedExpenseForEdit = expense },
                            onDeleteClick = { expenseViewModel.deleteExpense(expense.id) }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddDialog || selectedExpenseForEdit != null) {
        ExpenseAddEditDialog(
            expense = selectedExpenseForEdit,
            categories = expenseViewModel.categories,
            onDismiss = {
                showAddDialog = false
                selectedExpenseForEdit = null
            },
            onSave = { cat, amt, note, dt ->
                val editExp = selectedExpenseForEdit
                if (editExp != null) {
                    expenseViewModel.updateExpense(editExp, cat, amt, note, dt)
                } else {
                    expenseViewModel.addExpense(cat, amt, note, dt)
                }
            },
            isLoading = uiState is ExpenseUiState.Loading,
            isRtl = isRtl
        )
    }
}

@Composable
fun ExpenseItemRow(
    expense: Expense,
    currencySymbol: String,
    isRtl: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }
    val formattedDate = remember(expense.date) { formatter.format(Date(expense.date)) }

    val categoryColor = remember(expense.category) {
        getCategoryColor(expense.category)
    }

    val categoryIcon = remember(expense.category) {
        getCategoryIcon(expense.category)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("expense_row_${expense.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0x05FFFFFF)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x0AFFFFFF)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = expense.category,
                    tint = categoryColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Info block
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = expense.category,
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$currencySymbol ${String.format(Locale.getDefault(), "%,.2f", expense.amount)}",
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF5350))
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = expense.note ?: "No description",
                        style = TextStyle(fontSize = 12.sp, color = Color(0xFF90A4AE)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formattedDate,
                        style = TextStyle(fontSize = 11.sp, color = Color(0xFF78909C))
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action buttons row
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(32.dp).testTag("edit_expense_btn_${expense.id}")
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF03DAC5), modifier = Modifier.size(16.dp))
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(32.dp).testTag("delete_expense_btn_${expense.id}")
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun ExpenseAddEditDialog(
    expense: Expense?,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (category: String, amount: Double, note: String?, date: Long) -> Unit,
    isLoading: Boolean,
    isRtl: Boolean
) {
    var category by remember { mutableStateOf(expense?.category ?: categories.firstOrNull() ?: "Other") }
    var amountStr by remember { mutableStateOf(expense?.amount?.let { if (it > 0) it.toString() else "" } ?: "") }
    var note by remember { mutableStateOf(expense?.note ?: "") }
    var expenseDate by remember { mutableStateOf(expense?.date ?: System.currentTimeMillis()) }

    var expandedCategoryMenu by remember { mutableStateOf(false) }

    val formatter = remember { SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()) }
    val formattedDate = formatter.format(Date(expenseDate))

    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161722)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1FFFFFFF)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("expense_add_edit_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (expense == null) "Add Expense" else "Edit Expense",
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category Dropdown field
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = {
                            IconButton(onClick = { expandedCategoryMenu = !expandedCategoryMenu }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF03DAC5),
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedCategoryMenu = !expandedCategoryMenu }
                            .testTag("dialog_expense_category_input")
                    )

                    DropdownMenu(
                        expanded = expandedCategoryMenu,
                        onDismissRequest = { expandedCategoryMenu = false },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .background(Color(0xFF161722))
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = Color.White) },
                                onClick = {
                                    category = cat
                                    expandedCategoryMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Amount Text Field
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount *") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF03DAC5),
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_expense_amount_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Note Text Field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Description (Optional)") },
                    placeholder = { Text("e.g. Office stationery") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF03DAC5),
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_expense_note_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Date Picker Field
                OutlinedTextField(
                    value = formattedDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = expenseDate }
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val selected = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, y)
                                            set(Calendar.MONTH, m)
                                            set(Calendar.DAY_OF_MONTH, d)
                                        }
                                        expenseDate = selected.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Choose Date", tint = Color.White)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF03DAC5),
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_expense_date_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dialog_expense_cancel_btn")
                    ) {
                        Text("Cancel", color = Color(0xFF90A4AE))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull()
                            if (amt == null || amt <= 0.0) {
                                Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            onSave(category, amt, note.takeIf { it.isNotBlank() }, expenseDate)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5), contentColor = Color(0xFF0C0D14)),
                        modifier = Modifier.testTag("dialog_expense_save_btn"),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(size = 18.dp, color = Color(0xFF0C0D14))
                        } else {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

fun getCategoryColor(category: String): Color {
    return when (category.lowercase(Locale.ROOT)) {
        "rent" -> Color(0xFFE57373)
        "electricity" -> Color(0xFFFFB74D)
        "salary" -> Color(0xFF81C784)
        "transport" -> Color(0xFF64B5F6)
        "internet" -> Color(0xFF4DB6AC)
        "maintenance" -> Color(0xFFBA68C8)
        else -> Color(0xFF90A4AE)
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase(Locale.ROOT)) {
        "rent" -> Icons.Default.Home
        "electricity" -> Icons.Default.Bolt
        "salary" -> Icons.Default.Payments
        "transport" -> Icons.Default.DirectionsCar
        "internet" -> Icons.Default.Wifi
        "maintenance" -> Icons.Default.Build
        else -> Icons.Default.Category
    }
}

@Composable
fun CircularProgressIndicator(size: androidx.compose.ui.unit.Dp, color: Color) {
    androidx.compose.material3.CircularProgressIndicator(
        modifier = Modifier.size(size),
        color = color,
        strokeWidth = 2.dp
    )
}
