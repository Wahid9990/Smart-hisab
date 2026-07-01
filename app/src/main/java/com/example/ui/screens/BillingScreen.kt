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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localization.Loc
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.Product
import com.example.util.InvoicePdfGenerator
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.BillingUiState
import com.example.viewmodel.BillingViewModel
import com.example.viewmodel.CustomerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    viewModel: AuthViewModel,
    billingViewModel: BillingViewModel = viewModel(),
    customerViewModel: CustomerViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang by viewModel.appLanguage.collectAsState()
    val isRtl = Loc.isRtl(lang)
    val align = Loc.getAlign(lang)

    val userProfile by viewModel.userProfile.collectAsState()

    // Observe Billing states
    val productsList by billingViewModel.products.collectAsState()
    val cartItems by billingViewModel.cartItems.collectAsState()
    val customerName by billingViewModel.customerName.collectAsState()
    val selectedCustomerId by billingViewModel.customerId.collectAsState()
    val allCustomers by customerViewModel.allCustomers.collectAsState()
    val discountAmount by billingViewModel.discountAmount.collectAsState()
    val taxPercent by billingViewModel.taxPercent.collectAsState()
    val paidAmount by billingViewModel.paidAmount.collectAsState()
    val billingUiState by billingViewModel.billingUiState.collectAsState()
    val savedInvoice by billingViewModel.savedInvoice.collectAsState()

    // Search and Selector States
    var productSearchQuery by remember { mutableStateOf("") }
    var selectedProductForQty by remember { mutableStateOf<Product?>(null) }
    var inputQuantityToAdd by remember { mutableStateOf("1") }

    // Filter products list based on search
    val matchingProducts = remember(productSearchQuery, productsList) {
        if (productSearchQuery.isBlank()) {
            emptyList()
        } else {
            productsList.filter { product ->
                product.isActive && (
                    product.name.contains(productSearchQuery, ignoreCase = true) ||
                    (product.barcode?.contains(productSearchQuery, ignoreCase = true) == true) ||
                    product.category.contains(productSearchQuery, ignoreCase = true)
                )
            }
        }
    }

    // Calculations based on reactive inputs
    val subtotal = remember(cartItems) {
        cartItems.sumOf { it.lineTotal }
    }
    val taxAmount = remember(subtotal, discountAmount, taxPercent) {
        (subtotal - discountAmount) * (taxPercent / 100.0)
    }
    val grandTotal = remember(subtotal, discountAmount, taxAmount) {
        maxOf(0.0, subtotal - discountAmount + taxAmount)
    }
    val remainingAmount = remember(grandTotal, paidAmount) {
        maxOf(0.0, grandTotal - paidAmount)
    }

    // Select payment status automatically
    val computedPaymentStatus = remember(paidAmount, grandTotal) {
        when {
            paidAmount >= grandTotal && grandTotal > 0.0 -> "paid"
            paidAmount > 0.0 -> "partial"
            else -> "unpaid"
        }
    }

    // Handle toast alerts for billing success / error
    LaunchedEffect(billingUiState) {
        when (billingUiState) {
            is BillingUiState.Success -> {
                Toast.makeText(context, (billingUiState as BillingUiState.Success).message, Toast.LENGTH_SHORT).show()
                billingViewModel.clearUiState()
            }
            is BillingUiState.Error -> {
                Toast.makeText(context, (billingUiState as BillingUiState.Error).message, Toast.LENGTH_LONG).show()
                billingViewModel.clearUiState()
            }
            else -> {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0D14))
    ) {
        if (savedInvoice != null) {
            // Show Beautiful Invoice Preview Screen
            InvoicePreviewScreen(
                invoice = savedInvoice!!,
                shopName = userProfile?.shopName ?: "Smart Hisab Shop",
                ownerPhone = userProfile?.phone,
                lang = lang,
                isRtl = isRtl,
                align = align,
                onDismiss = {
                    billingViewModel.resetBillingState()
                }
            )
        } else {
            // Show Create Bill Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isRtl) {
                        Column {
                            Text(
                                text = Loc.t(lang, "billing"),
                                style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            )
                            Text(
                                text = Loc.t(lang, "create_invoice"),
                                style = TextStyle(fontSize = 12.sp, color = Color(0xFF90A4AE))
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = "Invoice Icon",
                        tint = Color(0xFF03DAC5),
                        modifier = Modifier.size(32.dp)
                    )

                    if (isRtl) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = Loc.t(lang, "billing"),
                                style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            )
                            Text(
                                text = Loc.t(lang, "create_invoice"),
                                style = TextStyle(fontSize = 12.sp, color = Color(0xFF90A4AE))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Product Search & Selection Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x0AFFFFFF))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "ADD PRODUCT TO BILL",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF03DAC5),
                            letterSpacing = 1.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Customer Name Field
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = {
                            billingViewModel.setCustomerName(it)
                            if (selectedCustomerId != null) {
                                billingViewModel.setCustomerId(null)
                            }
                        },
                        label = {
                            Text(
                                text = "Customer Name (Optional)",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = align,
                                fontSize = 12.sp
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("billing_customer_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFE0E0E0),
                            focusedBorderColor = Color(0xFF03DAC5),
                            unfocusedBorderColor = Color(0x1AFFFFFF)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = LocalTextStyle.current.copy(textAlign = align),
                        trailingIcon = if (selectedCustomerId != null) {
                            {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Linked to registered customer",
                                    tint = Color(0xFF03DAC5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else null
                    )

                    // Customer Selection / Quick Add Suggestion Row
                    val matchingCustomers = remember(customerName, allCustomers) {
                        if (customerName.isBlank()) {
                            emptyList()
                        } else {
                            allCustomers.filter { it.name.contains(customerName, ignoreCase = true) }
                        }
                    }

                    if (selectedCustomerId != null) {
                        val linkedCust = remember(selectedCustomerId, allCustomers) {
                            allCustomers.find { it.id == selectedCustomerId }
                        }
                        linkedCust?.let { c ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF03DAC5).copy(alpha = 0.08f))
                                    .border(1.dp, Color(0xFF03DAC5).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = if (isRtl) Arrangement.End else Arrangement.Start
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF03DAC5),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Linked: ${c.name} (Udhaar: Rs. ${String.format(Locale.getDefault(), "%,.2f", c.balance)})",
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF03DAC5)
                                    )
                                )
                            }
                        }
                    } else if (customerName.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Quick Add option
                            val exactMatchExists = matchingCustomers.any { it.name.equals(customerName.trim(), ignoreCase = true) }
                            
                            // Horizontal suggestions list
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (!exactMatchExists) {
                                    item {
                                        SuggestionChip(
                                            onClick = {
                                                customerViewModel.addCustomer(customerName, null, null) { newCust ->
                                                    billingViewModel.setCustomerId(newCust.id)
                                                    billingViewModel.setCustomerName(newCust.name)
                                                }
                                            },
                                            label = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Quick Add: \"${customerName}\"", fontSize = 11.sp)
                                                }
                                            },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = Color(0xFF03DAC5).copy(alpha = 0.15f),
                                                labelColor = Color(0xFF03DAC5)
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF03DAC5).copy(alpha = 0.5f))
                                        )
                                    }
                                }

                                items(matchingCustomers, key = { it.id }) { cust ->
                                    SuggestionChip(
                                        onClick = {
                                            billingViewModel.setCustomerId(cust.id)
                                            billingViewModel.setCustomerName(cust.name)
                                        },
                                        label = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("${cust.name} (Rs. ${String.format(Locale.getDefault(), "%,.0f", cust.balance)})", fontSize = 11.sp)
                                            }
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = Color(0x1AFFFFFF),
                                            labelColor = Color.White
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF))
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Interactive Search Field
                    OutlinedTextField(
                        value = productSearchQuery,
                        onValueChange = { productSearchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search Product by Name or Scan Barcode...",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = align,
                                fontSize = 12.sp,
                                color = Color(0xFF78909C)
                            )
                        },
                        leadingIcon = if (!isRtl) {
                            { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color(0xFFB0BEC5)) }
                        } else null,
                        trailingIcon = if (isRtl) {
                            { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color(0xFFB0BEC5)) }
                        } else null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("billing_search_products_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFE0E0E0),
                            focusedBorderColor = Color(0xFFBB86FC),
                            unfocusedBorderColor = Color(0x1AFFFFFF)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = LocalTextStyle.current.copy(textAlign = align)
                    )

                    // Autocomplete Search Matches Dropdown Box
                    if (matchingProducts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                                .background(Color(0xFF161722))
                                .testTag("billing_search_results_container")
                        ) {
                            LazyColumn {
                                items(matchingProducts) { product ->
                                    val isOutOfStock = product.stockQuantity <= 0
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = !isOutOfStock) {
                                                selectedProductForQty = product
                                                inputQuantityToAdd = "1"
                                                productSearchQuery = "" // Clear search
                                            }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = product.name,
                                                color = if (isOutOfStock) Color(0xFFEF5350) else Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Stock: ${product.stockQuantity} | Category: ${product.category}",
                                                color = Color(0xFF90A4AE),
                                                fontSize = 11.sp
                                            )
                                        }
                                        Text(
                                            text = "Rs ${product.salePrice}",
                                            color = Color(0xFF03DAC5),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    HorizontalDivider(color = Color(0x0AFFFFFF))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cart Items Section Header
                Text(
                    text = "CART ITEMS (${cartItems.size})",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF90A4AE),
                        letterSpacing = 0.5.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Cart LazyColumn
                if (cartItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("billing_cart_empty_state"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Empty Cart",
                                tint = Color(0x26FFFFFF),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Your Cart is Empty", color = Color(0xFF78909C), fontSize = 14.sp)
                            Text(text = "Search and select products above to add them.", color = Color(0xFF546E7A), fontSize = 11.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("billing_cart_items_list"),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(cartItems) { item ->
                            val linkedProduct = productsList.find { it.id == item.productId }
                            val availableStock = linkedProduct?.stockQuantity ?: 9999

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0x12FFFFFF)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Left: Item name and unit details
                                    Column(modifier = Modifier.weight(1.2f)) {
                                        Text(
                                            text = item.productName,
                                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Rs ${item.salePrice} / unit | Max Stock: $availableStock",
                                            style = TextStyle(fontSize = 11.sp, color = Color(0xFF90A4AE))
                                        )
                                    }

                                    // Center: Quantity Increment/Decrement Adjuster
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1.3f),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        IconButton(
                                            onClick = {
                                                billingViewModel.updateItemQuantity(item, item.quantity - 1, availableStock)
                                            },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x0FFFFFFF))
                                        ) {
                                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White, modifier = Modifier.size(14.dp))
                                        }

                                        Text(
                                            text = item.quantity.toString(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp)
                                        )

                                        IconButton(
                                            onClick = {
                                                billingViewModel.updateItemQuantity(item, item.quantity + 1, availableStock)
                                            },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x0FFFFFFF))
                                        ) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }

                                    // Right: Line Total and delete icon
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Rs ${item.lineTotal.toInt()}",
                                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF03DAC5))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = { billingViewModel.removeItemFromCart(item) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove Item",
                                                tint = Color(0xFFEF5350),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Calculations Summary Panel Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161722)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFFFFF))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Discount & Tax inline input fields
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = if (discountAmount == 0.0) "" else discountAmount.toInt().toString(),
                                onValueChange = {
                                    val amt = it.toDoubleOrNull() ?: 0.0
                                    billingViewModel.setDiscount(amt)
                                },
                                label = { Text("Discount (Rs)", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("billing_discount_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF03DAC5),
                                    unfocusedBorderColor = Color(0x1AFFFFFF)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = TextStyle(fontSize = 12.sp)
                            )

                            OutlinedTextField(
                                value = if (taxPercent == 0.0) "" else taxPercent.toString(),
                                onValueChange = {
                                    val percent = it.toDoubleOrNull() ?: 0.0
                                    billingViewModel.setTaxPercent(percent)
                                },
                                label = { Text("Tax %", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("billing_tax_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF03DAC5),
                                    unfocusedBorderColor = Color(0x1AFFFFFF)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = TextStyle(fontSize = 12.sp)
                            )

                            OutlinedTextField(
                                value = if (paidAmount == 0.0) "" else paidAmount.toInt().toString(),
                                onValueChange = {
                                    val amt = it.toDoubleOrNull() ?: 0.0
                                    billingViewModel.setPaidAmount(amt)
                                },
                                label = { Text("Paid (Rs) *", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("billing_paid_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF03DAC5),
                                    unfocusedBorderColor = Color(0x1AFFFFFF)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = TextStyle(fontSize = 12.sp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Net totals breakdown list
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Subtotal: Rs ${subtotal.toInt()}", color = Color(0xFF90A4AE), fontSize = 11.sp)
                            Text(text = "Tax: Rs ${taxAmount.toInt()}", color = Color(0xFF90A4AE), fontSize = 11.sp)
                            Text(text = "Remaining: Rs ${remainingAmount.toInt()}", color = Color(0xFFEF5350), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "GRAND TOTAL", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Rs ${grandTotal.toInt()}",
                                    color = Color(0xFF03DAC5),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            // Payment Status indicator pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when (computedPaymentStatus) {
                                            "paid" -> Color(0x1F03DAC5)
                                            "partial" -> Color(0x26FFB74D)
                                            else -> Color(0x1FEF5350)
                                        }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = computedPaymentStatus.uppercase(),
                                    color = when (computedPaymentStatus) {
                                        "paid" -> Color(0xFF03DAC5)
                                        "partial" -> Color(0xFFFFB74D)
                                        else -> Color(0xFFEF5350)
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Generate Bill CTA Button
                        val isSaving = billingUiState is BillingUiState.Loading
                        Button(
                            onClick = { billingViewModel.saveInvoice() },
                            enabled = cartItems.isNotEmpty() && !isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("create_bill_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5), contentColor = Color(0xFF0C0D14)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = Color(0xFF0C0D14), modifier = Modifier.size(20.dp))
                            } else {
                                Icon(imageVector = Icons.Default.Receipt, contentDescription = "Receipt")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SAVE & GENERATE BILL",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Add Product Dialog - Choose quantity pop-up
        selectedProductForQty?.let { product ->
            AlertDialog(
                onDismissRequest = { selectedProductForQty = null },
                containerColor = Color(0xFF161722),
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        text = "Add product: ${product.name}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Available stock: ${product.stockQuantity} items.\nEnter required quantity:",
                            color = Color(0xFF90A4AE),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = inputQuantityToAdd,
                            onValueChange = { inputQuantityToAdd = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF03DAC5),
                                unfocusedBorderColor = Color(0x1AFFFFFF)
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("billing_dialog_qty_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val qty = inputQuantityToAdd.toIntOrNull() ?: 1
                            if (qty <= 0) {
                                Toast.makeText(context, "Quantity must be at least 1.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (qty > product.stockQuantity) {
                                Toast.makeText(context, "Quantity exceeds available stock of ${product.stockQuantity}.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            billingViewModel.addProductToCart(product, qty)
                            selectedProductForQty = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5), contentColor = Color(0xFF0C0D14)),
                        modifier = Modifier.testTag("billing_dialog_confirm_add")
                    ) {
                        Text("Add to Cart")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedProductForQty = null }) {
                        Text("Cancel", color = Color(0xFF90A4AE))
                    }
                }
            )
        }
    }
}

// Gorgeous High-Fidelity Receipt Preview Screen Layout
@Composable
fun InvoicePreviewScreen(
    invoice: Invoice,
    shopName: String,
    ownerPhone: String? = null,
    lang: String,
    isRtl: Boolean,
    align: TextAlign,
    onDismiss: () -> Unit
) {
    val dateStr = remember(invoice.createdAt) {
        try {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            sdf.format(Date(invoice.createdAt))
        } catch (e: Exception) {
            "Just Now"
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isGeneratingA4 by remember { mutableStateOf(false) }
    var isGeneratingThermal by remember { mutableStateOf(false) }

    fun sharePdfFile(file: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share Invoice PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("invoice_preview_container"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Receipt Box Frame
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161722)),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF03DAC5).copy(alpha = 0.3f))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Receipt Header
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0x1F03DAC5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = Color(0xFF03DAC5),
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = shopName.uppercase(),
                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "SMART HISAB DIGITAL RECEIPT",
                            style = TextStyle(fontSize = 10.sp, color = Color(0xFF90A4AE), letterSpacing = 1.sp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // Metadata list
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Invoice Number:", color = Color(0xFF90A4AE), fontSize = 11.sp)
                            Text(text = invoice.invoiceNumber, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("preview_invoice_number"))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Date & Time:", color = Color(0xFF90A4AE), fontSize = 11.sp)
                            Text(text = dateStr, color = Color.White, fontSize = 11.sp)
                        }
                        invoice.customerName?.let { name ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Customer Name:", color = Color(0xFF90A4AE), fontSize = 11.sp)
                                Text(text = name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("preview_customer_name"))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // Table Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "ITEM / QTY", color = Color(0xFF90A4AE), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                        Text(text = "PRICE", color = Color(0xFF90A4AE), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text(text = "TOTAL", color = Color(0xFF90A4AE), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Items list mapping
                items(invoice.items) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(2f)) {
                            Text(text = item.productName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Qty: ${item.quantity}", color = Color(0xFF90A4AE), fontSize = 10.sp)
                        }
                        Text(text = "Rs ${item.salePrice.toInt()}", color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text(text = "Rs ${item.lineTotal.toInt()}", color = Color(0xFF03DAC5), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                    }
                }

                // Divider and Total details
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Subtotal:", color = Color(0xFF90A4AE), fontSize = 11.sp)
                            Text(text = "Rs ${invoice.subtotal.toInt()}", color = Color.White, fontSize = 11.sp)
                        }

                        if (invoice.discountAmount > 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Discount:", color = Color(0xFF90A4AE), fontSize = 11.sp)
                                Text(text = "- Rs ${invoice.discountAmount.toInt()}", color = Color(0xFFEF5350), fontSize = 11.sp)
                            }
                        }

                        invoice.taxAmount?.let { tax ->
                            if (tax > 0.0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Tax:", color = Color(0xFF90A4AE), fontSize = 11.sp)
                                    Text(text = "Rs ${tax.toInt()}", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "GRAND TOTAL:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Rs ${invoice.totalAmount.toInt()}", color = Color(0xFF03DAC5), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Paid Amount:", color = Color(0xFF90A4AE), fontSize = 11.sp)
                            Text(text = "Rs ${invoice.paidAmount.toInt()}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Remaining Balance:", color = Color(0xFF90A4AE), fontSize = 11.sp)
                            Text(text = "Rs ${invoice.remainingAmount.toInt()}", color = if (invoice.remainingAmount > 0.0) Color(0xFFEF5350) else Color(0xFF03DAC5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Payment Status:", color = Color(0xFF90A4AE), fontSize = 11.sp)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when (invoice.paymentStatus) {
                                            "paid" -> Color(0x1F03DAC5)
                                            "partial" -> Color(0x26FFB74D)
                                            else -> Color(0x1FEF5350)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = invoice.paymentStatus.uppercase(),
                                    color = when (invoice.paymentStatus) {
                                        "paid" -> Color(0xFF03DAC5)
                                        "partial" -> Color(0xFFFFB74D)
                                        else -> Color(0xFFEF5350)
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Thank you for your business!",
                            color = Color(0xFF78909C),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Serif
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // PDF Share Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // A4 Share Button
            Button(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        try {
                            isGeneratingA4 = true
                            val file = InvoicePdfGenerator.generateA4Pdf(
                                context = context,
                                invoice = invoice,
                                shopName = shopName,
                                ownerPhone = ownerPhone
                            )
                            withContext(Dispatchers.Main) {
                                isGeneratingA4 = false
                                sharePdfFile(file)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isGeneratingA4 = false
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = !isGeneratingA4 && !isGeneratingThermal,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("share_a4_pdf_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1F03DAC5), contentColor = Color(0xFF03DAC5)),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isGeneratingA4) {
                    CircularProgressIndicator(color = Color(0xFF03DAC5), modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share A4", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("A4 PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Thermal Share Button
            Button(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        try {
                            isGeneratingThermal = true
                            val file = InvoicePdfGenerator.generateThermalPdf(
                                context = context,
                                invoice = invoice,
                                shopName = shopName,
                                ownerPhone = ownerPhone
                            )
                            withContext(Dispatchers.Main) {
                                isGeneratingThermal = false
                                sharePdfFile(file)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isGeneratingThermal = false
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = !isGeneratingA4 && !isGeneratingThermal,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("share_thermal_pdf_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1FBB86FC), contentColor = Color(0xFFBB86FC)),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isGeneratingThermal) {
                    CircularProgressIndicator(color = Color(0xFFBB86FC), modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Default.Receipt, contentDescription = "Share Thermal", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("THERMAL PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Create New Bill action footer button
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("preview_close_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5), contentColor = Color(0xFF0C0D14)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "New")
            Spacer(modifier = Modifier.width(6.dp))
            Text("CREATE NEW BILL", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}
