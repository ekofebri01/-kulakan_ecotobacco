package com.aistudio.ecotobacco.kfzqw.ui.screens.products

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProductEntity
import com.aistudio.ecotobacco.kfzqw.ui.components.UnitManagementDialog
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(
    unitOptions: List<String>,
    subCategoryOptions: List<String> = emptyList(),
    onAddCustomUnit: (String) -> Unit,
    onUpdateCustomUnit: (String, String) -> Unit,
    onDeleteCustomUnit: (String) -> Unit,
    onAddCustomSubCategory: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onAdd: (String, Double, String, Double, String, Double, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var sellingPriceStr by remember { mutableStateOf("") }
    var contentQtyStr by remember { mutableStateOf("1") }
    var subCategory by remember { mutableStateOf("Tembakau") }
    val category = ""

    var selectedUnit by remember { mutableStateOf("kg") }
    var selectedSellingUnit by remember { mutableStateOf("pcs") }
    var customUnit by remember { mutableStateOf("") }
    var customSellingUnit by remember { mutableStateOf("") }
    var unitExpanded by remember { mutableStateOf(false) }
    var sellingUnitExpanded by remember { mutableStateOf(false) }
    var showUnitManagement by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val isDarkSurface = MaterialTheme.colorScheme.background.luminance() < 0.45f
    val inputBg = if (isDarkSurface) Color(0xFF101513) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = primaryColor,
        unfocusedBorderColor = borderColor,
        focusedContainerColor = inputBg,
        unfocusedContainerColor = inputBg,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
    )

    val cost = priceStr.toDoubleOrNull() ?: 0.0
    val sell = sellingPriceStr.toDoubleOrNull() ?: 0.0
    val contentQty = contentQtyStr.toDoubleOrNull() ?: 1.0
    val costPerSellingUnit = if (contentQty > 0) cost / contentQty else 0.0
    val marginPerSellingUnit = sell - costPerSellingUnit
    val profitPercent = if (costPerSellingUnit > 0) (marginPerSellingUnit / costPerSellingUnit) * 100 else 0.0
    val sellingUnitDisplay = if (selectedSellingUnit == "Custom") customSellingUnit.ifBlank { "unit" } else selectedSellingUnit

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
            maximumFractionDigits = 0
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .windowInsetsPadding(WindowInsets.systemBars)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor),
            tonalElevation = 6.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tambah Produk Baru",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        onClick = { showUnitManagement = true },
                        shape = CircleShape,
                        color = primaryColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f)),
                        modifier = Modifier.testTag("add_product_manage_units_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Kelola Satuan",
                                tint = primaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Satuan",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        }
                    }
                }

                // Body (fits without scrolling in normal portrait; scroll fallback only for tiny screens / keyboard)
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Produk", maxLines = 1) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_product_name_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = fieldColors
                    )

                    OutlinedTextField(
                        value = subCategory,
                        onValueChange = { subCategory = it },
                        label = { Text("Sub Kategori", maxLines = 1) },
                        placeholder = { Text("Tembakau, Alat, Papir...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_product_subcategory_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = fieldColors
                    )

                    if (subCategoryOptions.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            subCategoryOptions.forEach { option ->
                                val isSelected = subCategory.equals(option, ignoreCase = true)
                                Surface(
                                    onClick = { subCategory = option },
                                    shape = CircleShape,
                                    color = if (isSelected) primaryColor.copy(alpha = 0.16f) else inputBg,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) primaryColor else borderColor
                                    )
                                ) {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Row: Harga Kulakan | Harga Jual
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = { priceStr = it },
                            label = { Text("Harga Kulakan", fontSize = 12.sp, maxLines = 1, softWrap = false) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_product_cost_input"),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = fieldColors
                        )

                        OutlinedTextField(
                            value = sellingPriceStr,
                            onValueChange = { sellingPriceStr = it },
                            label = { Text("Harga Jual", fontSize = 12.sp, maxLines = 1, softWrap = false) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_product_sell_input"),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = fieldColors
                        )
                    }

                    // Row: Satuan Beli | Satuan Jual | Isi per Beli
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                singleLine = true,
                                label = { Text("Sat. Beli", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(14.dp),
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded)
                                },
                                colors = fieldColors
                            )
                            ExposedDropdownMenu(
                                expanded = unitExpanded,
                                onDismissRequest = { unitExpanded = false },
                                modifier = Modifier.background(surfaceColor)
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
                                    text = { Text("Custom", color = primaryColor, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        selectedUnit = "Custom"
                                        unitExpanded = false
                                    }
                                )
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = sellingUnitExpanded,
                            onExpandedChange = { sellingUnitExpanded = !sellingUnitExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedSellingUnit,
                                onValueChange = {},
                                readOnly = true,
                                singleLine = true,
                                label = { Text("Sat. Jual", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(14.dp),
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = sellingUnitExpanded)
                                },
                                colors = fieldColors
                            )
                            ExposedDropdownMenu(
                                expanded = sellingUnitExpanded,
                                onDismissRequest = { sellingUnitExpanded = false },
                                modifier = Modifier.background(surfaceColor)
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
                                    text = { Text("Custom", color = primaryColor, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        selectedSellingUnit = "Custom"
                                        sellingUnitExpanded = false
                                    }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = contentQtyStr,
                            onValueChange = { contentQtyStr = it },
                            label = { Text("Isi / Beli", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                            modifier = Modifier
                                .weight(0.75f)
                                .testTag("add_product_content_qty_input"),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = fieldColors
                        )
                    }

                    if (selectedUnit == "Custom" || selectedSellingUnit == "Custom") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (selectedUnit == "Custom") {
                                OutlinedTextField(
                                    value = customUnit,
                                    onValueChange = { customUnit = it },
                                    label = { Text("Sat. Beli Baru", fontSize = 11.sp, maxLines = 1) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    colors = fieldColors
                                )
                            }
                            if (selectedSellingUnit == "Custom") {
                                OutlinedTextField(
                                    value = customSellingUnit,
                                    onValueChange = { customSellingUnit = it },
                                    label = { Text("Sat. Jual Baru", fontSize = 11.sp, maxLines = 1) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    colors = fieldColors
                                )
                            }
                        }
                    }

                    // Compact Live Profit Estimation Banner
                    val profitTone = if (marginPerSellingUnit >= 0) primaryColor else MaterialTheme.colorScheme.error
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = profitTone.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, profitTone.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ESTIMASI LABA PER $sellingUnitDisplay",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    color = profitTone
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = currencyFormatter.format(marginPerSellingUnit).replace("Rp", "Rp "),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = profitTone
                                    )
                                    if (contentQty > 1) {
                                        Text(
                                            text = "(Modal ${currencyFormatter.format(costPerSellingUnit).replace("Rp", "Rp ")}/$sellingUnitDisplay)",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = profitTone.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, profitTone.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", profitPercent),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    color = profitTone,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // Pinned Footer Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("add_product_cancel_button")
                    ) {
                        Text(
                            text = "Batal",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            val price = priceStr.toDoubleOrNull() ?: 0.0
                            val sellingPrice = sellingPriceStr.toDoubleOrNull() ?: 0.0
                            val contentQtyVal = contentQtyStr.toDoubleOrNull() ?: 1.0
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

                                val finalSubCat = subCategory.trim().ifBlank { "Umum" }
                                if (finalSubCat.isNotBlank() && finalSubCat != "Umum") {
                                    onAddCustomSubCategory(finalSubCat)
                                }

                                onAdd(name, price, finalUnit, sellingPrice, finalSellingUnit, contentQtyVal, category, finalSubCat)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = onPrimaryColor
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.35f)
                            .height(48.dp)
                            .testTag("add_product_save_button")
                    ) {
                        Text("Simpan", fontWeight = FontWeight.ExtraBold, fontSize = 14.5.sp)
                    }
                }
            }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductDialog(
    product: ProductEntity,
    unitOptions: List<String>,
    subCategoryOptions: List<String> = emptyList(),
    onAddCustomUnit: (String) -> Unit,
    onUpdateCustomUnit: (String, String) -> Unit,
    onDeleteCustomUnit: (String) -> Unit,
    onAddCustomSubCategory: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, Double, String, Double, String, String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(product.name) }
    var priceStr by remember { mutableStateOf(product.price.toString().replace(".0", "")) }
    var sellingPriceStr by remember { mutableStateOf(product.sellingPrice.toString().replace(".0", "")) }
    var contentQtyStr by remember { mutableStateOf(product.contentQuantity.toString().replace(".0", "")) }
    var subCategory by remember { mutableStateOf(product.subCategory.ifBlank { "Tembakau" }) }
    var category by remember { mutableStateOf(product.category) }

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
        title = { Text("EDIT PRODUK", fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp, color = MaterialTheme.colorScheme.onSurface) },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .padding(bottom = 72.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // LIVE PREVIEW CARD (Pinned at top so it is never covered by keyboard)
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

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (marginPerSellingUnit >= 0) Color(0xFF00FF87).copy(alpha = 0.12f) else Color(0xFFFF3366).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, if (marginPerSellingUnit >= 0) Color(0xFF00FF87).copy(alpha = 0.45f) else Color(0xFFFF3366).copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PREVIEW LIVE LABA / $selectedSellingUnit",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                                color = if (marginPerSellingUnit >= 0) Color(0xFF00FF87) else Color(0xFFFF3366)
                            )
                            Text(
                                text = currencyFormatter.format(marginPerSellingUnit).replace("Rp", "Rp "),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (marginPerSellingUnit >= 0) Color(0xFF00FF87) else Color(0xFFFF3366)
                            )
                            if (contentQty > 1) {
                                Text(
                                    text = "Modal: ${currencyFormatter.format(costPerSellingUnit).replace("Rp", "Rp ")}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (marginPerSellingUnit >= 0) Color(0xFF00FF87).copy(alpha = 0.2f) else Color(0xFFFF3366).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, if (marginPerSellingUnit >= 0) Color(0xFF00FF87) else Color(0xFFFF3366))
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1f%%", profitPercent),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = if (marginPerSellingUnit >= 0) Color(0xFF00FF87) else Color(0xFFFF3366),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Nama Produk
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                // Sub Kategori
                OutlinedTextField(
                    value = subCategory,
                    onValueChange = { subCategory = it },
                    label = { Text("Sub Kategori") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                if (subCategoryOptions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subCategoryOptions.forEach { option ->
                            val isSelected = subCategory.equals(option, ignoreCase = true)
                            Surface(
                                onClick = { subCategory = option },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Harga Kulakan & Harga Jual
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Harga Kulakan") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )

                    OutlinedTextField(
                        value = sellingPriceStr,
                        onValueChange = { sellingPriceStr = it },
                        label = { Text("Harga Jual") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )
                }

                // Isi per Satuan
                OutlinedTextField(
                    value = contentQtyStr,
                    onValueChange = { contentQtyStr = it },
                    label = { Text("Isi per Satuan Beli (Misal: 10)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                // Satuan Beli & Satuan Jual
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
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
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sellingUnitExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
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
                }

                if (selectedUnit == "Custom" || selectedSellingUnit == "Custom") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (selectedUnit == "Custom") {
                            OutlinedTextField(
                                value = customUnit,
                                onValueChange = { customUnit = it },
                                label = { Text("Custom Satuan Beli") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }
                        if (selectedSellingUnit == "Custom") {
                            OutlinedTextField(
                                value = customSellingUnit,
                                onValueChange = { customSellingUnit = it },
                                label = { Text("Custom Satuan Jual") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }
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

                            val finalSubCat = subCategory.trim().ifBlank { "Umum" }
                            if (finalSubCat.isNotBlank() && finalSubCat != "Umum") {
                                onAddCustomSubCategory(finalSubCat)
                            }

                            onConfirm(name, price, finalUnit, sellingPrice, finalSellingUnit, contentQty, category, finalSubCat)
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
