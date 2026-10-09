package com.aistudio.ecotobacco.kfzqw.ui.screens.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProductEntity
import com.aistudio.ecotobacco.kfzqw.data.utils.LocalScanner
import kotlinx.coroutines.launch

private data class ScannedItemState(
    val id: Int,
    val item: LocalScanner.ScannedItem,
    val isSelected: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultDialog(
    scannedResult: LocalScanner.ScannedResult,
    productMasterList: List<ProductEntity> = emptyList(),
    showDebugOption: Boolean = false,
    onShowDebug: () -> Unit = {},
    onConfirm: (List<LocalScanner.ScannedItem>, String) -> Unit,
    onDismiss: () -> Unit
) {
    var editableShopName by remember { mutableStateOf(scannedResult.shopName ?: "") }

    var itemStates by remember {
        mutableStateOf(scannedResult.items.mapIndexed { index, item ->
            ScannedItemState(id = index, item = item, isSelected = true)
        })
    }
    var nextId by remember { mutableIntStateOf(scannedResult.items.size) }
    var editingId by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val emeraldGreen = Color(0xFF10B981)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(12.dp)
                .imePadding(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .fillMaxSize()
            ) {
                // Header with Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Hasil Ekstraksi Nota",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "${itemStates.size} Barang terdeteksi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Confidence Pill
                    if (scannedResult.overallConfidence > 0) {
                        Surface(
                            color = emeraldGreen.copy(alpha = 0.15f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, emeraldGreen.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AutoAwesome, null, tint = emeraldGreen, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "${scannedResult.overallConfidence}% Akurat",
                                    color = emeraldGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Editable Shop Info Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp).padding(start = 4.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = editableShopName,
                                onValueChange = { editableShopName = it },
                                label = { Text("Nama Supplier / Toko") },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                ),
                                singleLine = true
                            )
                            if (!scannedResult.shopAddress.isNullOrBlank()) {
                                Text(
                                    text = scannedResult.shopAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }

                // Sub-header & Add Item CTA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Daftar Barang Nota:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedButton(
                        onClick = {
                            val id = nextId
                            val newItem = ScannedItemState(
                                id = id,
                                item = LocalScanner.ScannedItem(name = "", price = 0.0, quantity = 1.0),
                                isSelected = true
                            )
                            itemStates = itemStates + newItem
                            editingId = id
                            nextId++

                            scope.launch {
                                listState.animateScrollToItem(itemStates.size)
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tambah Item", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Item List
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(itemStates, key = { it.id }) { state ->
                        val index = itemStates.indexOf(state)
                        val item = state.item
                        val isSelected = state.isSelected
                        val isEditing = editingId == state.id

                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                    val newList = itemStates.toMutableList()
                                    if (index != -1) {
                                        newList.removeAt(index)
                                        itemStates = newList
                                        true
                                    } else false
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
                                    Modifier
                                        .fillMaxSize()
                                        .background(color, RoundedCornerShape(16.dp))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            },
                            content = {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected)
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                                        else
                                            MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isEditing) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            var searchQuery by remember { mutableStateOf(item.name) }
                                            val suggestions = remember(searchQuery) {
                                                if (searchQuery.length >= 2) {
                                                    productMasterList.filter {
                                                        it.name.contains(searchQuery, ignoreCase = true)
                                                    }.take(5)
                                                } else emptyList()
                                            }

                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                OutlinedTextField(
                                                    value = searchQuery,
                                                    onValueChange = { newName ->
                                                        searchQuery = newName
                                                        val newList = itemStates.toMutableList()
                                                        newList[index] = state.copy(item = item.copy(name = newName))
                                                        itemStates = newList
                                                    },
                                                    label = { Text("Nama Barang") },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(12.dp),
                                                    trailingIcon = { Icon(Icons.Default.Search, null) }
                                                )

                                                AnimatedVisibility(visible = suggestions.isNotEmpty()) {
                                                    Surface(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .heightIn(max = 180.dp),
                                                        shape = RoundedCornerShape(12.dp),
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                                                        tonalElevation = 4.dp
                                                    ) {
                                                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                                            suggestions.forEach { product ->
                                                                ListItem(
                                                                    modifier = Modifier.clickable {
                                                                        searchQuery = product.name
                                                                        val newList = itemStates.toMutableList()
                                                                        newList[index] = state.copy(
                                                                            item = item.copy(
                                                                                name = product.name,
                                                                                price = product.price
                                                                            )
                                                                        )
                                                                        itemStates = newList
                                                                    },
                                                                    headlineContent = { Text(product.name, fontWeight = FontWeight.Bold) },
                                                                    trailingContent = {
                                                                        Text(
                                                                            "Rp %,.0f".format(product.price),
                                                                            color = MaterialTheme.colorScheme.primary,
                                                                            fontWeight = FontWeight.Bold
                                                                        )
                                                                    },
                                                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                var qtyText by remember {
                                                    mutableStateOf(if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString().replace(".", ","))
                                                }

                                                OutlinedTextField(
                                                    value = qtyText,
                                                    onValueChange = { newQty ->
                                                        qtyText = newQty
                                                        val sanitizedQty = newQty.replace(",", ".")
                                                        val doubleVal = sanitizedQty.toDoubleOrNull() ?: 0.0
                                                        val newList = itemStates.toMutableList()
                                                        newList[index] = state.copy(item = item.copy(quantity = doubleVal))
                                                        itemStates = newList
                                                    },
                                                    label = { Text("Qty") },
                                                    modifier = Modifier.weight(1f),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                OutlinedTextField(
                                                    value = if (item.price % 1.0 == 0.0) item.price.toInt().toString() else item.price.toString(),
                                                    onValueChange = { newPrice ->
                                                        val newList = itemStates.toMutableList()
                                                        newList[index] = state.copy(item = item.copy(price = newPrice.toDoubleOrNull() ?: 0.0))
                                                        itemStates = newList
                                                    },
                                                    label = { Text("Harga Satuan") },
                                                    modifier = Modifier.weight(1.5f),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                            }
                                            Button(
                                                onClick = { editingId = -1 },
                                                modifier = Modifier.align(Alignment.End),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Selesai")
                                            }
                                        }
                                    } else {
                                        ListItem(
                                            modifier = Modifier.clickable {
                                                val newList = itemStates.toMutableList()
                                                newList[index] = state.copy(isSelected = !isSelected)
                                                itemStates = newList
                                            },
                                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                            leadingContent = {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = { checked ->
                                                        val newList = itemStates.toMutableList()
                                                        newList[index] = state.copy(isSelected = checked)
                                                        itemStates = newList
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                                )
                                            },
                                            headlineContent = {
                                                Text(
                                                    item.name.ifBlank { "(Barang Tanpa Nama)" },
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (item.name.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                                )
                                            },
                                            supportingContent = {
                                                Text(
                                                    "${item.quantity} x Rp %,.0f".format(item.price),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            trailingContent = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        "Rp %,.0f".format(item.quantity * item.price),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                    IconButton(onClick = { editingId = state.id }) {
                                                        Icon(
                                                            Icons.Default.Edit,
                                                            contentDescription = "Edit",
                                                            modifier = Modifier.size(18.dp),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                // Total Summary & Action Buttons Footer
                val totalSelected = itemStates.filter { it.isSelected }.sumOf { it.item.quantity * it.item.price }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    if (showDebugOption) {
                        OutlinedButton(
                            onClick = onShowDebug,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Lihat Log Debug Scan")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Total (${itemStates.count { it.isSelected }} Terpilih)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Rp %,.0f".format(totalSelected),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = onDismiss) {
                                Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val selected = itemStates.filter { it.isSelected }.map { it.item }
                                    onConfirm(selected, editableShopName)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = emeraldGreen),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Simpan Transaksi", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
