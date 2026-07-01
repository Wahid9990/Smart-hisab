package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localization.Loc
import com.example.model.Product
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.ProductUiState
import com.example.viewmodel.ProductViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: AuthViewModel,
    productViewModel: ProductViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang by viewModel.appLanguage.collectAsState()
    val isRtl = Loc.isRtl(lang)
    val align = Loc.getAlign(lang)

    // Observe local products live stream from ViewModel
    val productsList by productViewModel.products.collectAsState()
    val productUiState by productViewModel.uiState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }

    // Dynamic Category List from existing products
    val categories = remember(productsList) {
        val uniqueCats = productsList
            .map { it.category }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        listOf("All") + uniqueCats
    }

    // Filter products list based on search and category
    val filteredProducts = remember(searchQuery, selectedCategory, productsList) {
        productsList.filter { product ->
            val matchesSearch = product.name.contains(searchQuery, ignoreCase = true) ||
                    (product.barcode?.contains(searchQuery, ignoreCase = true) == true) ||
                    product.category.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || product.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    // Handle ViewModel state changes (Toasts for success/error)
    LaunchedEffect(productUiState) {
        when (productUiState) {
            is ProductUiState.Success -> {
                Toast.makeText(context, (productUiState as ProductUiState.Success).message, Toast.LENGTH_SHORT).show()
                productViewModel.clearUiState()
            }
            is ProductUiState.Error -> {
                Toast.makeText(context, (productUiState as ProductUiState.Error).message, Toast.LENGTH_LONG).show()
                productViewModel.clearUiState()
            }
            else -> {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0D14))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isRtl) {
                    Column {
                        Text(
                            text = Loc.t(lang, "products"),
                            style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        )
                        Text(
                            text = Loc.t(lang, "manage_products"),
                            style = TextStyle(fontSize = 12.sp, color = Color(0xFF90A4AE))
                        )
                    }
                }

                // Add Product Button
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("add_product_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5), contentColor = Color(0xFF0C0D14)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = Loc.t(lang, "add_product"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                if (isRtl) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = Loc.t(lang, "products"),
                            style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        )
                        Text(
                            text = Loc.t(lang, "manage_products"),
                            style = TextStyle(fontSize = 12.sp, color = Color(0xFF90A4AE))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "${Loc.t(lang, "search")}...",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = align,
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
                    .testTag("product_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color(0xFFE0E0E0),
                    focusedBorderColor = Color(0xFFBB86FC),
                    unfocusedBorderColor = Color(0x24FFFFFF)
                ),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(textAlign = align)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Categories LazyRow Chips Filter
            if (categories.size > 1) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = { Text(text = category, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = Color(0xFFB0BEC5),
                                selectedLabelColor = Color(0xFF0C0D14),
                                selectedContainerColor = Color(0xFF03DAC5),
                                containerColor = Color(0x0AFFFFFF)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color(0x1AFFFFFF),
                                selectedBorderColor = Color(0xFF03DAC5)
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Products list or empty state
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("products_empty_state"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0x0AFFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = "No Products",
                                tint = Color(0x40FFFFFF),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No Matching Products Found" else "No Products Added Yet",
                            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try refining your search terms or filters." else "Tap the '+ Product' button above to add inventory.",
                            style = TextStyle(fontSize = 12.sp, color = Color(0xFF90A4AE)),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("products_list_column"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val isLowStock = product.stockQuantity <= product.lowStockLimit
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { productToEdit = product },
                            colors = CardDefaults.cardColors(containerColor = Color(0x12FFFFFF)),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isLowStock) Color(0x33EF5350) else Color(0x0AFFFFFF)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (!isRtl) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.name,
                                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0x1FBB86FC))
                                                    .padding(horizontal = 6.dp, vertical = 1.5.dp)
                                            ) {
                                                Text(
                                                    text = product.category.ifBlank { "General" },
                                                    style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBB86FC))
                                                )
                                            }
                                            if (!product.barcode.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "📷 ${product.barcode}",
                                                    style = TextStyle(fontSize = 10.sp, color = Color(0xFF78909C))
                                                )
                                            }
                                        }
                                    }
                                }

                                // Price & Stock block
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Rs ${String.format(Locale.getDefault(), "%,.1f", product.salePrice)}",
                                            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF03DAC5))
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isLowStock) Color(0x26EF5350) else Color(0x1F03DAC5)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${product.stockQuantity} Left",
                                                style = TextStyle(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isLowStock) Color(0xFFEF5350) else Color(0xFF03DAC5)
                                                )
                                            )
                                        }
                                    }
                                }

                                if (isRtl) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = product.name,
                                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.End,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (!product.barcode.isNullOrBlank()) {
                                                Text(
                                                    text = "📷 ${product.barcode}",
                                                    style = TextStyle(fontSize = 10.sp, color = Color(0xFF78909C))
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0x1FBB86FC))
                                                    .padding(horizontal = 6.dp, vertical = 1.5.dp)
                                            ) {
                                                Text(
                                                    text = product.category.ifBlank { "General" },
                                                    style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBB86FC))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Product Dialog Modal
        if (showAddDialog) {
            ProductFormDialog(
                title = Loc.t(lang, "add_product"),
                confirmButtonLabel = Loc.t(lang, "save"),
                isRtl = isRtl,
                align = align,
                onDismiss = { showAddDialog = false },
                onSave = { name, category, purchasePrice, salePrice, stock, lowStock, barcode ->
                    productViewModel.addProduct(
                        name = name,
                        category = category,
                        purchasePrice = purchasePrice,
                        salePrice = salePrice,
                        stockQuantity = stock,
                        lowStockLimit = lowStock,
                        barcode = barcode
                    )
                    showAddDialog = false
                }
            )
        }

        // Edit Product Dialog Modal
        productToEdit?.let { product ->
            ProductFormDialog(
                title = Loc.t(lang, "edit"),
                confirmButtonLabel = Loc.t(lang, "save"),
                isRtl = isRtl,
                align = align,
                product = product,
                showDeleteButton = true,
                onDismiss = { productToEdit = null },
                onSave = { name, category, purchasePrice, salePrice, stock, lowStock, barcode ->
                    productViewModel.updateProduct(
                        product.copy(
                            name = name,
                            category = category,
                            purchasePrice = purchasePrice,
                            salePrice = salePrice,
                            stockQuantity = stock,
                            lowStockLimit = lowStock,
                            barcode = barcode,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    productToEdit = null
                },
                onDelete = {
                    productViewModel.deleteProduct(product)
                    productToEdit = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormDialog(
    title: String,
    confirmButtonLabel: String,
    isRtl: Boolean,
    align: TextAlign,
    product: Product? = null,
    showDeleteButton: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Double, Int, Int, String?) -> Unit,
    onDelete: () -> Unit = {}
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "") }
    var purchasePrice by remember { mutableStateOf(product?.purchasePrice?.toString() ?: "") }
    var salePrice by remember { mutableStateOf(product?.salePrice?.toString() ?: "") }
    var stockQuantity by remember { mutableStateOf(product?.stockQuantity?.toString() ?: "") }
    var lowStockLimit by remember { mutableStateOf(product?.lowStockLimit?.toString() ?: "5") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }

    var formError by remember { mutableStateOf<String?>(null) }
    var saleLessThanPurchaseWarning by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_form_dialog"),
        containerColor = Color(0xFF161722),
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = title,
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White),
                modifier = Modifier.fillMaxWidth(),
                textAlign = align
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    if (formError != null) {
                        Text(
                            text = formError!!,
                            color = Color(0xFFEF5350),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = align,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x1AEF5350), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // Name
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; formError = null },
                        label = { Text("Product Name *", modifier = Modifier.fillMaxWidth(), textAlign = align) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFE0E0E0),
                            focusedBorderColor = Color(0xFF03DAC5),
                            unfocusedBorderColor = Color(0x1AFFFFFF)
                        ),
                        textStyle = LocalTextStyle.current.copy(textAlign = align),
                        modifier = Modifier.fillMaxWidth().testTag("dialog_prod_name"),
                        singleLine = true
                    )
                }

                // Category
                item {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category (e.g. Rice, Soap)", modifier = Modifier.fillMaxWidth(), textAlign = align) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFE0E0E0),
                            focusedBorderColor = Color(0xFF03DAC5),
                            unfocusedBorderColor = Color(0x1AFFFFFF)
                        ),
                        textStyle = LocalTextStyle.current.copy(textAlign = align),
                        modifier = Modifier.fillMaxWidth().testTag("dialog_prod_category"),
                        singleLine = true
                    )
                }

                // Prices row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = purchasePrice,
                            onValueChange = { purchasePrice = it; formError = null; saleLessThanPurchaseWarning = false },
                            label = { Text("Cost Price *", modifier = Modifier.fillMaxWidth(), textAlign = align) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color(0xFFE0E0E0),
                                focusedBorderColor = Color(0xFF03DAC5),
                                unfocusedBorderColor = Color(0x1AFFFFFF)
                            ),
                            textStyle = LocalTextStyle.current.copy(textAlign = align),
                            modifier = Modifier.weight(1f).testTag("dialog_prod_purchase_price"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = salePrice,
                            onValueChange = { salePrice = it; formError = null; saleLessThanPurchaseWarning = false },
                            label = { Text("Sale Price *", modifier = Modifier.fillMaxWidth(), textAlign = align) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color(0xFFE0E0E0),
                                focusedBorderColor = Color(0xFF03DAC5),
                                unfocusedBorderColor = Color(0x1AFFFFFF)
                            ),
                            textStyle = LocalTextStyle.current.copy(textAlign = align),
                            modifier = Modifier.weight(1f).testTag("dialog_prod_sale_price"),
                            singleLine = true
                        )
                    }
                }

                // Stock details row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = stockQuantity,
                            onValueChange = { stockQuantity = it; formError = null },
                            label = { Text("Stock Qty *", modifier = Modifier.fillMaxWidth(), textAlign = align) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color(0xFFE0E0E0),
                                focusedBorderColor = Color(0xFF03DAC5),
                                unfocusedBorderColor = Color(0x1AFFFFFF)
                            ),
                            textStyle = LocalTextStyle.current.copy(textAlign = align),
                            modifier = Modifier.weight(1f).testTag("dialog_prod_stock"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = lowStockLimit,
                            onValueChange = { lowStockLimit = it; formError = null },
                            label = { Text("Low Limit", modifier = Modifier.fillMaxWidth(), textAlign = align) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color(0xFFE0E0E0),
                                focusedBorderColor = Color(0xFF03DAC5),
                                unfocusedBorderColor = Color(0x1AFFFFFF)
                            ),
                            textStyle = LocalTextStyle.current.copy(textAlign = align),
                            modifier = Modifier.weight(1f).testTag("dialog_prod_low_stock"),
                            singleLine = true
                        )
                    }
                }

                // Optional Barcode
                item {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barcode (Optional)", modifier = Modifier.fillMaxWidth(), textAlign = align) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFE0E0E0),
                            focusedBorderColor = Color(0xFF03DAC5),
                            unfocusedBorderColor = Color(0x1AFFFFFF)
                        ),
                        textStyle = LocalTextStyle.current.copy(textAlign = align),
                        modifier = Modifier.fillMaxWidth().testTag("dialog_prod_barcode"),
                        singleLine = true
                    )
                }

                // Soft-delete button
                if (showDeleteButton) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AEF5350), contentColor = Color(0xFFEF5350)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dialog_delete_product_button")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete Product", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Less than Warning Alert Box inside the list
                if (saleLessThanPurchaseWarning) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFB74D).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFFFB74D), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = "Warning", tint = Color(0xFFFFB74D))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Warning: Sale Price is less than Purchase Price! Tap Save again to proceed.",
                                color = Color(0xFFFFB74D),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pName = name.trim()
                    if (pName.isBlank()) {
                        formError = "Product Name is required."
                        return@Button
                    }

                    val cost = purchasePrice.toDoubleOrNull()
                    if (cost == null || cost < 0.0) {
                        formError = "Purchase Price must be a valid non-negative number."
                        return@Button
                    }

                    val sale = salePrice.toDoubleOrNull()
                    if (sale == null || sale < 0.0) {
                        formError = "Sale Price must be a valid non-negative number."
                        return@Button
                    }

                    val qty = stockQuantity.toIntOrNull()
                    if (qty == null || qty < 0) {
                        formError = "Stock Quantity must be a valid non-negative integer."
                        return@Button
                    }

                    val lowStock = lowStockLimit.toIntOrNull()
                    if (lowStock == null || lowStock < 0) {
                        formError = "Low Stock Limit must be a valid non-negative integer."
                        return@Button
                    }

                    // Warning condition: Sale price less than purchase price
                    if (sale < cost && !saleLessThanPurchaseWarning) {
                        saleLessThanPurchaseWarning = true
                        return@Button
                    }

                    // Proceed to save
                    onSave(pName, category.trim(), cost, sale, qty, lowStock, barcode.trim().takeIf { it.isNotEmpty() })
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5), contentColor = Color(0xFF0C0D14)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("dialog_confirm_add")
            ) {
                Text(confirmButtonLabel, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_cancel_add")
            ) {
                Text("Cancel", color = Color(0xFF90A4AE))
            }
        }
    )
}
