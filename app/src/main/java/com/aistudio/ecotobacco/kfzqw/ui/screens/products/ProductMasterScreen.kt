package com.aistudio.ecotobacco.kfzqw.ui.screens.products

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProductEntity
import com.aistudio.ecotobacco.kfzqw.ui.theme.CyberAmber
import com.aistudio.ecotobacco.kfzqw.ui.theme.CyberBg
import com.aistudio.ecotobacco.kfzqw.ui.theme.CyberBgElevated
import com.aistudio.ecotobacco.kfzqw.ui.theme.CyberBorder
import com.aistudio.ecotobacco.kfzqw.ui.theme.CyberCard
import com.aistudio.ecotobacco.kfzqw.ui.theme.CyberCardElevated
import com.aistudio.ecotobacco.kfzqw.ui.theme.CyberCyan
import com.aistudio.ecotobacco.kfzqw.ui.theme.CyberEmerald
import com.aistudio.ecotobacco.kfzqw.ui.theme.CyberNeonGreen
import com.aistudio.ecotobacco.kfzqw.ui.theme.CyberRose
import com.aistudio.ecotobacco.kfzqw.ui.theme.CyberSky
import com.aistudio.ecotobacco.kfzqw.ui.theme.CyberTextBright
import com.aistudio.ecotobacco.kfzqw.ui.theme.CyberTextDim
import com.aistudio.ecotobacco.kfzqw.ui.theme.CyberTextMuted
import com.aistudio.ecotobacco.kfzqw.ui.theme.CyberViolet
import com.aistudio.ecotobacco.kfzqw.viewmodel.TobaccoViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductMasterScreen(
    viewModel: TobaccoViewModel,
    onNavigateBack: () -> Unit
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val unitOptions by viewModel.customUnits.collectAsStateWithLifecycle()
    val customSubCategories by viewModel.customSubCategories.collectAsStateWithLifecycle()
    val subCategoryOptions = remember(customSubCategories, products) {
        (products.map { it.subCategory } + customSubCategories).filter { it.isNotBlank() }.distinct().sorted()
    }
    val isFirestoreSyncing by viewModel.isFirestoreSyncing.collectAsStateWithLifecycle()
    val firestoreSyncProgress by viewModel.firestoreSyncProgress.collectAsStateWithLifecycle()
    val firestoreMessage by viewModel.firestoreMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("SEMUA") }

    val excelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importProductsFromCsv(context, it)
        }
    }

    // Number formatter
    val numberFormatter = remember {
        NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).apply {
            maximumFractionDigits = 0
        }
    }

    // Filter Logic
    val filteredProducts = remember(products, searchQuery, selectedCategoryFilter) {
        val queryLower = searchQuery.trim().lowercase()
        val list = products.filter { prod ->
            val matchQuery = if (queryLower.isEmpty()) {
                true
            } else {
                prod.name.lowercase().contains(queryLower) ||
                        prod.unit.lowercase().contains(queryLower) ||
                        prod.sellingUnit.lowercase().contains(queryLower)
            }

            val nameLower = prod.name.lowercase()
            val matchCategory = when {
                selectedCategoryFilter == "SEMUA" -> true
                selectedCategoryFilter == "ADA_JUAL" -> prod.sellingPrice > 0.0
                selectedCategoryFilter == "BELUM_JUAL" -> prod.sellingPrice <= 0.0
                else -> prod.subCategory.equals(selectedCategoryFilter, ignoreCase = true)
            }

            matchQuery && matchCategory
        }
        list.sortedBy { it.name.lowercase() }
    }

    // Metric Counters
    val totalCount = products.size
    val readyToSellCount = products.count { it.sellingPrice > 0 }
    val needSellPriceCount = products.count { it.sellingPrice <= 0 }

    // Ambient Cyber Glow Background
    Scaffold(
        containerColor = CyberBg,
        floatingActionButton = {
            CyberFloatingActionButton(
                onClick = { showDialog = true },
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .drawBehind {
                    // Subtle Cyber Ambient Radial Glows at corners
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(CyberCyan.copy(alpha = 0.09f), Color.Transparent),
                            center = Offset(size.width * 0.15f, 0f),
                            radius = size.width * 0.7f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(CyberEmerald.copy(alpha = 0.06f), Color.Transparent),
                            center = Offset(size.width * 0.85f, size.height * 0.35f),
                            radius = size.width * 0.6f
                        )
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                // --- 1. FUTURISTIC HUD HEADER ---
                CyberTopHudHeader(
                    totalItems = totalCount,
                    isSyncing = isFirestoreSyncing,
                    onNavigateBack = onNavigateBack,
                    onSyncClick = {
                        viewModel.syncFirestore { _, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onImportClick = {
                        excelPickerLauncher.launch("text/comma-separated-values")
                    }
                )

                // --- 2. FIRESTORE SYNC HUD SCANNER (When Syncing) ---
                if (isFirestoreSyncing) {
                    CyberSyncScannerBanner(
                        message = firestoreMessage ?: "Menyinkronkan database...",
                        progress = firestoreSyncProgress
                    )
                }

                // --- 3. QUICK STATS TELEMETRY BAR ---
                CyberTelemetryBar(
                    total = totalCount,
                    readyToSell = readyToSellCount,
                    needSell = needSellPriceCount,
                    activeFilter = selectedCategoryFilter,
                    onFilterSelect = { selectedCategoryFilter = it }
                )

                // --- 4. FUTURISTIC CAPSULE SEARCH BAR ---
                CyberSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    resultCount = filteredProducts.size,
                    totalCount = totalCount
                )

                // --- 5. CATEGORY FILTER CHIPS ---
                CyberFilterChipsRow(
                    selectedFilter = selectedCategoryFilter,
                    subCategoryOptions = subCategoryOptions,
                    onSelectFilter = { selectedCategoryFilter = it }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // --- 6. FUTURISTIC PRODUCT LIST WITH SWIPE GESTURES ---
                if (filteredProducts.isEmpty()) {
                    CyberEmptyState(
                        isSearching = searchQuery.isNotBlank() || selectedCategoryFilter != "SEMUA",
                        onResetFilter = {
                            searchQuery = ""
                            selectedCategoryFilter = "SEMUA"
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredProducts, key = { it.id }) { product ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissValue ->
                                    when (dismissValue) {
                                        SwipeToDismissBoxValue.StartToEnd -> {
                                            // Swipe Right -> Edit
                                            productToEdit = product
                                            false // reset dismiss state
                                        }
                                        SwipeToDismissBoxValue.EndToStart -> {
                                            // Swipe Left -> Delete
                                            productToDelete = product
                                            false // reset dismiss state, show confirmation
                                        }
                                        SwipeToDismissBoxValue.Settled -> false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val direction = dismissState.dismissDirection
                                    val color = when (direction) {
                                        SwipeToDismissBoxValue.StartToEnd -> CyberCyan.copy(alpha = 0.25f)
                                        SwipeToDismissBoxValue.EndToStart -> CyberRose.copy(alpha = 0.25f)
                                        else -> Color.Transparent
                                    }
                                    val alignment = when (direction) {
                                        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                        else -> Alignment.Center
                                    }
                                    val icon = when (direction) {
                                        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                                        SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                                        else -> Icons.Default.Edit
                                    }
                                    val tint = when (direction) {
                                        SwipeToDismissBoxValue.StartToEnd -> CyberCyan
                                        SwipeToDismissBoxValue.EndToStart -> CyberRose
                                        else -> Color.Transparent
                                    }
                                    val textLabel = when (direction) {
                                        SwipeToDismissBoxValue.StartToEnd -> "EDIT"
                                        SwipeToDismissBoxValue.EndToStart -> "HAPUS"
                                        else -> ""
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(color, RoundedCornerShape(18.dp))
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = alignment
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (direction == SwipeToDismissBoxValue.StartToEnd) {
                                                Icon(icon, contentDescription = textLabel, tint = tint, modifier = Modifier.size(20.dp))
                                                Text(textLabel, fontWeight = FontWeight.Bold, color = tint, fontSize = 11.sp)
                                            } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                                                Text(textLabel, fontWeight = FontWeight.Bold, color = tint, fontSize = 11.sp)
                                                Icon(icon, contentDescription = textLabel, tint = tint, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            ) {
                                CyberProductCard(
                                    product = product,
                                    numberFormatter = numberFormatter,
                                    onCardClick = { productToEdit = product },
                                    onEditClick = { productToEdit = product }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- DIALOGS ---
        if (showDialog) {
            AddProductDialog(
                unitOptions = unitOptions,
                subCategoryOptions = subCategoryOptions,
                onAddCustomUnit = { viewModel.addCustomUnit(it) },
                onUpdateCustomUnit = { old, new -> viewModel.updateCustomUnit(old, new) },
                onDeleteCustomUnit = { viewModel.deleteCustomUnit(it) },
                onAddCustomSubCategory = { viewModel.addCustomSubCategory(it) },
                onDismiss = { showDialog = false },
                onAdd = { name, price, unit, sellingPrice, sellingUnit, contentQuantity, category, subCategory ->
                    val exists = products.any { it.name.equals(name, ignoreCase = true) }
                    if (exists) {
                        Toast.makeText(context, "Nama produk sudah terdaftar!", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.addProduct(name, price, unit, sellingPrice, sellingUnit, contentQuantity, category, subCategory)
                        showDialog = false
                    }
                }
            )
        }

        if (productToEdit != null) {
            EditProductDialog(
                product = productToEdit!!,
                unitOptions = unitOptions,
                subCategoryOptions = subCategoryOptions,
                onAddCustomUnit = { viewModel.addCustomUnit(it) },
                onUpdateCustomUnit = { old, new -> viewModel.updateCustomUnit(old, new) },
                onDeleteCustomUnit = { viewModel.deleteCustomUnit(it) },
                onAddCustomSubCategory = { viewModel.addCustomSubCategory(it) },
                onDismiss = { productToEdit = null },
                onConfirm = { name, price, unit, sellingPrice, sellingUnit, contentQuantity, category, subCategory ->
                    viewModel.updateProduct(productToEdit!!.id, name, price, unit, sellingPrice, sellingUnit, contentQuantity, category, subCategory)
                    productToEdit = null
                },
                onDelete = {
                    viewModel.deleteProduct(productToEdit!!.id)
                    productToEdit = null
                }
            )
        }

        // Swipe Left Delete Confirmation Dialog
        if (productToDelete != null) {
            AlertDialog(
                onDismissRequest = { productToDelete = null },
                title = { Text("KONFIRMASI HAPUS", fontWeight = FontWeight.ExtraBold, color = CyberTextBright) },
                text = { Text("Apakah Anda yakin ingin menghapus produk '${productToDelete?.name}'?", color = CyberTextMuted) },
                containerColor = CyberCard,
                shape = RoundedCornerShape(20.dp),
                confirmButton = {
                    Button(
                        onClick = {
                            productToDelete?.let { viewModel.deleteProduct(it.id) }
                            productToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberRose)
                    ) {
                        Text("HAPUS", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { productToDelete = null }) {
                        Text("BATAL", color = CyberTextMuted)
                    }
                }
            )
        }
    }
}

// -------------------------------------------------------------
// COMPONENT: FUTURISTIC HUD TOP BAR
// -------------------------------------------------------------
@Composable
private fun CyberTopHudHeader(
    totalItems: Int,
    isSyncing: Boolean,
    onNavigateBack: () -> Unit,
    onSyncClick: () -> Unit,
    onImportClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Surface(
        color = CyberBgElevated.copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button (Cyber Squircle)
                Surface(
                    onClick = onNavigateBack,
                    shape = RoundedCornerShape(12.dp),
                    color = CyberCard,
                    border = BorderStroke(1.dp, CyberBorder),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title + Status Pulse
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(CyberNeonGreen.copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ECO TOBACCO // INVENTORY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.8.sp,
                            color = CyberCyan
                        )
                    }
                    Text(
                        text = "Database Produk",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.3.sp,
                        color = CyberTextBright
                    )
                }

                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sync Button
                    Surface(
                        onClick = onSyncClick,
                        enabled = !isSyncing,
                        shape = RoundedCornerShape(12.dp),
                        color = CyberCard,
                        border = BorderStroke(1.dp, CyberBorder),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = CyberCyan
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sinkronkan",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Import CSV Button
                    Surface(
                        onClick = onImportClick,
                        shape = RoundedCornerShape(12.dp),
                        color = CyberCard,
                        border = BorderStroke(1.dp, CyberBorder),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Import CSV",
                                tint = CyberNeonGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // High-Tech Bottom Line Accent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                CyberCyan.copy(alpha = 0.5f),
                                CyberNeonGreen.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

// -------------------------------------------------------------
// COMPONENT: FIRESTORE SCANNER BANNER
// -------------------------------------------------------------
@Composable
private fun CyberSyncScannerBanner(message: String, progress: Float) {
    Surface(
        color = CyberCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = CyberCyan
                    )
                    Text(
                        text = message,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = CyberTextBright
                    )
                }

                Surface(
                    color = CyberCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress.coerceIn(0.05f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = CyberCyan,
                trackColor = CyberCardElevated
            )
        }
    }
}

// -------------------------------------------------------------
// COMPONENT: TELEMETRY QUICK STATS BAR
// -------------------------------------------------------------
@Composable
private fun CyberTelemetryBar(
    total: Int,
    readyToSell: Int,
    needSell: Int,
    activeFilter: String,
    onFilterSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CyberTelemetryCard(
            label = "TOTAL SKU",
            value = "$total",
            unit = "Item",
            accentColor = CyberCyan,
            icon = Icons.Default.Inventory2,
            isSelected = activeFilter == "SEMUA",
            onClick = { onFilterSelect("SEMUA") },
            modifier = Modifier.weight(1f)
        )

        CyberTelemetryCard(
            label = "SIAP JUAL",
            value = "$readyToSell",
            unit = "Aktif",
            accentColor = CyberNeonGreen,
            icon = Icons.Default.TrendingUp,
            isSelected = activeFilter == "ADA_JUAL",
            onClick = { onFilterSelect("ADA_JUAL") },
            modifier = Modifier.weight(1f)
        )

        CyberTelemetryCard(
            label = "SET HARGA",
            value = "$needSell",
            unit = "Butuh",
            accentColor = if (needSell > 0) CyberAmber else CyberTextDim,
            icon = Icons.Default.WarningAmber,
            isSelected = activeFilter == "BELUM_JUAL",
            onClick = { onFilterSelect("BELUM_JUAL") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CyberTelemetryCard(
    label: String,
    value: String,
    unit: String,
    accentColor: Color,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) CyberCardElevated else CyberCard,
        border = BorderStroke(1.dp, if (isSelected) accentColor else CyberBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = CyberTextMuted
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = CyberTextBright
                )
                Text(
                    text = unit,
                    fontSize = 10.sp,
                    color = accentColor,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// COMPONENT: FUTURISTIC CAPSULE SEARCH BAR
// -------------------------------------------------------------
@Composable
private fun CyberSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    resultCount: Int,
    totalCount: Int
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CyberCard,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    CyberCyan.copy(alpha = if (query.isNotEmpty()) 0.8f else 0.35f),
                    CyberNeonGreen.copy(alpha = if (query.isNotEmpty()) 0.6f else 0.2f)
                )
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Cari",
                tint = if (query.isNotEmpty()) CyberCyan else CyberTextMuted,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        "Cari barang, merek, satuan...",
                        fontSize = 13.sp,
                        color = CyberTextDim
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = CyberTextBright,
                    unfocusedTextColor = CyberTextBright,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = CyberCyan,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("product_search_input")
            )

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = CyberTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CyberCyan.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f)),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    text = "$resultCount/$totalCount",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// COMPONENT: FUTURISTIC FILTER CHIPS
// -------------------------------------------------------------
@Composable
private fun CyberFilterChipsRow(
    selectedFilter: String,
    subCategoryOptions: List<String>,
    onSelectFilter: (String) -> Unit
) {
    val filters = mutableListOf("SEMUA" to "⚡ Semua")
    subCategoryOptions.forEach { sub ->
        filters.add(sub to sub)
    }
    filters.add("ADA_JUAL" to "💎 Ada Jual")
    filters.add("BELUM_JUAL" to "⚠️ Belum Jual")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (id, label) ->
            val isSelected = selectedFilter == id
            Surface(
                onClick = { onSelectFilter(id) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) CyberCyan else CyberCard,
                border = BorderStroke(1.dp, if (isSelected) CyberCyan else CyberBorder)
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                    color = if (isSelected) Color(0xFF090D16) else CyberTextBright,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// COMPONENT: FUTURISTIC PRODUCT CARD
// -------------------------------------------------------------
@Composable
private fun CyberProductCard(
    product: ProductEntity,
    numberFormatter: NumberFormat,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val nameLower = product.name.lowercase()
    val (iconVector, iconTint, iconBg) = remember(nameLower) {
        when {
            nameLower.contains("tembakau") || nameLower.contains("tobacco") ||
                    nameLower.contains("tingwe") || nameLower.contains("mole") ||
                    nameLower.contains("flavour") || nameLower.contains("flavor") ||
                    nameLower.contains("mild") || nameLower.contains("surya") ||
                    nameLower.contains("bufalo") || nameLower.contains("buffalo") ||
                    nameLower.contains("caml") || nameLower.contains("djarum") ||
                    nameLower.contains("samsu") || nameLower.contains("nona") ||
                    nameLower.contains("putri") -> {
                Triple(Icons.Default.Eco, CyberNeonGreen, CyberNeonGreen.copy(alpha = 0.12f))
            }
            nameLower.contains("alat") || nameLower.contains("linting") ||
                    nameLower.contains("slorok") || nameLower.contains("roller") -> {
                Triple(Icons.Default.Build, CyberSky, CyberSky.copy(alpha = 0.12f))
            }
            nameLower.contains("papir") || nameLower.contains("kertas") ||
                    nameLower.contains("busa") || nameLower.contains("filter") ||
                    nameLower.contains("gizeh") || nameLower.contains("raw") -> {
                Triple(Icons.Default.Layers, CyberViolet, CyberViolet.copy(alpha = 0.12f))
            }
            else -> {
                Triple(Icons.Default.Inventory2, CyberCyan, CyberCyan.copy(alpha = 0.12f))
            }
        }
    }

    val hasSellingPrice = product.sellingPrice > 0.0
    val contentQty = if (product.contentQuantity > 0.0) product.contentQuantity else 1.0
    val costPerSellingUnit = product.price / contentQty
    val marginPerUnit = if (hasSellingPrice) product.sellingPrice - costPerSellingUnit else 0.0
    val marginPercent = if (hasSellingPrice && costPerSellingUnit > 0.0) {
        (marginPerUnit / costPerSellingUnit) * 100
    } else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCard),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    CyberCyan.copy(alpha = 0.35f),
                    CyberBorder,
                    CyberNeonGreen.copy(alpha = 0.2f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconBg,
                    border = BorderStroke(1.dp, iconTint.copy(alpha = 0.35f)),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberTextBright,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CyberCardElevated,
                            border = BorderStroke(1.dp, CyberBorder)
                        ) {
                            Text(
                                text = "SATUAN: ${product.unit.uppercase()}",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (product.contentQuantity > 1.0) {
                            Text(
                                text = "1 ${product.unit} = ${product.contentQuantity.toInt()} ${product.sellingUnit}",
                                fontSize = 9.sp,
                                color = CyberTextMuted
                            )
                        }
                    }
                }

                Surface(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(10.dp),
                    color = CyberCardElevated,
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = CyberCyan,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CyberBg.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MODAL KULAK",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberTextMuted,
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Rp ${numberFormatter.format(product.price)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberSky
                            )
                            Text(
                                text = "/ ${product.unit}",
                                fontSize = 10.sp,
                                color = CyberTextDim,
                                modifier = Modifier.padding(bottom = 1.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp)
                            .background(CyberBorder)
                    )

                    if (hasSellingPrice) {
                        Column(horizontalAlignment = Alignment.End) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "HARGA JUAL",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberTextMuted,
                                    letterSpacing = 0.5.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (marginPerUnit >= 0) CyberNeonGreen.copy(alpha = 0.15f) else CyberRose.copy(alpha = 0.15f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (marginPerUnit >= 0) CyberNeonGreen.copy(alpha = 0.4f) else CyberRose.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Text(
                                        text = "${if (marginPercent >= 0) "+" else ""}${marginPercent.toInt()}%",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (marginPerUnit >= 0) CyberNeonGreen else CyberRose,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Rp ${numberFormatter.format(product.sellingPrice)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberNeonGreen
                                )
                                Text(
                                    text = "/ ${product.sellingUnit}",
                                    fontSize = 10.sp,
                                    color = CyberTextDim,
                                    modifier = Modifier.padding(bottom = 1.dp)
                                )
                            }
                        }
                    } else {
                        Surface(
                            onClick = onEditClick,
                            shape = RoundedCornerShape(8.dp),
                            color = CyberAmber.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, CyberAmber.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = CyberAmber,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Set Harga Jual",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberAmber
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// COMPONENT: FUTURISTIC FLOATING ACTION BUTTON
// -------------------------------------------------------------
@Composable
private fun CyberFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.8f)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(CyberCyan, CyberNeonGreen)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah Barang",
                    tint = Color(0xFF090D16),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "TAMBAH BARANG",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = Color(0xFF090D16)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// COMPONENT: FUTURISTIC EMPTY STATE
// -------------------------------------------------------------
@Composable
private fun CyberEmptyState(
    isSearching: Boolean,
    onResetFilter: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = CyberCard,
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = CyberCyan.copy(alpha = 0.7f)
                    )
                }
            }

            Text(
                text = if (isSearching) "PRODUK TIDAK DITEMUKAN" else "DATABASE MASIH KOSONG",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp,
                color = CyberTextBright
            )

            Text(
                text = if (isSearching)
                    "Coba ganti kata kunci pencarian atau reset filter kategori."
                else
                    "Tekan tombol '+ TAMBAH BARANG' atau import CSV untuk memasukkan daftar barang.",
                fontSize = 12.sp,
                color = CyberTextMuted,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (isSearching) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    onClick = onResetFilter,
                    shape = RoundedCornerShape(10.dp),
                    color = CyberCardElevated,
                    border = BorderStroke(1.dp, CyberCyan)
                ) {
                    Text(
                        text = "Reset Pencarian & Filter",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
