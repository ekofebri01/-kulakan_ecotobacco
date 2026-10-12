package com.aistudio.ecotobacco.kfzqw.widget

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProcurementItemEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProcurementPlanEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProductEntity
import com.aistudio.ecotobacco.kfzqw.ui.theme.EcoTobaccoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

data class QuickDraftPlanItem(
    val id: String = UUID.randomUUID().toString(),
    val productName: String,
    val targetQuantity: Double,
    val estimatedUnitPrice: Double,
    val unit: String
)

class QuickPlanActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EcoTobaccoTheme {
                val coroutineScope = rememberCoroutineScope()
                val db = remember { AppDatabase.getInstance(applicationContext) }
                val availableProducts = remember { mutableStateListOf<ProductEntity>() }
                val knownSuppliers = remember { mutableStateListOf<QuickSupplierRecord>() }

                var supplierName by remember { mutableStateOf("") }
                val draftItems = remember { mutableStateListOf<QuickDraftPlanItem>() }

                // Item Input Form State
                var itemName by remember { mutableStateOf("") }
                var targetQtyStr by remember { mutableStateOf("") }
                var unit by remember { mutableStateOf("kg") }
                var estimatedPriceStr by remember { mutableStateOf("") }
                var editingItemId by remember { mutableStateOf<String?>(null) }

                var isSaving by remember { mutableStateOf(false) }
                var showDiscardConfirm by remember { mutableStateOf(false) }
                var itemToDeleteFromDraft by remember { mutableStateOf<QuickDraftPlanItem?>(null) }

                val hasChanges = supplierName.isNotBlank() || draftItems.isNotEmpty() || itemName.isNotBlank() || targetQtyStr.isNotBlank() || estimatedPriceStr.isNotBlank()

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

                val currentInputQty = targetQtyStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                val currentInputPrice = estimatedPriceStr.replace(",", ".").toDoubleOrNull() ?: 0.0

                // Filter matching suppliers
                val filteredSuppliers = remember(supplierName, knownSuppliers.size) {
                    val query = supplierName.trim()
                    if (query.isBlank()) {
                        knownSuppliers.take(4)
                    } else {
                        knownSuppliers.filter { it.name.contains(query, ignoreCase = true) }.take(6)
                    }
                }

                // Matched supplier for preview
                val matchedSupplier = remember(supplierName, knownSuppliers.size) {
                    val query = supplierName.trim()
                    if (query.isBlank()) null
                    else knownSuppliers.find { it.name.equals(query, ignoreCase = true) }
                        ?: knownSuppliers.firstOrNull { it.name.contains(query, ignoreCase = true) }
                }

                // Filter matching products
                val filteredProducts = remember(itemName, availableProducts.size) {
                    val query = itemName.trim()
                    if (query.isBlank()) {
                        availableProducts.take(4)
                    } else {
                        availableProducts.filter { it.name.contains(query, ignoreCase = true) }.take(6)
                    }
                }

                // Matched product for preview
                val matchedProduct = remember(itemName, availableProducts.size) {
                    val query = itemName.trim()
                    if (query.isBlank()) null
                    else availableProducts.find { it.name.equals(query, ignoreCase = true) }
                        ?: availableProducts.firstOrNull { it.name.contains(query, ignoreCase = true) }
                }

                val totalPlanEstCost = draftItems.sumOf { it.targetQuantity * it.estimatedUnitPrice }

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
                            .padding(vertical = 20.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { /* Prevent dismissing on card click */ },
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.5.dp, Color(0xFF38BDF8))
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
                                        color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.EditNote,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.padding(6.dp).size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            "Bikin Rencana Kulakan",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            "Isi Cepat Multi-Produk Widget",
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

                            // 1. Supplier / Location
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Target Pemasok / Supplier / Lokasi Kulakan *",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFCBD5E1)
                                )
                                OutlinedTextField(
                                    value = supplierName,
                                    onValueChange = { supplierName = it },
                                    placeholder = { Text("Ketik target supplier / gudang...", color = Color(0xFF64748B)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF38BDF8),
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
                                                onClick = { supplierName = sup.name },
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
                                if (supplierName.isNotBlank()) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (matchedSupplier != null) Color(0xFF0C2A38) else Color(0xFF1E293B),
                                        border = BorderStroke(
                                            1.dp,
                                            if (matchedSupplier != null) Color(0xFF38BDF8).copy(alpha = 0.6f) else Color(0xFF475569)
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
                                                    tint = if (matchedSupplier != null) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Column {
                                                    Text(
                                                        if (matchedSupplier != null) "Lokasi / Pemasok: ${matchedSupplier.name}" else "Target Baru: \"${supplierName.trim()}\"",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        if (matchedSupplier != null) "Ditemukan dalam riwayat (${matchedSupplier.count} transaksi/rencana)" else "Akan dicatat sebagai target supplier baru",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. DRAFT PLAN ITEMS LIST (MULTI-PRODUCT SUPPORT)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.PlaylistAdd,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            "Daftar Barang dalam Rencana",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    Surface(
                                        color = if (draftItems.isNotEmpty()) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFF334155),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            "${draftItems.size} Produk",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (draftItems.isNotEmpty()) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                if (draftItems.isEmpty()) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF1E293B).copy(alpha = 0.6f),
                                        border = BorderStroke(1.dp, Color(0xFF334155))
                                    ) {
                                        Text(
                                            "Belum ada barang dalam rencana ini. Isi formulir di bawah lalu ketuk '+ Tambah Produk ke Rencana' untuk memasukkan beberapa produk.",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8),
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        draftItems.forEachIndexed { index, item ->
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color(0xFF1E293B),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (editingItemId == item.id) Color(0xFF38BDF8) else Color(0xFF334155)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Surface(
                                                            color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                                                            shape = RoundedCornerShape(8.dp)
                                                        ) {
                                                            Text(
                                                                "#${index + 1}",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF38BDF8),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                                            )
                                                        }

                                                        Column {
                                                            Text(
                                                                item.productName,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.sp,
                                                                color = Color.White
                                                            )
                                                            Text(
                                                                "Target: ${formatDecimal(item.targetQuantity)} ${item.unit}" +
                                                                        if (item.estimatedUnitPrice > 0) " @ ${formatRupiah(item.estimatedUnitPrice)}" else "",
                                                                fontSize = 11.sp,
                                                                color = Color(0xFFCBD5E1)
                                                            )
                                                        }
                                                    }

                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        if (item.estimatedUnitPrice > 0) {
                                                            Text(
                                                                formatRupiah(item.targetQuantity * item.estimatedUnitPrice),
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF38BDF8),
                                                                modifier = Modifier.padding(end = 4.dp)
                                                            )
                                                        }

                                                        IconButton(
                                                            onClick = {
                                                                editingItemId = item.id
                                                                itemName = item.productName
                                                                targetQtyStr = if (item.targetQuantity % 1.0 == 0.0) item.targetQuantity.toLong().toString() else item.targetQuantity.toString()
                                                                unit = item.unit
                                                                estimatedPriceStr = if (item.estimatedUnitPrice > 0) item.estimatedUnitPrice.toLong().toString() else ""
                                                            },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                                        }

                                                        IconButton(
                                                            onClick = { itemToDeleteFromDraft = item },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Total Estimated Cost Card
                                        if (totalPlanEstCost > 0) {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color(0xFF0C2A38),
                                                border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f))
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Total Est. Rencana:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                                    Text(
                                                        formatRupiah(totalPlanEstCost),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFF38BDF8)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. ITEM INPUT / EDIT FORM
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1E293B).copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, if (editingItemId != null) Color(0xFF38BDF8) else Color(0xFF334155))
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
                                            if (editingItemId != null) "Edit Produk dalam Rencana" else "Tambah Produk Baru ke Rencana",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (editingItemId != null) Color(0xFF38BDF8) else Color.White
                                        )

                                        if (editingItemId != null) {
                                            OutlinedButton(
                                                onClick = {
                                                    editingItemId = null
                                                    itemName = ""
                                                    targetQtyStr = ""
                                                    estimatedPriceStr = ""
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("Batal Edit", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                            }
                                        }
                                    }

                                    // Product Name Input
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            "Nama Barang / Produk *",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFCBD5E1)
                                        )
                                        OutlinedTextField(
                                            value = itemName,
                                            onValueChange = { itemName = it },
                                            placeholder = { Text("Ketik nama barang...", color = Color(0xFF64748B)) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF38BDF8),
                                                unfocusedBorderColor = Color(0xFF334155),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            )
                                        )

                                        // Dynamic Product Suggestions
                                        if (filteredProducts.isNotEmpty()) {
                                            @OptIn(ExperimentalLayoutApi::class)
                                            FlowRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                filteredProducts.forEach { prod ->
                                                    SuggestionChip(
                                                        onClick = {
                                                            itemName = prod.name
                                                            if (prod.price > 0) {
                                                                estimatedPriceStr = prod.price.toLong().toString()
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
                                                            containerColor = Color(0xFF0F172A)
                                                        ),
                                                        border = null
                                                    )
                                                }
                                            }
                                        }

                                        // LIVE PREVIEW BARANG CARD
                                        if (itemName.isNotBlank()) {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (matchedProduct != null) Color(0xFF0C2A38) else Color(0xFF0F172A),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (matchedProduct != null) Color(0xFF38BDF8).copy(alpha = 0.6f) else Color(0xFF475569)
                                                )
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(8.dp),
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
                                                                tint = if (matchedProduct != null) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Text(
                                                                if (matchedProduct != null) "Katalog Terdaftar: ${matchedProduct.name}" else "Barang Baru: \"${itemName.trim()}\"",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (matchedProduct != null) Color(0xFF38BDF8) else Color(0xFFCBD5E1)
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
                                                                color = Color(0xFF38BDF8)
                                                            )
                                                            if (matchedProduct.sellingPrice > 0) {
                                                                Text(
                                                                    text = "Jual: ${formatRupiah(matchedProduct.sellingPrice)}",
                                                                    fontSize = 10.sp,
                                                                    color = Color(0xFF94A3B8)
                                                                )
                                                            }
                                                        }
                                                        if (estimatedPriceStr != matchedProduct.price.toLong().toString()) {
                                                            Row(
                                                                modifier = Modifier
                                                                    .clickable {
                                                                        estimatedPriceStr = matchedProduct.price.toLong().toString()
                                                                        unit = matchedProduct.unit
                                                                    }
                                                                    .padding(top = 2.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Icon(Icons.Default.Check, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(12.dp))
                                                                Text(
                                                                    "Terapkan harga acuan (${formatRupiah(matchedProduct.price)}) & satuan (${matchedProduct.unit})",
                                                                    fontSize = 10.sp,
                                                                    color = Color(0xFF7DD3FC),
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        Text(
                                                            "Barang '${itemName.trim()}' belum terdaftar. Akan dicatat sebagai rencana barang baru.",
                                                            fontSize = 10.sp,
                                                            color = Color(0xFF94A3B8)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Qty & Unit Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Target Jumlah *", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFCBD5E1))
                                            OutlinedTextField(
                                                value = targetQtyStr,
                                                onValueChange = { targetQtyStr = it },
                                                placeholder = { Text("0", color = Color(0xFF64748B)) },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color(0xFF38BDF8),
                                                    unfocusedBorderColor = Color(0xFF334155),
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White
                                                )
                                            )
                                        }

                                        Column(modifier = Modifier.weight(0.9f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Satuan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFCBD5E1))
                                            OutlinedTextField(
                                                value = unit,
                                                onValueChange = { unit = it },
                                                placeholder = { Text("kg", color = Color(0xFF64748B)) },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color(0xFF38BDF8),
                                                    unfocusedBorderColor = Color(0xFF334155),
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White
                                                )
                                            )
                                        }
                                    }

                                    // Estimated Price
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Estimasi Harga Satuan (Rp Opsional)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFCBD5E1))
                                        OutlinedTextField(
                                            value = estimatedPriceStr,
                                            onValueChange = { estimatedPriceStr = it },
                                            placeholder = { Text("0", color = Color(0xFF64748B)) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF38BDF8),
                                                unfocusedBorderColor = Color(0xFF334155),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            )
                                        )
                                    }

                                    // Add Item to Draft Button
                                    Button(
                                        onClick = {
                                            val trimmedItem = itemName.trim()
                                            if (trimmedItem.isBlank()) {
                                                Toast.makeText(this@QuickPlanActivity, "Nama barang tidak boleh kosong", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            if (currentInputQty <= 0) {
                                                Toast.makeText(this@QuickPlanActivity, "Target jumlah harus lebih dari 0", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }

                                            if (editingItemId != null) {
                                                val editIndex = draftItems.indexOfFirst { it.id == editingItemId }
                                                if (editIndex >= 0) {
                                                    draftItems[editIndex] = QuickDraftPlanItem(
                                                        id = editingItemId!!,
                                                        productName = trimmedItem,
                                                        targetQuantity = currentInputQty,
                                                        estimatedUnitPrice = currentInputPrice,
                                                        unit = unit.trim().ifEmpty { "kg" }
                                                    )
                                                    Toast.makeText(this@QuickPlanActivity, "Produk $trimmedItem diperbarui", Toast.LENGTH_SHORT).show()
                                                }
                                                editingItemId = null
                                            } else {
                                                draftItems.add(
                                                    QuickDraftPlanItem(
                                                        productName = trimmedItem,
                                                        targetQuantity = currentInputQty,
                                                        estimatedUnitPrice = currentInputPrice,
                                                        unit = unit.trim().ifEmpty { "kg" }
                                                    )
                                                )
                                                Toast.makeText(this@QuickPlanActivity, "Produk $trimmedItem ditambahkan ke rencana!", Toast.LENGTH_SHORT).show()
                                            }

                                            // Clear item input form for next product
                                            itemName = ""
                                            targetQtyStr = ""
                                            estimatedPriceStr = ""
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (editingItemId != null) Color(0xFF0284C7) else Color(0xFF1E293B)
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFF38BDF8))
                                    ) {
                                        Icon(
                                            if (editingItemId != null) Icons.Default.Save else Icons.Default.Add,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            if (editingItemId != null) "Simpan Perubahan Produk" else "+ Tambah Produk ke Rencana",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }
                                }
                            }

                            // 4. MAIN ACTION BUTTONS
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
                                        val trimmedSupplier = supplierName.trim()
                                        if (trimmedSupplier.isBlank()) {
                                            Toast.makeText(this@QuickPlanActivity, "Nama supplier/lokasi tidak boleh kosong", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        // Auto-add current input form if user hasn't clicked "+ Tambah" yet
                                        val currentItemName = itemName.trim()
                                        if (currentItemName.isNotBlank() && currentInputQty > 0) {
                                            draftItems.add(
                                                QuickDraftPlanItem(
                                                    productName = currentItemName,
                                                    targetQuantity = currentInputQty,
                                                    estimatedUnitPrice = currentInputPrice,
                                                    unit = unit.trim().ifEmpty { "kg" }
                                                )
                                            )
                                            itemName = ""
                                            targetQtyStr = ""
                                            estimatedPriceStr = ""
                                        }

                                        if (draftItems.isEmpty()) {
                                            Toast.makeText(this@QuickPlanActivity, "Tambahkan minimal 1 produk ke rencana kulakan", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        isSaving = true
                                        coroutineScope.launch {
                                            try {
                                                withContext(Dispatchers.IO) {
                                                    val now = System.currentTimeMillis()
                                                    val plan = ProcurementPlanEntity(
                                                        date = now,
                                                        supplierName = trimmedSupplier
                                                    )
                                                    db.procurementDao().insertPlan(plan)

                                                    val plans = db.procurementDao().getAllPlansWithItemsDirect()
                                                    val insertedPlan = plans.maxByOrNull { it.plan.id }?.plan
                                                    if (insertedPlan != null) {
                                                        val itemsToInsert = draftItems.map { draft ->
                                                            ProcurementItemEntity(
                                                                planId = insertedPlan.id,
                                                                productName = draft.productName,
                                                                targetQuantity = draft.targetQuantity,
                                                                estimatedUnitPrice = draft.estimatedUnitPrice,
                                                                unit = draft.unit,
                                                                isBought = false
                                                            )
                                                        }
                                                        db.procurementDao().insertItems(itemsToInsert)
                                                    }

                                                    // Notify widget immediately
                                                    TobaccoAppWidgetProvider.notifyDataChanged(applicationContext)
                                                }

                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        this@QuickPlanActivity,
                                                        "Rencana kulakan $trimmedSupplier (${draftItems.size} produk) tersimpan!",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    finish()
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    isSaving = false
                                                    Toast.makeText(this@QuickPlanActivity, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isSaving,
                                    modifier = Modifier.weight(1.6f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (draftItems.size > 1) "Simpan Rencana (${draftItems.size} Produk)" else "Simpan Rencana",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Delete Item From Draft Confirmation Dialog
                if (itemToDeleteFromDraft != null) {
                    AlertDialog(
                        onDismissRequest = { itemToDeleteFromDraft = null },
                        title = { Text("Hapus Produk dari Rencana?", fontWeight = FontWeight.Bold, color = Color.White) },
                        text = { Text("Apakah Anda yakin ingin menghapus '${itemToDeleteFromDraft?.productName}' dari daftar rencana?", color = Color(0xFFCBD5E1)) },
                        containerColor = Color(0xFF1E293B),
                        confirmButton = {
                            Button(
                                onClick = {
                                    itemToDeleteFromDraft?.let { draftItems.remove(it) }
                                    itemToDeleteFromDraft = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Ya, Hapus", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { itemToDeleteFromDraft = null }) {
                                Text("Batal", color = Color(0xFF94A3B8))
                            }
                        }
                    )
                }

                // Discard confirmation dialog
                if (showDiscardConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDiscardConfirm = false },
                        title = { Text("Batalkan Rencana?", fontWeight = FontWeight.Bold, color = Color.White) },
                        text = { Text("Data yang sudah Anda ketik akan dibuang. Apakah Anda yakin ingin keluar?", color = Color(0xFFCBD5E1)) },
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

    private fun formatDecimal(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format(Locale.getDefault(), "%.1f", value)
        }
    }
}
