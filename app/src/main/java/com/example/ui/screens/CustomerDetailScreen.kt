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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.Loc
import com.example.model.Customer
import com.example.model.Payment
import com.example.model.Invoice
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.CustomerUiState
import com.example.viewmodel.CustomerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    viewModel: AuthViewModel,
    customerViewModel: CustomerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang by viewModel.appLanguage.collectAsState()
    val isRtl = Loc.isRtl(lang)
    val align = Loc.getAlign(lang)

    val customer by customerViewModel.selectedCustomer.collectAsState()
    val paymentsList by customerViewModel.payments.collectAsState()
    val invoicesList by customerViewModel.customerInvoices.collectAsState()
    val uiState by customerViewModel.customerUiState.collectAsState()

    var showPaymentDialog by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0 = Payments, 1 = Invoices

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is CustomerUiState.Success -> {
                Toast.makeText(context, (uiState as CustomerUiState.Success).message, Toast.LENGTH_SHORT).show()
                customerViewModel.clearUiState()
                showPaymentDialog = false
            }
            is CustomerUiState.Error -> {
                Toast.makeText(context, (uiState as CustomerUiState.Error).message, Toast.LENGTH_LONG).show()
                customerViewModel.clearUiState()
            }
            else -> {}
        }
    }

    if (customer == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF03DAC5))
        }
        return
    }

    val activeCustomer = customer!!

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
                    modifier = Modifier.testTag("detail_back_button")
                ) {
                    Icon(
                        imageVector = if (isRtl) Icons.Default.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Customer Ledger",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = align
                )

                Button(
                    onClick = { showPaymentDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF03DAC5),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("record_payment_button")
                ) {
                    Icon(imageVector = Icons.Default.Payment, contentDescription = "Pay", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Payment", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Customer Identity Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x0CFFFFFF)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFBB86FC).copy(alpha = 0.15f))
                            .border(1.5.dp, Color(0xFFBB86FC), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeCustomer.name.take(1).uppercase(),
                            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFBB86FC))
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeCustomer.name,
                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        )
                        if (!activeCustomer.phone.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Color(0xFF90A4AE), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = activeCustomer.phone!!, style = TextStyle(fontSize = 12.sp, color = Color(0xFF90A4AE)))
                            }
                        }
                        if (!activeCustomer.address.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF90A4AE), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = activeCustomer.address!!, style = TextStyle(fontSize = 12.sp, color = Color(0xFF90A4AE)))
                            }
                        }
                    }
                }
            }

            // Ledger Finances Overview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x18FFFFFF)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x20FFFFFF))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "BALANCE DUE",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78909C),
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = align
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Rs. ${String.format(java.util.Locale.getDefault(), "%,.2f", activeCustomer.balance)}",
                        style = TextStyle(
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = if (activeCustomer.balance > 0) Color(0xFFEF5350) else Color(0xFF03DAC5),
                            textAlign = align
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color(0x12FFFFFF))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Total Udhaar", fontSize = 11.sp, color = Color(0xFF90A4AE))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Rs. ${String.format(java.util.Locale.getDefault(), "%,.2f", activeCustomer.totalCredit)}",
                                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            )
                        }

                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(text = "Total Paid", fontSize = 11.sp, color = Color(0xFF90A4AE))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Rs. ${String.format(java.util.Locale.getDefault(), "%,.2f", activeCustomer.totalPaid)}",
                                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF03DAC5))
                            )
                        }
                    }
                }
            }

            // Tab Rows
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF03DAC5),
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = Color(0xFF03DAC5),
                        height = 3.dp
                    )
                },
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Payments (${paymentsList.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    selectedContentColor = Color(0xFF03DAC5),
                    unselectedContentColor = Color(0xFF90A4AE)
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Invoices (${invoicesList.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    selectedContentColor = Color(0xFF03DAC5),
                    unselectedContentColor = Color(0xFF90A4AE)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content
            if (activeTab == 0) {
                // Payments List
                if (paymentsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Receipt, contentDescription = null, tint = Color(0xFF37474F), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No cash payments recorded yet.", fontSize = 13.sp, color = Color(0xFF90A4AE))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("payments_lazy_column"),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(paymentsList, key = { it.id }) { payment ->
                            PaymentRowItem(payment = payment, formatter = dateFormatter)
                        }
                    }
                }
            } else {
                // Invoices List
                if (invoicesList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = Color(0xFF37474F), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No billing invoices linked to this customer.", fontSize = 13.sp, color = Color(0xFF90A4AE))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("linked_invoices_lazy_column"),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(invoicesList, key = { it.id }) { invoice ->
                            InvoiceRowItem(invoice = invoice, formatter = dateFormatter)
                        }
                    }
                }
            }
        }

        // Add Payment Dialog
        if (showPaymentDialog) {
            PaymentRecordDialog(
                customerBalance = activeCustomer.balance,
                onDismiss = { showPaymentDialog = false },
                onConfirm = { amount, note ->
                    customerViewModel.addPayment(activeCustomer.id, amount, note)
                },
                isLoading = uiState is CustomerUiState.Loading
            )
        }
    }
}

@Composable
fun PaymentRowItem(
    payment: Payment,
    formatter: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0x0AFFFFFF)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF03DAC5).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Payment Recd", tint = Color(0xFF03DAC5), modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = payment.note ?: "Cash Payment Received",
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    Text(
                        text = formatter.format(Date(payment.createdAt)),
                        style = TextStyle(fontSize = 11.sp, color = Color(0xFF78909C))
                    )
                }
            }

            Text(
                text = "Rs. ${String.format(java.util.Locale.getDefault(), "%,.2f", payment.amount)}",
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF03DAC5))
            )
        }
    }
}

@Composable
fun InvoiceRowItem(
    invoice: Invoice,
    formatter: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0x0AFFFFFF)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFBB86FC).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Description, contentDescription = "Invoice", tint = Color(0xFFBB86FC), modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = invoice.invoiceNumber,
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    Text(
                        text = formatter.format(Date(invoice.createdAt)),
                        style = TextStyle(fontSize = 11.sp, color = Color(0xFF78909C))
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Rs. ${String.format(java.util.Locale.getDefault(), "%,.2f", invoice.totalAmount)}",
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                )
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when (invoice.paymentStatus) {
                                "paid" -> Color(0xFF03DAC5).copy(alpha = 0.15f)
                                "partial" -> Color(0xFFFFB74D).copy(alpha = 0.15f)
                                else -> Color(0xFFEF5350).copy(alpha = 0.15f)
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = invoice.paymentStatus.uppercase(),
                        style = TextStyle(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (invoice.paymentStatus) {
                                "paid" -> Color(0xFF03DAC5)
                                "partial" -> Color(0xFFFFB74D)
                                else -> Color(0xFFEF5350)
                            },
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentRecordDialog(
    customerBalance: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, String?) -> Unit,
    isLoading: Boolean
) {
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    var errorText by remember { mutableStateOf<String?>(null) }
    var confirmAdvance by remember { mutableStateOf(false) }

    val amount = amountStr.toDoubleOrNull() ?: 0.0
    val isAdvanceScenario = amount > customerBalance && customerBalance > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Record Customer Payment",
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
                Text(
                    text = "Outstanding Balance: Rs. ${String.format(java.util.Locale.getDefault(), "%,.2f", customerBalance)}",
                    fontSize = 14.sp,
                    color = Color(0xFF90A4AE),
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        errorText = null
                    },
                    label = { Text("Payment Amount *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("0.00") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF03DAC5),
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_payment_amount_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Payment Note / Reference (Optional)") },
                    placeholder = { Text("e.g. Cash / Online transfer") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF03DAC5),
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_payment_note_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                if (isAdvanceScenario) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x18FFB74D)),
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Advance Credit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB74D))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "This payment exceeds the customer's outstanding debt of Rs. ${String.format(java.util.Locale.getDefault(), "%,.2f", customerBalance)}. This will record an advance balance credit.",
                                fontSize = 11.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { confirmAdvance = !confirmAdvance }
                            ) {
                                Checkbox(
                                    checked = confirmAdvance,
                                    onCheckedChange = { confirmAdvance = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFFB74D))
                                )
                                Text("I confirm this is an advance payment", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }

                errorText?.let {
                    Text(text = it, color = Color(0xFFEF5350), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val enteredAmt = amountStr.toDoubleOrNull()
                    when {
                        enteredAmt == null || enteredAmt <= 0.0 -> {
                            errorText = "Amount must be a positive number greater than zero."
                        }
                        isAdvanceScenario && !confirmAdvance -> {
                            errorText = "Please confirm the advance payment checkbox to proceed."
                        }
                        else -> {
                            onConfirm(enteredAmt, note)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5), contentColor = Color.Black),
                modifier = Modifier.testTag("dialog_payment_save_button"),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Confirm Payment", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_payment_cancel_button")
            ) {
                Text("Cancel", color = Color(0xFF90A4AE))
            }
        },
        containerColor = Color(0xFF1E1F2A),
        shape = RoundedCornerShape(16.dp)
    )
}
