package com.aistudio.ecotobacco.kfzqw

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
import com.aistudio.ecotobacco.kfzqw.ui.theme.MyApplicationTheme
import com.aistudio.ecotobacco.kfzqw.viewmodel.TobaccoViewModel
import com.aistudio.ecotobacco.kfzqw.viewmodel.TobaccoViewModelFactory

class MainActivity : ComponentActivity() {

    // --- Migrations ---
    private val migration1to2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `transactions_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` INTEGER NOT NULL, `productName` TEXT NOT NULL, `quantity` REAL NOT NULL, `unitPrice` REAL NOT NULL, `total` REAL NOT NULL)")
            db.execSQL("INSERT INTO `transactions_new` (`id`, `date`, `productName`, `quantity`, `unitPrice`, `total`) SELECT `id`, `date`, `productName`, CAST(`quantity` AS REAL), `unitPrice`, `total` FROM `transactions`")
            db.execSQL("DROP TABLE `transactions`")
            db.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")
        }
    }
    private val migration2to3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `transactions` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
        }
    }
    private val migration4to5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `transactions` ADD COLUMN `supplier` TEXT NOT NULL DEFAULT ''")
        }
    }
    private val migration5to6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)")
        }
    }
    private val migration6to7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `transactions` ADD COLUMN `unit` TEXT NOT NULL DEFAULT 'kg'")
            
            // Fix procurement_plans table (remove createdAt and isCompleted)
            db.execSQL("CREATE TABLE IF NOT EXISTS `procurement_plans_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` INTEGER NOT NULL, `supplierName` TEXT NOT NULL)")
            db.execSQL("INSERT INTO `procurement_plans_new` (`id`, `date`, `supplierName`) SELECT `id`, `date`, `supplierName` FROM `procurement_plans`")
            db.execSQL("DROP TABLE `procurement_plans`")
            db.execSQL("ALTER TABLE `procurement_plans_new` RENAME TO `procurement_plans`")
        }
    }
    private val migration7to8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `products` ADD COLUMN `unit` TEXT NOT NULL DEFAULT 'kg'")
            db.execSQL("ALTER TABLE `procurement_items` ADD COLUMN `unit` TEXT NOT NULL DEFAULT 'kg'")
        }
    }
    private val migration8to9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `products` ADD COLUMN `sellingPrice` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `products` ADD COLUMN `sellingUnit` TEXT NOT NULL DEFAULT 'pcs'")
        }
    }
    private val migration9to10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `products` ADD COLUMN `contentQuantity` REAL NOT NULL DEFAULT 1.0")
        }
    }

    // --- Database & ViewModel Setup ---
    private val db by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "eco_tobacco_database")
            .addMigrations(migration1to2, migration2to3, migration4to5, migration5to6, migration6to7, migration7to8, migration8to9, migration9to10)
            .fallbackToDestructiveMigration(dropAllTables = false)
            .build()
    }

    private val repository by lazy {
        TobaccoRepository(
            db.productDao(),
            db.transactionDao(),
            db.procurementDao()
        )
    }

    private val viewModel: TobaccoViewModel by viewModels {
        TobaccoViewModelFactory(repository, application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val customColor by viewModel.customColor.collectAsState()
            val fontSizeScale by viewModel.fontSizeScale.collectAsState()
            val paddingScale by viewModel.paddingScale.collectAsState()
            val buttonHeight by viewModel.buttonHeight.collectAsState()
            val cornerRadius by viewModel.cornerRadius.collectAsState()
            val customTextColor by viewModel.customTextColor.collectAsState()

            val uiConfig = com.aistudio.ecotobacco.kfzqw.ui.theme.UiConfig(
                fontSizeScale = fontSizeScale,
                paddingScale = paddingScale,
                buttonHeight = buttonHeight,
                cornerRadius = cornerRadius,
                customTextColor = customTextColor
            )

            val appInitProgress by viewModel.appInitProgress.collectAsState()
            val initStatusText by viewModel.initStatusText.collectAsState()
            val isAppReady by viewModel.isAppReady.collectAsState()

            var showSplash by remember { mutableStateOf(true) }

            MyApplicationTheme(
                themeMode = themeMode, 
                customColor = customColor,
                uiConfig = uiConfig
            ) {
                if (showSplash || !isAppReady) {
                    SplashScreen(
                        progress = appInitProgress,
                        statusText = initStatusText,
                        onFinished = { showSplash = false }
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val context = LocalContext.current
                        val sharedPref =
                            remember { context.getSharedPreferences("app_prefs", MODE_PRIVATE) }
                        val currentVersion = Routes.APP_VERSION
                        var showWhatsNew by remember {
                            mutableStateOf(
                                !sharedPref.getBoolean(
                                    "has_seen_whats_new_v$currentVersion",
                                    false
                                )
                            )
                        }

                        if (showWhatsNew) {
                            WhatsNewDialog(currentVersion = currentVersion, onDismiss = {
                                sharedPref.edit {
                                    putBoolean(
                                        "has_seen_whats_new_v$currentVersion",
                                        true
                                    )
                                }
                                showWhatsNew = false
                            })
                        }
                        TobaccoApp(viewModel)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun WhatsNewDialog(currentVersion: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Tentang Aplikasi", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("ECO TOBACCO v$currentVersion", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("Info Aplikasi:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Aplikasi manajemen stok dan kulakan tembakau pintar dengan fitur sinkronisasi Cloud.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
                item { Text("Fitur & Pembaruan Terbaru:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }
                item { WhatsNewItem("🎨", "UI Personalization", "Atur ukuran font, padding, dan bentuk tombol sesukamu.") }
                item { WhatsNewItem("📊", "Modern Dashboard v4.0", "Tampilan dashboard baru yang lebih premium dan informatif.") }
                item { WhatsNewItem("🛡️", "Konfirmasi Keluar", "Mencegah aplikasi tertutup secara tidak sengaja.") }
                item { WhatsNewItem("🚀", "Peningkatan AI Scan", "Optimasi akurasi pemindaian nota dengan struktur kompleks.") }
                item { WhatsNewItem("☁️", "Sinkronisasi Google Drive", "Cadangkan dan pulihkan data Anda kapan saja.") }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(12.dp)) {
                Text("Mulai Sekarang")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun WhatsNewItem(icon: String, title: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = icon, fontSize = 20.sp)
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TobaccoApp(viewModel: TobaccoViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel,
                { navController.navigate(Routes.TRANSACTION_ENTRY) },
                { navController.navigate(Routes.PRODUCT_MASTER) },
                { navController.navigate(Routes.REPORTS) },
                { navController.navigate(Routes.REPORTS_THIS_MONTH) },
                { navController.navigate(Routes.SETTINGS) },
                { navController.navigate(Routes.DEBUG_MENU) },
                { navController.navigate(Routes.PROCUREMENT_PLANNING) })
        }
        composable(Routes.TRANSACTION_ENTRY) { backStackEntry ->
            val result = backStackEntry.savedStateHandle.get<LocalScanner.ScannedResult>("scanned_result")
            TransactionEntryScreen(
                viewModel,
                scannedResult = result,
                onNavigateToScanner = { navController.navigate(Routes.RECEIPT_SCANNER) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.PRODUCT_MASTER) {
            ProductMasterScreen(
                viewModel,
                { navController.popBackStack() })
        }
        composable(Routes.REPORTS) {
            ReportsScreen(
                viewModel, { navController.navigate(Routes.TRANSACTION_ENTRY) },
                { navController.navigate(Routes.SETTINGS) })
        }
        composable(Routes.REPORTS_THIS_MONTH) {
            ReportsScreen(
                viewModel,
                { navController.navigate(Routes.TRANSACTION_ENTRY) },
                { navController.navigate(Routes.SETTINGS) },
                initialFilterBy = "Bulanan"
            )
        }
        composable(Routes.PROCUREMENT_PLANNING) { backStackEntry ->
            val result = backStackEntry.savedStateHandle.get<LocalScanner.ScannedResult>("scanned_result")
            ProcurementPlanningScreen(
                viewModel,
                scannedResult = result,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel,
                { navController.popBackStack() },
                { navController.navigate(Routes.DEBUG_MENU) })
        }
        composable(Routes.DEBUG_MENU) { DebugMenuScreen(viewModel) { navController.popBackStack() } }
        composable(Routes.RECEIPT_SCANNER) {
            ReceiptScannerScreen(
                onNavigateBack = { navController.popBackStack() },
                onScanComplete = { result ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        "scanned_result",
                        result
                    )
                    navController.popBackStack()
                }
            )
        }
    }
}
