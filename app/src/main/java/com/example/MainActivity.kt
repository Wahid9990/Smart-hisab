package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.localization.Loc
import com.example.ui.screens.BillingScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SignupScreen
import com.example.ui.screens.CustomerListScreen
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.runtime.collectAsState
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.BillingViewModel
import com.example.viewmodel.CustomerViewModel
import com.example.viewmodel.NavigationTarget
import com.example.viewmodel.ProductViewModel
import com.example.viewmodel.ExpenseViewModel
import com.example.viewmodel.ReportViewModel
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.ReportsScreen
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()
                val productViewModel: ProductViewModel = viewModel()
                val billingViewModel: BillingViewModel = viewModel()
                val customerViewModel: CustomerViewModel = viewModel()
                val expenseViewModel: ExpenseViewModel = viewModel()
                val reportViewModel: ReportViewModel = viewModel()

                val userProfile by authViewModel.userProfile.collectAsState()
                LaunchedEffect(userProfile) {
                    userProfile?.uid?.let { uid ->
                        productViewModel.initialize(uid)
                        billingViewModel.initialize(uid)
                        customerViewModel.initialize(uid)
                        expenseViewModel.initialize(uid)
                        reportViewModel.initialize(uid)
                    }
                }

                // Handle navigation events emitted by the ViewModel
                LaunchedEffect(authViewModel.navigationEvent) {
                    authViewModel.navigationEvent.collectLatest { target ->
                        when (target) {
                            is NavigationTarget.Login -> {
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                            is NavigationTarget.Dashboard -> {
                                navController.navigate("main") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            LoginScreen(
                                viewModel = authViewModel,
                                onNavigateToSignup = { navController.navigate("signup") }
                            )
                        }
                        composable("signup") {
                            SignupScreen(
                                viewModel = authViewModel,
                                onNavigateToLogin = { navController.navigate("login") }
                            )
                        }
                        composable("main") {
                            MainAppContainer(
                                authViewModel = authViewModel,
                                productViewModel = productViewModel,
                                billingViewModel = billingViewModel,
                                customerViewModel = customerViewModel,
                                expenseViewModel = expenseViewModel,
                                reportViewModel = reportViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContainer(
    authViewModel: AuthViewModel,
    productViewModel: ProductViewModel,
    billingViewModel: BillingViewModel,
    customerViewModel: CustomerViewModel,
    expenseViewModel: ExpenseViewModel,
    reportViewModel: ReportViewModel,
    modifier: Modifier = Modifier
) {
    val lang by authViewModel.appLanguage.collectAsState()
    var currentTab by remember { mutableStateOf("dashboard") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF161722),
                contentColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("app_bottom_navigation")
            ) {
                // Dashboard Tab
                NavigationBarItem(
                    selected = currentTab == "dashboard",
                    onClick = { currentTab = "dashboard" },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = Loc.t(lang, "dashboard"),
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text(Loc.t(lang, "dashboard"), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0C0D14),
                        selectedTextColor = Color(0xFF03DAC5),
                        indicatorColor = Color(0xFF03DAC5),
                        unselectedIconColor = Color(0xFF78909C),
                        unselectedTextColor = Color(0xFF78909C)
                    ),
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                // Products Tab
                NavigationBarItem(
                    selected = currentTab == "products",
                    onClick = { currentTab = "products" },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = Loc.t(lang, "products"),
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text(Loc.t(lang, "products"), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0C0D14),
                        selectedTextColor = Color(0xFF03DAC5),
                        indicatorColor = Color(0xFF03DAC5),
                        unselectedIconColor = Color(0xFF78909C),
                        unselectedTextColor = Color(0xFF78909C)
                    ),
                    modifier = Modifier.testTag("nav_tab_products")
                )

                // Billing Tab
                NavigationBarItem(
                    selected = currentTab == "billing",
                    onClick = { currentTab = "billing" },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = Loc.t(lang, "billing"),
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text(Loc.t(lang, "billing"), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0C0D14),
                        selectedTextColor = Color(0xFF03DAC5),
                        indicatorColor = Color(0xFF03DAC5),
                        unselectedIconColor = Color(0xFF78909C),
                        unselectedTextColor = Color(0xFF78909C)
                    ),
                    modifier = Modifier.testTag("nav_tab_billing")
                )

                // Customers Tab
                NavigationBarItem(
                    selected = currentTab == "customers" || currentTab == "customer_detail",
                    onClick = { currentTab = "customers" },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = Loc.t(lang, "customers"),
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text(Loc.t(lang, "customers"), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0C0D14),
                        selectedTextColor = Color(0xFF03DAC5),
                        indicatorColor = Color(0xFF03DAC5),
                        unselectedIconColor = Color(0xFF78909C),
                        unselectedTextColor = Color(0xFF78909C)
                    ),
                    modifier = Modifier.testTag("nav_tab_customers")
                )

                // Settings Tab
                NavigationBarItem(
                    selected = currentTab == "settings",
                    onClick = { currentTab = "settings" },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = Loc.t(lang, "settings"),
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text(Loc.t(lang, "settings"), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0C0D14),
                        selectedTextColor = Color(0xFF03DAC5),
                        indicatorColor = Color(0xFF03DAC5),
                        unselectedIconColor = Color(0xFF78909C),
                        unselectedTextColor = Color(0xFF78909C)
                    ),
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                "dashboard" -> DashboardScreen(
                    viewModel = authViewModel,
                    onNavigateToBilling = { currentTab = "billing" },
                    onNavigateToProducts = { currentTab = "products" },
                    onNavigateToSettings = { currentTab = "settings" },
                    onNavigateToCustomers = { currentTab = "customers" },
                    onNavigateToExpenses = { currentTab = "expenses" },
                    onNavigateToReports = { currentTab = "reports" }
                )
                "products" -> ProductsScreen(
                    viewModel = authViewModel,
                    productViewModel = productViewModel
                )
                "billing" -> BillingScreen(
                    viewModel = authViewModel,
                    billingViewModel = billingViewModel
                )
                "customers" -> CustomerListScreen(
                    viewModel = authViewModel,
                    customerViewModel = customerViewModel,
                    onNavigateBack = { currentTab = "dashboard" },
                    onNavigateToDetail = { currentTab = "customer_detail" }
                )
                "customer_detail" -> CustomerDetailScreen(
                    viewModel = authViewModel,
                    customerViewModel = customerViewModel,
                    onNavigateBack = { currentTab = "customers" }
                )
                "expenses" -> ExpensesScreen(
                    authViewModel = authViewModel,
                    expenseViewModel = expenseViewModel,
                    onNavigateBack = { currentTab = "dashboard" }
                )
                "reports" -> ReportsScreen(
                    authViewModel = authViewModel,
                    reportViewModel = reportViewModel,
                    onNavigateBack = { currentTab = "dashboard" }
                )
                "settings" -> SettingsScreen(
                    viewModel = authViewModel
                )
            }
        }
    }
}

// Deprecated or legacy signature preserved for unit / screenshot test compilation
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name! Welcome to Smart Hisab.",
        modifier = modifier
    )
}
