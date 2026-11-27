package com.aistudio.ecotobacco.kfzqw.ui.screens.settings

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.ecotobacco.kfzqw.data.remote.SyncStatus
import com.aistudio.ecotobacco.kfzqw.ui.Routes
import com.aistudio.ecotobacco.kfzqw.ui.components.WhatsNewChangelogDialog
import com.aistudio.ecotobacco.kfzqw.ui.theme.LocalUiConfig
import com.aistudio.ecotobacco.kfzqw.viewmodel.TobaccoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SettingsCategory(
    val shortLabel: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tag: String
) {
    HUB(
        shortLabel = "Semua",
        title = "Pengaturan",
        subtitle = "Pusat kontrol & personalisasi",
        icon = Icons.Default.Apps,
        tag = "settings_cat_hub"
    ),
    THEME(
        shortLabel = "Tema",
        title = "Tema & Tampilan",
        subtitle = "Preset, warna, font & efek Matrix",
        icon = Icons.Default.Palette,
        tag = "settings_cat_theme"
    ),
    BACKUP(
        shortLabel = "Backup",
        title = "Backup & Cloud",
        subtitle = "Cloud sync, snapshot & JSON lokal",
        icon = Icons.Default.CloudSync,
        tag = "settings_cat_backup"
    ),
    DATA_WIDGET(
        shortLabel = "Data & Widget",
        title = "Data & Widget",
        subtitle = "Rekap default & widget beranda",
        icon = Icons.Default.Widgets,
        tag = "settings_cat_data"
    ),
    UPDATE_ABOUT(
        shortLabel = "Pembaruan",
        title = "Pembaruan & Info",
        subtitle = "Rilis GitHub, What's New & reset",
        icon = Icons.Default.SystemUpdate,
        tag = "settings_cat_update"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TobaccoViewModel,
    isOpen: Boolean = true,
    onNavigateBack: () -> Unit,
    onNavigateToDebug: () -> Unit
) {
    val context = LocalContext.current
    val syncStatus by viewModel.syncStatus.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val customColor by viewModel.customColor.collectAsState()
    val fontSizeScale by viewModel.fontSizeScale.collectAsState()
    val defaultReportFilter by viewModel.defaultReportFilter.collectAsState()

    val firebaseUser by viewModel.firebaseUser.collectAsState()
    val isFirestoreSyncing by viewModel.isFirestoreSyncing.collectAsState()
    val currentGitHubRepo by viewModel.githubRepo.collectAsState()
    val isAutoCheckUpdateEnabled by viewModel.autoCheckUpdateEnabled.collectAsState()
    val autoBackupInfo by viewModel.autoBackupInfo.collectAsState()

    var activeCategory by remember { mutableStateOf(SettingsCategory.HUB) }
    var isAutoSync by remember { mutableStateOf(viewModel.syncHelper.isAutoSyncEnabled) }
    var showRepoSettingsDialog by remember { mutableStateOf(false) }
    var repoSettingInput by remember(currentGitHubRepo) { mutableStateOf(currentGitHubRepo) }
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

    LaunchedEffect(isOpen) {
        if (!isOpen && activeCategory != SettingsCategory.HUB) {
            kotlinx.coroutines.delay(280L)
            activeCategory = SettingsCategory.HUB
        }
    }

    BackHandler(enabled = isOpen && activeCategory != SettingsCategory.HUB) {
        activeCategory = SettingsCategory.HUB
    }

    val isCloudConnected = (syncStatus != SyncStatus.DISCONNECTED) ||
        (firebaseUser != null) ||
        viewModel.syncHelper.isLocalUserConnected
    val cloudAccountLabel = if (isCloudConnected) {
        viewModel.syncHelper.userEmail
            ?: firebaseUser?.displayName
            ?: firebaseUser?.email
            ?: "Akun Cloud Aktif"
    } else {
        "Belum terhubung ke Cloud"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = activeCategory.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = activeCategory.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (activeCategory != SettingsCategory.HUB) {
                                    activeCategory = SettingsCategory.HUB
                                } else {
                                    onNavigateBack()
                                }
                            },
                            modifier = Modifier.testTag("settings_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (activeCategory != SettingsCategory.HUB) "Kembali ke Menu Utama Pengaturan" else "Tutup Pengaturan"
                            )
                        }
                    },
                    actions = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(CircleShape)
                                .clickable { showWhatsNew = true }
                        ) {
                            Text(
                                text = "v${Routes.APP_VERSION}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Modern Pill Navigation Strip across functional categories
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SettingsCategory.entries, key = { it.name }) { category ->
                        val isSelected = activeCategory == category
                        Surface(
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .clip(CircleShape)
                                .clickable { activeCategory = category }
                                .testTag(category.tag),
                            shape = CircleShape,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            border = if (isSelected) {
                                null
                            } else {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = category.shortLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = activeCategory,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            transitionSpec = {
                val forward = targetState.ordinal >= initialState.ordinal
                val slideIn = slideInHorizontally(tween(240)) { w -> if (forward) w / 4 else -w / 4 } +
                    fadeIn(tween(200))
                val slideOut = slideOutHorizontally(tween(200)) { w -> if (forward) -w / 5 else w / 5 } +
                    fadeOut(tween(150))
                (slideIn togetherWith slideOut).using(SizeTransform(clip = false))
            },
            label = "settings_category_transition"
        ) { category ->
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                when (category) {
                    SettingsCategory.HUB -> {
                        SettingsHubView(
                            isCloudConnected = isCloudConnected,
                            cloudAccountLabel = cloudAccountLabel,
                            themeMode = themeMode,
                            customColor = customColor,
                            fontSizeScale = fontSizeScale,
                            defaultReportFilter = defaultReportFilter,
                            lastAutoBackupTime = autoBackupInfo?.lastBackupTime ?: 0L,
                            onSelectCategory = { activeCategory = it },
                            onQuickSyncOrLogin = {
                                if (isCloudConnected) {
                                    viewModel.forceSyncNow { status ->
                                        (context as? Activity)?.runOnUiThread {
                                            Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    viewModel.syncFirestore()
                                } else {
                                    activeCategory = SettingsCategory.BACKUP
                                }
                            },
                            onQuickCheckUpdate = {
                                viewModel.checkForGitHubUpdate(currentVersion = Routes.APP_VERSION, silent = false)
                            }
                        )
                    }

                    SettingsCategory.THEME -> {
                        PremiumCardGroup {
                            Box(modifier = Modifier.padding(16.dp)) {
                                ThemeCustomizationStudio(viewModel = viewModel)
                            }
                        }
                    }

                    SettingsCategory.BACKUP -> {
                        BackupCloudSection(
                            viewModel = viewModel,
                            syncStatus = syncStatus,
                            firebaseUser = firebaseUser,
                            isFirestoreSyncing = isFirestoreSyncing,
                            isAutoSync = isAutoSync,
                            onAutoSyncChange = {
                                isAutoSync = it
                                viewModel.syncHelper.isAutoSyncEnabled = it
                            },
                            autoBackupInfo = autoBackupInfo,
                            isManualBackupInProgress = isManualBackupInProgress,
                            onManualBackupInProgressChange = { isManualBackupInProgress = it },
                            onOpenEmailLogin = { showEmailLoginDialog = true },
                            onOpenRestorePicker = { showRestorePicker = true },
                            onOpenDisconnectDialog = { showDisconnectDialog = true },
                            onOpenRestoreAutoBackup = { showRestoreAutoBackupDialog = true },
                            onExportJsonClick = {
                                val fileName = "eco_tobacco_backup_${
                                    SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                }.json"
                                createBackupLauncher.launch(fileName)
                            },
                            onImportJsonClick = {
                                restoreLauncher.launch(arrayOf("application/json", "*/*"))
                            }
                        )
                    }

                    SettingsCategory.DATA_WIDGET -> {
                        DataAndWidgetSection(viewModel = viewModel)
                    }

                    SettingsCategory.UPDATE_ABOUT -> {
                        UpdateAndAboutSection(
                            currentGitHubRepo = currentGitHubRepo,
                            isAutoCheckUpdateEnabled = isAutoCheckUpdateEnabled,
                            onOpenWhatsNew = { showWhatsNew = true },
                            onCheckUpdate = {
                                viewModel.checkForGitHubUpdate(currentVersion = Routes.APP_VERSION, silent = false)
                            },
                            onOpenRepoDialog = {
                                repoSettingInput = currentGitHubRepo
                                showRepoSettingsDialog = true
                            },
                            onOpenResetDialog = { showResetDialog = true }
                        )
                    }
                }

                // Footer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Eco Tobacco Premium",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "© 2026 ekofebriarianto",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }

    // --- Dialogs ---
    if (showWhatsNew) {
        WhatsNewChangelogDialog(
            onDismiss = { showWhatsNew = false }
        )
    }

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
                        placeholder = { Text("ekofebri01/-kulakan_ecotobacco") },
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

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Database?") },
            text = { Text("All your data will be permanently deleted. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        viewModel.clearDatabase()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("Disconnect Account?") },
            text = { Text("You will no longer be able to sync data with Google Drive until you reconnect.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDisconnectDialog = false
                        viewModel.disconnectDrive()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Disconnect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEmailLoginDialog) {
        AlertDialog(
            onDismissRequest = { showEmailLoginDialog = false },
            title = { Text("Masuk dengan Email / Akun Cloud", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Masukkan email dan kata sandi Anda untuk menghubungkan cloud storage & sinkronisasi data:",
                        style = MaterialTheme.typography.bodyMedium
                    )
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
                        visualTransformation = PasswordVisualTransformation(),
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
                                Toast.makeText(
                                    context,
                                    msg ?: if (success) "Login Berhasil" else "Login Gagal",
                                    Toast.LENGTH_SHORT
                                ).show()
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
                viewModel.restoreCloudBackup(file) { _, msg ->
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
                        viewModel.restoreFromAutoBackup(context) { _, msg ->
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
private fun SettingsHubView(
    isCloudConnected: Boolean,
    cloudAccountLabel: String,
    themeMode: String,
    customColor: Int,
    fontSizeScale: Float,
    defaultReportFilter: String,
    lastAutoBackupTime: Long,
    onSelectCategory: (SettingsCategory) -> Unit,
    onQuickSyncOrLogin: () -> Unit,
    onQuickCheckUpdate: () -> Unit
) {
    val uiConfig = LocalUiConfig.current
    val heroShape = RoundedCornerShape(uiConfig.cornerRadius.coerceAtLeast(18).dp)
    val accentColor = remember(customColor, MaterialTheme.colorScheme.primary) {
        if (customColor != 0) Color(customColor) else Color(0xFF10B981)
    }
    val themeModeLabel = remember(themeMode) {
        when (themeMode.uppercase()) {
            "DARK" -> "Gelap"
            "LIGHT" -> "Terang"
            "CYBER" -> "Cyber"
            "AMOLED" -> "AMOLED"
            else -> "Otomatis"
        }
    }
    val backupTimeLabel = remember(lastAutoBackupTime) {
        if (lastAutoBackupTime > 0L) {
            SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(lastAutoBackupTime))
        } else {
            "Belum ada"
        }
    }

    // Modern Gradient Status Hero Banner
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = heroShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isCloudConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isCloudConnected) "Cloud Sync Aktif" else "Mode Penyimpanan Lokal",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = cloudAccountLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (isCloudConnected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = if (isCloudConnected) "Terhubung" else "Offline",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isCloudConnected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onQuickSyncOrLogin)
                            .testTag("settings_hero_sync_button"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isCloudConnected) Icons.Default.Sync else Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isCloudConnected) "Sinkron Sekarang" else "Hubungkan Cloud",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(0.85f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onQuickCheckUpdate)
                            .testTag("settings_hero_update_button"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Cek Update",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    // Group 1: Tampilan & Gaya
    PremiumSettingSection(title = "Tampilan & Gaya") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ModernCategoryHubCard(
                title = "Tema & Kustomisasi UI",
                subtitle = "Mode $themeModeLabel • Teks ${(fontSizeScale * 100).toInt()}% • Warna & Matrix",
                icon = Icons.Default.Palette,
                accentColor = MaterialTheme.colorScheme.primary,
                badgeText = themeModeLabel,
                colorDots = listOf(
                    accentColor,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.tertiary
                ),
                testTag = "settings_hub_card_theme",
                onClick = { onSelectCategory(SettingsCategory.THEME) }
            )

            ModernCategoryHubCard(
                title = "Tampilan Data & Widget",
                subtitle = "Periode default Rekap Kulakan & Widget beranda",
                icon = Icons.Default.Widgets,
                accentColor = MaterialTheme.colorScheme.tertiary,
                badgeText = defaultReportFilter,
                testTag = "settings_hub_card_data",
                onClick = { onSelectCategory(SettingsCategory.DATA_WIDGET) }
            )
        }
    }

    // Group 2: Backup & Penyimpanan
    PremiumSettingSection(title = "Backup & Penyimpanan") {
        ModernCategoryHubCard(
            title = "Backup, Cloud & File JSON",
            subtitle = "Cadangan otomatis: $backupTimeLabel • Ekspor & pulihkan data",
            icon = Icons.Default.CloudSync,
            accentColor = MaterialTheme.colorScheme.secondary,
            badgeText = if (isCloudConnected) "Cloud Aktif" else "Lokal",
            testTag = "settings_hub_card_backup",
            onClick = { onSelectCategory(SettingsCategory.BACKUP) }
        )
    }

    // Group 3: Pembaruan & Keamanan
    PremiumSettingSection(title = "Sistem, Update & Keamanan") {
        ModernCategoryHubCard(
            title = "Pembaruan, Tentang & Reset",
            subtitle = "What's New, cek update GitHub & zona reset data",
            icon = Icons.Default.SystemUpdate,
            accentColor = MaterialTheme.colorScheme.primary,
            badgeText = "v${Routes.APP_VERSION}",
            testTag = "settings_hub_card_update",
            onClick = { onSelectCategory(SettingsCategory.UPDATE_ABOUT) }
        )
    }
}
