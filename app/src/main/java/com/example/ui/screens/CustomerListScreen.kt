package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.Loc
import com.example.model.Customer
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.CustomerUiState
import com.example.viewmodel.CustomerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    viewModel: AuthViewModel,
    customerViewModel: CustomerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Customer) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang by viewModel.appLanguage.collectAsState()
    val isRtl = Loc.isRtl(lang)
    val align = Loc.getAlign(lang)

    val customersList by customerViewModel.customers.collectAsState()
    val totalUdhaarAmount by customerViewModel.totalUdhaar.collectAsState()
    val customerUiState by customerViewModel.customerUiState.collectAsState()
    val searchQuery by customerViewModel.searchQuery.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<Customer?>(null) }

    // Observe Ui State
    LaunchedEffect(customerUiState) {
        when (customerUiState) {
            is CustomerUiState.Success -> {
                Toast.makeText(context, (customerUiState as CustomerUiState.Success).message, Toast.LENGTH_SHORT).show()
                customerViewModel.clearUiState()
                showAddDialog = false
                customerToEdit = null
            }
            is CustomerUiState.Error -> {
                Toast.makeText(context, (customerUiState as CustomerUiState.Error).message, Toast.LENGTH_LONG).show()
                customerViewModel.clearUiState()
            }
            else -> {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0C0D14), Color(0xFF161722))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = if (isRtl) Icons.Default.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = Loc.t(lang, "customers"),
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = align
                )

                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF03DAC5).copy(alpha = 0.15f))
                        .testTag("add_customer_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Customer",
                        tint = Color(0xFF03DAC5)
                    )
                }
            }

            // Stats / Total Udhaar Summary Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x0EFFFFFF)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Loc.t(lang, "total_udhaar").uppercase(),
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF90A4AE),
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rs. ${String.format(java.util.Locale.getDefault(), "%,.2f", totalUdhaarAmount)}",
                            style = TextStyle(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (totalUdhaarAmount > 0) Color(0xFFEF5350) else Color(0xFF03DAC5)
                            )
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "DEBTORS",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF90A4AE),
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEF5350).copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${customersList.count { it.balance > 0 }} Active",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF5350)
                                )
                            )
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { customerViewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .testTag("customer_search_input"),
                placeholder = {
                    Text(
                        text = "Search by name or phone...",
                        fontSize = 14.sp,
                        color = Color(0xFF78909C)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF78909C)
                    )
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { customerViewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                        }
                    }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF03DAC5),
                    unfocusedBorderColor = Color(0x33FFFFFF),
                    focusedContainerColor = Color(0x0AFFFFFF),
                    unfocusedContainerColor = Color(0x0AFFFFFF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )

            // Customers List
            if (customersList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PeopleOutline,
                            contentDescription = "No customers",
                            tint = Color(0xFF455A64),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "No customers registered yet." else "No matching customers found.",
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = Color(0xFF90A4AE),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("customers_lazy_column"),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(customersList, key = { it.id }) { customer ->
                        CustomerRowItem(
                            customer = customer,
                            onItemClick = {
                                customerViewModel.selectCustomer(customer)
                                onNavigateToDetail(customer)
                            },
                            onEditClick = {
                                customerToEdit = customer
                                showAddDialog = true
                            }
                        )
                    }
                }
            }
        }

        // Add / Edit Customer Dialog
        if (showAddDialog) {
            CustomerAddEditDialog(
                customer = customerToEdit,
                onDismiss = {
                    showAddDialog = false
                    customerToEdit = null
                },
                onConfirm = { name, phone, address ->
                    if (customerToEdit == null) {
                        customerViewModel.addCustomer(name, phone, address)
                    } else {
                        customerViewModel.updateCustomer(customerToEdit!!, name, phone, address)
                    }
                },
                isLoading = customerUiState is CustomerUiState.Loading
            )
        }
    }
}

@Composable
fun CustomerRowItem(
    customer: Customer,
    onItemClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .testTag("customer_item_${customer.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0x0AFFFFFF)),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x12FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Customer Avatar/Icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (customer.balance > 0) Color(0x1FEF5350) else Color(0x1F03DAC5)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = customer.name.take(1).uppercase(),
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (customer.balance > 0) Color(0xFFEF5350) else Color(0xFF03DAC5)
                    )
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                if (!customer.phone.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Phone",
                            tint = Color(0xFF78909C),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = customer.phone,
                            style = TextStyle(
                                fontSize = 11.sp,
                                color = Color(0xFF78909C)
                            )
                        )
                    }
                }
            }

            // Balance outstanding display
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Rs. ${String.format(java.util.Locale.getDefault(), "%,.2f", customer.balance)}",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (customer.balance > 0) Color(0xFFEF5350) else Color(0xFF03DAC5)
                    )
                )
                Text(
                    text = if (customer.balance > 0) "Udhaar/Owed" else "Settled",
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = if (customer.balance > 0) Color(0xFFEF5350) else Color(0xFF78909C),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onEditClick,
                modifier = Modifier.testTag("edit_customer_${customer.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Customer",
                    tint = Color(0xFF78909C),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerAddEditDialog(
    customer: Customer?,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?) -> Unit,
    isLoading: Boolean
) {
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }

    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (customer == null) "Add Customer" else "Edit Customer",
                style = TextStyle(fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (nameError) nameError = false
                    },
                    label = { Text("Customer Name *") },
                    isError = nameError,
                    placeholder = { Text("e.g. Ramesh Kumar") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF03DAC5),
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_customer_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                if (nameError) {
                    Text("Name is required", color = Color(0xFFEF5350), fontSize = 11.sp)
                }

                // Phone Input
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (Optional)") },
                    placeholder = { Text("e.g. 9876543210") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF03DAC5),
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_customer_phone_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Address Input
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address (Optional)") },
                    placeholder = { Text("e.g. G-23, Sector 4, New Delhi") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF03DAC5),
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_customer_address_input"),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isBlank()) {
                        nameError = true
                    } else {
                        onConfirm(name, phone, address)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5), contentColor = Color.Black),
                modifier = Modifier.testTag("dialog_customer_save_button"),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_customer_cancel_button")
            ) {
                Text("Cancel", color = Color(0xFF90A4AE))
            }
        },
        containerColor = Color(0xFF1E1F2A),
        shape = RoundedCornerShape(16.dp)
    )
}
