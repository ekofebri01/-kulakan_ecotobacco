package com.aistudio.ecotobacco.kfzqw

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.ecotobacco.kfzqw.data.local.AppDatabase
import com.aistudio.ecotobacco.kfzqw.data.repository.TobaccoRepository
import com.aistudio.ecotobacco.kfzqw.data.utils.LocalScanner
import com.aistudio.ecotobacco.kfzqw.ui.Routes
import com.aistudio.ecotobacco.kfzqw.ui.screens.debug.DebugMenuScreen
import com.aistudio.ecotobacco.kfzqw.ui.screens.home.HomeScreen
import com.aistudio.ecotobacco.kfzqw.ui.screens.procurement.ProcurementPlanningScreen
import com.aistudio.ecotobacco.kfzqw.ui.screens.products.ProductMasterScreen
import com.aistudio.ecotobacco.kfzqw.ui.screens.reports.ReportsScreen
import com.aistudio.ecotobacco.kfzqw.ui.screens.scanner.ReceiptScannerScreen
import com.aistudio.ecotobacco.kfzqw.ui.screens.settings.SettingsScreen
import com.aistudio.ecotobacco.kfzqw.ui.screens.splash.SplashScreen
import com.aistudio.ecotobacco.kfzqw.ui.screens.transactions.TransactionEntryScreen
import com.aistudio.ecotobacco.kfzqw.ui.theme.EcoTobaccoTheme
import com.aistudio.ecotobacco.kfzqw.viewmodel.TobaccoViewModel
import com.aistudio.ecotobacco.kfzqw.viewmodel.TobaccoViewModelFactory
import com.aistudio.ecotobacco.kfzqw.widget.TobaccoAppWidgetProvider

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: TobaccoViewModel
    private var pendingRoute: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Window & status bar configuration
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())

        // Database and ViewModel setup
        val db = AppDatabase.getInstance(applicationContext)
        val repository = TobaccoRepository(db.productDao(), db.transactionDao(), db.procurementDao())
        val factory = TobaccoViewModelFactory(repository, application)
        viewModel = ViewModelProvider(this, factory)[TobaccoViewModel::class.java]

        pendingRoute = intent?.getStringExtra(TobaccoAppWidgetProvider.EXTRA_NAVIGATE_TO)

        setContent {
            EcoTobaccoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        viewModel = viewModel,
                        initialRoute = pendingRoute ?: Routes.HOME
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(TobaccoAppWidgetProvider.EXTRA_NAVIGATE_TO)?.let { route ->
            pendingRoute = route
            setContent {
                EcoTobaccoTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(
                            viewModel = viewModel,
                            initialRoute = route
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    viewModel: TobaccoViewModel,
    initialRoute: String
) {
    var currentRoute by remember { mutableStateOf(initialRoute) }
    var reportFilter by remember { mutableStateOf("Semua") }
    var scannedResult by remember { mutableStateOf<LocalScanner.ScannedResult?>(null) }
    var previousRoute by remember { mutableStateOf(Routes.HOME) }

    val isAppReady by viewModel.isAppReady.collectAsStateWithLifecycle()
    val initProgress by viewModel.appInitProgress.collectAsStateWithLifecycle()
    val initStatusText by viewModel.initStatusText.collectAsStateWithLifecycle()
    var splashDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(initialRoute) {
        if (initialRoute == Routes.REPORTS_THIS_MONTH) {
            reportFilter = "Bulan Ini"
            currentRoute = Routes.REPORTS
        } else {
            currentRoute = initialRoute
        }
    }

    if (!isAppReady && !splashDismissed) {
        SplashScreen(
            progress = initProgress,
            statusText = initStatusText,
            onFinished = { splashDismissed = true }
        )
    } else {
        when (currentRoute) {
            Routes.HOME -> {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToTransaction = {
                        previousRoute = Routes.HOME
                        currentRoute = Routes.TRANSACTION_ENTRY
                    },
                    onNavigateToProducts = {
                        previousRoute = Routes.HOME
                        currentRoute = Routes.PRODUCT_MASTER
                    },
                    onNavigateToReports = {
                        reportFilter = "Semua"
                        previousRoute = Routes.HOME
                        currentRoute = Routes.REPORTS
                    },
                    onNavigateToReportsThisMonth = {
                        reportFilter = "Bulan Ini"
                        previousRoute = Routes.HOME
                        currentRoute = Routes.REPORTS
                    },
                    onNavigateToSettings = {
                        previousRoute = Routes.HOME
                        currentRoute = Routes.SETTINGS
                    },
                    onNavigateToDebug = {
                        previousRoute = Routes.HOME
                        currentRoute = Routes.DEBUG_MENU
                    },
                    onNavigateToProcurement = {
                        previousRoute = Routes.HOME
                        currentRoute = Routes.PROCUREMENT_PLANNING
                    }
                )
            }

            Routes.TRANSACTION_ENTRY -> {
                TransactionEntryScreen(
                    viewModel = viewModel,
                    scannedResult = scannedResult,
                    onNavigateToScanner = {
                        currentRoute = Routes.RECEIPT_SCANNER
                    },
                    onNavigateBack = {
                        scannedResult = null
                        currentRoute = Routes.HOME
                    }
                )
            }

            Routes.RECEIPT_SCANNER -> {
                ReceiptScannerScreen(
                    onNavigateBack = {
                        currentRoute = Routes.TRANSACTION_ENTRY
                    },
                    onScanComplete = { result ->
                        scannedResult = result
                        currentRoute = Routes.TRANSACTION_ENTRY
                    }
                )
            }

            Routes.PRODUCT_MASTER -> {
                ProductMasterScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        currentRoute = Routes.HOME
                    }
                )
            }

            Routes.REPORTS, Routes.REPORTS_THIS_MONTH -> {
                ReportsScreen(
                    viewModel = viewModel,
                    onNavigateToTransaction = {
                        currentRoute = Routes.TRANSACTION_ENTRY
                    },
                    onNavigateToSettings = {
                        currentRoute = Routes.SETTINGS
                    },
                    initialFilterBy = reportFilter
                )
            }

            Routes.SETTINGS -> {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        currentRoute = Routes.HOME
                    },
                    onNavigateToDebug = {
                        currentRoute = Routes.DEBUG_MENU
                    }
                )
            }

            Routes.DEBUG_MENU -> {
                DebugMenuScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        currentRoute = previousRoute
                    }
                )
            }

            Routes.PROCUREMENT_PLANNING -> {
                ProcurementPlanningScreen(
                    viewModel = viewModel,
                    scannedResult = scannedResult,
                    onNavigateBack = {
                        scannedResult = null
                        currentRoute = Routes.HOME
                    }
                )
            }

            else -> {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToTransaction = { currentRoute = Routes.TRANSACTION_ENTRY },
                    onNavigateToProducts = { currentRoute = Routes.PRODUCT_MASTER },
                    onNavigateToReports = {
                        reportFilter = "Semua"
                        currentRoute = Routes.REPORTS
                    },
                    onNavigateToReportsThisMonth = {
                        reportFilter = "Bulan Ini"
                        currentRoute = Routes.REPORTS
                    },
                    onNavigateToSettings = { currentRoute = Routes.SETTINGS },
                    onNavigateToDebug = { currentRoute = Routes.DEBUG_MENU },
                    onNavigateToProcurement = { currentRoute = Routes.PROCUREMENT_PLANNING }
                )
            }
        }
    }
}
