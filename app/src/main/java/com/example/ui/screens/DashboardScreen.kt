package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localization.Loc
import com.example.model.UserProfile
import com.example.model.Product
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.CustomerViewModel
import com.example.viewmodel.ProductViewModel
import com.example.viewmodel.ExpenseViewModel
import com.example.viewmodel.ReportViewModel
import com.example.utils.NetworkMonitor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DashboardStats(
    val todaySales: Double,
    val todayProfit: Double,
    val totalUdhaar: Double,
    val lowStockCount: Int,
    val totalProducts: Int,
    val thisMonthSales: Double,
    val thisMonthExpenses: Double,
    val netProfit: Double,
    val lowStockProducts: List<Product>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: AuthViewModel,
    onNavigateToBilling: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToReports: () -> Unit,
    customerViewModel: CustomerViewModel = viewModel(),
    productViewModel: ProductViewModel = viewModel(),
    expenseViewModel: ExpenseViewModel = viewModel(),
    reportViewModel: ReportViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val userProfileState by viewModel.userProfile.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()
    val totalUdhaarAmount by customerViewModel.totalUdhaar.collectAsState()

    val userProfile = userProfileState ?: UserProfile(
        shopName = "Smart Ledger",
        ownerName = "Valued Merchant",
        email = "merchant@smarthisab.com",
        phone = "+91 98765 43210"
    )

    val isRtl = Loc.isRtl(lang)
    val align = Loc.getAlign(lang)

    // Safeguard to trigger initialization in all ViewModels for live stream flows
    val uid = userProfile.uid
    LaunchedEffect(uid) {
        if (uid.isNotBlank()) {
            productViewModel.initialize(uid)
            customerViewModel.initialize(uid)
            expenseViewModel.initialize(uid)
            reportViewModel.initialize(uid)
        }
    }

    // Monitor connectivity
    val context = LocalContext.current
    val networkMonitor = remember { NetworkMonitor(context) }
    val isOnline by networkMonitor.isOnline.collectAsState(initial = networkMonitor.isCurrentlyConnected())

    // Live streams collected from ViewModels
    val invoices by reportViewModel.allInvoices.collectAsState()
    val expenses by reportViewModel.allExpenses.collectAsState()
    val products by productViewModel.products.collectAsState()

    // Real-time calculation of DashboardStats from the live-updating database streams
    val dashboardStats = remember(invoices, expenses, products, totalUdhaarAmount) {
        // Today bounds
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        // This Month bounds
        val thisMonthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // 1. Today Sales
        val todayInvoices = invoices.filter { it.createdAt in todayStart..todayEnd }
        val todaySales = todayInvoices.sumOf { it.totalAmount }

        // 2. Today Profit
        val todayProfit = todayInvoices.flatMap { it.items }.sumOf { item ->
            (item.salePrice - item.purchasePrice) * item.quantity
        }

        // 3. Total Udhaar is totalUdhaarAmount

        // 4. Low Stock Products Count & list
        val lowStockProducts = products.filter { it.isActive && it.stockQuantity <= it.lowStockLimit }
        val lowStockCount = lowStockProducts.size

        // 5. Total Products
        val totalProducts = products.filter { it.isActive }.size

        // 6. This Month Sales
        val thisMonthInvoices = invoices.filter { it.createdAt >= thisMonthStart }
        val thisMonthSales = thisMonthInvoices.sumOf { it.totalAmount }

        // 7. This Month Expenses
        val thisMonthExpensesList = expenses.filter { it.date >= thisMonthStart }
        val thisMonthExpenses = thisMonthExpensesList.sumOf { it.amount }

        // 8. Net Profit = (This Month Sales Gross Profit) - (This Month Expenses)
        val thisMonthProfit = thisMonthInvoices.flatMap { it.items }.sumOf { item ->
            (item.salePrice - item.purchasePrice) * item.quantity
        }
        val netProfit = thisMonthProfit - thisMonthExpenses

        DashboardStats(
            todaySales = todaySales,
            todayProfit = todayProfit,
            totalUdhaar = totalUdhaarAmount,
            lowStockCount = lowStockCount,
            totalProducts = totalProducts,
            thisMonthSales = thisMonthSales,
            thisMonthExpenses = thisMonthExpenses,
            netProfit = netProfit,
            lowStockProducts = lowStockProducts
        )
    }

    // Dialog state for Restocking feature
    var showRestockDialog by remember { mutableStateOf(false) }
    var productToRestock by remember { mutableStateOf<Product?>(null) }

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
            // Main Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!isRtl) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF03DAC5).copy(alpha = 0.15f))
                                .border(1.5.dp, Color(0xFF03DAC5), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userProfile.ownerName.take(1).uppercase(Locale.getDefault()),
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF03DAC5)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = userProfile.shopName,
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${Loc.t(lang, "proprietor")}: ${userProfile.ownerName}",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    color = Color(0xFF90A4AE)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Logout button top right / left
                IconButton(
                    onClick = { viewModel.logOut() },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1AEF5350))
                        .size(40.dp)
                        .testTag("logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (isRtl) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = userProfile.shopName,
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Right
                            )
                            Text(
                                text = "${Loc.t(lang, "proprietor")}: ${userProfile.ownerName}",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    color = Color(0xFF90A4AE)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Right
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF03DAC5).copy(alpha = 0.15f))
                                .border(1.5.dp, Color(0xFF03DAC5), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userProfile.ownerName.take(1).uppercase(Locale.getDefault()),
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF03DAC5)
                                )
                            )
                        }
                    }
                }
            }

            // Quick Status Info Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x0AFFFFFF))
                    .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isFirebaseAvailable = viewModel.isFirebaseAvailable
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (!isFirebaseAvailable) Color(0xFFE57373)
                                else if (isOnline) Color(0xFF03DAC5)
                                else Color(0xFFFFB74D)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (!isFirebaseAvailable) {
                            Loc.t(lang, "demo_local_mode")
                        } else if (isOnline) {
                            Loc.t(lang, "active_cloud")
                        } else {
                            Loc.t(lang, "offline_cloud")
                        },
                        style = TextStyle(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (!isFirebaseAvailable) Color(0xFFE57373)
                                    else if (isOnline) Color(0xFF03DAC5)
                                    else Color(0xFFFFB74D)
                        )
                    )
                }
                
                Text(
                    text = "${Loc.t(lang, "currency_label")}: ${userProfile.currency} | LANG: ${lang.uppercase()}",
                    style = TextStyle(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF90A4AE)
                    )
                )
            }

            // Scrollable Dashboard Layout
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("dashboard_content_column"),
                contentPadding = PaddingValues(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Offline Warning Banner if connection lost
                if (!isOnline) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("offline_banner_card"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F).copy(alpha = 0.15f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = "Offline Mode",
                                    tint = Color(0xFFEF5350),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = Loc.t(lang, "offline_banner"),
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                // Personal Greeting
                item {
                    Text(
                        text = "${Loc.t(lang, "custom_greeting")} ${userProfile.ownerName}!",
                        style = TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = align
                    )
                }

                // Dashboard Quick Actions Row
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = Loc.t(lang, "active_ledger").uppercase(),
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFBB86FC),
                                letterSpacing = 1.2.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = align
                            ),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // New Bill Action
                            QuickActionItem(
                                icon = Icons.Default.ReceiptLong,
                                label = Loc.t(lang, "new_bill"),
                                color = Color(0xFF03DAC5),
                                onClick = onNavigateToBilling,
                                testTag = "quick_action_new_bill"
                            )
                            // Add Product Action
                            QuickActionItem(
                                icon = Icons.Default.AddBox,
                                label = Loc.t(lang, "add_product"),
                                color = Color(0xFFBB86FC),
                                onClick = onNavigateToProducts,
                                testTag = "quick_action_add_product"
                            )
                            // Add Customer Action
                            QuickActionItem(
                                icon = Icons.Default.PersonAdd,
                                label = Loc.t(lang, "add_customer"),
                                color = Color(0xFF2196F3),
                                onClick = onNavigateToCustomers,
                                testTag = "quick_action_add_customer"
                            )
                            // Add Expense Action
                            QuickActionItem(
                                icon = Icons.Default.AccountBalanceWallet,
                                label = Loc.t(lang, "add_expense"),
                                color = Color(0xFFFFB74D),
                                onClick = onNavigateToExpenses,
                                testTag = "quick_action_add_expense"
                            )
                            // View Reports Action
                            QuickActionItem(
                                icon = Icons.Default.Analytics,
                                label = Loc.t(lang, "view_reports"),
                                color = Color(0xFF4DB6AC),
                                onClick = onNavigateToReports,
                                testTag = "quick_action_view_reports"
                            )
                        }
                    }
                }

                // Today's KPIs Summary Cards
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${Loc.t(lang, "welcome_back").uppercase()} • ${Loc.t(lang, "ledger_summary")}",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFBB86FC),
                                letterSpacing = 1.2.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = align
                            ),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Today Sales Card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(110.dp)
                                    .testTag("today_sales_card"),
                                colors = CardDefaults.cardColors(containerColor = Color(0x0CFFFFFF)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF)),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = if (isRtl) Alignment.End else Alignment.Start
                                ) {
                                    Text(
                                        text = Loc.t(lang, "today_sales"),
                                        fontSize = 11.sp,
                                        color = Color(0xFF90A4AE),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${userProfile.currency} ${String.format(Locale.getDefault(), "%,.0f", dashboardStats.todaySales)}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Today Profit Card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(110.dp)
                                    .testTag("today_profit_card"),
                                colors = CardDefaults.cardColors(containerColor = Color(0x0CFFFFFF)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF)),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = if (isRtl) Alignment.End else Alignment.Start
                                ) {
                                    Text(
                                        text = Loc.t(lang, "today_profit"),
                                        fontSize = 11.sp,
                                        color = Color(0xFF90A4AE),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${userProfile.currency} ${String.format(Locale.getDefault(), "%,.0f", dashboardStats.todayProfit)}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF03DAC5)
                                    )
                                }
                            }
                        }
                    }
                }

                // This Month Summary KPI Matrix
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("this_month_kpi_card"),
                        colors = CardDefaults.cardColors(containerColor = Color(0x12FFFFFF)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x18FFFFFF)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "THIS MONTH OVERVIEW",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFBB86FC),
                                    letterSpacing = 1.2.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = align
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = Loc.t(lang, "this_month_sales"),
                                        fontSize = 11.sp,
                                        color = Color(0xFF90A4AE),
                                        textAlign = align,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${userProfile.currency} ${String.format(Locale.getDefault(), "%,.0f", dashboardStats.thisMonthSales)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = align,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = Loc.t(lang, "this_month_expenses"),
                                        fontSize = 11.sp,
                                        color = Color(0xFF90A4AE),
                                        textAlign = align,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${userProfile.currency} ${String.format(Locale.getDefault(), "%,.0f", dashboardStats.thisMonthExpenses)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEF5350),
                                        textAlign = align,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Divider(color = Color(0x10FFFFFF))

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = Loc.t(lang, "net_profit"),
                                    fontSize = 12.sp,
                                    color = Color(0xFF03DAC5),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = align,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${userProfile.currency} ${String.format(Locale.getDefault(), "%,.0f", dashboardStats.netProfit)}",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF03DAC5),
                                    textAlign = align,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Udhaar & Catalog Overview Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Total Udhaar Card
                        Card(
                            modifier = Modifier
                                .weight(1.2f)
                                .height(110.dp)
                                .clickable { onNavigateToCustomers() }
                                .testTag("total_udhaar_card"),
                            colors = CardDefaults.cardColors(containerColor = Color(0x0CFFFFFF)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF)),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = if (isRtl) Alignment.End else Alignment.Start
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = if (isRtl) Arrangement.End else Arrangement.Start,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (!isRtl) {
                                        Icon(
                                            imageVector = Icons.Default.TrendingUp,
                                            contentDescription = null,
                                            tint = Color(0xFFEF5350),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = Loc.t(lang, "total_udhaar"),
                                        fontSize = 11.sp,
                                        color = Color(0xFF90A4AE),
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (isRtl) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.TrendingUp,
                                            contentDescription = null,
                                            tint = Color(0xFFEF5350),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${userProfile.currency} ${String.format(Locale.getDefault(), "%,.0f", dashboardStats.totalUdhaar)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFEF5350)
                                )
                            }
                        }

                        // Total Products Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(110.dp)
                                .clickable { onNavigateToProducts() }
                                .testTag("total_products_card"),
                            colors = CardDefaults.cardColors(containerColor = Color(0x0CFFFFFF)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF)),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = if (isRtl) Alignment.End else Alignment.Start
                            ) {
                                Text(
                                    text = Loc.t(lang, "total_products"),
                                    fontSize = 11.sp,
                                    color = Color(0xFF90A4AE),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${dashboardStats.totalProducts}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Low Stock Alerts section
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (isRtl) Arrangement.End else Arrangement.Start,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (!isRtl) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Alerts",
                                    tint = if (dashboardStats.lowStockCount > 0) Color(0xFFFFB74D) else Color(0xFF03DAC5),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = Loc.t(lang, "low_stock_alert"),
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dashboardStats.lowStockCount > 0) Color(0xFFFFB74D) else Color(0xFF90A4AE),
                                    letterSpacing = 1.2.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                            if (isRtl) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Alerts",
                                    tint = if (dashboardStats.lowStockCount > 0) Color(0xFFFFB74D) else Color(0xFF03DAC5),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (dashboardStats.lowStockCount > 0) Color(0x2EFFB74D) else Color(0x1F03DAC5)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${dashboardStats.lowStockCount} items",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dashboardStats.lowStockCount > 0) Color(0xFFFFB74D) else Color(0xFF03DAC5)
                                )
                            )
                        }
                    }
                }

                // Low Stock Products dynamic list
                if (dashboardStats.lowStockProducts.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("low_stock_empty_card"),
                            colors = CardDefaults.cardColors(containerColor = Color(0x06FFFFFF)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "All products are safely stocked!",
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    color = Color(0xFF78909C),
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            )
                        }
                    }
                } else {
                    items(dashboardStats.lowStockProducts) { prod ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("low_stock_item_${prod.id}"),
                            colors = CardDefaults.cardColors(containerColor = Color(0x0AFFFFFF)),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prod.name,
                                        style = TextStyle(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${Loc.t(lang, "current_stock")}: ",
                                            style = TextStyle(fontSize = 11.sp, color = Color(0xFF90A4AE))
                                        )
                                        Text(
                                            text = "${prod.stockQuantity}",
                                            style = TextStyle(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (prod.stockQuantity <= 0) Color(0xFFEF5350) else Color(0xFFFFB74D)
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "${Loc.t(lang, "low_stock_limit")}: ",
                                            style = TextStyle(fontSize = 11.sp, color = Color(0xFF90A4AE))
                                        )
                                        Text(
                                            text = "${prod.lowStockLimit}",
                                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        productToRestock = prod
                                        showRestockDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x1A03DAC5)),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("restock_button_${prod.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Publish,
                                        contentDescription = "Restock",
                                        tint = Color(0xFF03DAC5),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = Loc.t(lang, "restock"),
                                        style = TextStyle(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF03DAC5)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Interactive Restock Dialog
    if (showRestockDialog && productToRestock != null) {
        val prod = productToRestock!!
        var qtyString by remember { mutableStateOf("") }
        var noteString by remember { mutableStateOf("") }
        var inputError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                showRestockDialog = false
                productToRestock = null
            },
            title = {
                Text(
                    text = "${Loc.t(lang, "restock")}: ${prod.name}",
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = align
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Info Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = Loc.t(lang, "current_stock"), fontSize = 11.sp, color = Color(0xFF90A4AE))
                            Text(text = "${prod.stockQuantity}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = Loc.t(lang, "low_stock_limit"), fontSize = 11.sp, color = Color(0xFF90A4AE))
                            Text(text = "${prod.lowStockLimit}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB74D))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Quantity Input
                    OutlinedTextField(
                        value = qtyString,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() }) {
                                qtyString = it
                                inputError = null
                            }
                        },
                        label = { Text(Loc.t(lang, "quantity_to_add")) },
                        isError = inputError != null,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF03DAC5),
                            unfocusedLabelColor = Color(0xFF90A4AE),
                            focusedBorderColor = Color(0xFF03DAC5),
                            unfocusedBorderColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("restock_quantity_input")
                    )

                    if (inputError != null) {
                        Text(text = inputError!!, color = Color(0xFFEF5350), fontSize = 11.sp)
                    }

                    // Optional Note
                    OutlinedTextField(
                        value = noteString,
                        onValueChange = { noteString = it },
                        label = { Text(Loc.t(lang, "optional_note")) },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF03DAC5),
                            unfocusedLabelColor = Color(0xFF90A4AE),
                            focusedBorderColor = Color(0xFF03DAC5),
                            unfocusedBorderColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("restock_note_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val addedQty = qtyString.toIntOrNull()
                        if (addedQty == null || addedQty <= 0) {
                            inputError = "Please enter a valid positive quantity."
                            return@Button
                        }

                        val newStock = prod.stockQuantity + addedQty
                        val updatedProd = prod.copy(stockQuantity = newStock, updatedAt = System.currentTimeMillis())
                        
                        // Update Product in ViewModel
                        productViewModel.updateProduct(updatedProd)

                        // Write Restock Log to Firestore (Offline queued if offline)
                        val currentUid = userProfile.uid
                        if (currentUid.isNotBlank() && viewModel.isFirebaseAvailable) {
                            try {
                                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                val logId = java.util.UUID.randomUUID().toString()
                                val logData = mapOf(
                                    "id" to logId,
                                    "productId" to prod.id,
                                    "productName" to prod.name,
                                    "addedQuantity" to addedQty,
                                    "oldQuantity" to prod.stockQuantity,
                                    "newQuantity" to newStock,
                                    "note" to noteString.trim(),
                                    "timestamp" to System.currentTimeMillis()
                                )
                                db.collection("users")
                                    .document(currentUid)
                                    .collection("restock_logs")
                                    .document(logId)
                                    .set(logData)
                            } catch (e: Exception) {
                                android.util.Log.e("DashboardScreen", "Failed to save restock log", e)
                            }
                        }

                        showRestockDialog = false
                        productToRestock = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5)),
                    modifier = Modifier.testTag("restock_save_button")
                ) {
                    Text(Loc.t(lang, "save_update"), color = Color(0xFF0C0D14), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestockDialog = false
                        productToRestock = null
                    },
                    modifier = Modifier.testTag("restock_cancel_button")
                ) {
                    Text(Loc.t(lang, "cancel"), color = Color(0xFF90A4AE))
                }
            },
            containerColor = Color(0xFF1E1F2C),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun QuickActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(105.dp)
            .height(95.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = Color(0x0CFFFFFF)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
