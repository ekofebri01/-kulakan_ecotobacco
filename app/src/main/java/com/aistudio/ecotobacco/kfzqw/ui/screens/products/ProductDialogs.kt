package com.aistudio.ecotobacco.kfzqw.ui.screens.products

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProductEntity
import com.aistudio.ecotobacco.kfzqw.ui.components.UnitManagementDialog
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(
    unitOptions: List<String>,
    onAddCustomUnit: (String) -> Unit,
    onUpdateCustomUnit: (String, String) -> Unit,
    onDeleteCustomUnit: (String) -> Unit,
    onDismiss: () -> Unit, 
    onAdd: (String, Double, String, Double, String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var sellingPriceStr by remember { mutableStateOf("") }
    var contentQtyStr by remember { mutableStateOf("1") }

    var selectedUnit by remember { mutableStateOf("kg") }
    var selectedSellingUnit by remember { mutableStateOf("pcs") }
    var customUnit by remember { mutableStateOf("") }
    var customSellingUnit by remember { mutableStateOf("") }
    var unitExpanded by remember { mutableStateOf(false) }
    var sellingUnitExpanded by remember { mutableStateOf(false) }
    var showUnitManagement by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false),
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars).imePadding(),
        title = { Text("Tambah Barang Baru", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                // Sisakan ruang agar tombol Kelola Satuan tidak tertutup tombol aksi dialog.
                modifier = Modifier
                    .padding(bottom = 88.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Harga Kulakan") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )

                    OutlinedTextField(
                        value = sellingPriceStr,
                        onValueChange = { sellingPriceStr = it },
                        label = { Text("Harga Jual") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )
                }

                OutlinedTextField(
                    value = contentQtyStr,
                    onValueChange = { contentQtyStr = it },
                    label = { Text("Isi per Satuan Beli (Contoh: 10)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    supportingText = { Text("Misal: Beli 1 Pak isi 10 Pcs, maka isi 10") }
                )

                // Profit Estimation
                val cost = priceStr.toDoubleOrNull() ?: 0.0
                val sell = sellingPriceStr.toDoubleOrNull() ?: 0.0
                val contentQty = contentQtyStr.toDoubleOrNull() ?: 1.0
                
                val costPerSellingUnit = if (contentQty > 0) cost / contentQty else 0.0
                val marginPerSellingUnit = sell - costPerSellingUnit
                val profitPercent = if (costPerSellingUnit > 0) (marginPerSellingUnit / costPerSellingUnit) * 100 else 0.0
                
                val currencyFormatter = remember {
                    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
                        maximumFractionDigits = 0
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (marginPerSellingUnit >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Untung per $selectedSellingUnit",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (marginPerSellingUnit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                        Text(
                            text = currencyFormatter.format(marginPerSellingUnit).replace("Rp", "Rp "),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (marginPerSellingUnit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                        if (contentQty > 1) {
                            Text(
                                text = "Modal: ${currencyFormatter.format(costPerSellingUnit).replace("Rp", "Rp ")} / $selectedSellingUnit",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                    Text(
                        text = String.format(Locale.US, "%.1f%%", profitPercent),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = if (marginPerSellingUnit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }

                // Purchase Unit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = !unitExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedUnit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Satuan Beli") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            unitOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedUnit = option
                                        unitExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Custom", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    selectedUnit = "Custom"
                                    unitExpanded = false
                                }
                            )
                        }
                    }

                    if (selectedUnit == "Custom") {
                        OutlinedTextField(
                            value = customUnit,
                            onValueChange = { customUnit = it },
                            label = { Text("Unit Baru") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // Selling Unit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = sellingUnitExpanded,
                        onExpandedChange = { sellingUnitExpanded = !sellingUnitExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedSellingUnit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Satuan Jual") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = sellingUnitExpanded)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = sellingUnitExpanded,
                            onDismissRequest = { sellingUnitExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            unitOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedSellingUnit = option
                                        sellingUnitExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Custom", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    selectedSellingUnit = "Custom"
                                    sellingUnitExpanded = false
                                }
                            )
                        }
                    }

                    if (selectedSellingUnit == "Custom") {
                        OutlinedTextField(
                            value = customSellingUnit,
                            onValueChange = { customSellingUnit = it },
                            label = { Text("Unit Baru") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                TextButton(
                    onClick = { showUnitManagement = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Kelola Satuan", style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    val sellingPrice = sellingPriceStr.toDoubleOrNull() ?: 0.0
                    val contentQty = contentQtyStr.toDoubleOrNull() ?: 1.0
                    if (name.isNotBlank()) {
                        val finalUnit = if (selectedUnit == "Custom") {
                            val trimmed = customUnit.trim()
                            if (trimmed.isNotBlank()) {
                                onAddCustomUnit(trimmed)
                                trimmed
                            } else "unit"
                        } else selectedUnit

                        val finalSellingUnit = if (selectedSellingUnit == "Custom") {
                            val trimmed = customSellingUnit.trim()
                            if (trimmed.isNotBlank()) {
                                onAddCustomUnit(trimmed)
                                trimmed
                            } else "unit"
                        } else selectedSellingUnit

                        onAdd(name, price, finalUnit, sellingPrice, finalSellingUnit, contentQty)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Simpan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )

    if (showUnitManagement) {
        UnitManagementDialog(
            units = unitOptions,
            onAdd = onAddCustomUnit,
            onUpdate = onUpdateCustomUnit,
            onDelete = onDeleteCustomUnit,
            onDismiss = { showUnitManagement = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductDialog(
    product: ProductEntity,
    unitOptions: List<String>,
    onAddCustomUnit: (String) -> Unit,
    onUpdateCustomUnit: (String, String) -> Unit,
    onDeleteCustomUnit: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, Double, String, Double) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(product.name) }
    var priceStr by remember { mutableStateOf(product.price.toString().replace(".0", "")) }
    var sellingPriceStr by remember { mutableStateOf(product.sellingPrice.toString().replace(".0", "")) }
    var contentQtyStr by remember { mutableStateOf(product.contentQuantity.toString().replace(".0", "")) }

    var selectedUnit by remember {
        mutableStateOf(if (product.unit in unitOptions) product.unit else "Custom")
    }
    var selectedSellingUnit by remember {
        mutableStateOf(if (product.sellingUnit in unitOptions) product.sellingUnit else "Custom")
    }
    var customUnit by remember {
        mutableStateOf(if (selectedUnit == "Custom") product.unit else "")
    }
    var customSellingUnit by remember {
        mutableStateOf(if (selectedSellingUnit == "Custom") product.sellingUnit else "")
    }
    var unitExpanded by remember { mutableStateOf(false) }
    var sellingUnitExpanded by remember { mutableStateOf(false) }
    var showUnitManagement by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false),
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars).imePadding(),
        title = { Text("Edit Produk", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                // Sisakan ruang agar tombol Kelola Satuan tidak tertutup tombol aksi dialog.
                modifier = Modifier
                    .padding(bottom = 88.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Harga Kulakan") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )

                    OutlinedTextField(
                        value = sellingPriceStr,
                        onValueChange = { sellingPriceStr = it },
                        label = { Text("Harga Jual") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )
                }

                OutlinedTextField(
                    value = contentQtyStr,
                    onValueChange = { contentQtyStr = it },
                    label = { Text("Isi per Satuan Beli (Contoh: 10)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    supportingText = { Text("Misal: Beli 1 Pak isi 10 Pcs, maka isi 10") }
                )

                // Profit Estimation
                val cost = priceStr.toDoubleOrNull() ?: 0.0
                val sell = sellingPriceStr.toDoubleOrNull() ?: 0.0
                val contentQty = contentQtyStr.toDoubleOrNull() ?: 1.0
                
                val costPerSellingUnit = if (contentQty > 0) cost / contentQty else 0.0
                val marginPerSellingUnit = sell - costPerSellingUnit
                val profitPercent = if (costPerSellingUnit > 0) (marginPerSellingUnit / costPerSellingUnit) * 100 else 0.0
                
                val currencyFormatter = remember {
                    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
                        maximumFractionDigits = 0
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (marginPerSellingUnit >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Untung per $selectedSellingUnit",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (marginPerSellingUnit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                        Text(
                            text = currencyFormatter.format(marginPerSellingUnit).replace("Rp", "Rp "),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (marginPerSellingUnit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                        if (contentQty > 1) {
                            Text(
                                text = "Modal: ${currencyFormatter.format(costPerSellingUnit).replace("Rp", "Rp ")} / $selectedSellingUnit",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                    Text(
                        text = String.format(Locale.US, "%.1f%%", profitPercent),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = if (marginPerSellingUnit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }

                // Purchase Unit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = !unitExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedUnit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Satuan Beli") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            unitOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedUnit = option
                                        unitExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Custom", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    selectedUnit = "Custom"
                                    unitExpanded = false
                                }
                            )
                        }
                    }

                    if (selectedUnit == "Custom") {
                        OutlinedTextField(
                            value = customUnit,
                            onValueChange = { customUnit = it },
                            label = { Text("Unit Baru") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // Selling Unit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = sellingUnitExpanded,
                        onExpandedChange = { sellingUnitExpanded = !sellingUnitExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedSellingUnit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Satuan Jual") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = sellingUnitExpanded)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = sellingUnitExpanded,
                            onDismissRequest = { sellingUnitExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            unitOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedSellingUnit = option
                                        sellingUnitExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Custom", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    selectedSellingUnit = "Custom"
                                    sellingUnitExpanded = false
                                }
                            )
                        }
                    }

                    if (selectedSellingUnit == "Custom") {
                        OutlinedTextField(
                            value = customSellingUnit,
                            onValueChange = { customSellingUnit = it },
                            label = { Text("Unit Baru") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                TextButton(
                    onClick = { showUnitManagement = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Kelola Satuan", style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDelete != null) {
                    TextButton(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text("Hapus", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Spacer(Modifier.weight(0.8f))
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(0.8f)
                ) {
                    Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = {
                        val price = priceStr.toDoubleOrNull() ?: 0.0
                        val sellingPrice = sellingPriceStr.toDoubleOrNull() ?: 0.0
                        val contentQty = contentQtyStr.toDoubleOrNull() ?: 1.0
                        if (name.isNotBlank()) {
                            val finalUnit = if (selectedUnit == "Custom") {
                                val trimmed = customUnit.trim()
                                if (trimmed.isNotBlank()) {
                                    onAddCustomUnit(trimmed)
                                    trimmed
                                } else "unit"
                            } else selectedUnit

                            val finalSellingUnit = if (selectedSellingUnit == "Custom") {
                                val trimmed = customSellingUnit.trim()
                                if (trimmed.isNotBlank()) {
                                    onAddCustomUnit(trimmed)
                                    trimmed
                                } else "unit"
                            } else selectedSellingUnit

                            onConfirm(name, price, finalUnit, sellingPrice, finalSellingUnit, contentQty)
                        }
                    },
                    modifier = Modifier.height(44.dp).weight(1.7f),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Simpan", fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        },
        dismissButton = {}
    )

    if (showDeleteConfirmation && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Konfirmasi Hapus") },
            text = { Text("Apakah Anda yakin ingin menghapus produk '$name'? Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus Sekarang")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showUnitManagement) {
        UnitManagementDialog(
            units = unitOptions,
            onAdd = onAddCustomUnit,
            onUpdate = onUpdateCustomUnit,
            onDelete = onDeleteCustomUnit,
            onDismiss = { showUnitManagement = false }
        )
    }
}
