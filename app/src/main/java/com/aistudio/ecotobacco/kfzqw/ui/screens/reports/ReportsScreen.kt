package com.aistudio.ecotobacco.kfzqw.ui.screens.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.ecotobacco.kfzqw.data.local.entities.TransactionEntity
import com.aistudio.ecotobacco.kfzqw.data.remote.SyncStatus
import com.aistudio.ecotobacco.kfzqw.viewmodel.TobaccoViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: TobaccoViewModel,
    onNavigateToTransaction: () -> Unit,
    onNavigateToSettings: () -> Unit,
    initialFilterBy: String = "Semua"
) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val customUnits by viewModel.customUnits.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    var showSyncDialog by remember { mutableStateOf(false) }

    var filterBy by remember(initialFilterBy) { mutableStateOf(initialFilterBy) }
    val filters = listOf("Semua", "Harian", "Mingguan", "Bulanan", "Tahunan")
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionOptionsTarget by remember { mutableStateOf<TransactionEntity?>(null) }
    var baseDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedSupplier by remember { mutableStateOf("Semua Supplier") }
    
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var showBulkSupplierDialog by remember { mutableStateOf(false) }
    var bulkSupplier by remember { mutableStateOf("") }
    var bulkUnit by remember { mutableStateOf("") }

    val filteredTransactions = remember(transactions, filterBy, baseDate, selectedSupplier) {
        val calendar = Calendar.getInstance().apply { timeInMillis = baseDate }
        val start: Long
        val end: Long
        when (filterBy) {
            "Harian" -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, 1); end = calendar.timeInMillis
            }
            "Mingguan" -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                start = calendar.timeInMillis
                calendar.add(Calendar.WEEK_OF_YEAR, 1); end = calendar.timeInMillis
            }
            "Bulanan" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1); end = calendar.timeInMillis
            }
            "Tahunan" -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                start = calendar.timeInMillis
                calendar.add(Calendar.YEAR, 1); end = calendar.timeInMillis
            }
            else -> { start = 0L; end = Long.MAX_VALUE }
        }
        transactions.filter { 
            it.date in start until end && 
            (selectedSupplier == "Semua Supplier" || it.supplier == selectedSupplier)
        }
    }

    val supplierList = remember(transactions) {
        listOf("Semua Supplier") + transactions.map { it.supplier }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val periodDisplay = remember(filterBy, baseDate) {
        val cal = Calendar.getInstance().apply { timeInMillis = baseDate }
        val today = Calendar.getInstance()

        // Gunakan Locale.getDefault() atau Locale.ROOT untuk format yang aman
        val locale = Locale.getDefault()

        when (filterBy) {
            "Harian" -> {
                val isSameDay = cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
                if (isSameDay) "Hari Ini"
                else SimpleDateFormat("dd MMM yyyy", locale).format(cal.time)
            }
            "Mingguan" -> {
                val weekStart = cal.clone() as Calendar
                weekStart.set(Calendar.DAY_OF_WEEK, weekStart.firstDayOfWeek)
                val weekEnd = weekStart.clone() as Calendar
                weekEnd.add(Calendar.DAY_OF_YEAR, 6)
                
                val df = SimpleDateFormat("dd MMM", locale)
                "${df.format(weekStart.time)} - ${df.format(weekEnd.time)}"
            }
            "Bulanan" -> SimpleDateFormat("MMMM yyyy", locale).format(cal.time)
            "Tahunan" -> cal.get(Calendar.YEAR).toString()
            else -> "Semua Waktu"
        }
    }

    val totalSpent = remember(filteredTransactions) { filteredTransactions.sumOf { it.total } }
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
            maximumFractionDigits = 0
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) Text("${selectedIds.size} dipilih", fontWeight = FontWeight.Bold)
                    else Text("Rekap Kulakan", fontWeight = FontWeight.ExtraBold)
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { isSelectionMode = false; selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Batal")
                        }
                    } else {
                        IconButton(onClick = onNavigateToTransaction) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            val selected = transactions.filter { selectedIds.contains(it.id) }
                            bulkSupplier = selected.map { it.supplier }.distinct().singleOrNull() ?: ""
                            bulkUnit = selected.map { it.unit }.distinct().singleOrNull() ?: ""
                            showBulkSupplierDialog = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit transaksi", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            viewModel.deleteTransactions(selectedIds.toList())
                            isSelectionMode = false
                            selectedIds = emptySet()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = { viewModel.exportTransactionsToCsv(context, filteredTransactions) }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { showSyncDialog = true }) {
                            Icon(
                                imageVector = when (syncStatus) {
                                    SyncStatus.SYNCED -> Icons.Default.CloudDone
                                    SyncStatus.SYNCING -> Icons.Default.CloudUpload
                                    else -> Icons.Default.CloudQueue
                                },
                                contentDescription = "Sinkronisasi",
                                tint = if (syncStatus == SyncStatus.SYNCED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Pengaturan")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    modifier = Modifier.navigationBarsPadding(),
                    onClick = onNavigateToTransaction,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Kulakan")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().imePadding()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column {
                            Text("TOTAL PENGELUARAN", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currencyFormatter.format(totalSpent).replace("Rp", "Rp "), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                        
                        // Supplier Filter Dropdown
                        var supplierExpanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { supplierExpanded = true }, contentPadding = PaddingValues(0.dp)) {
                                Icon(Icons.Default.FilterAlt, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(4.dp))
                                Text(if (selectedSupplier == "Semua Supplier") "Filter Supplier" else selectedSupplier, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            DropdownMenu(expanded = supplierExpanded, onDismissRequest = { supplierExpanded = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                                supplierList.forEach { s ->
                                    DropdownMenuItem(text = { Text(s) }, onClick = { selectedSupplier = s; supplierExpanded = false })
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (filterBy != "Semua") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = {
                                        val cal = Calendar.getInstance().apply { timeInMillis = baseDate }
                                        when (filterBy) {
                                            "Harian" -> cal.add(Calendar.DAY_OF_YEAR, -1)
                                            "Mingguan" -> cal.add(Calendar.WEEK_OF_YEAR, -1)
                                            "Bulanan" -> cal.add(Calendar.MONTH, -1)
                                            "Tahunan" -> cal.add(Calendar.YEAR, -1)
                                        }
                                        baseDate = cal.timeInMillis
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ChevronLeft, "Sebelumnya", tint = MaterialTheme.colorScheme.primary)
                                }

                                Text(
                                    text = periodDisplay,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                IconButton(
                                    onClick = {
                                        val cal = Calendar.getInstance().apply { timeInMillis = baseDate }
                                        when (filterBy) {
                                            "Harian" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                                            "Mingguan" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                                            "Bulanan" -> cal.add(Calendar.MONTH, 1)
                                            "Tahunan" -> cal.add(Calendar.YEAR, 1)
                                        }
                                        baseDate = cal.timeInMillis
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ChevronRight, "Selanjutnya", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        } else {
                            Text(
                                text = "Semua Waktu",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        var filterExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedCard(
                                onClick = { filterExpanded = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(filterBy, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(20.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = filterExpanded,
                                onDismissRequest = { filterExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                filters.forEach { f ->
                                    DropdownMenuItem(
                                        text = { Text(f) },
                                        onClick = {
                                            filterBy = f
                                            filterExpanded = false
                                            baseDate = System.currentTimeMillis()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredTransactions, key = { it.id }) { t ->
                    val isSelected = selectedIds.contains(t.id)
                    TransactionItemCard(
                        transaction = t,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        currencyFormatter = currencyFormatter,
                        onClick = { 
                            if (isSelectionMode) {
                                selectedIds = if (isSelected) selectedIds - t.id else selectedIds + t.id
                                if (selectedIds.isEmpty()) isSelectionMode = false
                            } else {
                                transactionOptionsTarget = t 
                            }
                        },
                        onLongClick = { 
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedIds = setOf(t.id)
                            }
                        }
                    )
                }
            }
        }
    }

    if (transactionToEdit != null) {
        EditTransactionDialog(
            transaction = transactionToEdit!!,
            viewModel = viewModel,
            onDismiss = { transactionToEdit = null },
            onConfirm = { date, name, qty, price, supplier, unit ->
                viewModel.updateTransaction(transactionToEdit!!.id, date, name, qty, price, supplier, unit)
                transactionToEdit = null
            }
        )
    }

    if (transactionOptionsTarget != null) {
        TransactionOptionsDialog(
            transaction = transactionOptionsTarget!!,
            onDismiss = { transactionOptionsTarget = null },
            onEdit = { transactionToEdit = transactionOptionsTarget; transactionOptionsTarget = null },
            onDelete = { viewModel.deleteTransaction(transactionOptionsTarget!!.id); transactionOptionsTarget = null }
        )
    }

    if (showSyncDialog) {
        SyncDialog(viewModel = viewModel, onDismiss = { showSyncDialog = false })
    }

    if (showBulkSupplierDialog) {
        AlertDialog(
            onDismissRequest = { showBulkSupplierDialog = false },
            title = {
                Text("Edit Transaksi Massal", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${selectedIds.size} transaksi dipilih")
                    val savedSuppliers = transactions
                        .map { it.supplier.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()
                    val supplierMatches = savedSuppliers.filter {
                        bulkSupplier.isNotBlank() && it.contains(bulkSupplier.trim(), ignoreCase = true)
                    }.take(5)

                    OutlinedTextField(
                        value = bulkSupplier,
                        onValueChange = { bulkSupplier = it },
                        label = { Text("Supplier") },
                        placeholder = { Text("Ketik atau pilih supplier tersimpan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    if (supplierMatches.isNotEmpty()) {
                        Text(
                            "Supplier tersimpan:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        supplierMatches.forEach { supplier ->
                            TextButton(
                                onClick = { bulkSupplier = supplier },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    supplier,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    val savedUnits = (transactions.map { it.unit } + customUnits)
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()
                    val unitMatches = savedUnits.filter {
                        bulkUnit.isNotBlank() && it.contains(bulkUnit.trim(), ignoreCase = true)
                    }.take(5)

                    OutlinedTextField(
                        value = bulkUnit,
                        onValueChange = { bulkUnit = it },
                        label = { Text("Satuan") },
                        placeholder = { Text("Ketik atau pilih satuan tersimpan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    if (unitMatches.isNotEmpty()) {
                        Text(
                            "Satuan tersimpan:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        unitMatches.forEach { unit ->
                            TextButton(
                                onClick = { bulkUnit = unit },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    unit,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    transactions
                        .filter { selectedIds.contains(it.id) }
                        .forEach { transaction ->
                            viewModel.updateTransaction(
                                transaction.id,
                                transaction.date,
                                transaction.productName,
                                transaction.quantity,
                                transaction.unitPrice,
                                bulkSupplier.trim(),
                                bulkUnit.trim().ifBlank { transaction.unit }
                            )
                        }
                    showBulkSupplierDialog = false
                    isSelectionMode = false
                    selectedIds = emptySet()
                }) {
                    Text("Simpan", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkSupplierDialog = false }) {
                    Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
