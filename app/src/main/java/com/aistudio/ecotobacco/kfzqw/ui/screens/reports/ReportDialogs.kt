package com.aistudio.ecotobacco.kfzqw.ui.screens.reports

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.ecotobacco.kfzqw.data.local.entities.TransactionEntity
import com.aistudio.ecotobacco.kfzqw.data.remote.SyncStatus
import com.aistudio.ecotobacco.kfzqw.viewmodel.TobaccoViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(transaction: TransactionEntity, viewModel: TobaccoViewModel, onDismiss: () -> Unit, onConfirm: (Long, String, Double, Double, String, String) -> Unit) {
    var dateMs by remember { mutableLongStateOf(transaction.date) }
    var productName by remember { mutableStateOf(transaction.productName) }
    var quantity by remember { mutableStateOf(transaction.quantity.toString()) }
    var unitPrice by remember { mutableStateOf(transaction.unitPrice.toString().replace(".0", "")) }
    var supplier by remember { mutableStateOf(transaction.supplier) }
    
    val unitOptions = listOf("kg", "ons", "pcs", "Custom")
    var selectedUnit by remember { mutableStateOf(if (unitOptions.contains(transaction.unit)) transaction.unit else "Custom") }
    var customUnit by remember { mutableStateOf(if (selectedUnit == "Custom") transaction.unit else "") }
    var unitExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false),
        modifier = Modifier
            .imePadding()
            .navigationBarsPadding(),
        title = { Text("Edit Transaksi", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .padding(bottom = 72.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dateMs)), onValueChange = {}, readOnly = true, label = { Text("Tanggal") }, modifier = Modifier.fillMaxWidth().clickable {
                    val cal = Calendar.getInstance().apply { timeInMillis = dateMs }
                    DatePickerDialog(context, { _, y, m, d ->
                        val newCal = Calendar.getInstance(); newCal.set(y, m, d); dateMs = newCal.timeInMillis
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                }, enabled = false, colors = OutlinedTextFieldDefaults.colors(disabledBorderColor = MaterialTheme.colorScheme.outlineVariant, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant, disabledTextColor = MaterialTheme.colorScheme.onSurface))
                OutlinedTextField(value = productName, onValueChange = { productName = it }, label = { Text("Nama Produk") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary))
                OutlinedTextField(value = supplier, onValueChange = { supplier = it }, label = { Text("Supplier") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary))
                
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
                            label = { Text("Satuan") },
                            modifier = Modifier
                                .fillMaxWidth() // Opsional: biar rapi
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
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
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Qty") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary))
                    OutlinedTextField(value = unitPrice, onValueChange = { unitPrice = it }, label = { Text("Harga") }, modifier = Modifier.weight(2f), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary))
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = {
                        val q = viewModel.parsePrice(quantity)
                        val p = viewModel.parsePrice(unitPrice)
                        val finalUnit = if (selectedUnit == "Custom") customUnit.ifBlank { "unit" } else selectedUnit
                        onConfirm(dateMs, productName, q, p, supplier, finalUnit)
                    },
                    modifier = Modifier.height(44.dp).weight(1.2f),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Simpan", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {}
    )
}

@Composable
fun TransactionOptionsDialog(transaction: TransactionEntity, onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(transaction.productName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        text = {
            Column {
                ListItem(headlineContent = { Text("Edit Transaksi") }, leadingContent = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) }, modifier = Modifier.clickable { onEdit() })
                ListItem(headlineContent = { Text("Hapus Transaksi", color = MaterialTheme.colorScheme.error) }, leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }, modifier = Modifier.clickable { onDelete() })
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    )
}

@Composable
fun SyncDialog(viewModel: TobaccoViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

    LaunchedEffect(syncStatus) {
        when (syncStatus) {
            SyncStatus.SYNCED -> {
                Toast.makeText(context, "Sinkronisasi berhasil!", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
            SyncStatus.ERROR -> {
                Toast.makeText(context, "Sinkronisasi gagal. Periksa koneksi.", Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudSync, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Sinkronisasi Cloud", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        text = { 
            Column {
                Text(
                    text = when(syncStatus) {
                        SyncStatus.SYNCING -> "Sedang menyinkronkan data..."
                        SyncStatus.ERROR -> "Terjadi kesalahan saat sinkronisasi."
                        else -> "Cadangkan data Anda ke Google Drive agar aman dan bisa diakses di perangkat lain."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (syncStatus == SyncStatus.SYNCING) {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.syncData() }, 
                enabled = syncStatus != SyncStatus.SYNCING,
                shape = RoundedCornerShape(12.dp), 
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { 
                Text(if (syncStatus == SyncStatus.SYNCING) "Memproses..." else "Sinkron Sekarang", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss, enabled = syncStatus != SyncStatus.SYNCING) {
                Text("Tutup", color = MaterialTheme.colorScheme.onSurfaceVariant) 
            } 
        }
    )
}
