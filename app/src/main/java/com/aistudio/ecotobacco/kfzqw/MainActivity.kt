package com.aistudio.ecotobacco.kfzqw

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.ecotobacco.kfzqw.data.local.TobaccoDatabase
import com.aistudio.ecotobacco.kfzqw.data.repository.TobaccoRepository
import com.aistudio.ecotobacco.kfzqw.data.utils.LocalScanner
import com.aistudio.ecotobacco.kfzqw.ui.Routes
import com.aistudio.ecotobacco.kfzqw.ui.components.InAppUpdateHost
import com.aistudio.ecotobacco.kfzqw.ui.components.WhatsNewChangelogDialog
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
import com.aistudio.ecotobacco.kfzqw.ui.theme.UiConfig
import com.aistudio.ecotobacco.kfzqw.viewmodel.TobaccoViewModel
import com.aistudio.ecotobacco.kfzqw.viewmodel.TobaccoViewModelFactory
import com.aistudio.ecotobacco.kfzqw.widget.TobaccoAppWidgetProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: TobaccoViewModel
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = TobaccoDatabase.getDatabase(applicationContext)
        val repository = TobaccoRepository(db.tobaccoDao())
        val factory = TobaccoViewModelFactory(repository, application)
        viewModel = ViewModelProvider(this, factory)[TobaccoViewModel::class.java]

        pendingRoute = intent?.getStringExtra(TobaccoAppWidgetProvider.EXTRA_NAVIGATE_TO)

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val customColor by viewModel.customColor.collectAsStateWithLifecycle()
            val fontSizeScale by viewModel.fontSizeScale.collectAsStateWithLifecycle()
            val paddingScale by viewModel.paddingScale.collectAsStateWithLifecycle()
            val buttonHeight by viewModel.buttonHeight.collectAsStateWithLifecycle()
            val cornerRadius by viewModel.cornerRadius.collectAsStateWithLifecycle()
            val customTextColor by viewModel.customTextColor.collectAsStateWithLifecycle()
            val customBackgroundColor by viewModel.customBackgroundColor.collectAsStateWithLifecycle()
            val showMatrixBackground by viewModel.showMatrixBackground.collectAsStateWithLifecycle()

            val uiConfig = remember(
                fontSizeScale,
                paddingScale,
                buttonHeight,
                cornerRadius,
                customTextColor,
                customBackgroundColor,
                showMatrixBackground
            ) {
                UiConfig(
                    fontSizeScale = fontSizeScale,
                    paddingScale = paddingScale,
                    buttonHeight = buttonHeight,
                    cornerRadius = cornerRadius,
                    customTextColor = customTextColor,
                    customBackgroundColor = customBackgroundColor,
                    showMatrixBackground = showMatrixBackground
                )
            }

            EcoTobaccoTheme(
                themeMode = themeMode,
                customColor = customColor,
                uiConfig = uiConfig
            ) {
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
        }
    }
}

@Composable
fun AppNavigation(
    viewModel: TobaccoViewModel,
    initialRoute: String
) {
    var currentRoute by remember {
        mutableStateOf(if (initialRoute == Routes.SETTINGS) Routes.HOME else initialRoute)
    }
    var isSettingsOpen by remember { mutableStateOf(initialRoute == Routes.SETTINGS) }
    var reportFilter by remember { mutableStateOf<String?>(null) }
    var scannedResult by remember { mutableStateOf<LocalScanner.ScannedResult?>(null) }
    var previousRoute by remember { mutableStateOf(Routes.HOME) }

    val defaultReportFilter by viewModel.defaultReportFilter.collectAsStateWithLifecycle()
    val isAppReady by viewModel.isAppReady.collectAsStateWithLifecycle()
    val initProgress by viewModel.appInitProgress.collectAsStateWithLifecycle()
    val initStatusText by viewModel.initStatusText.collectAsStateWithLifecycle()
    val showWhatsNewDialog by viewModel.showWhatsNewDialog.collectAsStateWithLifecycle()
    var splashDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(initialRoute) {
        when (initialRoute) {
            Routes.REPORTS_THIS_MONTH -> {
                reportFilter = "Bulanan"
                currentRoute = Routes.REPORTS
                isSettingsOpen = false
            }
            Routes.SETTINGS -> {
                currentRoute = Routes.HOME
                isSettingsOpen = true
            }
            else -> {
                currentRoute = initialRoute
                isSettingsOpen = false
            }
        }
    }

    if (!isAppReady && !splashDismissed) {
        SplashScreen(
            progress = initProgress,
            statusText = initStatusText,
            onFinished = { splashDismissed = true }
        )
    } else {
        BackHandler(enabled = !isSettingsOpen && currentRoute != Routes.HOME) {
            when (currentRoute) {
                Routes.RECEIPT_SCANNER -> {
                    currentRoute = if (previousRoute == Routes.HOME) Routes.HOME else Routes.TRANSACTION_ENTRY
                }
                Routes.DEBUG_MENU -> currentRoute = previousRoute
                else -> {
                    scannedResult = null
                    currentRoute = Routes.HOME
                }
            }
        }

        val coroutineScope = rememberCoroutineScope()
        val settingsOpenProgress = remember {
            Animatable(if (initialRoute == Routes.SETTINGS) 1f else 0f)
        }
        var pendingCloseVelocity by remember { mutableFloatStateOf(0f) }
        var isSettingsPrewarmed by remember { mutableStateOf(initialRoute == Routes.SETTINGS) }

        LaunchedEffect(isAppReady, splashDismissed) {
            if (!isSettingsPrewarmed) {
                delay(350L)
                isSettingsPrewarmed = true
            }
        }

        LaunchedEffect(isSettingsOpen) {
            if (isSettingsOpen) {
                isSettingsPrewarmed = true
                settingsOpenProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = 290f
                    )
                )
            } else {
                val initialVelocity = pendingCloseVelocity
                pendingCloseVelocity = 0f
                settingsOpenProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = 340f
                    ),
                    initialVelocity = initialVelocity
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val panelWidthPx = with(density) {
                min(maxWidth.toPx() * 0.92f, 460.dp.toPx()).coerceAtLeast(1f)
            }
            val isPanelVisible by remember {
                derivedStateOf { isSettingsOpen || settingsOpenProgress.value > 0.002f }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = settingsOpenProgress.value
                        translationX = if (p > 0.001f) -panelWidthPx * 0.07f * p else 0f
                    }
            ) {
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
                            reportFilter = null
                            previousRoute = Routes.HOME
                            currentRoute = Routes.REPORTS
                        },
                        onNavigateToReportsThisMonth = {
                            reportFilter = "Bulanan"
                            previousRoute = Routes.HOME
                            currentRoute = Routes.REPORTS
                        },
                        onNavigateToSettings = {
                            isSettingsOpen = true
                        },
                        onNavigateToDebug = {
                            previousRoute = Routes.HOME
                            currentRoute = Routes.DEBUG_MENU
                        },
                        onNavigateToProcurement = {
                            previousRoute = Routes.HOME
                            currentRoute = Routes.PROCUREMENT_PLANNING
                        },
                        onNavigateToScanner = {
                            previousRoute = Routes.HOME
                            currentRoute = Routes.RECEIPT_SCANNER
                        }
                    )
                }

                Routes.TRANSACTION_ENTRY -> {
                    TransactionEntryScreen(
                        viewModel = viewModel,
                        scannedResult = scannedResult,
                        onNavigateToScanner = {
                            previousRoute = Routes.TRANSACTION_ENTRY
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
                            currentRoute = if (previousRoute == Routes.HOME) Routes.HOME else Routes.TRANSACTION_ENTRY
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
                            isSettingsOpen = true
                        },
                        initialFilterBy = reportFilter ?: defaultReportFilter
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
                            reportFilter = null
                            currentRoute = Routes.REPORTS
                        },
                        onNavigateToReportsThisMonth = {
                            reportFilter = "Bulanan"
                            currentRoute = Routes.REPORTS
                        },
                        onNavigateToSettings = { isSettingsOpen = true },
                        onNavigateToDebug = { currentRoute = Routes.DEBUG_MENU },
                        onNavigateToProcurement = { currentRoute = Routes.PROCUREMENT_PLANNING },
                        onNavigateToScanner = {
                            previousRoute = Routes.HOME
                            currentRoute = Routes.RECEIPT_SCANNER
                        }
                    )
                }
            }
            }

            // --- SETTINGS SLIDING WINDOW (GPU-ACCELERATED SMOOTH SPRING + GESTURE) ---
            BackHandler(enabled = isSettingsOpen) {
                isSettingsOpen = false
            }

            if (isPanelVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = settingsOpenProgress.value }
                        .background(Color.Black.copy(alpha = 0.46f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isSettingsOpen = false
                        }
                )
            }

            if (isSettingsPrewarmed || isPanelVisible) {
                val windowShape = remember {
                    RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .fillMaxWidth(0.92f)
                        .widthIn(max = 460.dp)
                        .graphicsLayer {
                            val p = settingsOpenProgress.value
                            translationX = (1f - p) * panelWidthPx
                            alpha = if (p <= 0.001f) 0f else (p * 5f).coerceIn(0f, 1f)
                        }
                        .then(
                            if (isPanelVisible) {
                                Modifier.draggable(
                                    orientation = Orientation.Horizontal,
                                    state = rememberDraggableState { delta ->
                                        coroutineScope.launch {
                                            val updated = (settingsOpenProgress.value - (delta / panelWidthPx))
                                                .coerceIn(0f, 1f)
                                            settingsOpenProgress.snapTo(updated)
                                        }
                                    },
                                    onDragStopped = { velocityPxPerSec ->
                                        val normVelocity = -velocityPxPerSec / panelWidthPx
                                        if (velocityPxPerSec > 650f || settingsOpenProgress.value < 0.72f) {
                                            pendingCloseVelocity = normVelocity
                                            isSettingsOpen = false
                                        } else {
                                            coroutineScope.launch {
                                                settingsOpenProgress.animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                        stiffness = 320f
                                                    ),
                                                    initialVelocity = normVelocity
                                                )
                                            }
                                        }
                                    }
                                )
                            } else {
                                Modifier.clearAndSetSemantics {}
                            }
                        ),
                    shape = windowShape,
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 8.dp,
                    shadowElevation = 24.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                ) {
                    SettingsScreen(
                        viewModel = viewModel,
                        isOpen = isSettingsOpen,
                        onNavigateBack = {
                            isSettingsOpen = false
                        },
                        onNavigateToDebug = {
                            isSettingsOpen = false
                            previousRoute = currentRoute
                            currentRoute = Routes.DEBUG_MENU
                        }
                    )
                }
            }
        }

        InAppUpdateHost(viewModel = viewModel)

        if (showWhatsNewDialog) {
            WhatsNewChangelogDialog(
                onDismiss = { viewModel.dismissWhatsNewDialog() }
            )
        }
    }
}
