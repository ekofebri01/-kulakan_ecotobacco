package com.aistudio.ecotobacco.kfzqw.widget

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.aistudio.ecotobacco.kfzqw.data.local.AppDatabase
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProductEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.TransactionEntity
import com.aistudio.ecotobacco.kfzqw.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

data class QuickSupplierRecord(
    val name: String,
    val count: Int
)

class QuickKulakanActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApplicationTheme(themeMode = "DARK") {
                val coroutineScope = rememberCoroutineScope()
                val db = remember { AppDatabase.getInstance(applicationContext) }
                val availableProducts = remember { mutableStateListOf<ProductEntity>() }
                val knownSuppliers = remember { mutableStateListOf<QuickSupplierRecord>() }

                var productName by remember { mutableStateOf("") }
                var quantityStr by remember { mutableStateOf("") }
                var unit by remember { mutableStateOf("kg") }
                var unitPriceStr by remember { mutableStateOf("") }
                var supplier by remember { mutableStateOf("") }
                var isSaving by remember { mutableStateOf(false) }
                var showDiscardConfirm by remember { mutableStateOf(false) }

                val hasChanges = productName.isNotBlank() || quantityStr.isNotBlank() || unitPriceStr.isNotBlank() || supplier.isNotBlank()

                BackHandler {
                    if (hasChanges) {
                        showDiscardConfirm = true
                    } else {
                        finish()
                    }
                }

                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        val prods = db.productDao().getAllProductsDirect()
                        val txs = db.transactionDao().getAllTransactionsDirect()
                        val plans = db.procurementDao().getAllPlansWithItemsDirect()

                        val supplierCounts = mutableMapOf<String, Int>()
                        txs.forEach { tx ->
                            val s = tx.supplier.trim()
                            if (s.isNotBlank()) {
                                supplierCounts[s] = (supplierCounts[s] ?: 0) + 1
                            }
                        }
                        plans.forEach { p ->
                            val s = p.plan.supplierName.trim()
                            if (s.isNotBlank()) {
                                supplierCounts[s] = (supplierCounts[s] ?: 0) + 1
                            }
                        }
                        val suppliersList = supplierCounts.map { QuickSupplierRecord(it.key, it.value) }
                            .sortedByDescending { it.count }

                        withContext(Dispatchers.Main) {
                            availableProducts.clear()
                            availableProducts.addAll(prods)
                            knownSuppliers.clear()
                            knownSuppliers.addAll(suppliersList)
                        }
                    }
                }

                val quantity = quantityStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                val unitPrice = unitPriceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                val totalAmount = quantity * unitPrice

                // Filter matching products
                val filteredProducts = remember(productName, availableProducts.size) {
                    val query = productName.trim()
                    if (query.isBlank()) {
                        availableProducts.take(4)
                    } else {
                        availableProducts.filter { it.name.contains(query, ignoreCase = true) }.take(6)
                    }
                }

                // Matched product for live preview
                val matchedProduct = remember(productName, availableProducts.size) {
                    val query = productName.trim()
                    if (query.isBlank()) null
                    else availableProducts.find { it.name.equals(query, ignoreCase = true) }
                        ?: availableProducts.firstOrNull { it.name.contains(query, ignoreCase = true) }
                }

                // Filter matching suppliers
                val filteredSuppliers = remember(supplier, knownSuppliers.size) {
                    val query = supplier.trim()
                    if (query.isBlank()) {
                        knownSuppliers.take(4)
                    } else {
                        knownSuppliers.filter { it.name.contains(query, ignoreCase = true) }.take(6)
                    }
                }

                // Matched supplier for live preview
                val matchedSupplier = remember(supplier, knownSuppliers.size) {
                    val query = supplier.trim()
                    if (query.isBlank()) null
                    else knownSuppliers.find { it.name.equals(query, ignoreCase = true) }
                        ?: knownSuppliers.firstOrNull { it.name.contains(query, ignoreCase = true) }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (hasChanges) showDiscardConfirm = true else finish()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(vertical = 24.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { /* Prevent dismissing on card click */ },
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ShoppingBag,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.padding(6.dp).size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            "Input Kulakan Cepat",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            "Shortcut Widget Home Screen",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                IconButton(onClick = {
                                    if (hasChanges) showDiscardConfirm = true else finish()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color(0xFF94A3B8))
                                }
                            }

                            // Product Name
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Nama Barang / Produk Kulakan *",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFCBD5E1)
                                )
                                OutlinedTextField(
                                    value = productName,
                                    onValueChange = { productName = it },
                                    placeholder = { Text("Ketik nama produk...", color = Color(0xFF64748B)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF10B981),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                // Dynamic Suggestion Chips while typing
                                if (filteredProducts.isNotEmpty()) {
                                    @OptIn(ExperimentalLayoutApi::class)
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        filteredProducts.forEach { prod ->
                                            SuggestionChip(
                                                onClick = {
                                                    productName = prod.name
                                                    if (prod.price > 0) {
                                                        unitPriceStr = prod.price.toLong().toString()
                                                    }
                                                    unit = prod.unit
                                                },
                                                label = {
                                                    Text(
                                                        "${prod.name} (${formatRupiah(prod.price)})",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFFE2E8F0)
                                                    )
                                                },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    containerColor = Color(0xFF1E293B)
                                                ),
                                                border = null
                                            )
                                        }
                                    }
                                }

                                // LIVE PREVIEW PRODUK CARD
                                if (productName.isNotBlank()) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (matchedProduct != null) Color(0xFF132A22) else Color(0xFF1E293B),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (matchedProduct != null) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFF475569)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        if (matchedProduct != null) Icons.Default.Inventory2 else Icons.Default.Store,
                                                        contentDescription = null,
                                                        tint = if (matchedProduct != null) Color(0xFF34D399) else Color(0xFF38BDF8),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        if (matchedProduct != null) "Preview Produk Ditemukan" else "Preview: Produk Baru",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (matchedProduct != null) Color(0xFF34D399) else Color(0xFF38BDF8)
                                                    )
                                                }
                                                Surface(
                                                    color = if (matchedProduct != null) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF0284C7).copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        if (matchedProduct != null) "Katalog Aktif" else "Akan Didaftarkan",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (matchedProduct != null) Color(0xFF6EE7B7) else Color(0xFF7DD3FC),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            if (matchedProduct != null) {
                                                Text(
                                                    text = "Nama: ${matchedProduct.name} | Satuan: ${matchedProduct.unit}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFE2E8F0)
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Harga Beli Acuan: ${formatRupiah(matchedProduct.price)}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF34D399)
                                                    )
                                                    if (matchedProduct.sellingPrice > 0) {
                                                        Text(
                                                            text = "Jual: ${formatRupiah(matchedProduct.sellingPrice)}",
                                                            fontSize = 10.sp,
                                                            color = Color(0xFF94A3B8)
                                                        )
                                                    }
                                                }
                                                if (unitPriceStr != matchedProduct.price.toLong().toString()) {
                                                    Row(
                                                        modifier = Modifier
                                                            .clickable {
                                                                unitPriceStr = matchedProduct.price.toLong().toString()
                                                                unit = matchedProduct.unit
                                                            }
                                                            .padding(top = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(Icons.Default.Check, null, tint = Color(0xFF34D399), modifier = Modifier.size(12.dp))
                                                        Text(
                                                            "Terapkan harga acuan (${formatRupiah(matchedProduct.price)}) & satuan (${matchedProduct.unit})",
                                                            fontSize = 10.sp,
                                                            color = Color(0xFF6EE7B7),
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            } else {
                                                Text(
                                                    "Produk '${productName.trim()}' belum terdaftar. Akan otomatis disimpan ke Katalog Master saat transaksi dicatat.",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF94A3B8)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Quantity & Unit
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Jumlah Kulakan *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFCBD5E1))
                                    OutlinedTextField(
                                        value = quantityStr,
                                        onValueChange = { quantityStr = it },
                                        placeholder = { Text("0", color = Color(0xFF64748B)) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF10B981),
                                            unfocusedBorderColor = Color(0xFF334155),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )
                                }

                                Column(modifier = Modifier.weight(0.9f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Satuan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFCBD5E1))
                                    OutlinedTextField(
                                        value = unit,
                                        onValueChange = { unit = it },
                                        placeholder = { Text("kg", color = Color(0xFF64748B)) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF10B981),
                                            unfocusedBorderColor = Color(0xFF334155),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )
                                }
                            }

                            // Unit Price
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Harga Beli Satuan (Rp) *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFCBD5E1))
                                OutlinedTextField(
                                    value = unitPriceStr,
                                    onValueChange = { unitPriceStr = it },
                                    placeholder = { Text("0", color = Color(0xFF64748B)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF10B981),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }

                            // Supplier (Optional) with dynamic suggestions & preview
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Supplier / Pemasok (Opsional)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFCBD5E1))
                                OutlinedTextField(
                                    value = supplier,
                                    onValueChange = { supplier = it },
                                    placeholder = { Text("Ketik nama supplier / petani...", color = Color(0xFF64748B)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF10B981),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                // Dynamic Supplier Suggestions
                                if (filteredSuppliers.isNotEmpty()) {
                                    @OptIn(ExperimentalLayoutApi::class)
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        filteredSuppliers.forEach { sup ->
                                            SuggestionChip(
                                                onClick = { supplier = sup.name },
                                                label = {
                                                    Text(
                                                        "${sup.name} (${sup.count}x)",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFFE2E8F0)
                                                    )
                                                },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    containerColor = Color(0xFF1E293B)
                                                ),
                                                border = null
                                            )
                                        }
                                    }
                                }

                                // LIVE PREVIEW SUPPLIER CARD
                                if (supplier.isNotBlank()) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (matchedSupplier != null) Color(0xFF132A22) else Color(0xFF1E293B),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (matchedSupplier != null) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFF475569)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    if (matchedSupplier != null) Icons.Default.LocalShipping else Icons.Default.PersonAdd,
                                                    contentDescription = null,
                                                    tint = if (matchedSupplier != null) Color(0xFF34D399) else Color(0xFF38BDF8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Column {
                                                    Text(
                                                        if (matchedSupplier != null) "Pemasok Terdaftar: ${matchedSupplier.name}" else "Pemasok Baru: \"${supplier.trim()}\"",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        if (matchedSupplier != null) "Pernah bertransaksi ${matchedSupplier.count} kali sebelumnya" else "Akan disimpan sebagai mitra pemasok baru",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Realtime Total Banner
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(20.dp))
                                        Text("Total Kulakan:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                    }
                                    Text(
                                        formatRupiah(totalAmount),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF34D399)
                                    )
                                }
                            }

                            // Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (hasChanges) showDiscardConfirm = true else finish()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
                                ) {
                                    Text("Batal")
                                }

                                Button(
                                    onClick = {
                                        val trimmedName = productName.trim()
                                        if (trimmedName.isBlank()) {
                                            Toast.makeText(this@QuickKulakanActivity, "Nama barang tidak boleh kosong", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (quantity <= 0) {
                                            Toast.makeText(this@QuickKulakanActivity, "Jumlah kulakan harus lebih dari 0", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (unitPrice <= 0) {
                                            Toast.makeText(this@QuickKulakanActivity, "Harga satuan harus lebih dari 0", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        isSaving = true
                                        coroutineScope.launch {
                                            try {
                                                withContext(Dispatchers.IO) {
                                                    val now = System.currentTimeMillis()
                                                    val tx = TransactionEntity(
                                                        date = now,
                                                        productName = trimmedName,
                                                        quantity = quantity,
                                                        unitPrice = unitPrice,
                                                        total = totalAmount,
                                                        supplier = supplier.trim(),
                                                        unit = unit.trim().ifEmpty { "kg" },
                                                        isDeleted = 0
                                                    )
                                                    db.transactionDao().insertTransaction(tx)

                                                    // Also update or insert Product in catalog
                                                    val existingProds = db.productDao().getAllProductsDirect()
                                                    val matched = existingProds.find { it.name.equals(trimmedName, ignoreCase = true) }
                                                    if (matched == null) {
                                                        db.productDao().insertProduct(
                                                            ProductEntity(
                                                                name = trimmedName,
                                                                price = unitPrice,
                                                                unit = unit.trim().ifEmpty { "kg" }
                                                            )
                                                        )
                                                    } else {
                                                        db.productDao().updateProduct(
                                                            matched.copy(price = unitPrice, unit = unit.trim().ifEmpty { "kg" })
                                                        )
                                                    }

                                                    // Notify widget immediately
                                                    TobaccoAppWidgetProvider.notifyDataChanged(applicationContext)
                                                }

                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        this@QuickKulakanActivity,
                                                        "Kulakan $trimmedName (${formatRupiah(totalAmount)}) tersimpan!",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    finish()
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    isSaving = false
                                                    Toast.makeText(this@QuickKulakanActivity, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isSaving,
                                    modifier = Modifier.weight(1.5f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Simpan Kulakan", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Discard confirmation dialog
                if (showDiscardConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDiscardConfirm = false },
                        title = { Text("Batalkan Input Kulakan?", fontWeight = FontWeight.Bold, color = Color.White) },
                        text = { Text("Data yang sudah Anda ketik akan dibuang. Apakah Anda yakin ingin membatalkan?", color = Color(0xFFCBD5E1)) },
                        containerColor = Color(0xFF1E293B),
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDiscardConfirm = false
                                    finish()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Ya, Batalkan", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showDiscardConfirm = false }) {
                                Text("Lanjutkan Mengetik", color = Color(0xFF94A3B8))
                            }
                        }
                    )
                }
            }
        }
    }

    private fun formatRupiah(amount: Double): String {
        return "Rp " + NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount.toLong())
    }
}
