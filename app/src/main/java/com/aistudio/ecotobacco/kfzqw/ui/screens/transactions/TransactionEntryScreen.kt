package com.aistudio.ecotobacco.kfzqw.ui.screens.transactions

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.ecotobacco.kfzqw.data.utils.LocalScanner
import com.aistudio.ecotobacco.kfzqw.ui.components.UnitManagementDialog
import com.aistudio.ecotobacco.kfzqw.ui.theme.LocalUiConfig
import com.aistudio.ecotobacco.kfzqw.ui.theme.scaled
import com.aistudio.ecotobacco.kfzqw.viewmodel.TobaccoViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ManualCartItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val productName: String,
    val quantity: Double,
    val unitPrice: Double,
    val unit: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionEntryScreen(
    viewModel: TobaccoViewModel,
    scannedResult: LocalScanner.ScannedResult? = null,
    onNavigateToScanner: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val calendar = Calendar.getInstance()
    val uiConfig = LocalUiConfig.current

    var selectedDate by remember { mutableLongStateOf(calendar.timeInMillis) }
    var selectedProduct by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unitPrice by remember { mutableStateOf("") }
    var manualItemsList by remember { mutableStateOf(listOf<ManualCartItem>()) }
    var supplier by remember { mutableStateOf("") }
    var supplierExpanded by remember { mutableStateOf(false) }
    var productExpanded by remember { mutableStateOf(false) }

    val savedSuppliers = remember(transactions) {
        transactions
            .map { it.supplier.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .sortedBy { it.lowercase(Locale.ROOT) }
    }

    // Unit Logic
    val unitOptions by viewModel.customUnits.collectAsStateWithLifecycle()
    var selectedUnit by remember { mutableStateOf("kg") }
    var customUnit by remember { mutableStateOf("") }
    var showUnitManagement by remember { mutableStateOf(false) }

    var scannedItemsPreview by remember { mutableStateOf<LocalScanner.ScannedResult?>(null) }
    var latestScannerReport by remember { mutableStateOf<String?>(null) }
    var showScannerDebug by remember { mutableStateOf(false) }
    var isCameraDebugEnabled by rememberSaveable { mutableStateOf(false) }

    // Matching product from master data for smart pricing / margins
    val matchedProduct = remember(selectedProduct, products) {
        products.find { it.name.trim().equals(selectedProduct.trim(), ignoreCase = true) }
    }

    // Calculations
    val parsedQty = quantity.toDoubleOrNull() ?: 0.0
    val parsedPrice = unitPrice.toDoubleOrNull() ?: 0.0
    val currentInputTotal = if (selectedProduct.isNotBlank() && parsedQty > 0) parsedQty * parsedPrice else 0.0
    val cartItemsTotal = manualItemsList.sumOf { it.quantity * it.unitPrice }
    val totalTransaction = cartItemsTotal + currentInputTotal
    val activeUnit = if (selectedUnit == "Custom") {
        customUnit.ifBlank { "unit" }
    } else selectedUnit

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
            maximumFractionDigits = 0
        }
    }

    // Handle scanned result from parameter
    LaunchedEffect(scannedResult) {
        if (scannedResult != null) {
            scannedItemsPreview = scannedResult
            latestScannerReport = scannedResult.parsingReport
            scannedResult.date?.let { detectedDate ->
                selectedDate = detectedDate
            }
            scannedResult.shopName?.let { name ->
                supplier = name
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    var isGalleryScanning by remember { mutableStateOf(false) }

    val csvPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importTransactionsFromCsv(context, it) }
    }

    val galleryScanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isGalleryScanning = true
            coroutineScope.launch {
                val result = LocalScanner.scanReceipt(context, uri)
                isGalleryScanning = false
                scannedItemsPreview = result
                latestScannerReport = result.parsingReport
                result.date?.let { selectedDate = it }
                result.shopName?.let { supplier = it }
            }
        }
    }

    val bgColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val isDarkSurface = bgColor.luminance() < 0.45f
    val inputFieldBg = if (isDarkSurface) Color(0xFF101513) else surfaceVariant.copy(alpha = 0.45f)

    Scaffold(
        containerColor = bgColor,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp.scaled, vertical = 12.dp.scaled),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    onClick = onNavigateBack,
                    shape = CircleShape,
                    color = surfaceColor,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("transaction_back_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = "Input Kulakan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp.scaled
                    )
                    Text(
                        text = "Catat Pembelian & Stok",
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp.scaled
                    )
                }

                Surface(
                    onClick = { csvPickerLauncher.launch("text/*") },
                    shape = CircleShape,
                    color = primaryColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.28f)),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("transaction_import_csv_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Import CSV",
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            val isFormValid = manualItemsList.isNotEmpty() ||
                (selectedProduct.isNotBlank() && quantity.isNotBlank() && unitPrice.isNotBlank() && parsedQty > 0)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                color = if (isDarkSurface) Color(0xFF121815) else surfaceColor,
                shadowElevation = 14.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(1.dp, borderColor.copy(alpha = 0.85f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp.scaled, vertical = 14.dp.scaled),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Total Kulakan",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp.scaled
                        )
                        Text(
                            text = currencyFormatter.format(totalTransaction).replace("Rp", "Rp "),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (totalTransaction > 0) primaryColor else MaterialTheme.colorScheme.onSurface,
                            fontSize = 24.sp.scaled,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (parsedQty > 0) {
                            Text(
                                text = "Kuantitas: $quantity $activeUnit",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp.scaled
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Button(
                        onClick = {
                            val itemsToSave = mutableListOf<ManualCartItem>()
                            manualItemsList.forEach { itemsToSave.add(it) }
                            if (selectedProduct.isNotBlank() && parsedQty > 0 && parsedPrice >= 0) {
                                itemsToSave.add(
                                    ManualCartItem(
                                        productName = selectedProduct.trim(),
                                        quantity = parsedQty,
                                        unitPrice = parsedPrice,
                                        unit = activeUnit
                                    )
                                )
                            }

                            if (itemsToSave.isNotEmpty()) {
                                itemsToSave.forEach { item ->
                                    viewModel.addTransaction(
                                        date = selectedDate,
                                        productName = item.productName,
                                        quantity = item.quantity,
                                        unitPrice = item.unitPrice,
                                        supplier = supplier.trim(),
                                        unit = item.unit
                                    )
                                }
                                Toast.makeText(context, "${itemsToSave.size} Item Kulakan Berhasil Disimpan!", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            } else {
                                Toast.makeText(context, "Lengkapi produk, jumlah, dan harga atau tambahkan ke daftar", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .height(50.dp)
                            .width(156.dp)
                            .testTag("save_transaction_button"),
                        shape = RoundedCornerShape(16.dp),
                        enabled = isFormValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = onPrimaryColor,
                            disabledContainerColor = if (isDarkSurface) Color(0xFF1A2821) else borderColor.copy(alpha = 0.5f),
                            disabledContentColor = if (isDarkSurface) Color(0xFF7B8E84) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Simpan",
                            fontSize = 15.sp.scaled,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    ) { innerPadding: PaddingValues ->
        val scrollState = rememberScrollState()

        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle top radial emerald glow consistent with HomeScreen
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = if (isDarkSurface) 0.12f else 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.2f, size.height * 0.04f),
                        radius = size.width * 0.85f
                    ),
                    center = Offset(size.width * 0.2f, size.height * 0.04f),
                    radius = size.width * 0.85f
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        start = 16.dp.scaled,
                        end = 16.dp.scaled,
                        top = innerPadding.calculateTopPadding() + 4.dp,
                        bottom = innerPadding.calculateBottomPadding() + 24.dp
                    )
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp.scaled)
            ) {
                // --- 1. HERO AI NOTA SCANNER CARD ---
                val heroGradientStart = lerp(Color(0xFF18563C), primaryColor, 0.12f)
                val heroGradientEnd = lerp(Color(0xFF0E3424), primaryColor, 0.05f)

                Surface(
                    onClick = onNavigateToScanner,
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.30f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_scan_nota_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(listOf(heroGradientStart, heroGradientEnd))
                            )
                            .padding(16.dp.scaled),
                        verticalArrangement = Arrangement.spacedBy(14.dp.scaled)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.13f),
                                modifier = Modifier.size(46.dp.scaled)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan Nota",
                                        tint = Color.White,
                                        modifier = Modifier.size(23.dp.scaled)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp.scaled))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Scan Nota Otomatis",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        fontSize = 16.sp.scaled
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.14f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                                    ) {
                                        Text(
                                            text = "OCR",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Foto atau pilih struk dari galeri, data terisi otomatis",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.80f),
                                    fontSize = 12.5.sp.scaled,
                                    lineHeight = 17.sp.scaled
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp.scaled),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onNavigateToScanner,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor,
                                    contentColor = onPrimaryColor
                                ),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("scan_camera_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Pindai",
                                    fontSize = 15.sp.scaled,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Surface(
                                onClick = {
                                    galleryScanLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.10f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("scan_gallery_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "Scan Nota dari Galeri",
                                        tint = Color.White,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // --- 2. RINGKASAN NOTA CARD ---
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = surfaceColor,
                    border = BorderStroke(
                        1.dp,
                        if (totalTransaction > 0) primaryColor.copy(alpha = 0.45f) else borderColor
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp.scaled),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(
                                            if (totalTransaction > 0) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "RINGKASAN NOTA",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (totalTransaction > 0) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.1.sp,
                                    fontSize = 11.5.sp.scaled
                                )
                            }

                            Text(
                                text = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID")).format(Date(selectedDate)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.5.sp.scaled
                            )
                        }

                        if (selectedProduct.isNotBlank() || parsedQty > 0 || parsedPrice > 0 || manualItemsList.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = when {
                                            selectedProduct.isNotBlank() -> selectedProduct
                                            manualItemsList.isNotEmpty() -> "${manualItemsList.size} Item di Daftar"
                                            else -> "Pilih produk..."
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (supplier.isNotBlank()) "Pengepul: $supplier" else "Supplier belum diisi",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = currencyFormatter.format(totalTransaction).replace("Rp", "Rp "),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Black,
                                        color = primaryColor
                                    )
                                    if (parsedQty > 0 || parsedPrice > 0) {
                                        Text(
                                            text = "$quantity $activeUnit × ${currencyFormatter.format(parsedPrice).replace("Rp", "Rp ")}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            // Smart Insight (Margin / Jual jika ada data di master)
                            if (matchedProduct != null && matchedProduct.sellingPrice > 0) {
                                val potentialRevenue = parsedQty * matchedProduct.sellingPrice
                                val potentialProfit = potentialRevenue - currentInputTotal
                                val profitMarginPct = if (currentInputTotal > 0) ((potentialProfit / currentInputTotal) * 100) else 0.0

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = primaryColor.copy(alpha = 0.09f),
                                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.22f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.TrendingUp,
                                                contentDescription = null,
                                                tint = primaryColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Jual: ${currencyFormatter.format(matchedProduct.sellingPrice).replace("Rp", "Rp ")}/${matchedProduct.sellingUnit}",
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Text(
                                            text = "Est. Margin: +${String.format(Locale.US, "%.1f", profitMarginPct)}%",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryColor
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Ketik atau pilih produk di bawah untuk kalkulasi nota instan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.5.sp.scaled,
                                lineHeight = 18.sp.scaled
                            )
                        }
                    }
                }

                // --- 3. INFORMASI TRANSAKSI CARD ---
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = surfaceColor,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp.scaled),
                        verticalArrangement = Arrangement.spacedBy(12.dp.scaled)
                    ) {
                        Text(
                            text = "Informasi Transaksi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.5.sp.scaled
                        )

                        // Tanggal Pembelian Field
                        Surface(
                            onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        calendar.set(year, month, day)
                                        selectedDate = calendar.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = inputFieldBg,
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("transaction_date_picker")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Tanggal Pembelian",
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text(
                                        text = "Tanggal Pembelian",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp.scaled
                                    )
                                    Text(
                                        text = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.forLanguageTag("id-ID")).format(Date(selectedDate)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp.scaled
                                    )
                                }
                            }
                        }

                        // Supplier Autocomplete Dropdown
                        ExposedDropdownMenuBox(
                            expanded = supplierExpanded,
                            onExpandedChange = {
                                supplierExpanded = savedSuppliers.isNotEmpty()
                            }
                        ) {
                            OutlinedTextField(
                                value = supplier,
                                onValueChange = {
                                    supplier = it
                                    supplierExpanded = it.isNotBlank() && savedSuppliers.any { saved ->
                                        saved.contains(it.trim(), ignoreCase = true)
                                    }
                                },
                                placeholder = {
                                    Text(
                                        "Supplier / Pengepul",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp.scaled
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (supplier.isNotBlank()) {
                                        IconButton(onClick = { supplier = "" }) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "Hapus",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Pilih Supplier",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                                    .testTag("supplier_input"),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = borderColor,
                                    focusedContainerColor = inputFieldBg,
                                    unfocusedContainerColor = inputFieldBg,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            val supplierMatches = remember(supplier, savedSuppliers) {
                                if (supplier.isBlank()) savedSuppliers
                                else savedSuppliers.filter { it.contains(supplier.trim(), ignoreCase = true) }
                            }
                            ExposedDropdownMenu(
                                expanded = supplierExpanded && supplierMatches.isNotEmpty(),
                                onDismissRequest = { supplierExpanded = false },
                                modifier = Modifier
                                    .background(surfaceColor)
                                    .exposedDropdownSize()
                            ) {
                                supplierMatches.take(6).forEach { savedSupplier ->
                                    DropdownMenuItem(
                                        text = { Text(savedSupplier, fontWeight = FontWeight.Medium) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Storefront,
                                                null,
                                                tint = primaryColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        onClick = {
                                            supplier = savedSupplier
                                            supplierExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Quick Supplier Pill Chips (Wrapped cleanly)
                        if (savedSuppliers.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                savedSuppliers.take(6).forEach { chipSupplier ->
                                    val isSelected = supplier.equals(chipSupplier, ignoreCase = true)
                                    Surface(
                                        onClick = {
                                            supplier = if (isSelected) "" else chipSupplier
                                        },
                                        shape = CircleShape,
                                        color = if (isSelected) primaryColor.copy(alpha = 0.16f) else inputFieldBg,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) primaryColor else borderColor
                                        )
                                    ) {
                                        Text(
                                            text = chipSupplier,
                                            fontSize = 12.sp.scaled,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 4. TAMBAH ITEM KULAKAN CARD ---
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = surfaceColor,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp.scaled),
                        verticalArrangement = Arrangement.spacedBy(14.dp.scaled)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tambah Item Kulakan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.5.sp.scaled
                            )
                            Surface(
                                color = primaryColor.copy(alpha = 0.14f),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = "Dukungan 0,5",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 11.sp.scaled,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                            }
                        }

                        // 1. Product Autocomplete Input
                        ExposedDropdownMenuBox(
                            expanded = productExpanded,
                            onExpandedChange = { productExpanded = !productExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedProduct,
                                onValueChange = {
                                    selectedProduct = it
                                    productExpanded = true
                                },
                                placeholder = {
                                    Text(
                                        "Nama Produk Tembakau / Barang",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp.scaled
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (selectedProduct.isNotBlank()) {
                                        IconButton(onClick = { selectedProduct = "" }) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "Hapus",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Pilih Produk",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                                    .testTag("product_name_input"),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = borderColor,
                                    focusedContainerColor = inputFieldBg,
                                    unfocusedContainerColor = inputFieldBg,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            val matchingProducts = products.filter {
                                it.name.contains(selectedProduct, ignoreCase = true)
                            }

                            ExposedDropdownMenu(
                                expanded = productExpanded && matchingProducts.isNotEmpty(),
                                onDismissRequest = { productExpanded = false },
                                modifier = Modifier
                                    .background(surfaceColor)
                                    .exposedDropdownSize()
                            ) {
                                matchingProducts.forEach { product ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(product.name, fontWeight = FontWeight.SemiBold)
                                                Text(
                                                    "Modal: ${currencyFormatter.format(product.price).replace("Rp", "Rp ")} / ${product.unit}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Inventory2,
                                                contentDescription = null,
                                                tint = primaryColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            selectedProduct = product.name
                                            unitPrice = if (product.price > 0) {
                                                if (product.price % 1.0 == 0.0) product.price.toLong().toString() else product.price.toString()
                                            } else ""

                                            if (product.unit in unitOptions.filter { it != "Custom" }) {
                                                selectedUnit = product.unit
                                                customUnit = ""
                                            } else {
                                                selectedUnit = "Custom"
                                                customUnit = product.unit
                                            }

                                            productExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Quick Product Pill Chips
                        if (products.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                products.take(6).forEach { p ->
                                    val isSelected = selectedProduct.equals(p.name, ignoreCase = true)
                                    Surface(
                                        onClick = {
                                            if (isSelected) {
                                                selectedProduct = ""
                                            } else {
                                                selectedProduct = p.name
                                                if (p.price > 0) {
                                                    unitPrice = if (p.price % 1.0 == 0.0) p.price.toLong().toString() else p.price.toString()
                                                }
                                                if (p.unit in unitOptions.filter { it != "Custom" }) {
                                                    selectedUnit = p.unit
                                                    customUnit = ""
                                                } else {
                                                    selectedUnit = "Custom"
                                                    customUnit = p.unit
                                                }
                                            }
                                        },
                                        shape = CircleShape,
                                        color = if (isSelected) primaryColor.copy(alpha = 0.16f) else inputFieldBg,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) primaryColor else borderColor
                                        )
                                    ) {
                                        Text(
                                            text = p.name,
                                            fontSize = 12.sp.scaled,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Unit Selection Section
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Satuan Unit",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.5.sp.scaled
                                )
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showUnitManagement = true }
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Kelola",
                                        fontSize = 12.sp.scaled,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                unitOptions.forEach { opt ->
                                    val isSelected = selectedUnit == opt
                                    Surface(
                                        onClick = { selectedUnit = opt },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) primaryColor else inputFieldBg,
                                        border = if (isSelected) null else BorderStroke(1.dp, borderColor),
                                        modifier = Modifier.heightIn(min = 38.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = opt,
                                                fontSize = 12.5.sp.scaled,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                color = if (isSelected) onPrimaryColor else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                                val isCustomSelected = selectedUnit == "Custom"
                                Surface(
                                    onClick = { selectedUnit = "Custom" },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isCustomSelected) primaryColor else inputFieldBg,
                                    border = if (isCustomSelected) null else BorderStroke(1.dp, borderColor),
                                    modifier = Modifier.heightIn(min = 38.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "Lainnya...",
                                            fontSize = 12.5.sp.scaled,
                                            fontWeight = if (isCustomSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = if (isCustomSelected) onPrimaryColor else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            if (selectedUnit == "Custom") {
                                OutlinedTextField(
                                    value = customUnit,
                                    onValueChange = { customUnit = it },
                                    placeholder = { Text("Satuan Kustom (contoh: ikat, ball)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryColor,
                                        unfocusedBorderColor = borderColor,
                                        focusedContainerColor = inputFieldBg,
                                        unfocusedContainerColor = inputFieldBg
                                    )
                                )
                            }
                        }

                        // Side-by-Side Quantity & Price Fields
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = quantity,
                                onValueChange = { quantity = it },
                                placeholder = {
                                    Text(
                                        "Jumlah ($activeUnit)",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.5.sp.scaled
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quantity_input"),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = borderColor,
                                    focusedContainerColor = inputFieldBg,
                                    unfocusedContainerColor = inputFieldBg,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            OutlinedTextField(
                                value = unitPrice,
                                onValueChange = { unitPrice = it },
                                placeholder = {
                                    Text(
                                        "Harga / $activeUnit",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.5.sp.scaled
                                    )
                                },
                                prefix = if (unitPrice.isNotEmpty()) {
                                    { Text("Rp ", fontWeight = FontWeight.Bold, color = primaryColor) }
                                } else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("unit_price_input"),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = borderColor,
                                    focusedContainerColor = inputFieldBg,
                                    unfocusedContainerColor = inputFieldBg,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        // Quick Qty Preset Chips & Steppers
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Jumlah:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp.scaled
                            )
                            listOf(0.5, 1.0, 2.0, 5.0, 10.0, 25.0).forEach { presetVal ->
                                val label = if (presetVal == 0.5) "0,5" else "+${presetVal.toLong()}"
                                val isHalf = presetVal == 0.5
                                Surface(
                                    onClick = {
                                        val current = quantity.toDoubleOrNull() ?: 0.0
                                        val next = if (isHalf && current == 0.0) 0.5 else current + presetVal
                                        quantity = if (next % 1.0 == 0.0) next.toLong().toString() else String.format(Locale.US, "%.1f", next)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = primaryColor.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.24f)),
                                    modifier = Modifier.heightIn(min = 34.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp.scaled,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryColor
                                        )
                                    }
                                }
                            }

                            // ±0.5 Steppers
                            Surface(
                                onClick = {
                                    val current = quantity.toDoubleOrNull() ?: 0.0
                                    val next = (current - 0.5).coerceAtLeast(0.0)
                                    quantity = if (next % 1.0 == 0.0) next.toLong().toString() else String.format(Locale.US, "%.1f", next)
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = inputFieldBg,
                                border = BorderStroke(1.dp, borderColor),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = "Kurangi 0,5",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Surface(
                                onClick = {
                                    val current = quantity.toDoubleOrNull() ?: 0.0
                                    val next = current + 0.5
                                    quantity = if (next % 1.0 == 0.0) next.toLong().toString() else String.format(Locale.US, "%.1f", next)
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = inputFieldBg,
                                border = BorderStroke(1.dp, borderColor),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Tambah 0,5",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            TextButton(
                                onClick = { quantity = "" },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Reset", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Master Price Helper
                        if (matchedProduct != null && matchedProduct.price > 0 && unitPrice != matchedProduct.price.toLong().toString()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Harga Master: ${currencyFormatter.format(matchedProduct.price).replace("Rp", "Rp ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = {
                                        unitPrice = if (matchedProduct.price % 1.0 == 0.0) {
                                            matchedProduct.price.toLong().toString()
                                        } else matchedProduct.price.toString()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Gunakan Harga Master", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Add to List Button
                        Button(
                            onClick = {
                                if (selectedProduct.isNotBlank() && parsedQty > 0 && parsedPrice >= 0) {
                                    val newItem = ManualCartItem(
                                        productName = selectedProduct.trim(),
                                        quantity = parsedQty,
                                        unitPrice = parsedPrice,
                                        unit = activeUnit
                                    )
                                    manualItemsList = manualItemsList + newItem
                                    selectedProduct = ""
                                    quantity = ""
                                    unitPrice = ""
                                    Toast.makeText(context, "Item ditambahkan ke daftar kulakan", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Lengkapi nama produk, jumlah, dan harga terlebih dahulu", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("add_to_cart_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = onPrimaryColor
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Tambah ke Daftar Kulakan",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp.scaled
                            )
                        }
                    }
                }

                // --- 5. MULTI-ITEM CART LIST ---
                if (manualItemsList.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = surfaceColor,
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(primaryColor, shape = CircleShape)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "DAFTAR BARANG KULAKAN (${manualItemsList.size})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = primaryColor,
                                        letterSpacing = 1.sp
                                    )
                                }
                                TextButton(onClick = { manualItemsList = emptyList() }) {
                                    Text("Hapus Semua", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            manualItemsList.forEach { cartItem ->
                                Surface(
                                    color = inputFieldBg,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, borderColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = cartItem.productName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(Modifier.height(3.dp))
                                            Text(
                                                text = "${cartItem.quantity} ${cartItem.unit} × ${currencyFormatter.format(cartItem.unitPrice).replace("Rp", "Rp ")}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = currencyFormatter.format(cartItem.quantity * cartItem.unitPrice).replace("Rp", "Rp "),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp,
                                                color = primaryColor
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            IconButton(
                                                onClick = { manualItemsList = manualItemsList.filter { it.id != cartItem.id } },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Clear,
                                                    contentDescription = "Hapus",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 6. MODE DEBUG KAMERA & LOG ---
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = surfaceColor,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Mode Debug Kamera & Log",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.5.sp.scaled
                                    )
                                    Text(
                                        text = "Tampilkan detail log pembacaan OCR nota",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.5.sp.scaled
                                    )
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            Switch(
                                checked = isCameraDebugEnabled,
                                onCheckedChange = { isCameraDebugEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = onPrimaryColor,
                                    checkedTrackColor = primaryColor,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    uncheckedTrackColor = inputFieldBg,
                                    uncheckedBorderColor = borderColor
                                )
                            )
                        }

                        if (latestScannerReport != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { showScannerDebug = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.DocumentScanner, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Buka Laporan Terakhir Hasil Scan", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal & Dialogs
    if (isGalleryScanning) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = surfaceColor,
            title = { Text("Membaca Nota Galeri...", fontWeight = FontWeight.Bold) },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = primaryColor)
                    Text("Mendeteksi daftar barang, tanggal, dan harga...", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {}
        )
    }

    if (scannedItemsPreview != null) {
        ScanResultDialog(
            scannedResult = scannedItemsPreview!!,
            productMasterList = products,
            availableUnits = unitOptions,
            showDebugOption = isCameraDebugEnabled,
            onShowDebug = { showScannerDebug = true },
            onConfirm = { itemsToSave, supplierName, confirmedDate ->
                if (itemsToSave.isNotEmpty()) {
                    supplier = supplierName
                    selectedDate = confirmedDate
                    viewModel.addScannedTransactions(itemsToSave, supplierName, confirmedDate)
                    scannedItemsPreview = null
                    onNavigateBack()
                } else {
                    Toast.makeText(context, "Pilih minimal 1 item!", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = {
                scannedItemsPreview = null
            }
        )
    }

    if (showUnitManagement) {
        UnitManagementDialog(
            units = unitOptions,
            onAdd = { viewModel.addCustomUnit(it) },
            onUpdate = { old, new -> viewModel.updateCustomUnit(old, new) },
            onDelete = { viewModel.deleteCustomUnit(it) },
            onDismiss = { showUnitManagement = false }
        )
    }

    if (showScannerDebug) {
        AlertDialog(
            onDismissRequest = { showScannerDebug = false },
            containerColor = surfaceColor,
            title = { Text("Log Debug Camera", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .height(300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        latestScannerReport ?: "Belum ada hasil scan. Scan nota terlebih dahulu untuk melihat detail pembacaannya.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        latestScannerReport?.let { clipboardManager.setText(AnnotatedString(it)) }
                        Toast.makeText(context, "Log debug disalin", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Salin")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScannerDebug = false }) { Text("Tutup") }
            }
        )
    }
}
