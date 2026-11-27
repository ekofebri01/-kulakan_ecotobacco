package com.aistudio.ecotobacco.kfzqw.ui.screens.settings

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.ecotobacco.kfzqw.data.remote.AutoBackupInfo
import com.aistudio.ecotobacco.kfzqw.data.remote.DriveBackupFile
import com.aistudio.ecotobacco.kfzqw.data.remote.SyncStatus
import com.aistudio.ecotobacco.kfzqw.ui.components.CloudSyncStatusBanner
import com.aistudio.ecotobacco.kfzqw.viewmodel.TobaccoViewModel
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun BackupCloudSection(
    viewModel: TobaccoViewModel,
    syncStatus: SyncStatus,
    firebaseUser: FirebaseUser?,
    isFirestoreSyncing: Boolean,
    isAutoSync: Boolean,
    onAutoSyncChange: (Boolean) -> Unit,
    autoBackupInfo: AutoBackupInfo?,
    isManualBackupInProgress: Boolean,
    onManualBackupInProgressChange: (Boolean) -> Unit,
    onOpenEmailLogin: () -> Unit,
    onOpenRestorePicker: () -> Unit,
    onOpenDisconnectDialog: () -> Unit,
    onOpenRestoreAutoBackup: () -> Unit,
    onExportJsonClick: () -> Unit,
    onImportJsonClick: () -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PremiumSettingSection(title = "Cloud Sync & Snapshot") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CloudSyncStatusBanner(
                    viewModel = viewModel,
                    onNavigateToSettings = null
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        val isConnected = (syncStatus != SyncStatus.DISCONNECTED) ||
                            (firebaseUser != null) ||
                            viewModel.syncHelper.isLocalUserConnected
                        val accountTitle = if (isConnected) {
                            viewModel.syncHelper.userEmail
                                ?: firebaseUser?.displayName
                                ?: firebaseUser?.email
                                ?: "Akun Terhubung"
                        } else {
                            "Belum Terhubung ke Cloud"
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isConnected) "Akun Cloud Aktif" else "Cadangkan Data ke Cloud",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = accountTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        if (!isConnected) {
                            Button(
                                onClick = onOpenEmailLogin,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cloud_email_login_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.CloudSync, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Masuk dengan Email / Akun Cloud", fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    Toast.makeText(context, "Menghubungkan ke Cloud Firestore...", Toast.LENGTH_SHORT).show()
                                    viewModel.signInAnonymously { success, msg ->
                                        Toast.makeText(
                                            context,
                                            msg ?: if (success) "Terhubung ke Cloud!" else "Gagal",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cloud_guest_login_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Icon(Icons.Default.FlashOn, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Masuk Cepat Cloud (Mode Tamu / Instan)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            val isSyncingActive = isFirestoreSyncing || (syncStatus == SyncStatus.SYNCING)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (!isManualBackupInProgress) {
                                            onManualBackupInProgressChange(true)
                                            viewModel.createManualCloudBackup { _, msg ->
                                                onManualBackupInProgressChange(false)
                                                (context as? Activity)?.runOnUiThread {
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isManualBackupInProgress,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("cloud_backup_now_button"),
                                    contentPadding = PaddingValues(4.dp)
                                ) {
                                    Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("Cadang", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = onOpenRestorePicker,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .testTag("cloud_pick_backup_button"),
                                    contentPadding = PaddingValues(4.dp)
                                ) {
                                    Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("Pilih Backup", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.forceSyncNow { status ->
                                            (context as? Activity)?.runOnUiThread {
                                                Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        viewModel.syncFirestore()
                                    },
                                    enabled = !isSyncingActive,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(0.95f)
                                        .testTag("cloud_sync_now_button"),
                                    contentPadding = PaddingValues(4.dp)
                                ) {
                                    Icon(Icons.Default.Sync, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("Sinkron", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Sinkronisasi Otomatis", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Text("Simpan perubahan secara real-time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = isAutoSync,
                                    onCheckedChange = onAutoSyncChange
                                )
                            }

                            TextButton(
                                onClick = onOpenDisconnectDialog,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Putuskan Hubungan Akun", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        PremiumSettingSection(title = "Cadangan Lokal & File JSON") {
            PremiumCardGroup {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Save, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                                }
                            }
                            Column {
                                Text("Cadangan Otomatis Internal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                val lastTimeStr = autoBackupInfo?.lastBackupTime?.takeIf { it > 0 }?.let {
                                    SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(it))
                                } ?: "Belum pernah"
                                Text(
                                    "Terakhir: $lastTimeStr (${autoBackupInfo?.fileSizeFormatted ?: "0 B"})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButtonPremium(
                            text = "Cadangkan",
                            icon = Icons.Default.Save,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("local_autobackup_save_button"),
                            onClick = {
                                onManualBackupInProgressChange(true)
                                viewModel.performManualAutoBackup(context) { _, msg ->
                                    onManualBackupInProgressChange(false)
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        OutlinedButtonPremium(
                            text = "Pulihkan",
                            icon = Icons.Default.Download,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("local_autobackup_restore_button"),
                            onClick = {
                                if ((autoBackupInfo?.lastBackupTime ?: 0L) > 0L) {
                                    onOpenRestoreAutoBackup()
                                } else {
                                    Toast.makeText(context, "Belum ada file cadangan otomatis", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    PremiumSettingItem(
                        title = "Ekspor File JSON ke Perangkat",
                        subtitle = "Simpan file cadangan .json ke penyimpanan HP",
                        icon = Icons.Default.Upload,
                        onClick = onExportJsonClick
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    PremiumSettingItem(
                        title = "Pulihkan dari Perangkat",
                        subtitle = "Pilih & impor file cadangan .json dari HP Anda",
                        icon = Icons.Default.UploadFile,
                        iconColor = MaterialTheme.colorScheme.primary,
                        onClick = onImportJsonClick
                    )
                }
            }
        }

        PremiumSettingSection(title = "Pemeliharaan Penyimpanan") {
            PremiumCardGroup {
                PremiumSettingItem(
                    title = "Bersihkan Cache Installer APK",
                    subtitle = "Hapus file APK sisa update agar memori HP lega",
                    icon = Icons.Default.Delete,
                    onClick = {
                        viewModel.cleanupUpdateFilesNow()
                        Toast.makeText(context, "Cache file installer APK berhasil dibersihkan.", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun DriveBackupPicker(
    viewModel: TobaccoViewModel,
    onDismiss: () -> Unit,
    onFileSelected: (DriveBackupFile) -> Unit
) {
    val files by viewModel.driveBackupFiles.collectAsState()
    val isFetchingBackups by viewModel.isFetchingBackups.collectAsState()
    var fileToDelete by remember { mutableStateOf<DriveBackupFile?>(null) }
    var fileToRestore by remember { mutableStateOf<DriveBackupFile?>(null) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var isDeletingBackup by remember { mutableStateOf(false) }
    var isCreatingBackup by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.fetchDriveBackupFiles()
    }

    if (showDeleteAllConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingBackup) showDeleteAllConfirm = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Hapus Semua Backup?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("Semua ${files.size} daftar file cadangan di Cloud & penyimpanan lokal akan dihapus permanen. Data transaksi aktif di aplikasi saat ini tetap aman.") },
            confirmButton = {
                Button(
                    onClick = {
                        isDeletingBackup = true
                        viewModel.deleteAllCloudBackups { success ->
                            isDeletingBackup = false
                            showDeleteAllConfirm = false
                            Toast.makeText(
                                context,
                                if (success) "Semua file backup berhasil dibersihkan" else "Sebagian backup gagal dihapus",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = !isDeletingBackup,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isDeletingBackup) "Menghapus..." else "Ya, Hapus Semua", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirm = false }, enabled = !isDeletingBackup) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingBackup) fileToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Hapus Backup Cloud", fontWeight = FontWeight.ExtraBold) },
            text = { Text("Apakah Anda yakin ingin menghapus file backup '${fileToDelete!!.name}'? Tindakan ini akan menghapus cadangan secara permanen.") },
            confirmButton = {
                Button(
                    onClick = {
                        val file = fileToDelete!!
                        fileToDelete = null
                        viewModel.deleteDriveBackupFile(file) { success ->
                            Toast.makeText(
                                context,
                                if (success) "Backup berhasil dihapus" else "Gagal menghapus backup di server",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Ya, Hapus", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (fileToRestore != null) {
        val target = fileToRestore!!
        AlertDialog(
            onDismissRequest = { if (!isRestoring) fileToRestore = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Pulihkan Cadangan Cloud?", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "File: ${target.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${target.itemCountText} • ${target.source}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Seluruh data stok, transaksi nota, rencana kulakan, dan pengaturan personalisasi antarmuka (UI) akan dipulihkan sesuai isi cadangan ini. Lanjutkan?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isRestoring) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Sedang memulihkan data dari Cloud...", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isRestoring = true
                        viewModel.restoreCloudBackup(target) { success, msg ->
                            isRestoring = false
                            fileToRestore = null
                            (context as? Activity)?.runOnUiThread {
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                            if (success) {
                                onDismiss()
                            }
                        }
                    },
                    enabled = !isRestoring,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Ya, Pulihkan Sekarang", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { fileToRestore = null },
                    enabled = !isRestoring
                ) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CloudSync, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Text("Pilih Backup Cloud", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                }
                IconButton(
                    onClick = { viewModel.fetchDriveBackupFiles() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Segarkan",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            Toast.makeText(context, "Mengunduh data terbaru dari Cloud Firestore...", Toast.LENGTH_SHORT).show()
                            viewModel.restoreFromFirestore { _, msg ->
                                (context as? Activity)?.runOnUiThread { Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.FlashOn, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pulihkan Cloud Langsung", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Unduh seluruh stok & transaksi terkini dari Firestore", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!isCreatingBackup) {
                                isCreatingBackup = true
                                Toast.makeText(context, "Membuat file cadangan Cloud baru...", Toast.LENGTH_SHORT).show()
                                viewModel.createManualCloudBackup { _, msg ->
                                    isCreatingBackup = false
                                    (context as? Activity)?.runOnUiThread { Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                                }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Save, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (isCreatingBackup) "Menyimpan snapshot..." else "+ Buat Backup Cloud Baru Sekarang", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Daftar File Backup Cloud (${files.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (files.size > 1) {
                            TextButton(
                                onClick = { showDeleteAllConfirm = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "Hapus Semua",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        if (isFetchingBackups) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        }
                    }
                }

                if (isFetchingBackups && files.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Text("Memuat file backup dari Cloud...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else if (files.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Text("Belum ada file backup di Cloud", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Klik '+ Buat Backup Cloud Baru Sekarang' di atas untuk membuat file backup pertama Anda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(
                            count = files.size,
                            key = { idx ->
                                val f = files[idx]
                                "${f.id}|${f.name}|${f.ownerUid}|$idx"
                            }
                        ) { index ->
                            val file = files[index]
                            PremiumBackupItem(
                                file = file,
                                onClick = { fileToRestore = file },
                                onDelete = { fileToDelete = file }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Tutup", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun PremiumBackupItem(
    file: DriveBackupFile,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val displayDate = remember(file.modifiedTime) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(file.modifiedTime)
            if (date != null) {
                SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(date)
            } else file.modifiedTime
        } catch (e: Exception) {
            file.modifiedTime
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CloudDone, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (file.name.endsWith(".json")) file.name.substringBeforeLast(".json") else file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = file.source,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(displayDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (file.itemCountText.isNotBlank()) {
                    Text(
                        text = file.itemCountText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus Backup",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
