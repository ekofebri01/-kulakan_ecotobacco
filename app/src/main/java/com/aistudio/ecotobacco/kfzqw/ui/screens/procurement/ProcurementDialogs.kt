package com.aistudio.ecotobacco.kfzqw.ui.screens.procurement

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProcurementItemEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProcurementPlanWithItems
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProductEntity
import com.aistudio.ecotobacco.kfzqw.data.utils.LocalScanner
import com.aistudio.ecotobacco.kfzqw.ui.theme.LocalUiConfig
import com.aistudio.ecotobacco.kfzqw.ui.theme.scaled
import com.aistudio.ecotobacco.kfzqw.viewmodel.TobaccoViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ProcurementExportType {
    CHECKLIST, DETAIL, SUPPLIER_ORDER, PLAIN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPlanDialog(
    viewModel: TobaccoViewModel,
    planToEdit: ProcurementPlanWithItems? = null,
    initialScannedResult: LocalScanner.ScannedResult? = null,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, List<ProcurementItemEntity>) -> Unit
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val uiConfig = LocalUiConfig.current

    val existingSuppliers = remember(transactions) {
        transactions
            .map { it.supplier.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .sortedBy { it.lowercase(Locale.ROOT) }
    }

    val formatter = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
            maximumFractionDigits = 0
        }
    }

    val calendar = Calendar.getInstance()
    var selectedDate by remember {
        mutableLongStateOf(planToEdit?.plan?.date ?: System.currentTimeMillis())
    }
    var supplierName by remember { mutableStateOf(planToEdit?.plan?.supplierName ?: "") }
    var supplierExpanded by remember { mutableStateOf(false) }

    val itemsToBuy = remember {
        mutableStateListOf(*(planToEdit?.items?.toTypedArray() ?: emptyArray()))
    }
    var showAddItem by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ProcurementItemEntity?>(null) }
    var showExitConfirmation by remember { mutableStateOf(false) }

    BackHandler(enabled = !showExitConfirmation) {
        if (itemsToBuy.isNotEmpty() || supplierName.isNotBlank()) {
            showExitConfirmation = true
        } else {
            onDismiss()
        }
    }

    LaunchedEffect(initialScannedResult) {
        initialScannedResult?.let { result ->
            result.items.forEach { scannedItem ->
                val exists = itemsToBuy.any { it.productName.equals(scannedItem.name, ignoreCase = true) }
                if (!exists) {
                    val productMatch = products.find { it.name.equals(scannedItem.name, ignoreCase = true) }
                    itemsToBuy.add(
                        ProcurementItemEntity(
                            id = 0,
                            planId = 0,
                            productName = scannedItem.name,
                            targetQuantity = scannedItem.quantity,
                            estimatedUnitPrice = scannedItem.price,
                            unit = productMatch?.unit ?: "kg"
                        )
                    )
                }
            }
            result.date?.let { detectedDate -> selectedDate = detectedDate }
            result.shopName?.let { name -> if (supplierName.isBlank()) supplierName = name }
        }
    }

    val totalEst = itemsToBuy.sumOf { it.targetQuantity * it.estimatedUnitPrice }
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Dialog(
        onDismissRequest = {
            if (itemsToBuy.isNotEmpty() || supplierName.isNotBlank()) {
                showExitConfirmation = true
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (planToEdit != null) "Edit Rencana Kulakan" else "Rencana Baru",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 18.sp.scaled
                            )
                            Text(
                                text = if (planToEdit != null) "Perbarui target belanja & supplier" else "Susun daftar rencana belanja tembakau",
                                style = MaterialTheme.typography.labelSmall,
                                color = primaryColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (itemsToBuy.isNotEmpty() || supplierName.isNotBlank()) {
                                    showExitConfirmation = true
                                } else {
                                    onDismiss()
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.padding(start = 12.dp).size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
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
                                text = "Estimasi Anggaran",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formatter.format(totalEst).replace("Rp", "Rp "),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = if (totalEst > 0) primaryColor else MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp.scaled
                            )
                            Text(
                                text = "${itemsToBuy.size} item produk direncanakan",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        val canSave = itemsToBuy.isNotEmpty()

                        Button(
                            onClick = {
                                if (canSave) {
                                    onConfirm(selectedDate, supplierName.trim(), itemsToBuy.toList())
                                } else {
                                    Toast.makeText(context, "Tambahkan minimal 1 barang ke rencana", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .height(52.dp)
                                .weight(1.1f),
                            shape = RoundedCornerShape(16.dp),
                            enabled = canSave,
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
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(modifier = Modifier.height(2.dp))

                // --- 1. RINGKASAN & TARGET BELANJA CARD ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = BorderStroke(
                        1.dp,
                        if (totalEst > 0) primaryColor.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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
                                            if (totalEst > 0) primaryColor else MaterialTheme.colorScheme.outline,
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "TARGET BELANJA",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (totalEst > 0) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                            }

                            // Tanggal Chip
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = primaryColor.copy(alpha = 0.1f),
                                modifier = Modifier.clickable {
                                    calendar.timeInMillis = selectedDate
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            calendar.set(y, m, d)
                                            selectedDate = calendar.timeInMillis
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = primaryColor
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID")).format(Date(selectedDate)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = primaryColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Supplier Input with Suggestion Dropdown
                        ExposedDropdownMenuBox(
                            expanded = supplierExpanded,
                            onExpandedChange = {
                                supplierExpanded = existingSuppliers.isNotEmpty() && supplierName.isNotBlank()
                            }
                        ) {
                            OutlinedTextField(
                                value = supplierName,
                                onValueChange = {
                                    supplierName = it
                                    supplierExpanded = it.isNotBlank() && existingSuppliers.any { saved ->
                                        saved.contains(it.trim(), ignoreCase = true)
                                    }
                                },
                                label = { Text("Tujuan Supplier / Pengepul") },
                                placeholder = { Text("Contoh: Juragan Tembakau Temanggung") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (supplierName.isNotBlank()) {
                                        IconButton(onClick = { supplierName = "" }) {
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
                                ),
                                singleLine = true
                            )

                            val supplierMatches = existingSuppliers.filter {
                                supplierName.isNotBlank() && it.contains(supplierName.trim(), ignoreCase = true)
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
                                            supplierName = savedSupplier
                                            supplierExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Quick Supplier Chips
                        if (existingSuppliers.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                existingSuppliers.take(5).forEach { chipSupplier ->
                                    val isSelected = supplierName.equals(chipSupplier, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            supplierName = if (isSelected) "" else chipSupplier
                                        },
                                        label = { Text(chipSupplier, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp),
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

                // --- 2. HEADER DAFTAR PRODUK & TOMBOL TAMBAH CEPAT ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daftar Barang Belanja",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Geser kartu ke kiri untuk menghapus",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (itemsToBuy.size > 1) {
                            FilledTonalIconButton(
                                onClick = {
                                    val sorted = itemsToBuy.sortedBy { it.productName.lowercase(Locale.ROOT) }
                                    itemsToBuy.clear()
                                    itemsToBuy.addAll(sorted)
                                    Toast.makeText(context, "Daftar barang diurutkan A-Z", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(38.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = surfaceVariant.copy(alpha = 0.6f),
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(Icons.Default.SortByAlpha, contentDescription = "Urutkan A-Z", modifier = Modifier.size(18.dp))
                            }
                        }

                        FilledTonalButton(
                            onClick = {
                                itemToEdit = null
                                showAddItem = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = primaryColor.copy(alpha = 0.15f),
                                contentColor = primaryColor
                            )
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Tambah Barang", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // --- 3. LIST ITEM KULAKAN DENGAN STEPPER & HARGA INLINE ---
                if (itemsToBuy.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showAddItem = true },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(primaryColor.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Text(
                                    text = "Belum ada barang di rencana ini",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Ketuk di sini atau tombol '+ Tambah Barang' di atas untuk memasukkan tembakau yang ingin dibeli.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        itemsIndexed(itemsToBuy, key = { index, item -> "${item.productName}_$index" }) { index, item ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    if (it == SwipeToDismissBoxValue.EndToStart) {
                                        itemsToBuy.remove(item)
                                        true
                                    } else false
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                backgroundContent = {
                                    val color = when (dismissState.dismissDirection) {
                                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                        else -> Color.Transparent
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(color, RoundedCornerShape(16.dp))
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .background(primaryColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.Inventory2,
                                                        contentDescription = null,
                                                        tint = primaryColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = item.productName,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "Satuan: ${item.unit}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            // Subtotal Item
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = formatter.format(item.targetQuantity * item.estimatedUnitPrice).replace("Rp", "Rp "),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Black,
                                                    color = primaryColor
                                                )
                                                Text(
                                                    text = "Est. Subtotal",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                            thickness = 0.8.dp
                                        )

                                        // Stepper & Inline Price Controls
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Stepper Kuantitas
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                FilledIconButton(
                                                    onClick = {
                                                        if (item.targetQuantity > 1) {
                                                            itemsToBuy[index] = item.copy(targetQuantity = item.targetQuantity - 1)
                                                        } else if (item.targetQuantity > 0.1) {
                                                            val next = String.format(Locale.US, "%.1f", item.targetQuantity - 0.1).toDoubleOrNull() ?: 0.5
                                                            itemsToBuy[index] = item.copy(targetQuantity = maxOf(0.1, next))
                                                        }
                                                    },
                                                    modifier = Modifier.size(32.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = IconButtonDefaults.filledIconButtonColors(
                                                        containerColor = surfaceVariant.copy(alpha = 0.7f),
                                                        contentColor = MaterialTheme.colorScheme.onSurface
                                                    )
                                                ) {
                                                    Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp))
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = surfaceVariant.copy(alpha = 0.35f),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    BasicTextField(
                                                        value = if (item.targetQuantity % 1.0 == 0.0) item.targetQuantity.toInt().toString() else item.targetQuantity.toString(),
                                                        onValueChange = { nv ->
                                                            val d = nv.replace(',', '.').toDoubleOrNull() ?: 0.0
                                                            itemsToBuy[index] = item.copy(targetQuantity = d)
                                                        },
                                                        modifier = Modifier
                                                            .width(54.dp)
                                                            .padding(horizontal = 4.dp, vertical = 6.dp),
                                                        textStyle = MaterialTheme.typography.titleSmall.copy(
                                                            textAlign = TextAlign.Center,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                        singleLine = true
                                                    )
                                                }

                                                FilledIconButton(
                                                    onClick = {
                                                        itemsToBuy[index] = item.copy(targetQuantity = item.targetQuantity + 1)
                                                    },
                                                    modifier = Modifier.size(32.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = IconButtonDefaults.filledIconButtonColors(
                                                        containerColor = surfaceVariant.copy(alpha = 0.7f),
                                                        contentColor = MaterialTheme.colorScheme.onSurface
                                                    )
                                                ) {
                                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                                }
                                                Text(
                                                    text = item.unit,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(start = 2.dp)
                                                )
                                            }

                                            // Inline Price Editor
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = surfaceVariant.copy(alpha = 0.35f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 8.dp)
                                                ) {
                                                    Text(
                                                        "Rp",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = primaryColor,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    BasicTextField(
                                                        value = if (item.estimatedUnitPrice % 1.0 == 0.0) item.estimatedUnitPrice.toLong().toString() else item.estimatedUnitPrice.toString(),
                                                        onValueChange = { nv ->
                                                            val d = nv.replace(',', '.').toDoubleOrNull() ?: 0.0
                                                            itemsToBuy[index] = item.copy(estimatedUnitPrice = d)
                                                        },
                                                        modifier = Modifier.width(76.dp),
                                                        textStyle = MaterialTheme.typography.titleSmall.copy(
                                                            textAlign = TextAlign.End,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        singleLine = true
                                                    )
                                                }
                                            }

                                            // Delete Button
                                            IconButton(
                                                onClick = { itemsToBuy.remove(item) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = "Hapus",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
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
            }
        }
    }

    if (showAddItem) {
        AddItemDialog(
            products = products,
            initialItem = itemToEdit,
            onDismiss = { showAddItem = false; itemToEdit = null },
            onConfirm = { newItems ->
                if (itemToEdit != null) {
                    val index = itemsToBuy.indexOf(itemToEdit)
                    if (index != -1 && newItems.isNotEmpty()) itemsToBuy[index] = newItems.first()
                } else {
                    newItems.forEach { newItem ->
                        val existingIndex = itemsToBuy.indexOfFirst {
                            it.productName.equals(newItem.productName, ignoreCase = true)
                        }
                        if (existingIndex != -1) {
                            val current = itemsToBuy[existingIndex]
                            itemsToBuy[existingIndex] = current.copy(
                                targetQuantity = current.targetQuantity + newItem.targetQuantity
                            )
                        } else {
                            itemsToBuy.add(newItem)
                        }
                    }
                }
                showAddItem = false
                itemToEdit = null
            }
        )
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = {
                Text(
                    text = if (planToEdit != null) "Simpan perubahan rencana?" else "Simpan rencana belanja?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Daftar barang belanja yang sedang Anda susun belum tersimpan.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirmation = false
                        onConfirm(selectedDate, supplierName.trim(), itemsToBuy.toList())
                    }
                ) {
                    Text("Simpan", color = primaryColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExitConfirmation = false
                        onDismiss()
                    }
                ) {
                    Text("Keluar Tanpa Simpan", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    products: List<ProductEntity>,
    initialItem: ProcurementItemEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (List<ProcurementItemEntity>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedProducts = remember { mutableStateListOf<ProductEntity>() }
    
    var selectedProductObj by remember { 
        mutableStateOf(
            if (initialItem != null) {
                products.find { it.name == initialItem.productName } ?: ProductEntity(
                    id = 0,
                    name = initialItem.productName,
                    price = initialItem.estimatedUnitPrice,
                    unit = initialItem.unit
                )
            } else null
        ) 
    }
    
    var qtyStr by remember { 
        mutableStateOf(
            if (initialItem != null) {
                if (initialItem.targetQuantity % 1.0 == 0.0) initialItem.targetQuantity.toInt().toString()
                else initialItem.targetQuantity.toString()
            } else "1"
        )
    }
    
    var priceStr by remember(selectedProductObj) {
        mutableStateOf(selectedProductObj?.price?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "")
    }
    
    var unitStr by remember(selectedProductObj) {
        mutableStateOf(selectedProductObj?.unit ?: "kg")
    }
    
    val formatter = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply { maximumFractionDigits = 0 } }
    val filtered = remember(searchQuery, products) {
        products.filter { it.name.contains(searchQuery, true) }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val parsedQty = qtyStr.toDoubleOrNull() ?: 0.0
    val parsedPrice = priceStr.toDoubleOrNull() ?: 0.0
    val liveSubtotal = parsedQty * parsedPrice

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        ),
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .navigationBarsPadding()
            .imePadding(),
        containerColor = surfaceColor,
        shape = RoundedCornerShape(28.dp),
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selectedProductObj == null) "Pilih Produk Tembakau" else "Atur Jumlah & Harga", 
                        fontWeight = FontWeight.ExtraBold, 
                        style = MaterialTheme.typography.titleLarge, 
                        color = MaterialTheme.colorScheme.onSurface 
                    )
                    Text(
                        text = if (selectedProductObj == null) "Pilih satu atau beberapa produk sekaligus" else "Tentukan estimasi kuantitas dan harga beli",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (selectedProducts.isNotEmpty() && selectedProductObj == null) {
                    Surface(
                        color = primaryColor,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "${selectedProducts.size} Terpilih",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .animateContentSize()
            ) {
                if (selectedProductObj == null) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        label = { Text("Cari Produk") },
                        placeholder = { Text("Ketik nama tembakau...") },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = primaryColor) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            focusedLabelColor = primaryColor,
                            cursorColor = primaryColor
                        ),
                        singleLine = true
                    )

                    if (filtered.isNotEmpty()) {
                        Card(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = surfaceColor),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(filtered) { p ->
                                    val isSelected = selectedProducts.any { it.name == p.name }
                                    ListItem(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { 
                                                if (isSelected) selectedProducts.removeIf { it.name == p.name }
                                                else selectedProducts.add(p)
                                            },
                                        colors = ListItemDefaults.colors(
                                            containerColor = if (isSelected) primaryColor.copy(alpha = 0.12f) else Color.Transparent
                                        ),
                                        leadingContent = {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { checked ->
                                                    if (checked) selectedProducts.add(p)
                                                    else selectedProducts.removeIf { it.name == p.name }
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                                            )
                                        },
                                        headlineContent = { 
                                            Text(p.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) 
                                        },
                                        supportingContent = {
                                            Text(
                                                "Modal: ${formatter.format(p.price).replace("Rp", "Rp ")} / ${p.unit}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        trailingContent = { 
                                            IconButton(onClick = { selectedProductObj = p }) {
                                                Icon(Icons.Default.ChevronRight, null, tint = primaryColor)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    } else if (searchQuery.length >= 2) {
                        Button(
                            onClick = {
                                val newProd = ProductEntity(name = searchQuery.trim(), price = 0.0, unit = "kg")
                                if (!selectedProducts.any { it.name.equals(searchQuery.trim(), ignoreCase = true) }) {
                                    selectedProducts.add(newProd)
                                }
                                searchQuery = ""
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor.copy(alpha = 0.12f),
                                contentColor = primaryColor
                            )
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Tambah '$searchQuery' Baru", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Cari produk untuk ditambahkan", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    // --- TAMPILAN DETAIL PRODUK (ATUR JUMLAH & HARGA MODERN) ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Product Header Banner
                        Card(
                            colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = primaryColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = selectedProductObj!!.name,
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (selectedProductObj!!.price > 0) "Harga Master: ${formatter.format(selectedProductObj!!.price).replace("Rp", "Rp ")} / ${selectedProductObj!!.unit}" else "Satuan master: ${selectedProductObj!!.unit}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (initialItem == null) {
                                    Surface(
                                        onClick = { selectedProductObj = null },
                                        shape = RoundedCornerShape(10.dp),
                                        color = surfaceColor,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Edit, "Ganti Produk", tint = primaryColor, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Stepper & Input Kuantitas
                        Card(
                            colors = CardDefaults.cardColors(containerColor = surfaceColor),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Target Kuantitas & Satuan",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Stepper Qty Button [-]
                                    FilledIconButton(
                                        onClick = {
                                            if (parsedQty > 1) {
                                                val next = parsedQty - 1
                                                qtyStr = if (next % 1.0 == 0.0) next.toInt().toString() else String.format(Locale.US, "%.1f", next)
                                            } else if (parsedQty > 0.1) {
                                                val next = maxOf(0.1, parsedQty - 0.1)
                                                qtyStr = String.format(Locale.US, "%.1f", next)
                                            }
                                        },
                                        modifier = Modifier.size(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = surfaceVariant.copy(alpha = 0.7f),
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Kurang")
                                    }

                                    OutlinedTextField(
                                        value = qtyStr,
                                        onValueChange = { qtyStr = it.replace(',', '.') },
                                        label = { Text("Jumlah") },
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(14.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = primaryColor,
                                            focusedLabelColor = primaryColor,
                                            cursorColor = primaryColor
                                        ),
                                        singleLine = true
                                    )

                                    // Stepper Qty Button [+]
                                    FilledIconButton(
                                        onClick = {
                                            val next = parsedQty + 1
                                            qtyStr = if (next % 1.0 == 0.0) next.toInt().toString() else String.format(Locale.US, "%.1f", next)
                                        },
                                        modifier = Modifier.size(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = surfaceVariant.copy(alpha = 0.7f),
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Tambah")
                                    }

                                    OutlinedTextField(
                                        value = unitStr,
                                        onValueChange = { unitStr = it },
                                        label = { Text("Satuan") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = primaryColor,
                                            focusedLabelColor = primaryColor,
                                            cursorColor = primaryColor
                                        ),
                                        placeholder = { Text("kg") },
                                        singleLine = true
                                    )
                                }

                                // Quick Qty Add Presets
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(1, 2, 5, 10, 25, 50).forEach { preset ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = surfaceVariant.copy(alpha = 0.45f),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    val current = qtyStr.toDoubleOrNull() ?: 0.0
                                                    val next = current + preset
                                                    qtyStr = if (next % 1.0 == 0.0) next.toInt().toString() else String.format(Locale.US, "%.1f", next)
                                                }
                                        ) {
                                            Text(
                                                text = "+$preset",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Estimasi Harga Beli Satuan
                        Card(
                            colors = CardDefaults.cardColors(containerColor = surfaceColor),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Estimasi Harga Beli (Rp / $unitStr)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (selectedProductObj!!.price > 0) {
                                        TextButton(
                                            onClick = {
                                                priceStr = if (selectedProductObj!!.price % 1.0 == 0.0) selectedProductObj!!.price.toLong().toString() else selectedProductObj!!.price.toString()
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Gunakan Master", fontSize = 11.sp, color = primaryColor)
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = priceStr,
                                    onValueChange = { priceStr = it },
                                    label = { Text("Harga Beli Satuan") },
                                    placeholder = { Text("0") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryColor,
                                        focusedLabelColor = primaryColor,
                                        cursorColor = primaryColor
                                    ),
                                    leadingIcon = {
                                        Text(
                                            "Rp",
                                            modifier = Modifier.padding(start = 12.dp),
                                            fontWeight = FontWeight.Black,
                                            color = primaryColor
                                        )
                                    },
                                    singleLine = true
                                )
                            }
                        }

                        // Live Subtotal Card
                        Surface(
                            color = primaryColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Subtotal Estimasi",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "$qtyStr $unitStr × ${formatter.format(parsedPrice).replace("Rp", "Rp ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = formatter.format(liveSubtotal).replace("Rp", "Rp "),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = primaryColor
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedProductObj != null) {
                        val qty = qtyStr.toDoubleOrNull() ?: 1.0
                        val price = priceStr.toDoubleOrNull() ?: 0.0
                        onConfirm(listOf(ProcurementItemEntity(
                            id = initialItem?.id ?: 0,
                            planId = initialItem?.planId ?: 0,
                            productName = selectedProductObj!!.name,
                            targetQuantity = qty,
                            estimatedUnitPrice = price,
                            unit = unitStr,
                            isBought = initialItem?.isBought ?: false
                        )))
                    } else if (selectedProducts.isNotEmpty()) {
                        val items = selectedProducts.map { p ->
                            ProcurementItemEntity(
                                id = 0,
                                planId = 0,
                                productName = p.name,
                                targetQuantity = 1.0,
                                estimatedUnitPrice = p.price,
                                unit = p.unit,
                                isBought = false
                            )
                        }
                        onConfirm(items)
                    }
                },
                enabled = selectedProductObj != null || selectedProducts.isNotEmpty(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) { 
                Text(
                    text = when {
                        initialItem != null -> "Perbarui Item"
                        selectedProductObj != null -> "Tambah ke Rencana"
                        else -> "Tambah ${selectedProducts.size} Item"
                    },
                    fontWeight = FontWeight.Bold, 
                    color = Color.White,
                    fontSize = 15.sp
                ) 
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold) 
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailDialog(
    plan: ProcurementPlanWithItems,
    viewModel: TobaccoViewModel,
    onDismiss: () -> Unit,
    onToggle: (ProcurementItemEntity) -> Unit,
    onRemoveItem: (ProcurementItemEntity) -> Unit,
    onShare: (ProcurementPlanWithItems, ProcurementExportType, String) -> Unit,
    onShareImage: (ProcurementPlanWithItems) -> Unit
) {
    val totalEstimasiRealisasi = plan.items
        .filter { it.isBought }
        .sumOf { it.targetQuantity * it.estimatedUnitPrice }
    val totalEstimasiAnggaran = plan.items
        .sumOf { it.targetQuantity * it.estimatedUnitPrice }

    val formatter = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
            maximumFractionDigits = 0
        }
    }
    val dateStr = remember(plan.plan.date) {
        SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.forLanguageTag("id-ID")).format(Date(plan.plan.date))
    }
    
    var showShareDialog by remember { mutableStateOf(false) }

    val itemsBought = plan.items.count { it.isBought }
    val totalItems = plan.items.size
    val progress = if (totalItems > 0) itemsBought.toFloat() / totalItems else 0f
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val purchaseStatus = when {
        itemsBought == 0 -> "Belum Belanja"
        itemsBought == totalItems -> "Selesai Dibeli Semua"
        else -> "Sebagian Terpenuhi"
    }

    // Urutkan item berdasarkan nama A-Z untuk checklist
    val sortedPlanItems = remember(plan.items) {
        plan.items.sortedBy { it.productName.lowercase(Locale.ROOT) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Detail Rencana Kulakan",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 18.sp.scaled
                            )
                            Text(
                                text = plan.plan.supplierName.ifBlank { "Semua Supplier" },
                                style = MaterialTheme.typography.labelSmall,
                                color = primaryColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onDismiss,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.padding(start = 12.dp).size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    actions = {
                        FilledTonalButton(
                            onClick = { showShareDialog = true },
                            modifier = Modifier.padding(end = 12.dp).height(38.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = primaryColor.copy(alpha = 0.15f),
                                contentColor = primaryColor
                            )
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Bagikan", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Bagikan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
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
                                text = "Total Anggaran",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatter.format(totalEstimasiAnggaran).replace("Rp", "Rp "),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = primaryColor,
                                fontSize = 20.sp.scaled
                            )
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .height(50.dp)
                                .weight(0.9f),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Text("Tutup", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(modifier = Modifier.height(2.dp))

                // --- PROGRESS & OVERVIEW CARD ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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
                                        .background(
                                            if (itemsBought == totalItems && totalItems > 0) primaryColor else Color(0xFFE65100),
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = purchaseStatus.uppercase(Locale.ROOT),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (itemsBought == totalItems && totalItems > 0) primaryColor else Color(0xFFE65100),
                                    letterSpacing = 1.sp
                                )
                            }

                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Progress Indicator
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Progres Belanja ($itemsBought / $totalItems Barang)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = primaryColor,
                                trackColor = primaryColor.copy(alpha = 0.15f)
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 0.8.dp
                        )

                        // Realisasi Belanja Stat Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Sudah Dicentang",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatter.format(totalEstimasiRealisasi).replace("Rp", "Rp "),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = primaryColor
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Sisa Anggaran",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val sisa = totalEstimasiAnggaran - totalEstimasiRealisasi
                                Text(
                                    text = formatter.format(sisa).replace("Rp", "Rp "),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = if (sisa > 0) MaterialTheme.colorScheme.onSurface else primaryColor
                                )
                            }
                        }
                    }
                }

                // Header Checklist
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Checklist Belanja",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Ketuk kotak untuk centang",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Item Checklist Cards
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(sortedPlanItems, key = { it.id }) { item ->
                        val isChecked = item.isBought
                        val subtotal = item.targetQuantity * item.estimatedUnitPrice

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onToggle(item) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChecked) primaryColor.copy(alpha = 0.07f) else surfaceColor
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isChecked) primaryColor.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isChecked) 0.dp else 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { onToggle(item) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = primaryColor,
                                        uncheckedColor = MaterialTheme.colorScheme.outline
                                    )
                                )

                                Spacer(Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.productName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                        color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${if (item.targetQuantity % 1.0 == 0.0) item.targetQuantity.toInt() else item.targetQuantity} ${item.unit} × ${formatter.format(item.estimatedUnitPrice).replace("Rp", "Rp ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formatter.format(subtotal).replace("Rp", "Rp "),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        color = if (isChecked) primaryColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isChecked) {
                                        Text(
                                            text = "Sudah Dibeli",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = primaryColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showShareDialog) {
        SharePreviewDialog(
            plan = plan,
            onDismiss = { showShareDialog = false },
            onShareText = { exportType, note ->
                showShareDialog = false
                onShare(plan, exportType, note)
            },
            onShareImage = {
                showShareDialog = false
                onShareImage(plan)
            }
        )
    }
}

@Composable
fun SharePreviewDialog(
    plan: ProcurementPlanWithItems,
    onDismiss: () -> Unit,
    onShareText: (ProcurementExportType, String) -> Unit,
    onShareImage: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var shareNote by remember { mutableStateOf("") }
    
    val exportType = when (selectedTab) {
        0 -> ProcurementExportType.CHECKLIST
        1 -> ProcurementExportType.SUPPLIER_ORDER
        2 -> ProcurementExportType.DETAIL
        else -> ProcurementExportType.PLAIN
    }
    
    val previewText = remember(plan, exportType, shareNote) {
        generateShareText(plan, exportType, shareNote)
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        ),
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .navigationBarsPadding()
            .imePadding(),
        containerColor = surfaceColor,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Bagikan Rencana Kulakan",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Produk otomatis diurutkan A-Z secara alfabetis",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Clear, contentDescription = "Tutup")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tab Selection Format
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    listOf(
                        "Checklist",
                        "Order Supplier",
                        "Rincian Lengkap",
                        "Teks Polos"
                    ).forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            },
                            selectedContentColor = primaryColor,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Gambar Struk Button Option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShareImage() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(primaryColor.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Image, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Bagikan Gambar Struk",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Ekspor format visual struk belanja HD",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = primaryColor)
                    }
                }

                // Live Preview Bubble (WhatsApp Look-and-Feel)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PREVIEW TEKS PESAN",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryColor,
                            letterSpacing = 1.sp
                        )

                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Rencana Kulakan", previewText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Teks berhasil disalin ke clipboard", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp), tint = primaryColor)
                            Spacer(Modifier.width(4.dp))
                            Text("Salin", fontSize = 12.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 220.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = previewText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Note Field with Fixed Visible Height
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Catatan Tambahan (Opsional)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = shareNote,
                        onValueChange = { shareNote = it },
                        placeholder = { Text("Contoh: Tolong siapkan sebelum jam 12...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            focusedLabelColor = primaryColor,
                            cursorColor = primaryColor
                        ),
                        leadingIcon = {
                            Icon(Icons.Default.Notes, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                        },
                        minLines = 3,
                        maxLines = 5,
                        singleLine = false
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onShareText(exportType, shareNote) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Bagikan Sekarang",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
fun ScrollableTabRow(
    selectedTabIndex: Int,
    edgePadding: androidx.compose.ui.unit.Dp = 0.dp,
    containerColor: Color,
    shape: androidx.compose.ui.graphics.Shape,
    tabs: @Composable () -> Unit
) {
    Surface(
        color = containerColor,
        shape = shape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs()
        }
    }
}

fun String.underline(): String {
    return this.map { "$it\u0332" }.joinToString("")
}

fun generateShareText(
    planWithItems: ProcurementPlanWithItems,
    exportType: ProcurementExportType = ProcurementExportType.CHECKLIST,
    note: String = ""
): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
        maximumFractionDigits = 0
    }
    val supplier = planWithItems.plan.supplierName.ifBlank { "Tanpa Supplier" }
    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(planWithItems.plan.date))

    // Otomatis urutkan item berdasarkan nama alfabetis A-Z
    val sortedItems = planWithItems.items.sortedBy { it.productName.lowercase(Locale.ROOT) }

    return buildString {
        append("🛒 *RENCANA KULAKAN*\n")
        append("🏪 $supplier\n")
        append("📅 $dateStr\n\n")

        var grandTotal = 0.0

        sortedItems.forEachIndexed { index, item ->
            val subTotal = item.targetQuantity * item.estimatedUnitPrice
            grandTotal += subTotal

            val pos = (index + 1).toString().padStart(2, '0')
            val qtyStr = if (item.targetQuantity % 1.0 == 0.0) item.targetQuantity.toInt().toString() else item.targetQuantity.toString()

            when (exportType) {
                ProcurementExportType.CHECKLIST -> {
                    val status = if (item.isBought) "☑️" else "☐"
                    append("$pos. $status *${item.productName}*\n")
                    append("    Qty: $qtyStr ${item.unit}\n")
                }
                ProcurementExportType.DETAIL -> {
                    val status = if (item.isBought) "☑️" else "☐"
                    append("$pos. $status *${item.productName}*\n")
                    val unitPriceStr = formatter.format(item.estimatedUnitPrice).replace("Rp", "Rp ")
                    append("    $qtyStr ${item.unit} × $unitPriceStr")
                    if (item.targetQuantity > 1.0) {
                        append(" = ${formatter.format(subTotal).replace("Rp", "Rp ")}")
                    }
                    append("\n")
                }
                ProcurementExportType.SUPPLIER_ORDER -> {
                    append("• ${item.productName}: $qtyStr ${item.unit}\n")
                }
                ProcurementExportType.PLAIN -> {
                    append("${index + 1}. ${item.productName} ($qtyStr ${item.unit})\n")
                }
            }
            if (exportType != ProcurementExportType.SUPPLIER_ORDER && exportType != ProcurementExportType.PLAIN) {
                append("\n")
            }
        }

        if (exportType == ProcurementExportType.DETAIL || exportType == ProcurementExportType.CHECKLIST) {
            append("━━━━━━━━━━━━━━\n")
            append("📦 Total Item: ${sortedItems.size}\n")
            if (exportType == ProcurementExportType.DETAIL) {
                append("💰 *Total Estimasi*\n")
                append("*${formatter.format(grandTotal).replace("Rp", "Rp ")}*\n")
            }
        }

        if (note.isNotBlank()) {
            append("\n*Note :*\n$note")
        }
    }
}
