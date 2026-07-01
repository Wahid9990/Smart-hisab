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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localization.Loc
import com.example.model.Invoice
import com.example.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    authViewModel: AuthViewModel,
    reportViewModel: ReportViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang by authViewModel.appLanguage.collectAsState()
    val isRtl = Loc.isRtl(lang)
    val align = Loc.getAlign(lang)
    val context = LocalContext.current

    val filterType by reportViewModel.filterType.collectAsState()
    val customDateRange by reportViewModel.customDateRange.collectAsState()
    val reportData by reportViewModel.reportData.collectAsState()
    val isLoading by reportViewModel.isLoading.collectAsState()

    var showCustomDateSelector by remember { mutableStateOf(false) }
    var tempStartDate by remember { mutableStateOf(System.currentTimeMillis() - 7 * 24 * 3600 * 1000L) }
    var tempEndDate by remember { mutableStateOf(System.currentTimeMillis()) }

    val formatter = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = Loc.t(lang, "reports"),
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("reports_back_button")
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
        containerColor = Color(0xFF0C0D14)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("reports_scrollable_container"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Filters Selector Item
            item {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Report Filters".uppercase(),
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBB86FC),
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Filters horizontal list
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val filterOptions = listOf(
                            Pair(ReportFilterType.TODAY, "Today"),
                            Pair(ReportFilterType.YESTERDAY, "Yesterday"),
                            Pair(ReportFilterType.THIS_WEEK, "This Week"),
                            Pair(ReportFilterType.THIS_MONTH, "This Month"),
                            Pair(ReportFilterType.CUSTOM, "Custom Range")
                        )

                        items(filterOptions) { (type, label) ->
                            val isSelected = filterType == type
                            SuggestionChip(
                                onClick = {
                                    if (type == ReportFilterType.CUSTOM) {
                                        showCustomDateSelector = true
                                    } else {
                                        reportViewModel.setFilterType(type)
                                    }
                                },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
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

                    // Display active custom range description
                    if (filterType == ReportFilterType.CUSTOM && customDateRange != null) {
                        val (start, end) = customDateRange!!
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF03DAC5).copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF03DAC5), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Custom Range: ${formatter.format(Date(start))} - ${formatter.format(Date(end))}",
                                style = TextStyle(fontSize = 11.sp, color = Color(0xFF03DAC5), fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Custom Range inputs
            if (showCustomDateSelector) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161722)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x15FFFFFF)),
                        modifier = Modifier.fillMaxWidth().testTag("custom_date_range_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Set Custom Date Range", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Start date select
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val cal = Calendar.getInstance().apply { timeInMillis = tempStartDate }
                                            DatePickerDialog(
                                                context,
                                                { _, y, m, d ->
                                                    val selected = Calendar.getInstance().apply {
                                                        set(Calendar.YEAR, y)
                                                        set(Calendar.MONTH, m)
                                                        set(Calendar.DAY_OF_MONTH, d)
                                                        set(Calendar.HOUR_OF_DAY, 0)
                                                        set(Calendar.MINUTE, 0)
                                                        set(Calendar.SECOND, 0)
                                                    }
                                                    tempStartDate = selected.timeInMillis
                                                },
                                                cal.get(Calendar.YEAR),
                                                cal.get(Calendar.MONTH),
                                                cal.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color(0x0AFFFFFF)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("From Date", fontSize = 10.sp, color = Color(0xFF90A4AE))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(formatter.format(Date(tempStartDate)), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // End date select
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val cal = Calendar.getInstance().apply { timeInMillis = tempEndDate }
                                            DatePickerDialog(
                                                context,
                                                { _, y, m, d ->
                                                    val selected = Calendar.getInstance().apply {
                                                        set(Calendar.YEAR, y)
                                                        set(Calendar.MONTH, m)
                                                        set(Calendar.DAY_OF_MONTH, d)
                                                        set(Calendar.HOUR_OF_DAY, 23)
                                                        set(Calendar.MINUTE, 59)
                                                        set(Calendar.SECOND, 59)
                                                    }
                                                    tempEndDate = selected.timeInMillis
                                                },
                                                cal.get(Calendar.YEAR),
                                                cal.get(Calendar.MONTH),
                                                cal.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color(0x0AFFFFFF)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("To Date", fontSize = 10.sp, color = Color(0xFF90A4AE))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(formatter.format(Date(tempEndDate)), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showCustomDateSelector = false }) {
                                    Text("Cancel", color = Color(0xFF90A4AE))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (tempStartDate > tempEndDate) {
                                            Toast.makeText(context, "Start date cannot be after end date", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        reportViewModel.setCustomDateRange(tempStartDate, tempEndDate)
                                        showCustomDateSelector = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5), contentColor = Color(0xFF0C0D14))
                                ) {
                                    Text("Apply Filter")
                                }
                            }
                        }
                    }
                }
            }

            // Report Cards Grid (Calculated Metrics as requested)
            item {
                Column {
                    Text(
                        text = "Executive Summary".uppercase(),
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF90A4AE),
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 1: Total Sales & Total Profit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportCard(
                            label = "Total Sales",
                            value = reportData.totalSales,
                            icon = Icons.Default.TrendingUp,
                            iconTint = Color(0xFF03DAC5),
                            modifier = Modifier.weight(1f).testTag("report_card_total_sales")
                        )
                        ReportCard(
                            label = "Total Profit",
                            value = reportData.totalProfit,
                            icon = Icons.Default.AttachMoney,
                            iconTint = Color(0xFF81C784),
                            modifier = Modifier.weight(1f).testTag("report_card_total_profit")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 2: Paid Amount & Unpaid Amount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportCard(
                            label = "Paid Amount",
                            value = reportData.totalPaidAmount,
                            icon = Icons.Default.CheckCircle,
                            iconTint = Color(0xFF03DAC5),
                            modifier = Modifier.weight(1f).testTag("report_card_paid_amount")
                        )
                        ReportCard(
                            label = "Unpaid Amount",
                            value = reportData.totalUnpaidAmount,
                            icon = Icons.Default.Pending,
                            iconTint = Color(0xFFEF5350),
                            modifier = Modifier.weight(1f).testTag("report_card_unpaid_amount")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 3: Total Expenses & Net Profit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportCard(
                            label = "Total Expenses",
                            value = reportData.totalExpenses,
                            icon = Icons.Default.AccountBalanceWallet,
                            iconTint = Color(0xFFEF5350),
                            modifier = Modifier.weight(1f).testTag("report_card_total_expenses")
                        )
                        ReportCard(
                            label = "Net Profit",
                            value = reportData.netProfit,
                            icon = Icons.Default.Stars,
                            iconTint = if (reportData.netProfit >= 0) Color(0xFF81C784) else Color(0xFFEF5350),
                            modifier = Modifier.weight(1f).testTag("report_card_net_profit")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 4: Total Invoices Counter
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x05FFFFFF)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x0AFFFFFF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Receipt, contentDescription = null, tint = Color(0xFFBB86FC), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Total Billing Invoices Raised:", fontSize = 12.sp, color = Color(0xFF90A4AE))
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${reportData.totalInvoicesCount}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Sales proportion visual card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x07FFFFFF)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sales Summary (Invoicing & Dues)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val total = reportData.totalSales
                        val paidPercent = if (total > 0) (reportData.totalPaidAmount / total).toFloat() else 0f
                        val unpaidPercent = if (total > 0) (reportData.totalUnpaidAmount / total).toFloat() else 0f

                        // Simple split bar chart using custom linear progress
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(CircleShape)
                                .background(Color(0x10FFFFFF))
                        ) {
                            if (total > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(maxOf(0.001f, paidPercent))
                                        .background(Color(0xFF03DAC5))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(maxOf(0.001f, unpaidPercent))
                                        .background(Color(0xFFEF5350))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0x15FFFFFF))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF03DAC5)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Collected (Paid)", fontSize = 11.sp, color = Color(0xFF90A4AE))
                            }
                            Text(
                                text = "Rs. ${String.format(Locale.getDefault(), "%,.1f", reportData.totalPaidAmount)} (${String.format(Locale.getDefault(), "%.1f", paidPercent * 100)}%)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF5350)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Outstanding (Dues)", fontSize = 11.sp, color = Color(0xFF90A4AE))
                            }
                            Text(
                                text = "Rs. ${String.format(Locale.getDefault(), "%,.1f", reportData.totalUnpaidAmount)} (${String.format(Locale.getDefault(), "%.1f", unpaidPercent * 100)}%)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Expense Breakdown Summary Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x07FFFFFF)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Expense Breakdown",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (reportData.categoryExpenses.isEmpty()) {
                            Text(
                                text = "No expenses recorded in this period.",
                                fontSize = 12.sp,
                                color = Color(0xFF90A4AE),
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        } else {
                            reportData.categoryExpenses.forEach { catExp ->
                                val color = getCategoryColor(catExp.category)
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(catExp.category, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                        }
                                        Text(
                                            text = "Rs. ${String.format(Locale.getDefault(), "%,.0f", catExp.totalAmount)} (${String.format(Locale.getDefault(), "%.1f", catExp.percentage)}%)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Custom Linear Progress bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x0DFFFFFF))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth((catExp.percentage / 100).toFloat())
                                                .background(color)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Top Selling Products Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x07FFFFFF)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Top Selling Products",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF03DAC5), modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        if (reportData.topSellingProducts.isEmpty()) {
                            Text(
                                text = "No sales recorded in this period.",
                                fontSize = 12.sp,
                                color = Color(0xFF90A4AE),
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        } else {
                            reportData.topSellingProducts.take(5).forEachIndexed { index, prod ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Rank Number Badge
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF03DAC5).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("#${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF03DAC5))
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prod.productName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("Sold: ${prod.quantitySold} units", fontSize = 10.sp, color = Color(0xFF90A4AE))
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Rs. ${String.format(Locale.getDefault(), "%,.0f", prod.totalRevenue)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Profit: Rs. ${String.format(Locale.getDefault(), "%,.0f", prod.totalProfit)}", fontSize = 10.sp, color = Color(0xFF03DAC5))
                                    }
                                }
                                if (index < minOf(4, reportData.topSellingProducts.size - 1)) {
                                    Divider(color = Color(0x0AFFFFFF), thickness = 1.dp)
                                }
                            }
                        }
                    }
                }
            }

            // Low Selling Products Card (optional but highly useful)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x07FFFFFF)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Low / Slow Selling Products",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(imageVector = Icons.Default.TrendingDown, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        if (reportData.lowSellingProducts.isEmpty()) {
                            Text(
                                text = "No catalog products found.",
                                fontSize = 12.sp,
                                color = Color(0xFF90A4AE),
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        } else {
                            reportData.lowSellingProducts.take(5).forEachIndexed { index, prod ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Slow warning Indicator
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF5350).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(11.dp))
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prod.productName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("Sold: ${prod.quantitySold} units", fontSize = 10.sp, color = Color(0xFF90A4AE))
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Rs. ${String.format(Locale.getDefault(), "%,.0f", prod.totalRevenue)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Profit: Rs. ${String.format(Locale.getDefault(), "%,.0f", prod.totalProfit)}", fontSize = 10.sp, color = Color(0xFF90A4AE))
                                    }
                                }
                                if (index < minOf(4, reportData.lowSellingProducts.size - 1)) {
                                    Divider(color = Color(0x0AFFFFFF), thickness = 1.dp)
                                }
                            }
                        }
                    }
                }
            }

            // Unpaid Invoices List Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x07FFFFFF)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Unpaid & Partial Invoices",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        if (reportData.unpaidInvoices.isEmpty()) {
                            Text(
                                text = "No unpaid invoices in this period.",
                                fontSize = 12.sp,
                                color = Color(0xFF81C784),
                                modifier = Modifier.padding(vertical = 10.dp),
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            reportData.unpaidInvoices.forEachIndexed { index, invoice ->
                                val dateStr = formatter.format(Date(invoice.createdAt))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = invoice.invoiceNumber,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(
                                                        if (invoice.paymentStatus == "unpaid") Color(0xFFEF5350).copy(alpha = 0.15f)
                                                        else Color(0xFFFFB74D).copy(alpha = 0.15f)
                                                    )
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = invoice.paymentStatus.uppercase(),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (invoice.paymentStatus == "unpaid") Color(0xFFEF5350) else Color(0xFFFFB74D)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Cust: ${invoice.customerName ?: "Walk-in"}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF90A4AE),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = dateStr,
                                            fontSize = 10.sp,
                                            color = Color(0xFF78909C)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Total: Rs. ${String.format(Locale.getDefault(), "%,.0f", invoice.totalAmount)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Due: Rs. ${String.format(Locale.getDefault(), "%,.0f", invoice.remainingAmount)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEF5350)
                                        )
                                    }
                                }
                                if (index < reportData.unpaidInvoices.size - 1) {
                                    Divider(color = Color(0x0AFFFFFF), thickness = 1.dp)
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Spacer
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ReportCard(
    label: String,
    value: Double,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0x05FFFFFF)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x0AFFFFFF)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = label, fontSize = 11.sp, color = Color(0xFF90A4AE), fontWeight = FontWeight.Medium)
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Rs. ${String.format(Locale.getDefault(), "%,.0f", value)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
