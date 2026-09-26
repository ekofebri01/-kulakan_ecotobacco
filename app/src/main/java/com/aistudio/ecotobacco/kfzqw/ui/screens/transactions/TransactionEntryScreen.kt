package com.aistudio.ecotobacco.kfzqw.ui.screens.transactions

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProductEntity
import com.aistudio.ecotobacco.kfzqw.data.utils.LocalScanner
import com.aistudio.ecotobacco.kfzqw.ui.components.UnitManagementDialog
import com.aistudio.ecotobacco.kfzqw.ui.theme.LocalUiConfig
import com.aistudio.ecotobacco.kfzqw.ui.theme.scaled
import com.aistudio.ecotobacco.kfzqw.viewmodel.TobaccoViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
    val totalTransaction = parsedQty * parsedPrice
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

    val csvPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importTransactionsFromCsv(context, it) }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Input Kulakan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp.scaled
                        )
                        Text(
                            text = "Catat Pembelian & Stok",
                            style = MaterialTheme.typography.labelSmall,
                            color = primaryColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.padding(start = 12.dp).size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    FilledTonalIconButton(
                        onClick = { csvPickerLauncher.launch("text/*") },
                        modifier = Modifier.padding(end = 12.dp).size(38.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = primaryColor.copy(alpha = 0.15f),
                            contentColor = primaryColor
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Import CSV",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // Modern Floating Bottom Bar with Live Total & Save Action
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                color = surfaceColor,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Total Kulakan",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = currencyFormatter.format(totalTransaction).replace("Rp", "Rp "),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = if (totalTransaction > 0) primaryColor else MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp.scaled
                        )
                        if (parsedQty > 0) {
                            Text(
                                text = "Kuantitas: $quantity $activeUnit",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    val isFormValid = selectedProduct.isNotBlank() && quantity.isNotBlank() && unitPrice.isNotBlank() && (parsedQty > 0)

                    Button(
                        onClick = {
                            if (isFormValid) {
                                viewModel.addTransaction(
                                    date = selectedDate,
                                    productName = selectedProduct.trim(),
                                    quantity = parsedQty,
                                    unitPrice = parsedPrice,
                                    supplier = supplier.trim(),
                                    unit = activeUnit
                                )
                                Toast.makeText(context, "Kulakan Berhasil Disimpan!", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            } else {
                                Toast.makeText(context, "Lengkapi produk, jumlah, dan harga", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .height(52.dp)
                            .weight(1.1f),
                        shape = RoundedCornerShape(16.dp),
                        enabled = isFormValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            disabledContainerColor = primaryColor.copy(alpha = 0.3f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Simpan",
                            fontSize = 16.sp.scaled,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding: PaddingValues ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // --- 1. HERO AI NOTA SCANNER BANNER ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onNavigateToScanner() },
                colors = CardDefaults.cardColors(
                    containerColor = surfaceVariant.copy(alpha = 0.45f)
                ),
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(primaryColor, primaryColor.copy(alpha = 0.7f))
                                        ),
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = "Scan Nota",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Scan Nota Otomatis",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(primaryColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "OCR",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = primaryColor
                                        )
                                    }
                                }
                                Text(
                                    text = "Foto struk faktur, data langsung terisi otomatis",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Button(
                            onClick = onNavigateToScanner,
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Pindai", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- 2. LIVE DIGITAL BILL / STRUK ELEKTRONIK CARD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = surfaceColor
                ),
                border = BorderStroke(
                    1.dp,
                    if (totalTransaction > 0) primaryColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
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
                                    .size(8.dp)
                                    .background(
                                        if (totalTransaction > 0) primaryColor else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RINGKASAN NOTA",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (totalTransaction > 0) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID")).format(Date(selectedDate)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (selectedProduct.isNotBlank() || parsedQty > 0 || parsedPrice > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedProduct.isNotBlank()) selectedProduct else "Pilih produk...",
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
                                Text(
                                    text = "$quantity $activeUnit × ${currencyFormatter.format(parsedPrice).replace("Rp", "Rp ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Smart Insight (Margin / Jual jika ada data di master)
                        if (matchedProduct != null && matchedProduct.sellingPrice > 0) {
                            val potentialRevenue = parsedQty * matchedProduct.sellingPrice
                            val potentialProfit = potentialRevenue - totalTransaction
                            val profitMarginPct = if (totalTransaction > 0) ((potentialProfit / totalTransaction) * 100) else 0.0

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = primaryColor.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f))
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
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        text = "Est. Margin: +${String.format(Locale.US, "%.1f", profitMarginPct)}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor
                                    )
                                }
                            }
                        }
                    } else {
                        // Empty bill hint
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "Ketik atau pilih produk di bawah untuk kalkulasi nota instan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // --- 3. DETAIL TRANSAKSI SECTION ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Informasi Transaksi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Tanggal Transaksi Modern
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.forLanguageTag("id-ID")).format(Date(selectedDate)),
                            onValueChange = {},
                            label = { Text("Tanggal Pembelian") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            enabled = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            calendar.set(year, month, day)
                                            selectedDate = calendar.timeInMillis
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                        )
                    }

                    // Supplier Input with Suggestion Dropdown & Quick Chips
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = supplierExpanded,
                            onExpandedChange = {
                                supplierExpanded = savedSuppliers.isNotEmpty() && supplier.isNotBlank()
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
                                label = { Text("Supplier / Pengepul") },
                                placeholder = { Text("Contoh: Pengepul Temanggung") },
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
                                            Icon(Icons.Default.Clear, contentDescription = "Hapus", modifier = Modifier.size(18.dp))
                                        }
                                    } else {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierExpanded)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryEditable, true),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )

                            val supplierMatches = savedSuppliers.filter {
                                supplier.isNotBlank() && it.contains(supplier.trim(), ignoreCase = true)
                            }
                            ExposedDropdownMenu(
                                expanded = supplierExpanded && supplierMatches.isNotEmpty(),
                                onDismissRequest = { supplierExpanded = false },
                                modifier = Modifier
                                    .background(surfaceColor)
                                    .exposedDropdownSize()
                            ) {
                                supplierMatches.take(5).forEach { savedSupplier ->
                                    DropdownMenuItem(
                                        text = { Text(savedSupplier, fontWeight = FontWeight.Medium) },
                                        leadingIcon = { Icon(Icons.Default.Storefront, null, modifier = Modifier.size(16.dp)) },
                                        onClick = {
                                            supplier = savedSupplier
                                            supplierExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Quick Supplier Chips
                        if (savedSuppliers.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                savedSuppliers.take(6).forEach { chipSupplier ->
                                    val isSelected = supplier.equals(chipSupplier, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            supplier = if (isSelected) "" else chipSupplier
                                        },
                                        label = { Text(chipSupplier, fontSize = 12.sp) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = primaryColor.copy(alpha = 0.2f),
                                            selectedLabelColor = primaryColor
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 4. ITEM & PRODUK SECTION ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Pilih Produk & Satuan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Autocomplete Product Input
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
                            label = { Text("Nama Produk Tembakau / Barang") },
                            placeholder = { Text("Ketik nama atau pilih dari daftar") },
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
                                        Icon(Icons.Default.Clear, contentDescription = "Hapus", modifier = Modifier.size(18.dp))
                                    }
                                } else {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryEditable, true),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
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

                    // Quick Product Chips (Top master items for 1-tap select)
                    if (products.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Rekomendasi Produk:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                products.take(6).forEach { p ->
                                    val isSelected = selectedProduct.equals(p.name, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
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
                                        label = { Text(p.name, fontSize = 12.sp) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = primaryColor.copy(alpha = 0.2f),
                                            selectedLabelColor = primaryColor
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Satuan Modern Chips & Option Row
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Satuan Unit",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(
                                onClick = { showUnitManagement = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Kelola Satuan", fontSize = 12.sp)
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
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedUnit = opt },
                                    label = { Text(opt, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = primaryColor,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                            FilterChip(
                                selected = selectedUnit == "Custom",
                                onClick = { selectedUnit = "Custom" },
                                label = { Text("Lainnya...", fontWeight = if (selectedUnit == "Custom") FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryColor,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        if (selectedUnit == "Custom") {
                            OutlinedTextField(
                                value = customUnit,
                                onValueChange = { customUnit = it },
                                label = { Text("Tulis Satuan Kustom") },
                                placeholder = { Text("Contoh: ikat, karung, box") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor
                                )
                            )
                        }
                    }
                }
            }

            // --- 5. KUANTITAS & HARGA SECTION ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Kuantitas & Harga Modal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Qty Field with Stepper Buttons
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Decrement button
                            FilledTonalIconButton(
                                onClick = {
                                    val current = quantity.toDoubleOrNull() ?: 0.0
                                    if (current > 1.0) {
                                        val next = current - 1.0
                                        quantity = if (next % 1.0 == 0.0) next.toLong().toString() else String.format(Locale.US, "%.1f", next)
                                    } else if (current > 0.1) {
                                        val next = (current - 0.5).coerceAtLeast(0.0)
                                        quantity = String.format(Locale.US, "%.1f", next)
                                    }
                                },
                                modifier = Modifier.size(52.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Kurang")
                            }

                            OutlinedTextField(
                                value = quantity,
                                onValueChange = { quantity = it },
                                label = { Text("Jumlah ($activeUnit)") },
                                placeholder = { Text("0") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor
                                )
                            )

                            // Increment button
                            FilledTonalIconButton(
                                onClick = {
                                    val current = quantity.toDoubleOrNull() ?: 0.0
                                    val next = current + 1.0
                                    quantity = if (next % 1.0 == 0.0) next.toLong().toString() else String.format(Locale.US, "%.1f", next)
                                },
                                modifier = Modifier.size(52.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Tambah")
                            }
                        }

                        // Quick Qty Preset Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1.0, 2.0, 5.0, 10.0, 25.0, 50.0).forEach { presetVal ->
                                val label = if (presetVal % 1.0 == 0.0) "+${presetVal.toLong()}" else "+$presetVal"
                                FilledTonalButton(
                                    onClick = {
                                        val current = quantity.toDoubleOrNull() ?: 0.0
                                        val next = current + presetVal
                                        quantity = if (next % 1.0 == 0.0) next.toLong().toString() else String.format(Locale.US, "%.1f", next)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            TextButton(
                                onClick = { quantity = "" },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Reset", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    // Price Field
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = unitPrice,
                            onValueChange = { unitPrice = it },
                            label = { Text("Harga Kulakan per $activeUnit") },
                            placeholder = { Text("Contoh: 85000") },
                            prefix = {
                                Text(
                                    "Rp ",
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (unitPrice.isNotBlank()) {
                                    IconButton(onClick = { unitPrice = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Hapus", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor
                            )
                        )

                        // Quick Price helper from master product if available
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
                    }
                }
            }

            // --- 6. ADVANCED SCANNER & DEBUG ACCORDION ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = surfaceVariant.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Mode Debug Kamera & Log",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Tampilkan detail log pembacaan OCR nota",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isCameraDebugEnabled,
                            onCheckedChange = { isCameraDebugEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = primaryColor,
                                checkedTrackColor = primaryColor.copy(alpha = 0.3f)
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

            Spacer(modifier = Modifier.height(80.dp)) // Clearance for floating bottom bar
        }
    }

    // Modal & Dialogs
    if (scannedItemsPreview != null) {
        ScanResultDialog(
            scannedResult = scannedItemsPreview!!,
            productMasterList = products,
            showDebugOption = isCameraDebugEnabled,
            onShowDebug = { showScannerDebug = true },
            onConfirm = { itemsToSave, supplierName ->
                if (itemsToSave.isNotEmpty()) {
                    supplier = supplierName
                    viewModel.addScannedTransactions(itemsToSave, supplierName, selectedDate)
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
