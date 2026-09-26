package com.aistudio.ecotobacco.kfzqw.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitManagementDialog(
    units: List<String>,
    onAdd: (String) -> Unit,
    onUpdate: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newUnitName by remember { mutableStateOf("") }
    var editingUnit by remember { mutableStateOf<String?>(null) }
    var editedName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false),
        modifier = Modifier.imePadding(),
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Kelola Satuan", fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                // Add New Unit Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newUnitName,
                        onValueChange = { newUnitName = it },
                        label = { Text("Tambah Baru") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newUnitName.isNotBlank()) {
                                onAdd(newUnitName.trim())
                                newUnitName = ""
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, "Tambah")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(units) { unit ->
                        ListItem(
                            headlineContent = {
                                if (editingUnit == unit) {
                                    TextField(
                                        value = editedName,
                                        onValueChange = { editedName = it },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Text(unit, fontWeight = FontWeight.Medium)
                                }
                            },
                            trailingContent = {
                                Row {
                                    if (editingUnit == unit) {
                                        TextButton(onClick = {
                                            if (editedName.isNotBlank()) {
                                                onUpdate(unit, editedName.trim())
                                                editingUnit = null
                                            }
                                        }) {
                                            Text("Simpan")
                                        }
                                        TextButton(onClick = { editingUnit = null }) {
                                            Text("Batal")
                                        }
                                    } else {
                                        IconButton(onClick = {
                                            editingUnit = unit
                                            editedName = unit
                                        }) {
                                            Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { onDelete(unit) }) {
                                            Icon(Icons.Default.Delete, "Hapus", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
