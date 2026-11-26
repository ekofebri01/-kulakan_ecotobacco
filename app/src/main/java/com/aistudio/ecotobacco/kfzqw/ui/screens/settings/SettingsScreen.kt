package com.aistudio.ecotobacco.kfzqw.ui.screens.settings

import android.app.Activity
import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.material.icons.filled.Widgets
import com.aistudio.ecotobacco.kfzqw.widget.TobaccoAppWidgetProvider
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import android.content.ClipData
import android.content.ClipboardManager
import com.aistudio.ecotobacco.kfzqw.ui.components.CloudSyncStatusBanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.ecotobacco.kfzqw.data.remote.DriveBackupFile
import com.aistudio.ecotobacco.kfzqw.data.remote.SyncStatus
import com.aistudio.ecotobacco.kfzqw.data.remote.InAppUpdateState
import com.aistudio.ecotobacco.kfzqw.data.remote.GitHubReleaseInfo
import com.aistudio.ecotobacco.kfzqw.ui.Routes
import com.aistudio.ecotobacco.kfzqw.ui.components.ThemeOptionItem
import com.aistudio.ecotobacco.kfzqw.ui.components.WhatsNewChangelogDialog
import com.aistudio.ecotobacco.kfzqw.ui.theme.LocalUiConfig
import com.aistudio.ecotobacco.kfzqw.ui.theme.scaled
import com.aistudio.ecotobacco.kfzqw.viewmodel.TobaccoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TobaccoViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDebug: () -> Unit
) {
    val context = LocalContext.current
    val syncStatus by viewModel.syncStatus.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    val firebaseUser by viewModel.firebaseUser.collectAsState()
    val isFirebaseLoading by viewModel.isFirebaseLoading.collectAsState()
    val isFirestoreSyncing by viewModel.isFirestoreSyncing.collectAsState()
    val firestoreSyncProgress by viewModel.firestoreSyncProgress.collectAsState()
    val firestoreLastSync by viewModel.firestoreLastSync.collectAsState()
    val firestoreMessage by viewModel.firestoreMessage.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val currentGitHubRepo by viewModel.githubRepo.collectAsState()
    val isAutoCheckUpdateEnabled by viewModel.autoCheckUpdateEnabled.collectAsState()

    var isAutoSync by remember { mutableStateOf<Boolean>(viewModel.syncHelper.isAutoSyncEnabled) }
    var showRepoSettingsDialog by remember { mutableStateOf(false) }
    var repoSettingInput by remember(currentGitHubRepo) { mutableStateOf(currentGitHubRepo) }
    val autoBackupInfo by viewModel.autoBackupInfo.collectAsState()
    var showRestoreAutoBackupDialog by remember { mutableStateOf(false) }
    var isManualBackupInProgress by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadAutoBackupInfo(context)
    }

    var showResetDialog by remember { mutableStateOf(false) }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showWhatsNew by remember { mutableStateOf(false) }
    var showRestorePicker by remember { mutableStateOf(false) }
    var showEmailLoginDialog by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.writeBackupToUri(context, it) }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.restoreBackupFromDevice(context, it) }
    }

    val googleLoginLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (data != null) {
            viewModel.handleOAuthRedirect(data) { success, message ->
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, message ?: "Login Selesai", Toast.LENGTH_SHORT).show()
                    if (success) {
                        viewModel.syncData()
                        viewModel.syncFirestore()
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pengaturan",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // --- SECTION 1: CLOUD & WEB DASHBOARD SYNCHRONIZATION ---
            PremiumSettingSection(title = "Cloud & Web Dashboard Sync") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CloudSyncStatusBanner(
                        viewModel = viewModel,
                        onNavigateToSettings = null
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            val isConnected = (syncStatus != SyncStatus.DISCONNECTED) || (firebaseUser != null) || viewModel.syncHelper.isLocalUserConnected
                            val accountTitle = if (isConnected) {
                                viewModel.syncHelper.userEmail ?: firebaseUser?.displayName ?: firebaseUser?.email ?: "Akun Terhubung"
                            } else "Belum Terhubung ke Cloud"

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
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            if (!isConnected) {
                                Button(
                                    onClick = { showEmailLoginDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
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
                                            Toast.makeText(context, msg ?: if (success) "Terhubung ke Cloud!" else "Gagal", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
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
                                                isManualBackupInProgress = true
                                                viewModel.createManualCloudBackup { _, msg ->
                                                    isManualBackupInProgress = false
                                                    (context as? Activity)?.runOnUiThread { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                                                }
                                            }
                                        },
                                        enabled = !isManualBackupInProgress,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                                    ) {
                                        Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(2.dp))
                                        Text("Cadang", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }

                                    OutlinedButton(
                                        onClick = { showRestorePicker = true },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1.3f),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                                    ) {
                                        Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(2.dp))
                                        Text("Pilih Backup", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.forceSyncNow { status ->
                                                (context as? Activity)?.runOnUiThread { Toast.makeText(context, status, Toast.LENGTH_SHORT).show() }
                                            }
                                            viewModel.syncFirestore()
                                        },
                                        enabled = !isSyncingActive,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(0.9f),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                                    ) {
                                        Icon(Icons.Default.Sync, null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(2.dp))
                                        Text("Sinkron", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
                                        onCheckedChange = { isAutoSync = it; viewModel.syncHelper.isAutoSyncEnabled = it }
                                    )
                                }

                                TextButton(
                                    onClick = { showDisconnectDialog = true },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text("Putuskan Hubungan Akun", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }


                }
            }

            // --- SECTION 2: TAMPILAN, PERSONALISASI UI & WIDGET ---
            PremiumSettingSection(title = "Tampilan, Personalisasi & Widget") {
                PremiumCardGroup {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ThemeCustomizationStudio(viewModel = viewModel)

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        val defaultReportFilter by viewModel.defaultReportFilter.collectAsState()
                        val reportFilterOptions = TobaccoViewModel.REPORT_FILTER_OPTIONS
                        Text("Tampilan Default Rekap Kulakan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            reportFilterOptions.chunked(3).forEach { rowOptions ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    rowOptions.forEach { option ->
                                        val isSelected = defaultReportFilter == option
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    viewModel.setDefaultReportFilter(option)
                                                    Toast.makeText(context, "Tampilan default Rekap Kulakan: $option", Toast.LENGTH_SHORT).show()
                                                },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = option,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                    repeat(3 - rowOptions.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        PremiumSettingItem(
                            title = "Pasang Widget ke Layar Utama",
                            subtitle = "Pantau stok & ringkasan dari home screen",
                            icon = Icons.Default.Widgets,
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                                    val myProvider = ComponentName(context, TobaccoAppWidgetProvider::class.java)
                                    if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                                        appWidgetManager.requestPinAppWidget(myProvider, null, null)
                                    } else {
                                        Toast.makeText(context, "Tekan lama area kosong layar utama HP Anda > pilih Widget", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Tekan lama area kosong layar utama HP Anda > pilih Widget", Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                }
            }

            // --- SECTION 3: PENYIMPANAN & CADANGAN DATA ---
            PremiumSettingSection(title = "Penyimpanan & Cadangan Data") {
                PremiumCardGroup {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(36.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Save, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Column {
                                    Text("Cadangan Otomatis Internal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    val lastTimeStr = autoBackupInfo?.lastBackupTime?.takeIf { it > 0 }?.let { SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(it)) } ?: "Belum pernah"
                                    Text("Terakhir: $lastTimeStr (${autoBackupInfo?.fileSizeFormatted ?: "0 B"})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButtonPremium(
                                text = "Cadangkan",
                                icon = Icons.Default.Save,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    isManualBackupInProgress = true
                                    viewModel.performManualAutoBackup(context) { _, msg ->
                                        isManualBackupInProgress = false
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            OutlinedButtonPremium(
                                text = "Pulihkan",
                                icon = Icons.Default.Download,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if ((autoBackupInfo?.lastBackupTime ?: 0L) > 0L) showRestoreAutoBackupDialog = true
                                    else Toast.makeText(context, "Belum ada file cadangan otomatis", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        PremiumSettingItem(
                            title = "Ekspor File JSON ke Perangkat",
                            subtitle = "Simpan file cadangan .json ke penyimpanan HP",
                            icon = Icons.Default.Upload,
                            onClick = {
                                val fileName = "eco_tobacco_backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.json"
                                createBackupLauncher.launch(fileName)
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        PremiumSettingItem(
                            title = "Pulihkan dari Perangkat",
                            subtitle = "Pilih & impor file cadangan .json dari HP Anda",
                            icon = Icons.Default.UploadFile,
                            iconColor = MaterialTheme.colorScheme.primary,
                            onClick = {
                                restoreLauncher.launch(arrayOf("application/json", "*/*"))
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

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

            // --- SECTION 4: TENTANG, PEMBARUAN GITHUB & KEAMANAN ---
            PremiumSettingSection(title = "Tentang, Pembaruan & Keamanan") {
                PremiumCardGroup {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PremiumSettingItem(
                            title = "Eco Tobacco v${Routes.APP_VERSION}",
                            subtitle = "Ketuk untuk melihat What's New & Changelog",
                            icon = Icons.Default.Info,
                            onClick = { showWhatsNew = true }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        PremiumSettingItem(
                            title = "Cek Pembaruan GitHub",
                            subtitle = "In-App Updater & Pembersih Installer Otomatis",
                            icon = Icons.Default.CloudDownload,
                            iconColor = MaterialTheme.colorScheme.primary,
                            onClick = { viewModel.checkForGitHubUpdate(currentVersion = Routes.APP_VERSION, silent = false) }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        PremiumSettingItem(
                            title = "Target Repository GitHub",
                            subtitle = currentGitHubRepo,
                            icon = Icons.Default.Computer,
                            onClick = { repoSettingInput = currentGitHubRepo; showRepoSettingsDialog = true }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        PremiumSettingItem(
                            title = "Reset Semua Data (Danger Zone)",
                            subtitle = "Hapus permanen seluruh data lokal aplikasi",
                            icon = Icons.Default.DeleteForever,
                            iconColor = MaterialTheme.colorScheme.error,
                            textColor = MaterialTheme.colorScheme.error,
                            onClick = { showResetDialog = true }
                        )
                    }
                }
            }
            
            // Footer
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Eco Tobacco Premium", 
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    "© 2026 ekofebriarianto",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
            
            Spacer(Modifier.height(40.dp))
        }
    }

    // --- Dialogs ---
    if (showWhatsNew) {
        WhatsNewChangelogDialog(
            onDismiss = { showWhatsNew = false }
        )
    }

    // --- DIALOG: TARGET REPO CONFIGURATION ---
    if (showRepoSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showRepoSettingsDialog = false },
            title = { Text("Pengaturan GitHub Update", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Masukkan target GitHub repository untuk auto-check rilis terbaru (format: owner/repo):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = repoSettingInput,
                        onValueChange = { repoSettingInput = it },
                        label = { Text("Repository Target") },
                        placeholder = { Text("ekofebriarianto/eco-tobacco") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-cek saat aplikasi dibuka", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = isAutoCheckUpdateEnabled,
                            onCheckedChange = { viewModel.setAutoCheckUpdateEnabled(it) }
                        )
                    }
                    Text(
                        "File installer APK yang telah diunduh akan otomatis dibersihkan setelah dipasang.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (repoSettingInput.isNotBlank()) {
                        viewModel.setGitHubRepo(repoSettingInput)
                    }
                    showRepoSettingsDialog = false
                    Toast.makeText(context, "Target repository disimpan.", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRepoSettingsDialog = false }) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    // In-App Update dialogs are rendered globally via InAppUpdateHost in MainActivity.kt

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Database?") },
            text = { Text("All your data will be permanently deleted. This action cannot be undone.") },
            confirmButton = { Button(onClick = { showResetDialog = false; viewModel.clearDatabase() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete Everything") } },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("Disconnect Account?") },
            text = { Text("You will no longer be able to sync data with Google Drive until you reconnect.") },
            confirmButton = { Button(onClick = { showDisconnectDialog = false; viewModel.disconnectDrive() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Disconnect") } },
            dismissButton = { TextButton(onClick = { showDisconnectDialog = false }) { Text("Cancel") } }
        )
    }

    if (showEmailLoginDialog) {
        AlertDialog(
            onDismissRequest = { showEmailLoginDialog = false },
            title = { Text("Masuk dengan Email / Akun Cloud", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Masukkan email dan kata sandi Anda untuk menghubungkan cloud storage & sinkronisasi data:", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Kata Sandi (Min. 6 Karakter)") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (emailInput.isNotBlank() && passwordInput.length >= 6) {
                            showEmailLoginDialog = false
                            Toast.makeText(context, "Memproses login...", Toast.LENGTH_SHORT).show()
                            viewModel.signInWithEmail(emailInput, passwordInput) { success, msg ->
                                Toast.makeText(context, msg ?: if (success) "Login Berhasil" else "Login Gagal", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Masukkan email yang valid & sandi minimal 6 karakter", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Masuk / Daftar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmailLoginDialog = false }) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showRestorePicker) {
        DriveBackupPicker(
            viewModel = viewModel,
            onDismiss = { showRestorePicker = false },
            onFileSelected = { file ->
                showRestorePicker = false
                viewModel.restoreCloudBackup(file) { success, msg ->
                    (context as? Activity)?.runOnUiThread { Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                }
            }
        )
    }

    if (showRestoreAutoBackupDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreAutoBackupDialog = false },
            icon = { Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Pulihkan dari Cadangan Otomatis?", fontWeight = FontWeight.Bold) },
            text = {
                val timeStr = autoBackupInfo?.lastBackupTime?.takeIf { it > 0 }?.let {
                    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(it))
                } ?: "Terbaru"
                Text(
                    "Aplikasi akan memulihkan data dari cadangan otomatis internal ($timeStr). Data yang ada akan diperbarui secara aman tanpa menghapus riwayat transaksi.\n\nLanjutkan pemulihan?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreAutoBackupDialog = false
                        viewModel.restoreFromAutoBackup(context) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Ya, Pulihkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreAutoBackupDialog = false }) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
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

    // Modal konfirmasi hapus file backup
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

    // Modal konfirmasi pulihkan (Restore) file backup terpilih
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
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Quick Action 1: Pulihkan Langsung dari Firestore
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().clickable {
                        onDismiss()
                        Toast.makeText(context, "Mengunduh data terbaru dari Cloud Firestore...", Toast.LENGTH_SHORT).show()
                        viewModel.restoreFromFirestore { success, msg ->
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

                // Top Quick Action 2: Buat Cadangan Baru Sekarang
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (!isCreatingBackup) {
                            isCreatingBackup = true
                            Toast.makeText(context, "Membuat file cadangan Cloud baru...", Toast.LENGTH_SHORT).show()
                            viewModel.createManualCloudBackup { success, msg ->
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
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
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
                            Text("Klik '+ Buat Backup Cloud Baru Sekarang' di atas untuk membuat file backup pertama Anda.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
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
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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

@Composable
fun UiSliderItem(
    label: String,
    value: Float,
    displayValue: String,
    icon: ImageVector,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    steps: Int = 0,
    presets: List<Pair<Float, String>> = emptyList()
) {
    val uiConfig = LocalUiConfig.current
    Surface(
        shape = RoundedCornerShape((uiConfig.cornerRadius / 1.5f).toInt().coerceAtLeast(10).dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp.scaled, vertical = 12.dp.scaled),
            verticalArrangement = Arrangement.spacedBy(8.dp.scaled)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp.scaled)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(32.dp.scaled)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp.scaled)
                            )
                        }
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp.scaled,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = displayValue,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp.scaled,
                        modifier = Modifier.padding(horizontal = 8.dp.scaled, vertical = 4.dp.scaled)
                    )
                }
            }

            if (presets.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp.scaled)
                ) {
                    for ((presetVal, presetLabel) in presets) {
                        val isSelected = kotlin.math.abs(value - presetVal) < 0.04f
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onValueChange(presetVal) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 5.dp.scaled),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = presetLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 10.sp.scaled,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PremiumSettingSection(title: String, content: @Composable () -> Unit) {
    val uiConfig = LocalUiConfig.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp.scaled)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp.scaled
            ),
            modifier = Modifier.padding(start = 8.dp.scaled)
        )
        content()
    }
}

@Composable
fun PremiumCardGroup(content: @Composable ColumnScope.() -> Unit) {
    val uiConfig = LocalUiConfig.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(uiConfig.cornerRadius.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun PremiumSettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    val uiConfig = LocalUiConfig.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp.scaled),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape((uiConfig.cornerRadius / 2).dp),
            color = iconColor.copy(alpha = 0.1f),
            modifier = Modifier.size(40.dp.scaled)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp.scaled))
            }
        }
        Spacer(Modifier.width(16.dp.scaled))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title, 
                style = MaterialTheme.typography.bodyLarge, 
                fontWeight = FontWeight.SemiBold, 
                color = textColor,
                fontSize = 16.sp.scaled
            )
            Text(
                subtitle, 
                style = MaterialTheme.typography.bodySmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp.scaled
            )
        }
    }
}

@Composable
fun OutlinedButtonPremium(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiConfig = LocalUiConfig.current
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape((uiConfig.cornerRadius / 1.5).toInt().dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = uiConfig.buttonHeight.dp)
                .padding(horizontal = 12.dp.scaled, vertical = 6.dp.scaled),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp.scaled), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp.scaled))
            Text(
                text = text, 
                style = MaterialTheme.typography.labelLarge, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp.scaled,
                maxLines = 1
            )
        }
    }
}
