package com.aistudio.ecotobacco.kfzqw.ui.screens.settings

import android.app.Activity
import android.app.AppOpsManager
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.ecotobacco.kfzqw.data.remote.DriveBackupFile
import com.aistudio.ecotobacco.kfzqw.data.remote.SyncStatus
import com.aistudio.ecotobacco.kfzqw.ui.Routes
import com.aistudio.ecotobacco.kfzqw.ui.components.ThemeOptionItem
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
    val firestoreLastSync by viewModel.firestoreLastSync.collectAsState()
    val firestoreMessage by viewModel.firestoreMessage.collectAsState()

    var isAutoSync by remember { mutableStateOf(viewModel.syncHelper.isAutoSyncEnabled) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showWhatsNew by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showTextColorPicker by remember { mutableStateOf(false) }
    var showRestorePicker by remember { mutableStateOf(false) }

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
                    if (success) viewModel.syncData()
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

            // --- SECTION 1: CLOUD SYNC (Simplified & Intelligent) ---
            PremiumSettingSection(title = "Cloud Synchronization") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (syncStatus != SyncStatus.DISCONNECTED) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (syncStatus != SyncStatus.DISCONNECTED) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = if (syncStatus != SyncStatus.DISCONNECTED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (syncStatus != SyncStatus.DISCONNECTED) "Google Drive Connected" else "Google Drive Disconnected",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (syncStatus != SyncStatus.DISCONNECTED) viewModel.syncHelper.userEmail ?: "Active Account" else "Backup your data to the cloud",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        if (syncStatus == SyncStatus.DISCONNECTED) {
                            Button(
                                onClick = { googleLoginLauncher.launch(viewModel.getAuthIntent()) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Connect to Google Drive", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // Account Controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButtonPremium(
                                    text = "Sync Now",
                                    icon = Icons.Default.Sync,
                                    modifier = Modifier.weight(1f),
                                    onClick = { 
                                        viewModel.forceSyncNow { status ->
                                            (context as? Activity)?.runOnUiThread { Toast.makeText(context, status, Toast.LENGTH_SHORT).show() }
                                        }
                                    }
                                )
                                OutlinedButtonPremium(
                                    text = "Restore",
                                    icon = Icons.Default.Download,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        showRestorePicker = true
                                    }
                                )
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            
                            // Auto Sync Toggle Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Auto Sync Data", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Switch(
                                    checked = isAutoSync, 
                                    onCheckedChange = { isAutoSync = it; viewModel.syncHelper.isAutoSyncEnabled = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                                )
                            }
                            
                            TextButton(
                                onClick = { showDisconnectDialog = true },
                                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                            ) {
                                Text("Disconnect Account", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            // --- SECTION 2B: FIREBASE AUTH & CLOUD FIRESTORE ---
            PremiumSettingSection(title = "Firebase & Cloud Firestore") {
                PremiumCardGroup {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        if (firebaseUser != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (firebaseUser != null) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = "Firebase Status",
                                    tint = if (firebaseUser != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (firebaseUser != null) "Firebase Auth Terhubung" else "Firebase Auth Belum Terhubung",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (firebaseUser != null) {
                                        val email = firebaseUser?.email ?: "Tamu (${firebaseUser?.uid?.take(8)}...)"
                                        email
                                    } else "Masuk dengan Google untuk identifikasi aman & persistensi Firestore",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (firestoreLastSync != null && firestoreLastSync!! > 0) {
                                    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                                    Text(
                                        text = "Terakhir sinkron Firestore: ${sdf.format(Date(firestoreLastSync!!))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }

                        if (firebaseUser == null) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        (context as? Activity)?.let { act ->
                                            viewModel.signInWithGoogle(act) { success, msg ->
                                                act.runOnUiThread {
                                                    Toast.makeText(act, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    enabled = !isFirebaseLoading
                                ) {
                                    if (isFirebaseLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                    } else {
                                        Text("Masuk dengan Google (Firebase Auth)", fontWeight = FontWeight.Bold)
                                    }
                                }

                                OutlinedButtonPremium(
                                    text = "Masuk Cepat Sebagai Tamu (Firebase)",
                                    icon = Icons.Default.Person,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        viewModel.signInAnonymously { success, msg ->
                                            (context as? Activity)?.runOnUiThread {
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.syncFirestore { success, msg ->
                                            (context as? Activity)?.runOnUiThread {
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    enabled = !isFirestoreSyncing
                                ) {
                                    if (isFirestoreSyncing) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Menyimpan...")
                                    } else {
                                        Icon(Icons.Default.CloudSync, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Simpan Firestore")
                                    }
                                }

                                OutlinedButtonPremium(
                                    text = "Pulihkan",
                                    icon = Icons.Default.Download,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        viewModel.restoreFromFirestore { success, msg ->
                                            (context as? Activity)?.runOnUiThread {
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }

                            TextButton(
                                onClick = { viewModel.signOutFirebase() },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Keluar dari Akun Firebase", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // --- SECTION 3: PERSONALISASI UI (Gasss!) ---
            val fontSizeScale by viewModel.fontSizeScale.collectAsState()
            val paddingScale by viewModel.paddingScale.collectAsState()
            val buttonHeight by viewModel.buttonHeight.collectAsState()
            val cornerRadius by viewModel.cornerRadius.collectAsState()

            PremiumSettingSection(title = "UI Personalization") {
                PremiumCardGroup {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Font Size Slider
                        UiSliderItem(
                            label = "Ukuran Font",
                            value = fontSizeScale,
                            valueRange = 0.8f..1.5f,
                            onValueChange = { viewModel.setFontSizeScale(it) }
                        )
                        
                        // Padding Scale Slider
                        UiSliderItem(
                            label = "Jarak/Padding",
                            value = paddingScale,
                            valueRange = 0.5f..2.0f,
                            onValueChange = { viewModel.setPaddingScale(it) }
                        )

                        // Button Height Slider
                        UiSliderItem(
                            label = "Tinggi Tombol",
                            value = buttonHeight.toFloat(),
                            valueRange = 40f..80f,
                            onValueChange = { viewModel.setButtonHeight(it.toInt()) }
                        )

                        // Corner Radius Slider
                        UiSliderItem(
                            label = "Kebulatan Sudut",
                            value = cornerRadius.toFloat(),
                            valueRange = 0f..32f,
                            onValueChange = { viewModel.setCornerRadius(it.toInt()) }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        PremiumSettingItem(
                            title = "Warna Teks",
                            subtitle = "Kustomisasi warna tulisan",
                            icon = Icons.Default.Palette,
                            onClick = { showTextColorPicker = true }
                        )
                    }
                }
            }

            // --- SECTION 4: DATA LOKAL ---
            PremiumSettingSection(title = "Local Data Management") {
                PremiumCardGroup {
                    PremiumSettingItem(
                        title = "Local Backup",
                        subtitle = "Simpan cadangan .json ke folder pilihan Anda",
                        icon = Icons.Default.Save,
                        onClick = { 
                            val fileName = "eco_tobacco_backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.json"
                            createBackupLauncher.launch(fileName)
                        }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    PremiumSettingItem(
                        title = "Local Restore",
                        subtitle = "Pulihkan data dari file .json",
                        icon = Icons.Default.UploadFile,
                        onClick = { restoreLauncher.launch(arrayOf("application/json", "*/*")) }
                    )
                }
            }

            // --- SECTION 4: INFORMASI & TENTANG ---
            PremiumSettingSection(title = "About & Info") {
                PremiumCardGroup {
                    PremiumSettingItem(
                        title = "About Eco Tobacco",
                        subtitle = "Version v${Routes.APP_VERSION}",
                        icon = Icons.Default.Info,
                        onClick = { showWhatsNew = true }
                    )
                }
            }

            // --- SECTION 5: DANGER ZONE ---
            PremiumSettingSection(title = "Danger Zone") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    PremiumSettingItem(
                        title = "Reset All Data",
                        subtitle = "Permanently delete everything",
                        icon = Icons.Default.DeleteForever,
                        iconColor = MaterialTheme.colorScheme.error,
                        textColor = MaterialTheme.colorScheme.error,
                        onClick = { showResetDialog = true }
                    )
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
    if (showThemeMenu) {
        AlertDialog(
            onDismissRequest = { showThemeMenu = false },
            title = { Text("Select Theme", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeOptionItem("Light Mode", Icons.Default.LightMode, themeMode == "LIGHT") { viewModel.setThemeMode("LIGHT"); showThemeMenu = false }
                    ThemeOptionItem("Dark Mode", Icons.Default.DarkMode, themeMode == "DARK") { viewModel.setThemeMode("DARK"); showThemeMenu = false }
                    ThemeOptionItem("System Default", Icons.Default.SettingsSuggest, themeMode == "SYSTEM") { viewModel.setThemeMode("SYSTEM"); showThemeMenu = false }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeMenu = false }) { Text("Close") } },
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showColorPicker) {
        val colors = listOf("Emerald" to 0xFF10B981, "Blue" to 0xFF3B82F6, "Purple" to 0xFF8B5CF6, "Orange" to 0xFFF59E0B, "Rose" to 0xFFF43F5E, "Slate" to 0xFF475569)
        val currentCustomColor by viewModel.customColor.collectAsState()
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("Choose Accent Color", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp.scaled)) {
                    colors.chunked(3).forEach { rowColors ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            rowColors.forEach { (name, color) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { viewModel.setCustomColor(color.toInt()); showColorPicker = false }) {
                                    Surface(modifier = Modifier.size(50.dp.scaled), shape = CircleShape, color = Color(color), border = if (currentCustomColor == color.toInt()) BorderStroke(3.dp.scaled, MaterialTheme.colorScheme.onSurface) else null) {}
                                    Text(name, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp.scaled)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showColorPicker = false }) { Text("Close", fontSize = 14.sp.scaled) } },
            shape = RoundedCornerShape(28.dp.scaled)
        )
    }

    if (showTextColorPicker) {
        val colors = listOf(
            "White" to 0xFFFFFFFF, "Gray" to 0xFFCCCCCC, "Amber" to 0xFFFFBF00, 
            "Cyan" to 0xFF00FFFF, "Lime" to 0xFF00FF00, "Pink" to 0xFFFFC0CB,
            "Default" to 0
        )
        val currentTextColor by viewModel.customTextColor.collectAsState()
        AlertDialog(
            onDismissRequest = { showTextColorPicker = false },
            title = { Text("Pilih Warna Teks", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp.scaled) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp.scaled)) {
                    colors.chunked(3).forEach { rowColors ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            rowColors.forEach { (name, color) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                                    viewModel.setCustomTextColor(color.toInt())
                                    showTextColorPicker = false 
                                }) {
                                    Surface(
                                        modifier = Modifier.size(50.dp.scaled), 
                                        shape = CircleShape, 
                                        color = if (color.toLong() == 0L) MaterialTheme.colorScheme.onSurface else Color(color.toLong()), 
                                        border = if (currentTextColor == color.toInt()) BorderStroke(3.dp.scaled, MaterialTheme.colorScheme.primary) else null
                                    ) {}
                                    Text(name, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp.scaled)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTextColorPicker = false }) { Text("Tutup", fontSize = 14.sp.scaled) } },
            shape = RoundedCornerShape(28.dp.scaled)
        )
    }

    if (showWhatsNew) {
        AlertDialog(
            onDismissRequest = { showWhatsNew = false },
            title = { Text("App Information", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("ECO TOBACCO v${Routes.APP_VERSION}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("A professional tobacco procurement management app with cloud synchronization and AI-powered receipt scanning.", style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Text("Developer: ekofebriarianto", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { Button(onClick = { showWhatsNew = false }) { Text("Close") } },
            shape = RoundedCornerShape(28.dp)
        )
    }

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

    if (showRestorePicker) {
        DriveBackupPicker(
            viewModel = viewModel,
            onDismiss = { showRestorePicker = false },
            onFileSelected = { file ->
                showRestorePicker = false
                viewModel.syncData(forceOverwriteRemote = false, fileId = file.id) { status ->
                    (context as? Activity)?.runOnUiThread { Toast.makeText(context, status, Toast.LENGTH_SHORT).show() }
                }
            }
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
    var isLoading by remember { mutableStateOf(true) }
    var fileToDelete by remember { mutableStateOf<DriveBackupFile?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.fetchDriveBackupFiles()
        isLoading = false
    }

    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Hapus Backup") },
            text = { Text("Apakah Anda yakin ingin menghapus backup ini dari Google Drive? Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val file = fileToDelete!!
                        fileToDelete = null
                        viewModel.deleteDriveBackupFile(file.id) { success ->
                            (context as? Activity)?.runOnUiThread {
                                if (success) {
                                    Toast.makeText(context, "Backup berhasil dihapus", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Gagal menghapus backup", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Backup Cloud", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                } else if (files.isEmpty()) {
                    Text("Tidak ada file backup ditemukan.", modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(files.size) { index ->
                            val file = files[index]
                            PremiumBackupItem(
                                file = file,
                                onClick = { onFileSelected(file) },
                                onDelete = { fileToDelete = file }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CloudDone, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Backup Database", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(displayDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus Backup",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun UiSliderItem(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(String.format("%.2f", value), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
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
            modifier = Modifier.height(uiConfig.buttonHeight.dp),
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
                fontSize = 14.sp.scaled
            )
        }
    }
}
