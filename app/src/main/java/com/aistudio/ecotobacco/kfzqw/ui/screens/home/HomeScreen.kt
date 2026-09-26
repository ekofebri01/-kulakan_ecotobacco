package com.aistudio.ecotobacco.kfzqw.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProductEntity
import com.aistudio.ecotobacco.kfzqw.ui.screens.products.EditProductDialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import android.widget.Toast
import android.app.Activity
import java.text.SimpleDateFormat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.ecotobacco.kfzqw.BuildConfig
import com.aistudio.ecotobacco.kfzqw.ui.theme.LocalUiConfig
import com.aistudio.ecotobacco.kfzqw.ui.theme.scaled
import com.aistudio.ecotobacco.kfzqw.ui.theme.SummaryGold
import com.aistudio.ecotobacco.kfzqw.ui.theme.SummaryGreen
import com.aistudio.ecotobacco.kfzqw.ui.theme.SummaryRed
import com.aistudio.ecotobacco.kfzqw.viewmodel.TobaccoViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TobaccoViewModel,
    onNavigateToTransaction: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToReportsThisMonth: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDebug: () -> Unit,
    onNavigateToProcurement: () -> Unit
) {
    val uiConfig = LocalUiConfig.current
    val context = LocalContext.current
    var versionClickCount by remember { mutableIntStateOf(0) }
    var lastVersionClickTime by remember { mutableLongStateOf(0L) }

    val totalSpent by viewModel.totalSpentThisMonth.collectAsStateWithLifecycle()
    val transCount by viewModel.transactionCountThisMonth.collectAsStateWithLifecycle()
    val prodCount by viewModel.productCount.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val unitOptions by viewModel.customUnits.collectAsStateWithLifecycle()

    val firebaseUser by viewModel.firebaseUser.collectAsStateWithLifecycle()
    val isFirestoreSyncing by viewModel.isFirestoreSyncing.collectAsStateWithLifecycle()
    val firestoreLastSync by viewModel.firestoreLastSync.collectAsStateWithLifecycle()

    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var showPriceListDialog by remember { mutableStateOf(false) }
    var showPurchasePriceDialog by remember { mutableStateOf(false) }
    var showExitConfirmation by remember { mutableStateOf(false) }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
            maximumFractionDigits = 0
        }
    }

    val PremiumNavy = MaterialTheme.colorScheme.background
    val PremiumNavyLight = MaterialTheme.colorScheme.surface
    val PremiumGold = MaterialTheme.colorScheme.primary
    val PremiumGoldLight = MaterialTheme.colorScheme.onSurface
    val PremiumTextGold = MaterialTheme.colorScheme.onSurface

    // Konfirmasi Exit
    BackHandler {
        showExitConfirmation = true
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("Konfirmasi Keluar", fontWeight = FontWeight.Bold, fontSize = 18.sp.scaled) },
            text = { Text("Apakah Anda yakin ingin keluar dari aplikasi Eco Tobacco?", fontSize = 14.sp.scaled) },
            confirmButton = {
                TextButton(
                    onClick = {
                        (context as? android.app.Activity)?.finish()
                    }
                ) {
                    Text("Keluar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 14.sp.scaled)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) {
                    Text("Batal", fontSize = 14.sp.scaled)
                }
            },
            shape = RoundedCornerShape(uiConfig.cornerRadius.dp),
            containerColor = PremiumNavy
        )
    }

    // --- ANIMASI GLOW ROTATING (Untuk Border) ---
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    val animatedGlowBrush = Brush.sweepGradient(
        colors = listOf(
            Color.Transparent,
            PremiumGold.copy(alpha = 0.8f),
            Color.Transparent
        )
    )

    // --- MATRIX EFFECT STATE ---
    val matrixInfiniteTransition = rememberInfiniteTransition(label = "matrix")
    val matrixProgress by matrixInfiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "matrixProgress"
    )

    Scaffold(
        containerColor = PremiumNavy,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // --- BACKGROUND MATRIX EFFECT ---
            Canvas(modifier = Modifier.fillMaxSize()) {
                val columns = (size.width / 22.dp.toPx()).toInt()
                val chars = "0101011001"
                
                for (i in 0 until columns) {
                    val x = i * 22.dp.toPx()
                    val speedFactor = (i % 5 + 1) * 0.4f
                    val yOffset = (matrixProgress * size.height * speedFactor) % size.height
                    
                    val rows = (size.height / 35.dp.toPx()).toInt() + 2
                    for (j in 0 until rows) {
                        val y = (yOffset + j * 35.dp.toPx()) % size.height
                        val char = chars[(i + j) % chars.length]
                        
                        // Kecerahan ditingkatkan agar lebih terlihat (0.1f - 0.4f)
                        val alpha = (0.4f * (y / size.height)).coerceIn(0.1f, 0.4f)
                        
                        drawContext.canvas.nativeCanvas.drawText(
                            char.toString(),
                            x,
                            y,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.argb(
                                    (alpha * 255).toInt(),
                                    (PremiumGold.red * 255).toInt(),
                                    (PremiumGold.green * 255).toInt(),
                                    (PremiumGold.blue * 255).toInt()
                                )
                                textSize = 11.sp.toPx()
                                isAntiAlias = true
                                typeface = android.graphics.Typeface.MONOSPACE
                            }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // --- GLASSMORPHISM HEADER WITH ROTATING BORDER GLOW ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp.scaled)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(PremiumNavyLight.copy(alpha = 0.95f), PremiumNavyLight.copy(alpha = 0.85f))
                            ),
                            RoundedCornerShape(uiConfig.cornerRadius.dp)
                        )
                        .border(
                            BorderStroke(2.dp, animatedGlowBrush),
                            RoundedCornerShape(uiConfig.cornerRadius.dp)
                        )
                        .drawBehind {
                            // Efek cahaya pinggiran halus
                            rotate(angle) {
                                drawCircle(
                                    brush = animatedGlowBrush,
                                    radius = size.maxDimension,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx()),
                                    alpha = 0.2f
                                )
                            }
                        }
                        .clip(RoundedCornerShape(uiConfig.cornerRadius.dp))
                        .padding(24.dp.scaled)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Eco, 
                                    null, 
                                    tint = PremiumGold, 
                                    modifier = Modifier.size(24.dp.scaled)
                                )
                                Spacer(Modifier.width(10.dp.scaled))
                                Text(
                                    text = "ECO TOBACCO",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PremiumGold,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    fontSize = 16.sp.scaled
                                )
                            }
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PremiumGold.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp.scaled,
                                modifier = Modifier.clickable {
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastVersionClickTime < 3000) versionClickCount++
                                    else versionClickCount = 1
                                    lastVersionClickTime = currentTime
                                    if (versionClickCount >= 7) {
                                        versionClickCount = 0
                                        onNavigateToDebug()
                                    }
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp.scaled))
                        Text(
                            text = "Selamat Datang,",
                            style = MaterialTheme.typography.headlineMedium,
                            color = PremiumGoldLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp.scaled
                        )
                        Text(
                            text = "Partner Eksekutif Eco Tobacco",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PremiumTextGold.copy(alpha = 0.6f),
                            fontSize = 14.sp.scaled
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp.scaled, vertical = 8.dp.scaled),
                    verticalArrangement = Arrangement.spacedBy(24.dp.scaled)
                ) {
                    // --- FIREBASE AUTH & CLOUD FIRESTORE BAR ---
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToSettings() },
                        shape = RoundedCornerShape(uiConfig.cornerRadius.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (firebaseUser != null) PremiumGold.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp.scaled),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp.scaled)
                                        .background(
                                            if (firebaseUser != null) PremiumGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (firebaseUser != null) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                        contentDescription = "Firebase Cloud Status",
                                        tint = if (firebaseUser != null) PremiumGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp.scaled)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp.scaled))
                                Column {
                                    Text(
                                        text = if (firebaseUser != null) (firebaseUser?.displayName ?: firebaseUser?.email ?: "Pengguna Cloud") else "Firebase & Cloud Firestore",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp.scaled
                                    )
                                    Text(
                                        text = if (firebaseUser != null) {
                                            if (isFirestoreSyncing) "Sedang sinkron ke Firestore..."
                                            else if (firestoreLastSync != null && firestoreLastSync!! > 0) "Tersinkron ke Firestore"
                                            else "Siap sinkronisasi"
                                        } else "Ketuk untuk hubungkan akun & Firestore",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (firebaseUser != null) PremiumGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp.scaled
                                    )
                                }
                            }

                            if (firebaseUser != null) {
                                IconButton(
                                    onClick = {
                                        viewModel.syncFirestore { success, msg ->
                                            (context as? Activity)?.runOnUiThread {
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    enabled = !isFirestoreSyncing
                                ) {
                                    if (isFirestoreSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = PremiumGold,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = "Sync Firestore",
                                            tint = PremiumGold
                                        )
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        (context as? Activity)?.let { act ->
                                            viewModel.signInWithGoogle(act) { success, msg ->
                                                act.runOnUiThread {
                                                    Toast.makeText(act, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PremiumGold),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp.scaled)
                                ) {
                                    Text("Masuk", fontSize = 12.sp.scaled, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // --- REDESIGNED DASHBOARD RINGKASAN ---
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp.scaled)) {
                        Text(
                            text = "Ringkasan Finansial",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = PremiumTextGold,
                            fontSize = 18.sp.scaled
                        )

                        // Main Stat: Total Kulakan (Large)
                        SummaryCardPremium(
                            label = "Total Kulakan Bulan Ini",
                            value = currencyFormatter.format(totalSpent).replace("Rp", "Rp "),
                            icon = Icons.Default.Payments,
                            bgColor = SummaryRed,
                            isLarge = true,
                            onClick = onNavigateToReportsThisMonth
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp.scaled)
                        ) {
                            SummaryCardPremium(
                                label = "Transaksi",
                                value = "$transCount Nota",
                                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                bgColor = SummaryGreen,
                                isLarge = false,
                                onClick = onNavigateToReportsThisMonth,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryCardPremium(
                                label = "Varian Produk",
                                value = "$prodCount Item",
                                icon = Icons.Default.Inventory2,
                                bgColor = SummaryGold,
                                isLarge = false,
                                onClick = onNavigateToReportsThisMonth,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // --- DYNAMIC MENU GRID ---
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp.scaled)) {
                        Text(
                            text = "Navigasi Cepat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = PremiumTextGold,
                            fontSize = 18.sp.scaled
                        )

                        // Prominent Quick Actions
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp.scaled)) {
                            MenuCardPremium(
                                title = "Input Kulakan", 
                                subtitle = "Scan nota & manual",
                                icon = Icons.Default.ShoppingCart, 
                                onClick = onNavigateToTransaction, 
                                modifier = Modifier.weight(1f),
                                highlight = true
                            )
                            MenuCardPremium(
                                title = "Rencana Baru", 
                                subtitle = "Buat daftar belanja",
                                icon = Icons.AutoMirrored.Filled.PlaylistAddCheck, 
                                onClick = onNavigateToProcurement, 
                                modifier = Modifier.weight(1f),
                                highlight = true
                            )
                        }

                        // Secondary Toolbox Grid (3 columns)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp.scaled)) {
                            GridMenuItem("Database", Icons.Default.Inventory, onNavigateToProducts, Modifier.weight(1f))
                            GridMenuItem("Laporan", Icons.Default.Assessment, onNavigateToReports, Modifier.weight(1f))
                            GridMenuItem("Harga Jual", Icons.Default.LocalOffer, { showPriceListDialog = true }, Modifier.weight(1f))
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp.scaled)) {
                            GridMenuItem("Harga Kulakan", Icons.Default.Payments, { showPurchasePriceDialog = true }, Modifier.weight(1f))
                            GridMenuItem("Pengaturan", Icons.Default.Settings, onNavigateToSettings, Modifier.weight(1f))
                            Spacer(Modifier.weight(1f))
                        }
                    }

                    Spacer(Modifier.height(40.dp.scaled))
                }
            }
        }
    }

    if (showPriceListDialog) {
        PriceViewDialog(
            title = "Daftar Harga Jual",
            products = products,
            currencyFormatter = currencyFormatter,
            isSellingPrice = true,
            onDismiss = { showPriceListDialog = false },
            onEditProduct = { 
                productToEdit = it
                showPriceListDialog = false
            }
        )
    }

    if (showPurchasePriceDialog) {
        PriceViewDialog(
            title = "Daftar Harga Kulakan",
            products = products,
            currencyFormatter = currencyFormatter,
            isSellingPrice = false,
            onDismiss = { showPurchasePriceDialog = false },
            onEditProduct = {
                productToEdit = it
                showPurchasePriceDialog = false
            }
        )
    }

    if (productToEdit != null) {
        EditProductDialog(
            product = productToEdit!!,
            unitOptions = unitOptions,
            onAddCustomUnit = { viewModel.addCustomUnit(it) },
            onUpdateCustomUnit = { old, new -> viewModel.updateCustomUnit(old, new) },
            onDeleteCustomUnit = { viewModel.deleteCustomUnit(it) },
            onDismiss = { productToEdit = null },
            onConfirm = { name, price, unit, sellingPrice, sellingUnit, contentQuantity ->
                viewModel.updateProduct(productToEdit!!.id, name, price, unit, sellingPrice, sellingUnit, contentQuantity)
                productToEdit = null
            }
        )
    }
}

@Composable
fun PriceViewDialog(
    title: String,
    products: List<ProductEntity>,
    currencyFormatter: NumberFormat,
    isSellingPrice: Boolean,
    onDismiss: () -> Unit,
    onEditProduct: (ProductEntity) -> Unit
) {
    val uiConfig = LocalUiConfig.current
    val PremiumNavy = MaterialTheme.colorScheme.background
    val PremiumGold = MaterialTheme.colorScheme.primary
    val PremiumGoldLight = MaterialTheme.colorScheme.onSurface
    val PremiumGoldDark = MaterialTheme.colorScheme.primary
    val PremiumTextGold = MaterialTheme.colorScheme.onSurface

    var searchQuery by remember { mutableStateOf("") }
    val filteredProducts = remember(products, searchQuery) {
        val list = if (searchQuery.isBlank()) products
        else products.filter { it.name.contains(searchQuery, ignoreCase = true) }
        list.sortedBy { it.name.lowercase() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(24.dp.scaled),
        containerColor = PremiumNavy,
        shape = RoundedCornerShape(uiConfig.cornerRadius.dp),
        title = {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                color = PremiumGoldLight,
                style = MaterialTheme.typography.headlineSmall,
                fontSize = 20.sp.scaled
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp.scaled)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari produk...", color = PremiumGold.copy(alpha = 0.5f), fontSize = 14.sp.scaled) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = PremiumGold.copy(alpha = 0.7f), modifier = Modifier.size(20.dp.scaled)) },
                    singleLine = true,
                    shape = RoundedCornerShape((uiConfig.cornerRadius / 2).dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PremiumGold,
                        unfocusedBorderColor = PremiumGold.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Text(
                    "Klik produk untuk mengedit",
                    style = MaterialTheme.typography.labelSmall,
                    color = PremiumGold.copy(alpha = 0.6f),
                    fontSize = 11.sp.scaled
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp.scaled),
                    modifier = Modifier.heightIn(max = 400.dp.scaled)
                ) {
                    items(filteredProducts) { product ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditProduct(product) }
                                .padding(vertical = 4.dp.scaled),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                product.name,
                                color = PremiumTextGold,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp.scaled,
                                modifier = Modifier.weight(1f)
                            )
                            val price = if (isSellingPrice) product.sellingPrice else product.price
                            val unit = if (isSellingPrice) product.sellingUnit else product.unit
                            val color = if (isSellingPrice) PremiumGold else Color(0xFFF87171)
                            
                            Text(
                                "${currencyFormatter.format(price).replace("Rp", "Rp ")}/$unit",
                                color = color,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp.scaled
                            )
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.padding(top = 8.dp.scaled)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = PremiumGold, fontWeight = FontWeight.Bold, fontSize = 14.sp.scaled)
            }
        }
    )
}

@Composable
private fun SummaryCardPremium(
    label: String,
    value: String,
    icon: ImageVector,
    bgColor: Color,
    isLarge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiConfig = LocalUiConfig.current
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(uiConfig.cornerRadius.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        modifier = modifier.height(if (isLarge) (uiConfig.buttonHeight * 2).dp else (uiConfig.buttonHeight * 1.5f).dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(bgColor.copy(alpha = 0.2f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(x = 0f, y = 0f),
                        radius = 400f * uiConfig.paddingScale
                    )
                )
                .padding(16.dp.scaled)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = (if (isLarge) 14.sp else 12.sp).scaled
                    )
                    Icon(
                        icon, 
                        null, 
                        modifier = Modifier.size((if (isLarge) 28.dp else 20.dp).scaled), 
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = value, 
                    style = if (isLarge) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.Black, 
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = (if (isLarge) 24.sp else 18.sp).scaled
                )
            }
        }
    }
}

@Composable
private fun MenuCardPremium(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    val uiConfig = LocalUiConfig.current
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(uiConfig.cornerRadius.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (highlight) 0.6f else 0.2f)),
        modifier = modifier.height((uiConfig.buttonHeight * 1.8f).dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp.scaled),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon, 
                null, 
                modifier = Modifier.size(32.dp.scaled), 
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp.scaled))
            Text(
                title, 
                style = MaterialTheme.typography.labelLarge, 
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp.scaled,
                textAlign = TextAlign.Center
            )
            Text(
                subtitle, 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp.scaled,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun GridMenuItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiConfig = LocalUiConfig.current
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(uiConfig.cornerRadius.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.height((uiConfig.buttonHeight * 1.4f).dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp.scaled),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon, 
                null, 
                modifier = Modifier.size(24.dp.scaled), 
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp.scaled))
            Text(
                title, 
                style = MaterialTheme.typography.labelSmall, 
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp.scaled,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
