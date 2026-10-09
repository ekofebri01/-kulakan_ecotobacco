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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.Locale
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
    val firestoreSyncProgress = firestoreSync.syncProgress
    val firestoreLastSync = firestoreSync.lastSyncedTime
    val firestoreMessage = firestoreSync.syncMessage

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
    }

    fun setCustomColor(colorInt: Int) {
        _customColor.value = colorInt
        appPrefs?.edit { putInt("custom_color", colorInt) }
    }

    fun setFontSizeScale(scale: Float) {
        _fontSizeScale.value = scale
        appPrefs.edit { putFloat("ui_font_size_scale", scale) }
    }

    fun setPaddingScale(scale: Float) {
        _paddingScale.value = scale
        appPrefs.edit { putFloat("ui_padding_scale", scale) }
    }

    fun setButtonHeight(height: Int) {
        _buttonHeight.value = height
        appPrefs.edit { putInt("ui_button_height", height) }
    }

    fun setCornerRadius(radius: Int) {
        _cornerRadius.value = radius
        appPrefs.edit { putInt("ui_corner_radius", radius) }
    }

    fun setCustomTextColor(colorInt: Int) {
        _customTextColor.value = colorInt
        appPrefs.edit { putInt("ui_custom_text_color", colorInt) }
    }

    fun resetUiCustomization() {
        setFontSizeScale(1.0f)
        setPaddingScale(1.0f)
        setButtonHeight(56)
        setCornerRadius(16)
        setCustomTextColor(0)
    }

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

    fun getBackupFile(context: Context): File {
        val internalDir = File(context.filesDir, "backups")
        if (!internalDir.exists()) internalDir.mkdirs()
        return File(internalDir, "eco_tobacco_sync_data.json")
    }

    fun loadAutoBackupInfo(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val file = getBackupFile(context)
            val extFile = context.getExternalFilesDir("backups")?.let { File(it, "eco_tobacco_sync_data.json") }
            val targetFile = if (file.exists() && file.length() > 0L) file else if (extFile != null && extFile.exists() && extFile.length() > 0L) extFile else null

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

    fun fetchDriveBackupFiles() = viewModelScope.launch {
        _driveBackupFiles.value = syncHelper.listBackupFiles()
    }

    fun deleteDriveBackupFile(fileId: String, onComplete: (Boolean) -> Unit) = viewModelScope.launch {
        val success = syncHelper.deleteFile(fileId)
        if (success) {
            fetchDriveBackupFiles()
        }
        onComplete(success)
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

                val result = firestoreSync.syncAllToFirestore(
                    userId = finalUid,
                    products = currentProducts,
                    transactions = currentTx,
                    plans = currentPlans,
                    categories = currentCategories
                )
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
                    val msg = "Berhasil memulihkan: ${summary?.restoredProducts ?: 0} produk, ${summary?.restoredTransactions ?: 0} transaksi"
                    onComplete?.invoke(true, msg)
                } else {
                    onComplete?.invoke(false, result.exceptionOrNull()?.localizedMessage ?: "Gagal memulihkan dari Cloud")
                }
            } catch (e: Exception) {
                onComplete?.invoke(false, e.localizedMessage ?: "Error pemulihan Cloud")
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
                                it.contentQuantity
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
                    contentQuantity = it.contentQuantity ?: 1.0
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
    fun addProduct(name: String, price: Double, unit: String = "kg", sellingPrice: Double = 0.0, sellingUnit: String = "pcs", contentQuantity: Double = 1.0) = viewModelScope.launch { 
        val product = ProductEntity(name = name, price = price, unit = unit, sellingPrice = sellingPrice, sellingUnit = sellingUnit, contentQuantity = contentQuantity)
        repository.insertProduct(product)
        firebaseAuth.uid?.let { uid ->
            firestoreSync.persistProduct(uid, product)
        }
        triggerAutoSync(forceOverwriteRemote = true) 
    }
    fun updateProduct(id: Int, name: String, price: Double, unit: String = "kg", sellingPrice: Double = 0.0, sellingUnit: String = "pcs", contentQuantity: Double = 1.0) = viewModelScope.launch { 
        val product = ProductEntity(id = id, name = name, price = price, unit = unit, sellingPrice = sellingPrice, sellingUnit = sellingUnit, contentQuantity = contentQuantity)
        repository.updateProduct(product)
        firebaseAuth.uid?.let { uid ->
            firestoreSync.persistProduct(uid, product)
        }
        triggerAutoSync(forceOverwriteRemote = true) 
    }
    fun deleteProduct(id: Int) = viewModelScope.launch { repository.deleteProductById(id); triggerAutoSync(forceOverwriteRemote = true) }
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
        if (tx != null) addDeletedTransactionKey(getTransactionKey(tx.date, tx.productName, tx.quantity, tx.unitPrice, tx.supplier))
        repository.deleteTransactionById(id)
        triggerAutoSync(forceOverwriteRemote = true)
    }
    fun deleteTransactions(ids: List<Int>) = viewModelScope.launch {
        ids.forEach { id ->
            val tx = repository.getTransactionById(id)
            if (tx != null) addDeletedTransactionKey(getTransactionKey(tx.date, tx.productName, tx.quantity, tx.unitPrice, tx.supplier))
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
        val localTransactionsData = repository.getAllTransactionsDirect()
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
                    it.sellingUnit
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
                fontSizeScale = _fontSizeScale.value,
                paddingScale = _paddingScale.value,
                buttonHeight = _buttonHeight.value,
                cornerRadius = _cornerRadius.value,
                customTextColor = _customTextColor.value
            ),
            categories = localCategoriesData,
            lastUpdated = System.currentTimeMillis(),
            deletedTransactions = getDeletedTransactionKeys().toList()
        )
        
        return adapter.toJson(syncBundle)
    }

    fun exportBackupToDevice(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val jsonData = generateBackupJson()
            val fileName = "eco_tobacco_sync_data.json"
            
            // 1. Simpan ke internal storage (selalu ada, aman, tidak butuh izin runtime)
            val internalBackupDir = File(context.filesDir, "backups")
            if (!internalBackupDir.exists()) {
                internalBackupDir.mkdirs()
            }
            val internalFile = File(internalBackupDir, fileName)
            internalFile.writeText(jsonData)

            // 2. Simpan cadangan ke external files directory aplikasi jika tersedia
            try {
                val appFilesDir = context.getExternalFilesDir("backups")
                if (appFilesDir != null) {
                    if (!appFilesDir.exists()) appFilesDir.mkdirs()
                    val extFile = File(appFilesDir, fileName)
                    extFile.writeText(jsonData)
                }
            } catch (e: Exception) {
                AppLogger.w("Backup", "External files write skipped: ${e.message}")
            }
            
            AppLogger.d("Backup", "Backup otomatis tersimpan di: ${internalFile.absolutePath} (${internalFile.length()} bytes)")
            
            context.getSharedPreferences("tobacco_sync_prefs", Context.MODE_PRIVATE)
                .edit { putLong("last_auto_backup_time", System.currentTimeMillis()) }

            loadAutoBackupInfo(context)
        } catch (e: Exception) {
            AppLogger.e("Backup", "Gagal auto-backup ke internal storage", e)
        }
    }

    fun performManualAutoBackup(context: Context, onResult: ((Boolean, String) -> Unit)? = null) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val jsonData = generateBackupJson()
            val fileName = "eco_tobacco_sync_data.json"
            
            val internalBackupDir = File(context.filesDir, "backups")
            if (!internalBackupDir.exists()) internalBackupDir.mkdirs()
            val internalFile = File(internalBackupDir, fileName)
            internalFile.writeText(jsonData)

            try {
                val appFilesDir = context.getExternalFilesDir("backups")
                if (appFilesDir != null) {
                    if (!appFilesDir.exists()) appFilesDir.mkdirs()
                    File(appFilesDir, fileName).writeText(jsonData)
                }
            } catch (e: Exception) {
                // ignore
            }

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

            return ProductSyncModel(
                id = obj.optInt("id", index + 1),
                name = name.trim(),
                price = price,
                unit = unit.trim(),
                sellingPrice = sellingPrice,
                sellingUnit = sellingUnit.trim(),
                contentQuantity = contentQty
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
                appSettings = AppSettingsSyncModel(
                    themeMode = settingsObj.optString("themeMode", "SYSTEM"),
                    customColor = settingsObj.optInt("customColor", 0xFF10B981.toInt()),
                    customUnits = if (unitsList.isNotEmpty()) unitsList else listOf("kg", "ons", "pcs"),
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

        // Remove tombstone blocks for explicitly restored transactions so they are restored correctly
        val currentDeleted = getDeletedTransactionKeys().toMutableSet()
        remoteBundle.transactions.forEach { tx ->
            val key = getTransactionKey(tx.date, tx.productName, tx.quantity, tx.unitPrice, tx.supplier ?: "")
            currentDeleted.remove(key)
        }
        val syncPrefs = getApplication<Application>().getSharedPreferences("tobacco_sync_prefs", Context.MODE_PRIVATE)
        syncPrefs.edit { putString("deleted_tx_keys_str", currentDeleted.joinToString("\n")) }

        val mergedTransactions = mergeTransactions(localTransactionsData, remoteBundle.transactions)
        val mergedProducts = mergeProducts(localProductsData, remoteBundle.products)

        if (mergedTransactions.inserted.isNotEmpty()) {
            repository.insertTransactions(mergedTransactions.inserted)
        }
        if (mergedProducts.inserted.isNotEmpty()) {
            repository.insertProducts(mergedProducts.inserted)
        }

        // Also auto-sync products from transactions if not present
        remoteBundle.transactions.forEach { tx ->
            val exists = localProductsData.any { it.name.trim().equals(tx.productName.trim(), ignoreCase = true) } ||
                         mergedProducts.inserted.any { it.name.trim().equals(tx.productName.trim(), ignoreCase = true) }
            if (!exists) {
                repository.insertProduct(ProductEntity(name = tx.productName.trim(), price = tx.unitPrice, unit = tx.unit ?: "kg"))
            }
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
            val rawData = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            if (rawData.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "File cadangan kosong!", Toast.LENGTH_SHORT).show()
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

    fun forceSyncNow(onProgress: (String) -> Unit) = viewModelScope.launch {
        AppLogger.d("Sync", "forceSyncNow clicked")
        if (syncHelper.isConnected) {
            syncData(forceOverwriteRemote = true, onProgress = onProgress)
        } else {
            AppLogger.log("Sync", "forceSyncNow skipped: Not connected", AppLogger.LogType.WARN)
            onProgress("Gagal: Login Google Drive terlebih dahulu")
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
