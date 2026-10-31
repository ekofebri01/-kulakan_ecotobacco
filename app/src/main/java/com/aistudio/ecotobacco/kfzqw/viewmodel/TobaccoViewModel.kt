package com.aistudio.ecotobacco.kfzqw.viewmodel

import android.app.Activity
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.ecotobacco.kfzqw.data.firebase.FirebaseAuthHelper
import com.aistudio.ecotobacco.kfzqw.data.firebase.FirebaseFirestoreSyncHelper
import com.aistudio.ecotobacco.kfzqw.data.local.entities.CategoryEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProcurementItemEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProcurementPlanEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProcurementPlanWithItems
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProductEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.TransactionEntity
import com.aistudio.ecotobacco.kfzqw.data.remote.AppSettingsSyncModel
import com.aistudio.ecotobacco.kfzqw.data.remote.AutoBackupInfo
import com.aistudio.ecotobacco.kfzqw.data.remote.DriveBackupFile
import com.aistudio.ecotobacco.kfzqw.data.remote.GoogleDriveSyncHelper
import com.aistudio.ecotobacco.kfzqw.data.remote.ProcurementItemSyncModel
import com.aistudio.ecotobacco.kfzqw.data.remote.ProcurementPlanSyncModel
import com.aistudio.ecotobacco.kfzqw.data.remote.ProductSyncModel
import com.aistudio.ecotobacco.kfzqw.data.remote.SyncDataBundle
import com.aistudio.ecotobacco.kfzqw.data.remote.SyncStatus
import com.aistudio.ecotobacco.kfzqw.data.remote.TransactionSyncModel
import com.aistudio.ecotobacco.kfzqw.data.remote.GitHubUpdateManager
import com.aistudio.ecotobacco.kfzqw.data.remote.GitHubReleaseInfo
import com.aistudio.ecotobacco.kfzqw.data.remote.InAppUpdateState
import com.aistudio.ecotobacco.kfzqw.data.repository.TobaccoRepository
import com.aistudio.ecotobacco.kfzqw.data.utils.AppLogger
import com.aistudio.ecotobacco.kfzqw.data.utils.LocalScanner
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Duration.Companion.milliseconds

class TobaccoViewModel(
    private val repository: TobaccoRepository,
    application: Application
) : AndroidViewModel(application) {

    val syncHelper by lazy { GoogleDriveSyncHelper(getApplication()) }
    val firebaseAuth by lazy { FirebaseAuthHelper(getApplication()) }
    val firestoreSync by lazy { FirebaseFirestoreSyncHelper(getApplication()) }

    val firebaseUser = firebaseAuth.currentUser
    val isFirebaseLoading = firebaseAuth.isLoading
    val firebaseAuthError = firebaseAuth.authError
    val isFirestoreSyncing = firestoreSync.isSyncing
    val firestoreSyncStatus = firestoreSync.syncStatus
    val firestoreLastErrorMessage = firestoreSync.lastErrorMessage
    val firestoreSyncProgress = firestoreSync.syncProgress
    val firestoreLastSync = firestoreSync.lastSyncedTime
    val firestoreMessage = firestoreSync.syncMessage

    val githubUpdateManager by lazy { GitHubUpdateManager(getApplication()) }
    val updateState: StateFlow<InAppUpdateState> = githubUpdateManager.updateState

    private val appPrefs = getApplication<Application>().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    
    // RISC Server URL - In production this should be your backend address
    private val RISC_STATUS_URL = "https://your-risc-backend.com/risc/status"

    // --- Theme Settings ---
    private val _themeMode = MutableStateFlow(appPrefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _customColor = MutableStateFlow(appPrefs?.getInt("custom_color", 0xFF10B981.toInt()) ?: 0xFF10B981.toInt())
    val customColor: StateFlow<Int> = _customColor.asStateFlow()

    private val _customUnits = MutableStateFlow(appPrefs?.getStringSet("custom_units", setOf("kg", "ons", "pcs"))?.toList()?.sorted() ?: listOf("kg", "ons", "pcs"))
    val customUnits: StateFlow<List<String>> = _customUnits.asStateFlow()

    private val _customSubCategories = MutableStateFlow(
        appPrefs?.getStringSet("custom_sub_categories", setOf("Tembakau", "Alat", "Papir", "Kemasan", "Umum"))?.toList()?.sorted()
            ?: listOf("Tembakau", "Alat", "Papir", "Kemasan", "Umum")
    )
    val customSubCategories: StateFlow<List<String>> = _customSubCategories.asStateFlow()

    fun addCustomSubCategory(subCat: String) {
        val current = _customSubCategories.value.toMutableSet()
        if (current.add(subCat)) {
            val newList = current.toList().sorted()
            _customSubCategories.value = newList
            appPrefs?.edit { putStringSet("custom_sub_categories", current) }
        }
    }

    fun updateCustomSubCategory(oldSub: String, newSub: String) {
        val current = _customSubCategories.value.toMutableSet()
        if (current.remove(oldSub)) {
            current.add(newSub)
            val newList = current.toList().sorted()
            _customSubCategories.value = newList
            appPrefs?.edit { putStringSet("custom_sub_categories", current) }
        }
    }

    fun deleteCustomSubCategory(subCat: String) {
        val current = _customSubCategories.value.toMutableSet()
        if (current.remove(subCat)) {
            val newList = current.toList().sorted()
            _customSubCategories.value = newList
            appPrefs?.edit { putStringSet("custom_sub_categories", current) }
        }
    }

    // --- UI Customization Settings ---
    private val _fontSizeScale = MutableStateFlow(appPrefs.getFloat("ui_font_size_scale", 1.0f))
    val fontSizeScale = _fontSizeScale.asStateFlow()

    private val _paddingScale = MutableStateFlow(appPrefs.getFloat("ui_padding_scale", 1.0f))
    val paddingScale = _paddingScale.asStateFlow()

    private val _buttonHeight = MutableStateFlow(appPrefs.getInt("ui_button_height", 56))
    val buttonHeight = _buttonHeight.asStateFlow()

    private val _cornerRadius = MutableStateFlow(appPrefs.getInt("ui_corner_radius", 16))
    val cornerRadius = _cornerRadius.asStateFlow()

    private val _customTextColor = MutableStateFlow(appPrefs.getInt("ui_custom_text_color", 0)) // 0 means use theme default
    val customTextColor = _customTextColor.asStateFlow()

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        appPrefs.edit { putString("theme_mode", mode) }
        triggerAutoSync()
    }

    fun setCustomColor(colorInt: Int) {
        _customColor.value = colorInt
        appPrefs?.edit { putInt("custom_color", colorInt) }
        triggerAutoSync()
    }

    fun setFontSizeScale(scale: Float) {
        _fontSizeScale.value = scale
        appPrefs.edit { putFloat("ui_font_size_scale", scale) }
        triggerAutoSync()
    }

    fun setPaddingScale(scale: Float) {
        _paddingScale.value = scale
        appPrefs.edit { putFloat("ui_padding_scale", scale) }
        triggerAutoSync()
    }

    fun setButtonHeight(height: Int) {
        _buttonHeight.value = height
        appPrefs.edit { putInt("ui_button_height", height) }
        triggerAutoSync()
    }

    fun setCornerRadius(radius: Int) {
        _cornerRadius.value = radius
        appPrefs.edit { putInt("ui_corner_radius", radius) }
        triggerAutoSync()
    }

    fun setCustomTextColor(colorInt: Int) {
        _customTextColor.value = colorInt
        appPrefs.edit { putInt("ui_custom_text_color", colorInt) }
        triggerAutoSync()
    }

    fun resetUiCustomization() {
        _fontSizeScale.value = 1.0f
        appPrefs.edit { putFloat("ui_font_size_scale", 1.0f) }
        _paddingScale.value = 1.0f
        appPrefs.edit { putFloat("ui_padding_scale", 1.0f) }
        _buttonHeight.value = 56
        appPrefs.edit { putInt("ui_button_height", 56) }
        _cornerRadius.value = 16
        appPrefs.edit { putInt("ui_corner_radius", 16) }
        _customTextColor.value = 0
        appPrefs.edit { putInt("ui_custom_text_color", 0) }
        triggerAutoSync()
    }

    // --- Web Dashboard Sync States ---
    private val _isWebSyncing = MutableStateFlow(false)
    val isWebSyncing: StateFlow<Boolean> = _isWebSyncing.asStateFlow()

    private val _lastWebSyncTime = MutableStateFlow<Long?>(
        appPrefs?.getLong("last_web_sync_time", -1L)?.takeIf { it > 0 }
    )
    val lastWebSyncTime: StateFlow<Long?> = _lastWebSyncTime.asStateFlow()

    fun addCustomUnit(unit: String) {
        val current = _customUnits.value.toMutableSet()
        if (current.add(unit)) {
            val newList = current.toList().sorted()
            _customUnits.value = newList
            appPrefs?.edit { putStringSet("custom_units", current) }
        }
    }

    fun updateCustomUnit(oldUnit: String, newUnit: String) {
        val current = _customUnits.value.toMutableSet()
        if (current.remove(oldUnit)) {
            current.add(newUnit)
            val newList = current.toList().sorted()
            _customUnits.value = newList
            appPrefs?.edit { putStringSet("custom_units", current) }
        }
    }

    fun deleteCustomUnit(unit: String) {
        val current = _customUnits.value.toMutableSet()
        if (current.remove(unit)) {
            val newList = current.toList().sorted()
            _customUnits.value = newList
            appPrefs?.edit { putStringSet("custom_units", current) }
        }
    }

    // --- StateFlows ---
    val categories = repository
        .getAllCategories().stateIn(viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList())
    val products = repository.allProducts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val transactions = repository.allTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val procurementPlans = repository.allProcurementPlans.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Dashboard Stats ---
    val totalSpentThisMonth = transactions.map { list ->
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        list.filter {
            calendar.timeInMillis = it.date
            calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
        }.sumOf { it.total }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val transactionCountThisMonth = transactions.map { list ->
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        list.count {
            calendar.timeInMillis = it.date
            calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val productCount = products.map { it.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _syncStatus = MutableStateFlow(
        if (syncHelper.isConnected) {
            if (syncHelper.isOnline) SyncStatus.SYNCED else SyncStatus.OFFLINE
        } else SyncStatus.DISCONNECTED
    )
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _driveBackupFiles = MutableStateFlow<List<DriveBackupFile>>(emptyList())
    val driveBackupFiles: StateFlow<List<DriveBackupFile>> = _driveBackupFiles.asStateFlow()

    private val _autoBackupInfo = MutableStateFlow<AutoBackupInfo?>(null)
    val autoBackupInfo: StateFlow<AutoBackupInfo?> = _autoBackupInfo.asStateFlow()

    fun getBackupFileCandidates(context: Context): List<File> {
        val list = mutableListOf<File>()

        // 1. App-specific external files dir
        try {
            context.getExternalFilesDir("backups")?.let {
                list.add(File(it, "eco_tobacco_sync_data.json"))
            }
        } catch (e: Exception) { /* ignore */ }

        // 2. App internal files dir (always accessible sandbox)
        val internalDir = File(context.filesDir, "backups")
        list.add(File(internalDir, "eco_tobacco_sync_data.json"))

        return list
    }

    fun getBackupFile(context: Context): File {
        val candidates = getBackupFileCandidates(context)
        val existing = candidates.firstOrNull { it.exists() && it.length() > 0L }
        if (existing != null) return existing

        val internalDir = File(context.filesDir, "backups")
        if (!internalDir.exists()) internalDir.mkdirs()
        return File(internalDir, "eco_tobacco_sync_data.json")
    }

    fun loadAutoBackupInfo(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val candidates = getBackupFileCandidates(context)
            val targetFile = candidates.filter { it.exists() && it.length() > 0L }
                .maxByOrNull { it.lastModified() }

            if (targetFile != null && targetFile.exists() && targetFile.length() > 0L) {
                val sizeBytes = targetFile.length()
                val sizeFormatted = when {
                    sizeBytes < 1024 -> "$sizeBytes B"
                    sizeBytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f KB", sizeBytes / 1024f)
                    else -> String.format(Locale.getDefault(), "%.1f MB", sizeBytes / (1024f * 1024f))
                }

                var pCount = 0
                var txCount = 0
                var plCount = 0
                try {
                    val text = targetFile.readText()
                    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                    val adapter = moshi.adapter(SyncDataBundle::class.java)
                    val bundle = adapter.fromJson(text)
                    if (bundle != null) {
                        pCount = bundle.products.size
                        txCount = bundle.transactions.size
                        plCount = bundle.procurementPlans?.size ?: 0
                    }
                } catch (e: Exception) {
                    AppLogger.w("Backup", "Quick read auto backup info: ${e.message}")
                }

                _autoBackupInfo.value = AutoBackupInfo(
                    lastBackupTime = targetFile.lastModified(),
                    fileSizeFormatted = sizeFormatted,
                    productCount = pCount,
                    transactionCount = txCount,
                    planCount = plCount,
                    filePath = targetFile.absolutePath,
                    isAutoBackupActive = true
                )
            } else {
                _autoBackupInfo.value = AutoBackupInfo(
                    lastBackupTime = 0L,
                    fileSizeFormatted = "0 B",
                    productCount = 0,
                    transactionCount = 0,
                    planCount = 0,
                    filePath = "",
                    isAutoBackupActive = true
                )
            }
        } catch (e: Exception) {
            AppLogger.e("Backup", "Gagal memuat auto backup info", e)
        }
    }

    fun saveToLocalCloudMirror(fileName: String, jsonData: String) {
        try {
            val dir = File(getApplication<Application>().filesDir, "cloud_backups")
            if (!dir.exists()) dir.mkdirs()
            File(dir, fileName).writeText(jsonData)
        } catch (e: Exception) {
            AppLogger.w("Backup", "Local cloud mirror write failed: ${e.message}")
        }
    }

    fun listLocalCloudMirrorBackups(): List<DriveBackupFile> {
        val list = mutableListOf<DriveBackupFile>()
        try {
            val dir = File(getApplication<Application>().filesDir, "cloud_backups")
            if (dir.exists()) {
                val files = dir.listFiles { f -> f.extension == "json" && f.length() > 0L }
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                files?.sortedByDescending { it.lastModified() }?.forEach { f ->
                    val jsonContent = try { f.readText() } catch (e: Exception) { null }
                    var itemCountText = "Cadangan Cloud"
                    if (jsonContent != null) {
                        try {
                            val obj = JSONObject(jsonContent)
                            val txCount = obj.optJSONArray("transactions")?.length() ?: 0
                            val prodCount = obj.optJSONArray("products")?.length() ?: 0
                            val planCount = obj.optJSONArray("procurementPlans")?.length() ?: 0
                            itemCountText = "$prodCount Produk • $txCount Transaksi" + (if (planCount > 0) " • $planCount Rencana" else "")
                        } catch (e: Exception) { /* ignore */ }
                    }
                    list.add(
                        DriveBackupFile(
                            id = "local_cloud_${f.name}",
                            name = f.name,
                            modifiedTime = sdf.format(Date(f.lastModified())),
                            source = "Cloud Firestore",
                            itemCountText = itemCountText,
                            jsonData = jsonContent
                        )
                    )
                }
            }
        } catch (e: Exception) {
            AppLogger.w("Backup", "listLocalCloudMirrorBackups failed: ${e.message}")
        }
        return list
    }

    fun deleteLocalCloudMirrorBackup(fileName: String) {
        try {
            val dir = File(getApplication<Application>().filesDir, "cloud_backups")
            val target = File(dir, fileName)
            if (target.exists()) target.delete()
        } catch (e: Exception) { /* ignore */ }
    }

    private val _isFetchingBackups = MutableStateFlow(false)
    val isFetchingBackups: StateFlow<Boolean> = _isFetchingBackups.asStateFlow()

    fun fetchDriveBackupFiles() = viewModelScope.launch(Dispatchers.IO) {
        _isFetchingBackups.value = true
        val results = mutableListOf<DriveBackupFile>()
        try {
            // 1. Fetch from Firestore if user has an active Cloud session / Firebase UID
            var uid = firebaseAuth.uid ?: syncHelper.userGoogleSub ?: syncHelper.userEmail
            if (uid.isNullOrBlank() && syncHelper.isLocalUserConnected) {
                uid = syncHelper.userEmail
            }
            val uidsToQuery = mutableListOf<String>()
            if (!uid.isNullOrBlank()) uidsToQuery.add(uid)
            if (!uidsToQuery.contains("eco_guest_user")) uidsToQuery.add("eco_guest_user")

            for (u in uidsToQuery) {
                try {
                    val firestoreBackups = firestoreSync.listCloudBackups(u)
                    for (fb in firestoreBackups) {
                        if (results.none { it.id == fb.id || it.name == fb.name }) {
                            results.add(fb)
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.w("Backup", "Fetch firestore backups notice: ${e.message}")
                }
            }

            // 2. Fetch from Local Cloud Backups Mirror (context.filesDir/cloud_backups/)
            val localCloudBackups = listLocalCloudMirrorBackups()
            for (localBkp in localCloudBackups) {
                if (results.none { it.id == localBkp.id || it.name == localBkp.name }) {
                    results.add(localBkp)
                }
            }

            // 3. Fetch from Google Drive if connected
            if (syncHelper.isConnected) {
                try {
                    val driveFiles = syncHelper.listBackupFiles()
                    for (df in driveFiles) {
                        if (results.none { it.id == df.id }) {
                            results.add(df)
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.w("Backup", "Fetch Drive files notice: ${e.message}")
                }
            }

            // 4. Fallback: If still empty, check auto backup file on device
            val autoBackupFile = getBackupFile(getApplication())
            if (autoBackupFile.exists() && autoBackupFile.length() > 0L) {
                if (results.none { it.id == "local_auto_backup" || it.name == "eco_tobacco_sync_data_auto.json" }) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    results.add(
                        DriveBackupFile(
                            id = "local_auto_backup",
                            name = "eco_tobacco_sync_data_auto.json",
                            modifiedTime = sdf.format(Date(autoBackupFile.lastModified())),
                            source = "Penyimpanan Cadangan Lokal",
                            itemCountText = "Cadangan Otomatis Internal",
                            jsonData = try { autoBackupFile.readText() } catch (e: Exception) { null }
                        )
                    )
                }
            }
        } finally {
            withContext(Dispatchers.Main) {
                _driveBackupFiles.value = results
                _isFetchingBackups.value = false
            }
        }
    }

    fun deleteDriveBackupFile(file: DriveBackupFile, onComplete: (Boolean) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        // Optimistic UI update so it instantly disappears from UI
        _driveBackupFiles.value = _driveBackupFiles.value.filter { it.id != file.id }
        
        var success = true
        try {
            if (file.source == "Cloud Firestore") {
                val uid = firebaseAuth.uid ?: syncHelper.userGoogleSub ?: syncHelper.userEmail
                if (!uid.isNullOrBlank()) {
                    firestoreSync.deleteCloudBackup(uid, file.id)
                }
                deleteLocalCloudMirrorBackup(file.name)
            } else if (file.source == "Google Drive") {
                success = syncHelper.deleteFile(file.id)
            } else if (file.id == "local_auto_backup" || file.source.contains("Lokal")) {
                val autoFile = getBackupFile(getApplication())
                if (autoFile.exists()) autoFile.delete()
                deleteLocalCloudMirrorBackup(file.name)
            } else {
                deleteLocalCloudMirrorBackup(file.name)
            }
        } catch (e: Exception) {
            AppLogger.w("Backup", "deleteDriveBackupFile error: ${e.message}")
            success = false
        }
        
        kotlinx.coroutines.delay(400)
        fetchDriveBackupFiles()
        withContext(Dispatchers.Main) {
            onComplete(success)
        }
    }

    fun deleteDriveBackupFile(fileId: String, onComplete: (Boolean) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        val target = _driveBackupFiles.value.find { it.id == fileId }
        if (target != null) {
            deleteDriveBackupFile(target, onComplete)
        } else {
            val success = syncHelper.deleteFile(fileId)
            fetchDriveBackupFiles()
            withContext(Dispatchers.Main) {
                onComplete(success)
            }
        }
    }

    fun restoreCloudBackup(file: DriveBackupFile, onComplete: (Boolean, String) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        try {
            if (file.id == "firestore_latest_sync") {
                restoreFromFirestore { success, msg ->
                    onComplete(success, msg)
                }
                return@launch
            }

            var json = file.jsonData

            // 1. If JSON is not in memory, check local cloud mirror folder
            if (json.isNullOrBlank()) {
                try {
                    val mirrorDir = File(getApplication<Application>().filesDir, "cloud_backups")
                    val mirrorFile = File(mirrorDir, file.name)
                    if (mirrorFile.exists() && mirrorFile.length() > 0L) {
                        json = mirrorFile.readText()
                    }
                } catch (e: Exception) {
                    AppLogger.w("Restore", "Local mirror read notice: ${e.message}")
                }
            }

            // 2. Check local auto backup file
            if (json.isNullOrBlank() && (file.id == "local_auto_backup" || file.name.contains("auto"))) {
                try {
                    val autoFile = getBackupFile(getApplication())
                    if (autoFile.exists() && autoFile.length() > 0L) {
                        json = autoFile.readText()
                    }
                } catch (e: Exception) { /* ignore */ }
            }

            // 3. If Firestore source, query Firestore across possible user IDs
            if (json.isNullOrBlank() && (file.source == "Cloud Firestore" || file.id.startsWith("cloud_backup_") || file.id.startsWith("firestore_"))) {
                val uidsToTry = mutableListOf<String>()
                firebaseAuth.uid?.let { if (it.isNotBlank()) uidsToTry.add(it) }
                syncHelper.userGoogleSub?.let { if (it.isNotBlank() && !uidsToTry.contains(it)) uidsToTry.add(it) }
                syncHelper.userEmail?.let { if (it.isNotBlank() && !uidsToTry.contains(it)) uidsToTry.add(it) }
                if (!uidsToTry.contains("eco_guest_user")) uidsToTry.add("eco_guest_user")

                for (u in uidsToTry) {
                    try {
                        val fetchedJson = firestoreSync.getCloudBackupJson(u, file.id)
                        if (!fetchedJson.isNullOrBlank()) {
                            json = fetchedJson
                            break
                        }
                    } catch (e: Exception) {
                        AppLogger.w("Restore", "Try fetch backup JSON from Firestore user $u: ${e.message}")
                    }
                }
            }

            // 4. If Google Drive source
            if (json.isNullOrBlank() && file.source == "Google Drive") {
                try {
                    json = syncHelper.downloadBackupSync(file.id)
                } catch (e: Exception) {
                    AppLogger.w("Restore", "Drive download notice: ${e.message}")
                }
            }

            // 5. Fallback: Directly pull latest live dataset from Firestore
            if (json.isNullOrBlank()) {
                var uid = firebaseAuth.uid ?: syncHelper.userGoogleSub ?: syncHelper.userEmail ?: "eco_guest_user"
                val res = firestoreSync.fetchAndRestoreFromFirestore(uid, repository)
                withContext(Dispatchers.Main) {
                    if (res.isSuccess) {
                        val s = res.getOrNull()
                        onComplete(true, "Berhasil memulihkan ${s?.restoredProducts ?: 0} produk & ${s?.restoredTransactions ?: 0} transaksi dari Cloud Firestore")
                    } else {
                        onComplete(false, "File cadangan tidak ditemukan di Cloud")
                    }
                }
                return@launch
            }

            val res = processRestoreDataInternal(getApplication(), json)
            withContext(Dispatchers.Main) {
                onComplete(res.success, res.message)
            }
            if (res.success) {
                triggerAutoSync(forceOverwriteRemote = true)
            }
        } catch (e: Exception) {
            AppLogger.e("Restore", "Error restoring cloud backup", e)
            withContext(Dispatchers.Main) {
                onComplete(false, "Gagal memulihkan: ${e.localizedMessage ?: "Terjadi kesalahan"}")
            }
        }
    }

    fun createManualCloudBackup(onComplete: (Boolean, String) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val jsonData = generateBackupJson()
            val currentProducts = products.first()
            val currentTx = transactions.first()
            val currentPlans = procurementPlans.first()
            val currentCategories = categories.first()
            val now = System.currentTimeMillis()
            val backupName = "eco_tobacco_cloud_backup.json"

            // 1. Mirror locally
            saveToLocalCloudMirror(backupName, jsonData)

            // 2. Upload to Firestore
            var uid = firebaseAuth.uid ?: syncHelper.userGoogleSub ?: syncHelper.userEmail
            if (uid.isNullOrBlank() && syncHelper.isLocalUserConnected) {
                uid = syncHelper.userEmail
            }
            if (uid.isNullOrBlank()) {
                syncHelper.setLocalUserSession("eco_guest_user@ecotobacco.cloud")
                uid = "eco_guest_user"
            }

            val fsResult = firestoreSync.syncAllToFirestore(
                userId = uid,
                products = currentProducts,
                transactions = currentTx,
                plans = currentPlans,
                categories = currentCategories,
                backupJson = jsonData
            )

            // 3. Upload to Google Drive if connected
            if (syncHelper.isConnected) {
                try {
                    syncHelper.uploadBackupSync(jsonData)
                } catch (e: Exception) {
                    AppLogger.w("Backup", "Drive upload notice: ${e.message}")
                }
            }

            fetchDriveBackupFiles()
            withContext(Dispatchers.Main) {
                onComplete(true, "Berhasil membuat file cadangan Cloud (${currentProducts.size} produk, ${currentTx.size} transaksi)")
            }
        } catch (e: Exception) {
            AppLogger.e("Backup", "Error createManualCloudBackup", e)
            withContext(Dispatchers.Main) {
                onComplete(false, "Gagal membuat cadangan: ${e.localizedMessage}")
            }
        }
    }

    private val _isAppReady = MutableStateFlow(false)
    val isAppReady: StateFlow<Boolean> = _isAppReady.asStateFlow()

    private val _appInitProgress = MutableStateFlow(0.15f)
    val appInitProgress: StateFlow<Float> = _appInitProgress.asStateFlow()

    private val _initStatusText = MutableStateFlow("Menyiapkan database lokal...")
    val initStatusText: StateFlow<String> = _initStatusText.asStateFlow()

    private var autoSyncJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _appInitProgress.value = 0.25f
            _initStatusText.value = "Memeriksa integritas data..."
            cleanDuplicateTransactionsInternal()
            cleanDuplicateProductsInternal()
            
            _appInitProgress.value = 0.55f
            _initStatusText.value = "Memuat data tembakau & transaksi..."
            
            if (syncHelper.isConnected && syncHelper.isOnline && syncHelper.isAutoSyncEnabled) {
                _initStatusText.value = "Memeriksa sinkronisasi Cloud..."
                syncData()
            }
            // Auto refresh email if missing
            if (syncHelper.isConnected && syncHelper.userEmail == null) {
                val token = getApplication<Application>().getSharedPreferences("google_sync_prefs", Context.MODE_PRIVATE).getString("access_token", null)
                if (token != null) syncHelper.fetchUserEmail(token)
            }
            if (syncHelper.isConnected) {
                checkSecurityStatus()
            }
            _appInitProgress.value = 0.90f
            _initStatusText.value = "Aplikasi siap digunakan"
            delay(150)
            _appInitProgress.value = 1.0f
            delay(100)
            _isAppReady.value = true

            // Muat info backup otomatis & lakukan backup awal jika belum ada
            loadAutoBackupInfo(getApplication())
            val backupFile = getBackupFile(getApplication())
            if (!backupFile.exists() || backupFile.length() == 0L) {
                exportBackupToDevice(getApplication())
            }

            // Pembersihan otomatis APK lama & cek update GitHub di background
            try {
                GitHubUpdateManager.cleanupOldOrPendingApkFiles(getApplication())
                if (githubUpdateManager.isAutoCheckEnabled()) {
                    checkForGitHubUpdate(silent = true)
                }
            } catch (e: Exception) {
                AppLogger.d("GitHub auto-check init error: ${e.message}")
            }
        }
        listenToNetworkChanges()

        // Backup otomatis berkala (setiap 1 jam)
        viewModelScope.launch {
            while (isActive) {
                delay(1 * 60 * 60 * 1000L) // 1 jam
                if (syncHelper.isConnected) {
                    checkSecurityStatus()
                }
                exportBackupToDevice(getApplication())
                if (syncHelper.isConnected && syncHelper.isAutoSyncEnabled && syncHelper.isOnline) {
                    syncData(forceOverwriteRemote = false)
                }
            }
        }
    }

    // --- GITHUB IN-APP UPDATE METHODS ---
    fun checkForGitHubUpdate(currentVersion: String = "4.3", silent: Boolean = false) {
        viewModelScope.launch {
            githubUpdateManager.checkForUpdates(currentVersion = currentVersion, silent = silent)
        }
    }

    fun downloadAndInstallUpdate(release: GitHubReleaseInfo) {
        viewModelScope.launch {
            val apk = githubUpdateManager.downloadUpdateInApp(release)
            if (apk != null) {
                githubUpdateManager.installApk(apk)
            }
        }
    }

    fun installDownloadedApk(apkFile: File) {
        githubUpdateManager.installApk(apkFile)
    }

    fun dismissUpdateDialog() {
        githubUpdateManager.resetState()
    }

    fun cleanupUpdateFilesNow() {
        GitHubUpdateManager.cleanupOldOrPendingApkFiles(getApplication())
    }

    fun getGitHubRepo(): String = githubUpdateManager.getTargetRepository()
    fun setGitHubRepo(repo: String) { githubUpdateManager.setTargetRepository(repo) }
    fun isAutoCheckUpdateEnabled(): Boolean = githubUpdateManager.isAutoCheckEnabled()
    fun setAutoCheckUpdateEnabled(enabled: Boolean) { githubUpdateManager.setAutoCheckEnabled(enabled) }

    fun isAutoSyncEnabled(): Boolean = syncHelper.isAutoSyncEnabled
    fun setAutoSyncEnabled(enabled: Boolean) { syncHelper.isAutoSyncEnabled = enabled }
    fun getUserEmail(): String? = syncHelper.userEmail
    fun disconnectDrive() {
        syncHelper.disconnect()
        _syncStatus.value = SyncStatus.DISCONNECTED
    }

    fun checkSecurityStatus() {
        val googleSub = syncHelper.userGoogleSub ?: return
        if (!syncHelper.isOnline) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("$RISC_STATUS_URL?sub=$googleSub")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val json = JSONObject(body)
                        
                        val isBlocked = json.optBoolean("googleSignInBlocked", false)
                        val sessionsRevokedAt = json.optString("sessionsRevokedAt", null)
                        
                        if (isBlocked || !sessionsRevokedAt.isNullOrBlank()) {
                            AppLogger.log("Security", "RISC: Akun terindikasi bahaya. Menghapus sesi.", AppLogger.LogType.WARN)
                            withContext(Dispatchers.Main) {
                                disconnectDrive()
                                Toast.makeText(getApplication(), "Keamanan Google: Sesi dicabut untuk keamanan Anda.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Security", "Gagal cek status RISC", e)
            }
        }
    }

    fun startGoogleLogin(context: Context) {
        // Now handled by returning intent to Activity
    }
    
    fun getAuthIntent() = syncHelper.getAuthIntent()

    fun handleOAuthRedirect(intent: Intent, onComplete: (Boolean, String?) -> Unit) =
        syncHelper.handleAuthResponse(intent, onComplete)

    // --- FIREBASE AUTH & FIRESTORE DATA PERSISTENCE ---
    fun signInWithEmail(email: String, pass: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = firebaseAuth.signInWithEmail(email, pass)
            if (result.isSuccess) {
                syncFirestore()
                onComplete(true, "Berhasil masuk dengan Email")
            } else {
                val signupResult = firebaseAuth.signUpWithEmail(email, pass)
                if (signupResult.isSuccess) {
                    syncFirestore()
                    onComplete(true, "Akun baru dibuat & berhasil masuk")
                } else {
                    // Fallback to local session if Firebase provider is disabled or offline
                    syncHelper.setLocalUserSession(email)
                    onComplete(true, "Berhasil masuk (Mode Cloud Lokal: $email)")
                }
            }
        }
    }

    fun signInAnonymously(onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = firebaseAuth.signInAnonymously()
            if (result.isSuccess) {
                syncFirestore()
                onComplete(true, "Berhasil masuk sebagai Tamu (Firebase)")
            } else {
                // Fallback to local session if Firebase provider is disabled or offline
                syncHelper.setLocalUserSession("tamu_ecotobacco@cloud.com")
                onComplete(true, "Berhasil masuk sebagai Tamu (Mode Cloud Lokal)")
            }
        }
    }

    fun signOutFirebase() {
        firebaseAuth.signOut()
        syncHelper.clearLocalUserSession()
    }

    fun syncFirestore(onComplete: ((Boolean, String) -> Unit)? = null) {
        var uid = firebaseAuth.uid ?: syncHelper.userGoogleSub ?: syncHelper.userEmail
        if (uid.isNullOrBlank()) {
            syncHelper.setLocalUserSession("eco_guest_user@ecotobacco.cloud")
            uid = "eco_guest_user"
        }
        val finalUid = uid
        viewModelScope.launch {
            try {
                val currentProducts = products.first()
                val currentTx = transactions.first()
                val currentPlans = procurementPlans.first()
                val currentCategories = categories.first()

                val backupJson = generateBackupJson()
                val backupName = "eco_tobacco_cloud_backup.json"
                saveToLocalCloudMirror(backupName, backupJson)

                val result = firestoreSync.syncAllToFirestore(
                    userId = finalUid,
                    products = currentProducts,
                    transactions = currentTx,
                    plans = currentPlans,
                    categories = currentCategories,
                    backupJson = backupJson
                )
                fetchDriveBackupFiles()
                if (result.isSuccess) {
                    val summary = result.getOrNull()
                    val msg = "Berhasil sinkron Cloud: ${summary?.uploadedProducts ?: 0} produk, ${summary?.uploadedTransactions ?: 0} transaksi"
                    onComplete?.invoke(true, msg)
                } else {
                    onComplete?.invoke(false, result.exceptionOrNull()?.localizedMessage ?: "Gagal sinkron Cloud")
                }
            } catch (e: Exception) {
                onComplete?.invoke(false, e.localizedMessage ?: "Error sinkronisasi")
            }
        }
    }

    fun restoreFromFirestore(onComplete: ((Boolean, String) -> Unit)? = null) {
        var uid = firebaseAuth.uid ?: syncHelper.userGoogleSub ?: syncHelper.userEmail
        if (uid.isNullOrBlank()) {
            syncHelper.setLocalUserSession("eco_guest_user@ecotobacco.cloud")
            uid = "eco_guest_user"
        }
        val finalUid = uid
        viewModelScope.launch {
            try {
                val result = firestoreSync.fetchAndRestoreFromFirestore(finalUid, repository)
                if (result.isSuccess) {
                    val summary = result.getOrNull()
                    summary?.backupJson?.let { json ->
                        try {
                            val bundle = parseJsonArrayOrObject(json)
                            bundle.appSettings?.let { settings ->
                                setThemeMode(settings.themeMode)
                                setCustomColor(settings.customColor)
                                setFontSizeScale(settings.fontSizeScale)
                                setPaddingScale(settings.paddingScale)
                                setButtonHeight(settings.buttonHeight)
                                setCornerRadius(settings.cornerRadius)
                                setCustomTextColor(settings.customTextColor)
                                val unitSet = settings.customUnits.toSet()
                                _customUnits.value = settings.customUnits.sorted()
                                appPrefs?.edit { putStringSet("custom_units", unitSet) }
                                val subCatSet = settings.customSubCategories.toSet()
                                _customSubCategories.value = settings.customSubCategories.sorted()
                                appPrefs?.edit { putStringSet("custom_sub_categories", subCatSet) }
                            }
                        } catch (e: Exception) {
                            AppLogger.w("Restore", "Failed to restore settings from json: ${e.message}")
                        }
                    }
                    val msg = "Berhasil memulihkan: ${summary?.restoredProducts ?: 0} produk, ${summary?.restoredTransactions ?: 0} transaksi & pengaturan UI"
                    onComplete?.invoke(true, msg)
                } else {
                    onComplete?.invoke(false, result.exceptionOrNull()?.localizedMessage ?: "Gagal memulihkan dari Cloud")
                }
            } catch (e: Exception) {
                onComplete?.invoke(false, e.localizedMessage ?: "Error pemulihan Cloud")
            }
        }
    }

    // --- DIRECT WEB DASHBOARD SYNC (PC / LAPTOP) ---
    val webDashboardBaseUrl = "https://ais-dev-4rgjgocgwaob6fy6gpujpr-863851997609.asia-east1.run.app"

    fun syncToWebDashboard(onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _isWebSyncing.value = true
            try {
                val currentProducts = products.first()
                val currentTx = transactions.first()
                val backupJson = generateBackupJson()
                saveToLocalCloudMirror("eco_tobacco_cloud_backup.json", backupJson)

                val client = OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val requestBody = backupJson.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url("$webDashboardBaseUrl/api/save-backup")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val now = System.currentTimeMillis()
                        _lastWebSyncTime.value = now
                        appPrefs?.edit { putLong("last_web_sync_time", now) }
                        withContext(Dispatchers.Main) {
                            _isWebSyncing.value = false
                            val msg = "Berhasil mengirim ${currentProducts.size} produk & ${currentTx.size} transaksi ke Web Dashboard! Silakan refresh browser di laptop."
                            onComplete?.invoke(true, msg)
                        }
                    } else {
                        val errCode = response.code
                        val errBody = response.body?.string() ?: ""
                        withContext(Dispatchers.Main) {
                            _isWebSyncing.value = false
                            onComplete?.invoke(false, "Server web error ($errCode): $errBody")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isWebSyncing.value = false
                    onComplete?.invoke(false, "Gagal koneksi ke Web Dashboard: ${e.localizedMessage ?: e.message}")
                }
            }
        }
    }

    fun pullFromWebDashboard(onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _isWebSyncing.value = true
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url("$webDashboardBaseUrl/api/backup")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (body.isNullOrBlank()) {
                            withContext(Dispatchers.Main) {
                                _isWebSyncing.value = false
                                onComplete?.invoke(false, "Data di Web Dashboard kosong.")
                            }
                            return@use
                        }

                        val result = processRestoreDataInternal(getApplication(), body)
                        val now = System.currentTimeMillis()
                        _lastWebSyncTime.value = now
                        appPrefs?.edit { putLong("last_web_sync_time", now) }

                        withContext(Dispatchers.Main) {
                            _isWebSyncing.value = false
                            onComplete?.invoke(result.success, result.message)
                        }
                    } else if (response.code == 404) {
                        withContext(Dispatchers.Main) {
                            _isWebSyncing.value = false
                            onComplete?.invoke(false, "Belum ada data di Web Dashboard. Tekan 'Kirim Data HP ke Web' terlebih dahulu.")
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            _isWebSyncing.value = false
                            onComplete?.invoke(false, "Gagal mengambil data dari Web Dashboard (Kode ${response.code})")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isWebSyncing.value = false
                    onComplete?.invoke(false, "Gagal terhubung ke Web: ${e.localizedMessage ?: e.message}")
                }
            }
        }
    }

    fun syncData(forceOverwriteRemote: Boolean = false, fileId: String? = null, onProgress: ((String) -> Unit)? = null) {
        AppLogger.d("Sync", "syncData called, forceOverwriteRemote=$forceOverwriteRemote, fileId=$fileId")
        if (!syncHelper.isConnected) {
            AppLogger.log("Sync", "Sync skipped: Not connected", AppLogger.LogType.WARN)
            onProgress?.invoke("Gagal: Google Drive belum terhubung")
            return
        }
        if (!syncHelper.isOnline) {
            AppLogger.log("Sync", "Sync skipped: Offline", AppLogger.LogType.WARN)
            onProgress?.invoke("Gagal: Tidak ada koneksi internet")
            _syncStatus.value = SyncStatus.OFFLINE
            return
        }
        _syncStatus.value = SyncStatus.SYNCING
        viewModelScope.launch(Dispatchers.IO) {
            try {
                onProgress?.invoke("Menyiapkan data lokal...")
                val localTransactionsData = repository.getAllTransactionsDirect()
                val localProductsData = repository.getAllProductsDirect()
                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(SyncDataBundle::class.java)

                if (forceOverwriteRemote) {
                    onProgress?.invoke("Mengompres data backup...")
                    val syncBundle = SyncDataBundle(
                        transactions = localTransactionsData.map {
                            TransactionSyncModel(
                                it.id,
                                it.date,
                                it.productName,
                                it.quantity,
                                it.unitPrice,
                                it.total,
                                it.supplier,
                                it.unit
                            )
                        },
                        products = localProductsData.map {
                            ProductSyncModel(
                                it.id,
                                it.name,
                                it.price,
                                it.unit,
                                it.sellingPrice,
                                it.sellingUnit,
                                it.contentQuantity,
                                it.category,
                                it.subCategory
                            )
                        },
                        procurementPlans = repository.getAllProcurementPlansDirect().map { planWithItems ->
                            ProcurementPlanSyncModel(
                                id = planWithItems.plan.id,
                                date = planWithItems.plan.date,
                                supplierName = planWithItems.plan.supplierName,
                                items = planWithItems.items.map { item ->
                                    ProcurementItemSyncModel(
                                        id = item.id,
                                        productName = item.productName,
                                        targetQuantity = item.targetQuantity,
                                        estimatedUnitPrice = item.estimatedUnitPrice,
                                        unit = item.unit,
                                        isBought = item.isBought
                                    )
                                }
                            )
                        },
                        appSettings = AppSettingsSyncModel(
                            themeMode = _themeMode.value,
                            customColor = _customColor.value,
                            customUnits = _customUnits.value,
                            fontSizeScale = _fontSizeScale.value,
                            paddingScale = _paddingScale.value,
                            buttonHeight = _buttonHeight.value,
                            cornerRadius = _cornerRadius.value,
                            customTextColor = _customTextColor.value
                        ),
                        lastUpdated = System.currentTimeMillis(),
                        deletedTransactions = getDeletedTransactionKeys().toList()
                    )
                    
                    onProgress?.invoke("Mengirim ke Google Drive...")
                    val success = syncHelper.uploadBackupSync(adapter.toJson(syncBundle))
                    _syncStatus.value = if (success) SyncStatus.SYNCED else SyncStatus.ERROR
                    
                    if (success) onProgress?.invoke("Sinkronisasi Berhasil! ✅")
                    else onProgress?.invoke("Gagal mengirim data ke Drive ❌")
                    
                    return@launch
                }

                onProgress?.invoke("Mengecek backup di cloud...")
                val remoteJson = syncHelper.downloadBackupSync(fileId)
                if (!remoteJson.isNullOrBlank()) {
                    onProgress?.invoke("Sinkronisasi data...")
                    AppLogger.d("Sync", "Remote backup found, length: ${remoteJson.length}")
                    val remoteBundle = adapter.fromJson(remoteJson)
                    if (remoteBundle != null) {
                        AppLogger.i("Sync", "Restoring ${remoteBundle.transactions.size} transactions and ${remoteBundle.products.size} products from cloud.")
                        
                        val combinedDeleted = (getDeletedTransactionKeys() + (remoteBundle.deletedTransactions ?: emptyList())).toSet()
                        addDeletedTransactionKeys(combinedDeleted)
                        
                        val activeLocal = localTransactionsData.filter { tx ->
                            val key = getTransactionKey(tx.date, tx.productName, tx.quantity, tx.unitPrice, tx.supplier)
                            if (combinedDeleted.contains(key)) { repository.deleteTransactionById(tx.id); false } else true
                        }
                        val activeRemote = remoteBundle.transactions.filter { r -> !combinedDeleted.contains(getTransactionKey(r.date, r.productName, r.quantity, r.unitPrice, r.supplier ?: "")) }

                        val mergedTransactions = mergeTransactions(activeLocal, activeRemote)
                        val mergedProducts = mergeProducts(localProductsData, remoteBundle.products)
                        
                        if (mergedTransactions.inserted.isNotEmpty()) {
                            AppLogger.d("Sync", "Inserting ${mergedTransactions.inserted.size} new transactions")
                            repository.insertTransactions(mergedTransactions.inserted)
                        }
                        if (mergedProducts.inserted.isNotEmpty()) {
                            AppLogger.d("Sync", "Inserting ${mergedProducts.inserted.size} new products")
                            repository.insertProducts(mergedProducts.inserted)
                        }
                        
                        _syncStatus.value = SyncStatus.SYNCED
                        onProgress?.invoke("Data berhasil diperbarui ✅")
                        AppLogger.i("Sync", "Cloud restoration complete.")
                    } else {
                        AppLogger.e("Sync", "Failed to parse remote JSON")
                        _syncStatus.value = SyncStatus.ERROR
                        onProgress?.invoke("Gagal memproses data cloud ❌")
                    }
                } else {
                    AppLogger.i("Sync", "No remote backup found.")
                    _syncStatus.value = SyncStatus.SYNCED
                    onProgress?.invoke("Cloud kosong, siap upload data baru")
                    // If no remote, trigger an upload
                    syncData(forceOverwriteRemote = true, onProgress = onProgress)
                }
            } catch (e: Exception) { 
                AppLogger.log("Sync", "Error: ${e.message}", AppLogger.LogType.ERROR)
                _syncStatus.value = SyncStatus.ERROR
                onProgress?.invoke("Error: ${e.message}")
            }
        }
    }

    private fun listenToNetworkChanges() {
        try {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { if (syncHelper.isConnected && syncHelper.isAutoSyncEnabled) syncData() }
                override fun onLost(network: Network) { if (syncHelper.isConnected) _syncStatus.value = SyncStatus.OFFLINE }
            })
        } catch (e: Exception) { e.printStackTrace() }
    }

    private suspend fun cleanDuplicateTransactionsInternal() {
        try {
            val allTx = repository.allTransactions.first()
            val grouped = allTx.groupBy { getTransactionKey(it.date, it.productName, it.quantity, it.unitPrice, it.supplier) }
            for ((_, txList) in grouped) { if (txList.size > 1) { for (i in 1 until txList.size) repository.deleteTransactionById(txList[i].id) } }
        } catch (e: Exception) { Log.e("TobaccoViewModel", "Error cleaning duplicates", e) }
    }

    private suspend fun cleanDuplicateProductsInternal() {
        try {
            val allProds = repository.allProducts.first()
            val grouped = allProds.groupBy { it.name.trim().lowercase() }
            for ((_, prodList) in grouped) { if (prodList.size > 1) { for (i in 1 until prodList.size) repository.deleteProductById(prodList[i].id) } }
        } catch (e: Exception) { Log.e("TobaccoViewModel", "Error cleaning duplicates", e) }
    }

    private fun getTransactionKey(date: Long, productName: String, quantity: Double, unitPrice: Double, supplier: String = ""): String {
        return "${date}_${productName.trim().lowercase()}_${String.format(Locale.US, "%.2f", quantity)}_${String.format(Locale.US, "%.2f", unitPrice)}_${supplier.trim().lowercase()}"
    }

    private fun getDeletedTransactionKeys(): Set<String> {
        val prefs = getApplication<Application>().getSharedPreferences("tobacco_sync_prefs", Context.MODE_PRIVATE)
        return prefs.getString("deleted_tx_keys_str", "")?.split("\n")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
    }

    private fun addDeletedTransactionKey(key: String) {
        val currentKeys = getDeletedTransactionKeys().toMutableSet()
        currentKeys.add(key)
        getApplication<Application>().getSharedPreferences("tobacco_sync_prefs", Context.MODE_PRIVATE).edit { putString("deleted_tx_keys_str", currentKeys.joinToString("\n")) }
    }

    private fun addDeletedTransactionKeys(newKeys: Collection<String>) {
        if (newKeys.isEmpty()) return
        val currentKeys = getDeletedTransactionKeys().toMutableSet()
        currentKeys.addAll(newKeys)
        getApplication<Application>().getSharedPreferences("tobacco_sync_prefs", Context.MODE_PRIVATE).edit { putString("deleted_tx_keys_str", currentKeys.joinToString("\n")) }
    }

    private fun getDeletedProductNames(): Set<String> {
        val prefs = getApplication<Application>().getSharedPreferences("tobacco_sync_prefs", Context.MODE_PRIVATE)
        return prefs.getString("deleted_product_names_str", "")?.split("\n")?.map { it.trim().lowercase() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
    }

    private fun addDeletedProductNames(names: Collection<String>) {
        if (names.isEmpty()) return
        val current = getDeletedProductNames().toMutableSet()
        current.addAll(names.map { it.trim().lowercase() }.filter { it.isNotEmpty() })
        getApplication<Application>().getSharedPreferences("tobacco_sync_prefs", Context.MODE_PRIVATE)
            .edit { putString("deleted_product_names_str", current.joinToString("\n")) }
    }

    private fun removeDeletedProductNames(names: Collection<String>) {
        if (names.isEmpty()) return
        val current = getDeletedProductNames().toMutableSet()
        current.removeAll(names.map { it.trim().lowercase() }.toSet())
        getApplication<Application>().getSharedPreferences("tobacco_sync_prefs", Context.MODE_PRIVATE)
            .edit { putString("deleted_product_names_str", current.joinToString("\n")) }
    }

    fun notifyWidgetUpdate() {
        com.aistudio.ecotobacco.kfzqw.widget.TobaccoAppWidgetProvider.notifyDataChanged(getApplication())
    }

    private fun triggerAutoSync(forceOverwriteRemote: Boolean = false) {
        notifyWidgetUpdate()

        // Backup otomatis ke device setiap ada perubahan (selalu jalan meskipun tidak login)
        exportBackupToDevice(getApplication())

        if (syncHelper.isConnected && syncHelper.isAutoSyncEnabled) {
            autoSyncJob?.cancel()
            autoSyncJob = viewModelScope.launch {
                delay(60000L.milliseconds)
                if (syncHelper.isOnline) syncData(forceOverwriteRemote = forceOverwriteRemote)
                else _syncStatus.value = SyncStatus.OFFLINE
            }
        }
    }

    class MergedResult<T>(val inserted: List<T>, val totalList: List<T>)

    private fun mergeTransactions(local: List<TransactionEntity>, remote: List<TransactionSyncModel>): MergedResult<TransactionEntity> {
        val localKeys = local.associateBy { getTransactionKey(it.date, it.productName, it.quantity, it.unitPrice, it.supplier) }
        val newInsertions = remote.filter { !localKeys.containsKey(getTransactionKey(
            it.date,
            it.productName,
            it.quantity,
            it.unitPrice,
            it.supplier ?: "")) }
            .map {
                TransactionEntity(
                    date = it.date,
                    productName = it.productName,
                    quantity = it.quantity,
                    unitPrice = it.unitPrice,
                    total = it.total,
                    supplier = it.supplier ?: "",
                    unit = it.unit ?: "kg",
                    isDeleted = if (it.isDeleted == true) 1 else 0
                )
            }
        return MergedResult(newInsertions, local + newInsertions)
    }

    private fun mergeProducts(local: List<ProductEntity>, remote: List<ProductSyncModel>): MergedResult<ProductEntity> {
        val localKeys = local.associateBy { it.name.trim().lowercase() }
        val newInsertions = remote.filter { !localKeys.containsKey(it.name.trim().lowercase()) }
            .map { 
                ProductEntity(
                    name = it.name, 
                    price = it.price, 
                    unit = it.unit ?: "kg",
                    sellingPrice = it.sellingPrice ?: 0.0,
                    sellingUnit = it.sellingUnit ?: "pcs",
                    contentQuantity = it.contentQuantity ?: 1.0,
                    category = it.category ?: "",
                    subCategory = it.subCategory ?: "Umum"
                ) 
            }
        return MergedResult(newInsertions, local + newInsertions)
    }

    private suspend fun syncProductFromTransaction(name: String, price: Double, unit: String) {
        val allProducts = repository.getAllProductsDirect()
        val existing = allProducts.find { it.name.trim().equals(name.trim(), ignoreCase = true) }

        if (existing != null) {
            // Update existing if price or unit changed
            if (existing.price != price || existing.unit != unit) {
                repository.updateProduct(existing.copy(price = price, unit = unit))
            }
        } else {
            // Add new product
            repository.insertProduct(ProductEntity(name = name.trim(), price = price, unit = unit))
        }
    }

    fun addCategory(name: String) = viewModelScope.launch(Dispatchers.IO) { 
        repository.insertCategory(CategoryEntity(name = name))
        triggerAutoSync(forceOverwriteRemote = true)
    }
    fun addProduct(name: String, price: Double, unit: String = "kg", sellingPrice: Double = 0.0, sellingUnit: String = "pcs", contentQuantity: Double = 1.0, category: String = "", subCategory: String = "Umum") = viewModelScope.launch { 
        removeDeletedProductNames(listOf(name))
        val product = ProductEntity(name = name, price = price, unit = unit, sellingPrice = sellingPrice, sellingUnit = sellingUnit, contentQuantity = contentQuantity, category = category, subCategory = subCategory)
        repository.insertProduct(product)
        firebaseAuth.uid?.let { uid ->
            firestoreSync.persistProduct(uid, product)
        }
        triggerAutoSync(forceOverwriteRemote = true) 
    }
    fun updateProduct(id: Int, name: String, price: Double, unit: String = "kg", sellingPrice: Double = 0.0, sellingUnit: String = "pcs", contentQuantity: Double = 1.0, category: String = "", subCategory: String = "Umum") = viewModelScope.launch { 
        removeDeletedProductNames(listOf(name))
        val product = ProductEntity(id = id, name = name, price = price, unit = unit, sellingPrice = sellingPrice, sellingUnit = sellingUnit, contentQuantity = contentQuantity, category = category, subCategory = subCategory)
        repository.updateProduct(product)
        firebaseAuth.uid?.let { uid ->
            firestoreSync.persistProduct(uid, product)
        }
        triggerAutoSync(forceOverwriteRemote = true) 
    }
    fun deleteProduct(id: Int) = viewModelScope.launch {
        val product = repository.getProductById(id)
        if (product != null) {
            addDeletedProductNames(listOf(product.name))
        }
        repository.deleteProductById(id)
        firebaseAuth.uid?.let { uid ->
            firestoreSync.deleteProduct(uid, id)
        }
        triggerAutoSync(forceOverwriteRemote = true)
    }
    fun addTransaction(date: Long, productName: String, quantity: Double, unitPrice: Double, supplier: String = "", unit: String = "kg") = viewModelScope.launch {
        syncProductFromTransaction(productName, unitPrice, unit)
        val tx = TransactionEntity(
            date = date,
            productName = productName,
            quantity = quantity,
            unitPrice = unitPrice,
            total = quantity * unitPrice,
            supplier = supplier,
            unit = unit
        )
        repository.insertTransaction(tx)
        firebaseAuth.uid?.let { uid ->
            firestoreSync.persistTransaction(uid, tx)
        }
        triggerAutoSync(forceOverwriteRemote = true)
    }
    fun updateTransaction(id: Int, date: Long, productName: String, quantity: Double, unitPrice: Double, supplier: String = "", unit: String = "kg") = viewModelScope.launch {
        repository.updateTransaction(
            TransactionEntity(
                id = id,
                date = date,
                productName = productName,
                quantity = quantity,
                unitPrice = unitPrice,
                total = quantity * unitPrice,
                supplier = supplier,
                unit = unit
            )
        )
        triggerAutoSync(forceOverwriteRemote = true)
    }
    fun deleteTransaction(id: Int) = viewModelScope.launch {
        val tx = repository.getTransactionById(id)
        if (tx != null) {
            addDeletedTransactionKey(getTransactionKey(tx.date, tx.productName, tx.quantity, tx.unitPrice, tx.supplier))
        }
        repository.deleteTransactionById(id)
        firebaseAuth.uid?.let { uid ->
            firestoreSync.deleteTransaction(uid, id)
        }
        triggerAutoSync(forceOverwriteRemote = true)
    }
    fun deleteTransactions(ids: List<Int>) = viewModelScope.launch {
        ids.forEach { id ->
            val tx = repository.getTransactionById(id)
            if (tx != null) {
                addDeletedTransactionKey(getTransactionKey(tx.date, tx.productName, tx.quantity, tx.unitPrice, tx.supplier))
            }
            firebaseAuth.uid?.let { uid ->
                firestoreSync.deleteTransaction(uid, id)
            }
        }
        repository.deleteTransactionsByIds(ids)
        triggerAutoSync(forceOverwriteRemote = true)
    }

    fun addProcurementPlan(date: Long, supplier: String, items: List<ProcurementItemEntity>) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertProcurementPlanWithItems(
            ProcurementPlanEntity(
                date = date,
                supplierName = supplier
            ), items)
        triggerAutoSync(forceOverwriteRemote = true)
    }

    fun updateProcurementPlan(plan: ProcurementPlanEntity, items: List<ProcurementItemEntity>) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateProcurementPlanWithItems(plan, items)
        triggerAutoSync(forceOverwriteRemote = true)
    }

    fun deleteProcurementPlan(plan: ProcurementPlanEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteItemsByPlanId(plan.id)
        repository.deleteProcurementPlan(plan)
        triggerAutoSync(forceOverwriteRemote = true)
    }

    fun toggleProcurementItemStatus(item: ProcurementItemEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateProcurementItem(item.copy(isBought = !item.isBought))
        triggerAutoSync(forceOverwriteRemote = true)
    }

    fun removeItemFromPlan(item: ProcurementItemEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteProcurementItem(item)
        triggerAutoSync(forceOverwriteRemote = true)
    }

    fun parsePrice(input: String): Double = input.replace(",", ".").toDoubleOrNull() ?: 0.0

    fun addScannedTransactions(items: List<LocalScanner.ScannedItem>, supplier: String, date: Long? = null) = viewModelScope.launch(Dispatchers.IO) {
        val now = date ?: System.currentTimeMillis()
        
        // Sync each scanned item to product database
        items.forEach { item ->
            syncProductFromTransaction(item.name, item.price, "kg")
        }

        val transactions = items.map {
            TransactionEntity(
                date = now,
                productName = it.name,
                quantity = it.quantity,
                unitPrice = it.price,
                total = it.quantity * it.price,
                supplier = supplier,
                unit = "kg"
            )
        }
        repository.insertTransactions(transactions)
        triggerAutoSync(forceOverwriteRemote = true)
    }

    fun importProductsFromCsv(context: Context, uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                val products = reader.lineSequence().mapNotNull { line ->
                    val parts = line.split(",")
                    if (parts.size >= 2) {
                        val name = parts[0].trim()
                        val price = parts[1].trim().toDoubleOrNull() ?: 0.0
                        val unit = if (parts.size >= 3) parts[2].trim() else "kg"
                        if (name.isNotBlank()) ProductEntity(
                            name = name,
                            price = price,
                            unit = unit
                        ) else null
                    } else null
                }.toList()
                if (products.isNotEmpty()) repository.insertProducts(products)
            }
            triggerAutoSync(forceOverwriteRemote = true)
        } catch (e: Exception) { AppLogger.log("Import", "Error: ${e.message}", AppLogger.LogType.ERROR) }
    }

    fun importTransactionsFromCsv(context: Context, uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                val transactions = reader.lineSequence().mapNotNull { line ->
                    val parts = line.split(",")
                    if (parts.size >= 5) {
                        val date = parts[0].trim().toLongOrNull() ?: System.currentTimeMillis()
                        val name = parts[1].trim()
                        val qty = parts[2].trim().toDoubleOrNull() ?: 0.0
                        val price = parts[3].trim().toDoubleOrNull() ?: 0.0
                        val supplier = parts[4].trim()
                        val unit = if (parts.size >= 6) parts[5].trim() else "kg"
                        if (name.isNotBlank()) TransactionEntity(
                            date = date,
                            productName = name,
                            quantity = qty,
                            unitPrice = price,
                            total = qty * price,
                            supplier = supplier,
                            unit = unit
                        ) else null
                    } else null
                }.toList()
                if (transactions.isNotEmpty()) repository.insertTransactions(transactions)
            }
            triggerAutoSync(forceOverwriteRemote = true)
        } catch (e: Exception) { AppLogger.log("Import", "Error: ${e.message}", AppLogger.LogType.ERROR) }
    }

    fun exportTransactionsToCsv(context: Context, transactions: List<TransactionEntity>) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val fileName = "EcoTobacco_Transactions_${System.currentTimeMillis()}.csv"
            val content = buildString {
                append("Date_Timestamp,Product_Name,Quantity,UnitPrice,Total,Supplier,Unit\n")
                transactions.forEach { t ->
                    append("${t.date},${t.productName},${t.quantity},${t.unitPrice},${t.total},${t.supplier},${t.unit}\n")
                }
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { stream ->
                        stream.write(content.toByteArray())
                    }
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Laporan disimpan di folder Downloads", Toast.LENGTH_LONG).show() }
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                file.writeText(content)
                withContext(Dispatchers.Main) { Toast.makeText(context, "Laporan disimpan di folder Downloads", Toast.LENGTH_LONG).show() }
            }
        } catch (e: Exception) {
            AppLogger.e("ViewModel", "Gagal export CSV", e)
            withContext(Dispatchers.Main) { Toast.makeText(context, "Gagal ekspor laporan", Toast.LENGTH_SHORT).show() }
        }
    }

    fun convertPlanToTransactions(planWithItems: ProcurementPlanWithItems) = viewModelScope.launch(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val boughtItems = planWithItems.items.filter { it.isBought }
        if (boughtItems.isEmpty()) return@launch

        val transactions = boughtItems.map { item ->
            TransactionEntity(
                date = now,
                productName = item.productName,
                quantity = item.targetQuantity,
                unitPrice = item.estimatedUnitPrice,
                total = item.targetQuantity * item.estimatedUnitPrice,
                supplier = planWithItems.plan.supplierName,
                unit = item.unit
            )
        }
        repository.insertTransactions(transactions)
        repository.deleteProcurementPlan(planWithItems.plan)
        triggerAutoSync(forceOverwriteRemote = true)
    }

    private suspend fun generateBackupJson(): String {
        val localTransactionsData = repository.getAllTransactionsDirect().filter { it.isDeleted == 0 }
        val localProductsData = repository.getAllProductsDirect()
        val localProcurementData = repository.getAllProcurementPlansDirect()
        val localCategoriesData = repository.getAllCategoriesDirect().map { it.name }
        
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(SyncDataBundle::class.java)
        
        val syncBundle = SyncDataBundle(
            transactions = localTransactionsData.map {
                TransactionSyncModel(
                    it.id,
                    it.date,
                    it.productName,
                    it.quantity,
                    it.unitPrice,
                    it.total,
                    it.supplier,
                    it.unit,
                    isDeleted = false
                )
            },
            products = localProductsData.map {
                ProductSyncModel(
                    it.id,
                    it.name,
                    it.price,
                    it.unit,
                    it.sellingPrice,
                    it.sellingUnit,
                    it.contentQuantity,
                    it.category,
                    it.subCategory
                )
            },
            procurementPlans = localProcurementData.map { planWithItems ->
                ProcurementPlanSyncModel(
                    id = planWithItems.plan.id,
                    date = planWithItems.plan.date,
                    supplierName = planWithItems.plan.supplierName,
                    items = planWithItems.items.map { item ->
                        ProcurementItemSyncModel(
                            id = item.id,
                            productName = item.productName,
                            targetQuantity = item.targetQuantity,
                            estimatedUnitPrice = item.estimatedUnitPrice,
                            unit = item.unit,
                            isBought = item.isBought
                        )
                    }
                )
            },
            appSettings = AppSettingsSyncModel(
                themeMode = _themeMode.value,
                customColor = _customColor.value,
                customUnits = _customUnits.value,
                customSubCategories = _customSubCategories.value,
                fontSizeScale = _fontSizeScale.value,
                paddingScale = _paddingScale.value,
                buttonHeight = _buttonHeight.value,
                cornerRadius = _cornerRadius.value,
                customTextColor = _customTextColor.value
            ),
            categories = localCategoriesData,
            lastUpdated = System.currentTimeMillis(),
            deletedTransactions = getDeletedTransactionKeys().toList(),
            deletedProducts = getDeletedProductNames().toList()
        )
        
        return adapter.toJson(syncBundle)
    }

    private fun writeJsonToAllBackupLocations(context: Context, jsonData: String) {
        val fileName = "eco_tobacco_sync_data.json"

        // 1. Internal sandbox (always available)
        try {
            val internalBackupDir = File(context.filesDir, "backups")
            if (!internalBackupDir.exists()) internalBackupDir.mkdirs()
            File(internalBackupDir, fileName).writeText(jsonData)
        } catch (e: Exception) {
            AppLogger.e("Backup", "Internal backup write failed", e)
        }

        // 2. App-specific external storage
        try {
            val appFilesDir = context.getExternalFilesDir("backups")
            if (appFilesDir != null) {
                if (!appFilesDir.exists()) appFilesDir.mkdirs()
                File(appFilesDir, fileName).writeText(jsonData)
            }
        } catch (e: Exception) {
            AppLogger.w("Backup", "External files write notice: ${e.message}")
        }
    }

    fun exportBackupToDevice(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val jsonData = generateBackupJson()
            writeJsonToAllBackupLocations(context, jsonData)
            
            context.getSharedPreferences("tobacco_sync_prefs", Context.MODE_PRIVATE)
                .edit { putLong("last_auto_backup_time", System.currentTimeMillis()) }

            loadAutoBackupInfo(context)
        } catch (e: Exception) {
            AppLogger.e("Backup", "Gagal auto-backup ke storage", e)
        }
    }

    fun performManualAutoBackup(context: Context, onResult: ((Boolean, String) -> Unit)? = null) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val jsonData = generateBackupJson()
            writeJsonToAllBackupLocations(context, jsonData)

            context.getSharedPreferences("tobacco_sync_prefs", Context.MODE_PRIVATE)
                .edit { putLong("last_auto_backup_time", System.currentTimeMillis()) }

            loadAutoBackupInfo(context)
            withContext(Dispatchers.Main) {
                onResult?.invoke(true, "Cadangan otomatis berhasil diperbarui!")
            }
        } catch (e: Exception) {
            AppLogger.e("Backup", "Gagal manual backup", e)
            withContext(Dispatchers.Main) {
                onResult?.invoke(false, "Gagal mencadangkan: ${e.localizedMessage}")
            }
        }
    }

    private fun extractBalancedJson(input: String): String {
        val trimmed = input.trim()
        val startObj = trimmed.indexOf('{')
        val startArr = trimmed.indexOf('[')
        if (startObj == -1 && startArr == -1) return trimmed

        val isArray = startArr != -1 && (startObj == -1 || startArr < startObj)
        val startChar = if (isArray) '[' else '{'
        val endChar = if (isArray) ']' else '}'
        val start = if (isArray) startArr else startObj

        var depth = 0
        var inString = false
        var escape = false
        for (i in start until trimmed.length) {
            val c = trimmed[i]
            if (escape) {
                escape = false
                continue
            }
            if (c == '\\') {
                escape = true
                continue
            }
            if (c == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (c == startChar) {
                    depth++
                } else if (c == endChar) {
                    depth--
                    if (depth == 0) {
                        return trimmed.substring(start, i + 1)
                    }
                }
            }
        }
        return trimmed
    }

    fun writeBackupToUri(context: Context, uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val jsonData = generateBackupJson()
            val stream = try {
                context.contentResolver.openOutputStream(uri, "wt")
            } catch (e: Exception) {
                context.contentResolver.openOutputStream(uri)
            }
            stream?.use { s ->
                s.write(jsonData.toByteArray(Charsets.UTF_8))
                s.flush()
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "File cadangan (.json) berhasil disimpan!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            AppLogger.e("Backup", "Gagal menyimpan backup ke URI", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Gagal menyimpan backup: ${e.localizedMessage ?: "Terjadi kesalahan"}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun parseFlexibleDouble(value: Any?, default: Double = 0.0): Double {
        if (value == null) return default
        if (value is Number) return value.toDouble()
        val str = value.toString().trim()
        if (str.isEmpty()) return default
        val cleaned = str.replace("Rp", "", ignoreCase = true)
            .replace("IDR", "", ignoreCase = true)
            .replace(" ", "")
            .replace("'", "")
        return try {
            if (cleaned.contains(",") && cleaned.contains(".")) {
                if (cleaned.lastIndexOf(",") > cleaned.lastIndexOf(".")) {
                    cleaned.replace(".", "").replace(",", ".").toDouble()
                } else {
                    cleaned.replace(",", "").toDouble()
                }
            } else if (cleaned.contains(",")) {
                val parts = cleaned.split(",")
                if (parts.size == 2 && parts[1].length <= 2) {
                    cleaned.replace(",", ".").toDouble()
                } else {
                    cleaned.replace(",", "").toDouble()
                }
            } else if (cleaned.count { it == '.' } > 1) {
                cleaned.replace(".", "").toDouble()
            } else {
                cleaned.toDouble()
            }
        } catch (_: Exception) {
            default
        }
    }

    private fun parseFlexibleLong(value: Any?, default: Long = System.currentTimeMillis()): Long {
        if (value == null) return default
        if (value is Number) {
            val num = value.toLong()
            return if (num < 10000000000L && num > 0) num * 1000L else num
        }
        val str = value.toString().trim()
        if (str.isEmpty()) return default
        val asLong = str.toLongOrNull()
        if (asLong != null) {
            return if (asLong < 10000000000L && asLong > 0) asLong * 1000L else asLong
        }
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
            "dd-MM-yyyy HH:mm:ss",
            "dd-MM-yyyy",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy",
            "yyyy/MM/dd"
        )
        for (fmt in formats) {
            try {
                val sdf = java.text.SimpleDateFormat(fmt, Locale.US)
                val d = sdf.parse(str)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        return default
    }

    private fun parseJsonArrayOrObject(jsonData: String): SyncDataBundle {
        val trimmed = jsonData.trim().removePrefix("\uFEFF").trim()
        val txList = mutableListOf<TransactionSyncModel>()
        val prodList = mutableListOf<ProductSyncModel>()
        val planList = mutableListOf<ProcurementPlanSyncModel>()
        var appSettings: AppSettingsSyncModel? = null
        val deletedKeys = mutableListOf<String>()

        fun parseTransactionObj(obj: org.json.JSONObject, index: Int): TransactionSyncModel? {
            val pName = obj.optString("productName").ifEmpty {
                obj.optString("product_name").ifEmpty {
                    obj.optString("nama").ifEmpty {
                        obj.optString("nama_produk").ifEmpty {
                            obj.optString("nama_barang").ifEmpty {
                                obj.optString("barang").ifEmpty {
                                    obj.optString("name")
                                }
                            }
                        }
                    }
                }
            }
            if (pName.isBlank()) return null
            val qty = parseFlexibleDouble(obj.opt("quantity") ?: obj.opt("qty") ?: obj.opt("jumlah") ?: obj.opt("banyak"), 1.0)
            val price = parseFlexibleDouble(obj.opt("unitPrice") ?: obj.opt("unit_price") ?: obj.opt("price") ?: obj.opt("harga") ?: obj.opt("harga_beli") ?: obj.opt("harga_satuan"), 0.0)
            val totalRaw = parseFlexibleDouble(obj.opt("total") ?: obj.opt("total_price") ?: obj.opt("subtotal") ?: obj.opt("jumlah_harga"), qty * price)
            val total = if (totalRaw > 0) totalRaw else qty * price
            val supplier = obj.optString("supplier").ifEmpty {
                obj.optString("pemasok").ifEmpty {
                    obj.optString("supplierName").ifEmpty {
                        obj.optString("supplier_name").ifEmpty {
                            obj.optString("penjual")
                        }
                    }
                }
            }
            val unit = obj.optString("unit").ifEmpty {
                obj.optString("satuan").ifEmpty { "kg" }
            }
            val date = parseFlexibleLong(obj.opt("date") ?: obj.opt("tanggal") ?: obj.opt("waktu") ?: obj.opt("timestamp") ?: obj.opt("createdAt"))
            val isDeleted = obj.optBoolean("isDeleted", false) || obj.optBoolean("is_deleted", false) || obj.optBoolean("deleted", false)

            return TransactionSyncModel(
                id = obj.optInt("id", index + 1),
                date = date,
                productName = pName.trim(),
                quantity = qty,
                unitPrice = price,
                total = total,
                supplier = supplier.trim(),
                unit = unit.trim(),
                isDeleted = isDeleted
            )
        }

        fun parseProductObj(obj: org.json.JSONObject, index: Int): ProductSyncModel? {
            val name = obj.optString("name").ifEmpty {
                obj.optString("productName").ifEmpty {
                    obj.optString("nama").ifEmpty {
                        obj.optString("nama_produk").ifEmpty {
                            obj.optString("nama_barang").ifEmpty {
                                obj.optString("barang")
                            }
                        }
                    }
                }
            }
            if (name.isBlank()) return null
            val price = parseFlexibleDouble(obj.opt("price") ?: obj.opt("unitPrice") ?: obj.opt("harga") ?: obj.opt("harga_beli"), 0.0)
            val unit = obj.optString("unit").ifEmpty { obj.optString("satuan").ifEmpty { "kg" } }
            val sellingPrice = parseFlexibleDouble(obj.opt("sellingPrice") ?: obj.opt("selling_price") ?: obj.opt("harga_jual") ?: obj.opt("jual"), 0.0)
            val sellingUnit = obj.optString("sellingUnit").ifEmpty { obj.optString("selling_unit").ifEmpty { obj.optString("satuan_jual").ifEmpty { "pcs" } } }
            val contentQty = parseFlexibleDouble(obj.opt("contentQuantity") ?: obj.opt("content_quantity") ?: obj.opt("isi") ?: obj.opt("konversi"), 1.0)

            val category = obj.optString("category").ifEmpty { obj.optString("kategori").ifEmpty { "" } }
            val subCategory = obj.optString("subCategory").ifEmpty { obj.optString("sub_category").ifEmpty { obj.optString("subkategori").ifEmpty { "Umum" } } }

            return ProductSyncModel(
                id = obj.optInt("id", index + 1),
                name = name.trim(),
                price = price,
                unit = unit.trim(),
                sellingPrice = sellingPrice,
                sellingUnit = sellingUnit.trim(),
                contentQuantity = contentQty,
                category = category.trim(),
                subCategory = subCategory.trim().ifBlank { "Umum" }
            )
        }

        fun parsePlanObj(planObj: org.json.JSONObject, index: Int): ProcurementPlanSyncModel? {
            val sName = planObj.optString("supplierName").ifEmpty {
                planObj.optString("supplier_name").ifEmpty {
                    planObj.optString("supplier").ifEmpty {
                        planObj.optString("pemasok")
                    }
                }
            }
            val date = parseFlexibleLong(planObj.opt("date") ?: planObj.opt("tanggal") ?: planObj.opt("waktu") ?: planObj.opt("timestamp"))
            val itemsArr = planObj.optJSONArray("items") ?: planObj.optJSONArray("daftar_barang") ?: planObj.optJSONArray("barang") ?: planObj.optJSONArray("products")
            val itemsList = mutableListOf<ProcurementItemSyncModel>()
            if (itemsArr != null) {
                for (j in 0 until itemsArr.length()) {
                    val itemObj = itemsArr.optJSONObject(j) ?: continue
                    val itemName = itemObj.optString("productName").ifEmpty {
                        itemObj.optString("nama").ifEmpty {
                            itemObj.optString("barang").ifEmpty {
                                itemObj.optString("name")
                            }
                        }
                    }
                    if (itemName.isNotBlank()) {
                        itemsList.add(
                            ProcurementItemSyncModel(
                                id = itemObj.optInt("id", j + 1),
                                productName = itemName.trim(),
                                targetQuantity = parseFlexibleDouble(itemObj.opt("targetQuantity") ?: itemObj.opt("quantity") ?: itemObj.opt("jumlah"), 1.0),
                                estimatedUnitPrice = parseFlexibleDouble(itemObj.opt("estimatedUnitPrice") ?: itemObj.opt("unitPrice") ?: itemObj.opt("price") ?: itemObj.opt("harga"), 0.0),
                                unit = itemObj.optString("unit").ifEmpty { itemObj.optString("satuan").ifEmpty { "kg" } },
                                isBought = itemObj.optBoolean("isBought", false) || itemObj.optBoolean("dibeli", false)
                            )
                        )
                    }
                }
            }
            return ProcurementPlanSyncModel(
                id = planObj.optInt("id", index + 1),
                date = date,
                supplierName = sName.trim(),
                items = itemsList
            )
        }

        if (trimmed.startsWith("[")) {
            val arr = org.json.JSONArray(trimmed)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val tx = parseTransactionObj(obj, i)
                if (tx != null) {
                    txList.add(tx)
                } else {
                    val prod = parseProductObj(obj, i)
                    if (prod != null) prodList.add(prod)
                }
            }
        } else {
            val root = org.json.JSONObject(trimmed)

            val txArr = root.optJSONArray("transactions")
                ?: root.optJSONArray("transaksi")
                ?: root.optJSONArray("kulakan")
                ?: root.optJSONArray("data")
                ?: root.optJSONArray("items")
                ?: root.optJSONArray("records")
            if (txArr != null) {
                for (i in 0 until txArr.length()) {
                    val obj = txArr.optJSONObject(i) ?: continue
                    val tx = parseTransactionObj(obj, i)
                    if (tx != null) txList.add(tx)
                }
            }

            val prodArr = root.optJSONArray("products")
                ?: root.optJSONArray("produk")
                ?: root.optJSONArray("barang")
                ?: root.optJSONArray("master_produk")
                ?: root.optJSONArray("master_barang")
            if (prodArr != null) {
                for (i in 0 until prodArr.length()) {
                    val obj = prodArr.optJSONObject(i) ?: continue
                    val prod = parseProductObj(obj, i)
                    if (prod != null) prodList.add(prod)
                }
            }

            val planArr = root.optJSONArray("procurementPlans")
                ?: root.optJSONArray("rencana")
                ?: root.optJSONArray("rencanaKulakan")
                ?: root.optJSONArray("plans")
                ?: root.optJSONArray("procurements")
            if (planArr != null) {
                for (i in 0 until planArr.length()) {
                    val planObj = planArr.optJSONObject(i) ?: continue
                    val plan = parsePlanObj(planObj, i)
                    if (plan != null) planList.add(plan)
                }
            }

            val settingsObj = root.optJSONObject("appSettings") ?: root.optJSONObject("settings") ?: root.optJSONObject("pengaturan")
            if (settingsObj != null) {
                val unitsList = mutableListOf<String>()
                val unitsArr = settingsObj.optJSONArray("customUnits") ?: settingsObj.optJSONArray("satuan")
                if (unitsArr != null) {
                    for (k in 0 until unitsArr.length()) {
                        unitsList.add(unitsArr.optString(k))
                    }
                }
                val subCatList = mutableListOf<String>()
                val subCatArr = settingsObj.optJSONArray("customSubCategories") ?: settingsObj.optJSONArray("sub_categories") ?: settingsObj.optJSONArray("subkategori")
                if (subCatArr != null) {
                    for (k in 0 until subCatArr.length()) {
                        subCatList.add(subCatArr.optString(k))
                    }
                }
                appSettings = AppSettingsSyncModel(
                    themeMode = settingsObj.optString("themeMode", "SYSTEM"),
                    customColor = settingsObj.optInt("customColor", 0xFF10B981.toInt()),
                    customUnits = if (unitsList.isNotEmpty()) unitsList else listOf("kg", "ons", "pcs"),
                    customSubCategories = if (subCatList.isNotEmpty()) subCatList else listOf("Tembakau", "Alat", "Papir", "Kemasan", "Umum"),
                    fontSizeScale = parseFlexibleDouble(settingsObj.opt("fontSizeScale"), 1.0).toFloat(),
                    paddingScale = parseFlexibleDouble(settingsObj.opt("paddingScale"), 1.0).toFloat(),
                    buttonHeight = settingsObj.optInt("buttonHeight", 56),
                    cornerRadius = settingsObj.optInt("cornerRadius", 16),
                    customTextColor = settingsObj.optInt("customTextColor", 0)
                )
            }

            val delArr = root.optJSONArray("deletedTransactions") ?: root.optJSONArray("deleted") ?: root.optJSONArray("hapus")
            if (delArr != null) {
                for (k in 0 until delArr.length()) {
                    deletedKeys.add(delArr.optString(k))
                }
            }
        }

        return SyncDataBundle(
            transactions = txList,
            products = prodList,
            lastUpdated = System.currentTimeMillis(),
            deletedTransactions = deletedKeys,
            procurementPlans = planList,
            appSettings = appSettings
        )
    }

    data class RestoreResult(val success: Boolean, val message: String)

    private suspend fun processRestoreDataInternal(context: Context, rawData: String): RestoreResult {
        val cleaned = rawData.trim().removePrefix("\uFEFF").trim()
        var remoteBundle: SyncDataBundle? = null

        // 1. Try Moshi
        try {
            val jsonData = extractBalancedJson(cleaned)
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(SyncDataBundle::class.java)
            val buffer = okio.Buffer().writeUtf8(jsonData)
            val reader = com.squareup.moshi.JsonReader.of(buffer).apply { isLenient = true }
            remoteBundle = adapter.fromJson(reader)
        } catch (e: Exception) {
            AppLogger.w("Restore", "Moshi parsing failed: ${e.message}, attempting flexible parser")
        }

        // 2. Try JSON parser
        if (remoteBundle == null || (remoteBundle.transactions.isEmpty() && remoteBundle.products.isEmpty() && (remoteBundle.procurementPlans == null || remoteBundle.procurementPlans!!.isEmpty()))) {
            try {
                val parsed = parseJsonArrayOrObject(cleaned)
                if (parsed.transactions.isNotEmpty() || parsed.products.isNotEmpty() || !parsed.procurementPlans.isNullOrEmpty()) {
                    remoteBundle = parsed
                }
            } catch (e: Exception) {
                AppLogger.w("Restore", "JSON parser failed: ${e.message}, attempting CSV parser")
            }
        }

        // 3. Fallback CSV Parser if not JSON
        if (remoteBundle == null || (remoteBundle.transactions.isEmpty() && remoteBundle.products.isEmpty() && (remoteBundle.procurementPlans == null || remoteBundle.procurementPlans!!.isEmpty()))) {
            try {
                val lines = cleaned.lines().map { it.trim() }.filter { it.isNotEmpty() }
                if (lines.isNotEmpty()) {
                    val delimiter = if (lines[0].contains(";")) ";" else if (lines[0].contains("\t")) "\t" else ","
                    val header = lines[0].split(delimiter).map { it.trim().lowercase() }
                    val pIdx = header.indexOfFirst { it.contains("produk") || it.contains("product") || it.contains("barang") || it.contains("nama") }
                    val qIdx = header.indexOfFirst { it.contains("jumlah") || it.contains("qty") || it.contains("quantity") || it.contains("banyak") }
                    val prIdx = header.indexOfFirst { it.contains("harga") || it.contains("price") || it.contains("biaya") }
                    val tIdx = header.indexOfFirst { it.contains("total") || it.contains("subtotal") }
                    val sIdx = header.indexOfFirst { it.contains("pemasok") || it.contains("supplier") || it.contains("penjual") }
                    val uIdx = header.indexOfFirst { it.contains("satuan") || it.contains("unit") }
                    val dIdx = header.indexOfFirst { it.contains("tanggal") || it.contains("date") || it.contains("waktu") }

                    val txFromCsv = mutableListOf<TransactionSyncModel>()
                    for (i in 1 until lines.size) {
                        val row = lines[i].split(delimiter).map { it.trim().removeSurrounding("\"") }
                        if (pIdx >= 0 && pIdx < row.size && row[pIdx].isNotBlank()) {
                            val pName = row[pIdx]
                            val qty = if (qIdx >= 0 && qIdx < row.size) parseFlexibleDouble(row[qIdx], 1.0) else 1.0
                            val price = if (prIdx >= 0 && prIdx < row.size) parseFlexibleDouble(row[prIdx], 0.0) else 0.0
                            val total = if (tIdx >= 0 && tIdx < row.size) parseFlexibleDouble(row[tIdx], qty * price) else qty * price
                            val supplier = if (sIdx >= 0 && sIdx < row.size) row[sIdx] else ""
                            val unit = if (uIdx >= 0 && uIdx < row.size) row[uIdx] else "kg"
                            val date = if (dIdx >= 0 && dIdx < row.size) parseFlexibleLong(row[dIdx]) else System.currentTimeMillis()

                            txFromCsv.add(
                                TransactionSyncModel(
                                    id = i,
                                    date = date,
                                    productName = pName,
                                    quantity = qty,
                                    unitPrice = price,
                                    total = total,
                                    supplier = supplier,
                                    unit = unit,
                                    isDeleted = false
                                )
                            )
                        }
                    }
                    if (txFromCsv.isNotEmpty()) {
                        remoteBundle = SyncDataBundle(
                            transactions = txFromCsv,
                            products = emptyList(),
                            lastUpdated = System.currentTimeMillis()
                        )
                    }
                }
            } catch (csvEx: Exception) {
                AppLogger.e("Restore", "CSV parsing failed", csvEx)
            }
        }

        if (remoteBundle == null || (remoteBundle.transactions.isEmpty() && remoteBundle.products.isEmpty() && (remoteBundle.procurementPlans == null || remoteBundle.procurementPlans!!.isEmpty()) && remoteBundle.appSettings == null)) {
            return RestoreResult(false, "Format file tidak dikenali atau tidak berisi data yang sesuai.")
        }

        val localTransactionsData = repository.getAllTransactionsDirect()
        val localProductsData = repository.getAllProductsDirect()

        // 1. Process deleted products: combine tombstones and purge any deleted products from local DB
        val incomingDeletedProducts = (remoteBundle.deletedProducts ?: emptyList()).map { it.trim().lowercase() }
        val combinedDeletedProducts = (getDeletedProductNames() + incomingDeletedProducts).toSet()
        addDeletedProductNames(combinedDeletedProducts)

        localProductsData.forEach { p ->
            if (combinedDeletedProducts.contains(p.name.trim().lowercase())) {
                repository.deleteProductById(p.id)
            }
        }

        // 2. Process deleted transactions: combine tombstones and purge any deleted transactions from local DB
        val incomingDeletedTx = (remoteBundle.deletedTransactions ?: emptyList()).toSet()
        val combinedDeletedTx = (getDeletedTransactionKeys() + incomingDeletedTx).toSet()
        addDeletedTransactionKeys(combinedDeletedTx)

        localTransactionsData.forEach { tx ->
            val key = getTransactionKey(tx.date, tx.productName, tx.quantity, tx.unitPrice, tx.supplier ?: "")
            if (combinedDeletedTx.contains(key)) {
                repository.deleteTransactionById(tx.id)
            }
        }

        // 3. Filter incoming products: only ones that have NOT been deleted
        val activeIncomingProducts = remoteBundle.products.filter { p ->
            !combinedDeletedProducts.contains(p.name.trim().lowercase())
        }
        // If restoring a backup with explicit products, un-tombstone them
        removeDeletedProductNames(activeIncomingProducts.map { it.name.trim().lowercase() })

        // 4. Filter incoming transactions: only active ones
        val activeIncomingTx = remoteBundle.transactions.filter { tx ->
            val key = getTransactionKey(tx.date, tx.productName, tx.quantity, tx.unitPrice, tx.supplier ?: "")
            !combinedDeletedTx.contains(key) && tx.isDeleted != true
        }

        // 5. Merge active products & active transactions
        val currentLocalProducts = repository.getAllProductsDirect()
        val currentLocalTx = repository.getAllTransactionsDirect().filter { it.isDeleted == 0 }

        val mergedTransactions = mergeTransactions(currentLocalTx, activeIncomingTx)
        val mergedProducts = mergeProducts(currentLocalProducts, activeIncomingProducts)

        if (mergedTransactions.inserted.isNotEmpty()) {
            repository.insertTransactions(mergedTransactions.inserted)
        }
        if (mergedProducts.inserted.isNotEmpty()) {
            repository.insertProducts(mergedProducts.inserted)
        }

        // Restore Categories
        remoteBundle.categories?.forEach { catName ->
            if (catName.isNotBlank()) {
                repository.insertCategory(CategoryEntity(name = catName))
            }
        }

        // Restore Procurement Plans
        val existingPlans = repository.getAllProcurementPlansDirect()
        var restoredPlansCount = 0
        remoteBundle.procurementPlans?.forEach { planSync ->
            val isDuplicate = existingPlans.any { it.plan.date == planSync.date && it.plan.supplierName.equals(planSync.supplierName, ignoreCase = true) }
            if (!isDuplicate) {
                val planEntity = ProcurementPlanEntity(
                    date = planSync.date,
                    supplierName = planSync.supplierName
                )
                val items = planSync.items.map { itemSync ->
                    ProcurementItemEntity(
                        planId = 0,
                        productName = itemSync.productName,
                        targetQuantity = itemSync.targetQuantity,
                        estimatedUnitPrice = itemSync.estimatedUnitPrice,
                        unit = itemSync.unit,
                        isBought = itemSync.isBought
                    )
                }
                repository.insertProcurementPlanWithItems(planEntity, items)
                restoredPlansCount++
            }
        }

        // Restore App Settings (Theme, UI Customization, Units)
        remoteBundle.appSettings?.let { settings ->
            setThemeMode(settings.themeMode)
            setCustomColor(settings.customColor)
            setFontSizeScale(settings.fontSizeScale)
            setPaddingScale(settings.paddingScale)
            setButtonHeight(settings.buttonHeight)
            setCornerRadius(settings.cornerRadius)
            setCustomTextColor(settings.customTextColor)
            val unitSet = settings.customUnits.toSet()
            _customUnits.value = settings.customUnits.sorted()
            appPrefs?.edit { putStringSet("custom_units", unitSet) }

            val subCatSet = settings.customSubCategories.toSet()
            _customSubCategories.value = settings.customSubCategories.sorted()
            appPrefs?.edit { putStringSet("custom_sub_categories", subCatSet) }
        }

        val totalTxRestored = mergedTransactions.inserted.size
        val totalProdRestored = mergedProducts.inserted.size
        val totalExistingTx = remoteBundle.transactions.size - totalTxRestored

        val note = if (totalExistingTx > 0) " ($totalExistingTx sudah ada sebelumnya)" else ""
        val msg = if (totalTxRestored > 0 || totalProdRestored > 0 || restoredPlansCount > 0) {
            "Berhasil memulihkan: $totalTxRestored transaksi, $totalProdRestored produk, & $restoredPlansCount rencana$note"
        } else {
            "Data cadangan terbaca (${remoteBundle.transactions.size} transaksi & ${remoteBundle.products.size} produk sudah ada di aplikasi)"
        }
        return RestoreResult(true, msg)
    }

    fun restoreBackupFromDevice(context: Context, uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        try {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Some providers do not support persistable URI permissions; ignore safely
            }

            val rawData = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            if (rawData.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "File cadangan kosong atau tidak dapat dibaca!", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val res = processRestoreDataInternal(context, rawData)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
            }
            if (res.success) {
                triggerAutoSync(forceOverwriteRemote = true)
            }
        } catch (secEx: SecurityException) {
            AppLogger.e("Restore", "Permission denied opening URI", secEx)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Akses file ditolak (Permission Denied). Silakan pilih ulang file cadangan .json.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            AppLogger.e("Restore", "Gagal restore dari device", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Gagal memulihkan cadangan: ${e.localizedMessage ?: "Terjadi kesalahan"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun restoreFromAutoBackup(context: Context, onResult: ((Boolean, String) -> Unit)? = null) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val file = getBackupFile(context)
            val extFile = context.getExternalFilesDir("backups")?.let { File(it, "eco_tobacco_sync_data.json") }
            val targetFile = if (file.exists() && file.length() > 0L) file else if (extFile != null && extFile.exists() && extFile.length() > 0L) extFile else null

            if (targetFile == null || !targetFile.exists() || targetFile.length() == 0L) {
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false, "Belum ada file cadangan otomatis di penyimpanan aplikasi.")
                }
                return@launch
            }

            val rawData = targetFile.readText()
            if (rawData.isBlank()) {
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false, "File cadangan otomatis kosong.")
                }
                return@launch
            }

            val res = processRestoreDataInternal(context, rawData)
            withContext(Dispatchers.Main) {
                onResult?.invoke(res.success, res.message)
            }
            if (res.success) {
                triggerAutoSync(forceOverwriteRemote = true)
            }
        } catch (e: Exception) {
            AppLogger.e("Restore", "Gagal restore dari auto backup", e)
            withContext(Dispatchers.Main) {
                onResult?.invoke(false, "Gagal memulihkan cadangan: ${e.localizedMessage ?: "Terjadi kesalahan"}")
            }
        }
    }

    val devLogs: StateFlow<List<AppLogger.LogEntry>> = AppLogger.logs
    fun clearDevLogs() = AppLogger.clear()
    fun clearDatabase() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAllTransactions()
        repository.deleteAllProducts()
        repository.deleteAllProcurement()
        
        // Clear sync metadata (deleted keys) but NOT drive credentials
        val syncPrefs = getApplication<Application>().getSharedPreferences("tobacco_sync_prefs", Context.MODE_PRIVATE)
        syncPrefs.edit(commit = true) { clear() }
        
        // Refresh sync status to disconnected if something went wrong, 
        // or just keep current connection if tokens are still valid.
        if (syncHelper.isConnected && syncHelper.userEmail == null) {
             // If connected but no email, try to fetch it
             val token = getApplication<Application>().getSharedPreferences("google_sync_prefs", Context.MODE_PRIVATE).getString("access_token", null)
             if (token != null) syncHelper.fetchUserEmail(token)
        }

        withContext(Dispatchers.Main) {
            _syncStatus.value = if (syncHelper.isConnected) SyncStatus.SYNCED else SyncStatus.DISCONNECTED
        }
    }

    fun forceSyncNow(onProgress: (String) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        AppLogger.d("Sync", "forceSyncNow clicked")
        withContext(Dispatchers.Main) { onProgress("Memulai sinkronisasi Cloud & pembuatan backup...") }
        var hasSynced = false

        // 1. Sync & Backup ke Cloud Firestore
        try {
            var uid = firebaseAuth.uid ?: syncHelper.userGoogleSub ?: syncHelper.userEmail
            if (uid.isNullOrBlank() && syncHelper.isLocalUserConnected) {
                uid = syncHelper.userEmail
            }
            if (uid.isNullOrBlank()) {
                syncHelper.setLocalUserSession("eco_guest_user@ecotobacco.cloud")
                uid = "eco_guest_user"
            }
            val currentProducts = products.first()
            val currentTx = transactions.first()
            val currentPlans = procurementPlans.first()
            val currentCategories = categories.first()
            val backupJson = generateBackupJson()

            val backupName = "eco_tobacco_cloud_backup.json"
            saveToLocalCloudMirror(backupName, backupJson)

            val fsResult = firestoreSync.syncAllToFirestore(
                userId = uid,
                products = currentProducts,
                transactions = currentTx,
                plans = currentPlans,
                categories = currentCategories,
                backupJson = backupJson
            )
            if (fsResult.isSuccess) {
                hasSynced = true
            }
        } catch (e: Exception) {
            AppLogger.w("Sync", "Firestore sync in forceSyncNow notice: ${e.message}")
        }

        // 2. Sync Google Drive jika terhubung
        if (syncHelper.isConnected) {
            syncData(forceOverwriteRemote = true) { progressMsg ->
                viewModelScope.launch(Dispatchers.Main) { onProgress(progressMsg) }
            }
            hasSynced = true
        }

        fetchDriveBackupFiles()

        withContext(Dispatchers.Main) {
            if (hasSynced) {
                onProgress("Sinkronisasi Cloud Berhasil & Backup Siap! ✅")
            } else {
                onProgress("Data berhasil dicadangkan di penyimpanan Cloud! ✅")
            }
        }
    }

    fun uploadLocalFileToDrive(context: Context, uri: Uri, onProgress: (String) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        if (!syncHelper.isConnected) {
            withContext(Dispatchers.Main) { onProgress("Gagal: Drive belum terhubung") }
            return@launch
        }

        try {
            withContext(Dispatchers.Main) { onProgress("Membaca file lokal...") }
            val jsonData = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            
            if (jsonData.isNullOrBlank()) {
                withContext(Dispatchers.Main) { onProgress("Gagal: File kosong atau tidak terbaca") }
                return@launch
            }

            // Validasi format JSON sebentar
            try {
                JSONObject(jsonData)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onProgress("Gagal: Format file bukan backup valid") }
                return@launch
            }

            withContext(Dispatchers.Main) { onProgress("Mengupload file ke Google Drive...") }
            val success = syncHelper.uploadBackupSync(jsonData)
            
            withContext(Dispatchers.Main) {
                if (success) {
                    _syncStatus.value = SyncStatus.SYNCED
                    onProgress("Berhasil upload backup ke Drive! ✅")
                } else {
                    onProgress("Gagal upload ke Drive ❌")
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Sync", "Manual upload failed", e)
            withContext(Dispatchers.Main) { onProgress("Error: ${e.message}") }
        }
    }

    fun resetSharedPreferences() {
        val syncPrefs = getApplication<Application>().getSharedPreferences("tobacco_sync_prefs", Context.MODE_PRIVATE)
        val appPrefs = getApplication<Application>().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        syncPrefs.edit(commit = true) { clear() }
        appPrefs.edit(commit = true) { clear() }
    }
}
