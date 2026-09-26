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
    fun signInWithGoogle(activity: Activity, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = firebaseAuth.signInWithGoogle(activity)
            if (result.isSuccess) {
                syncFirestore()
                onComplete(true, "Berhasil masuk: ${result.getOrNull()?.displayName ?: "Pengguna Google"}")
            } else {
                onComplete(false, result.exceptionOrNull()?.localizedMessage ?: "Gagal masuk Google")
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
                onComplete(false, result.exceptionOrNull()?.localizedMessage ?: "Gagal masuk tamu")
            }
        }
    }

    fun signOutFirebase() {
        firebaseAuth.signOut()
    }

    fun syncFirestore(onComplete: ((Boolean, String) -> Unit)? = null) {
        val uid = firebaseAuth.uid ?: run {
            onComplete?.invoke(false, "Silakan login terlebih dahulu untuk sinkronisasi Cloud Firestore")
            return
        }
        viewModelScope.launch {
            try {
                val currentProducts = products.first()
                val currentTx = transactions.first()
                val currentPlans = procurementPlans.first()
                val currentCategories = categories.first()

                val result = firestoreSync.syncAllToFirestore(
                    userId = uid,
                    products = currentProducts,
                    transactions = currentTx,
                    plans = currentPlans,
                    categories = currentCategories
                )
                if (result.isSuccess) {
                    val summary = result.getOrNull()
                    val msg = "Berhasil sinkron Firestore: ${summary?.uploadedProducts ?: 0} produk, ${summary?.uploadedTransactions ?: 0} transaksi"
                    onComplete?.invoke(true, msg)
                } else {
                    onComplete?.invoke(false, result.exceptionOrNull()?.localizedMessage ?: "Gagal sinkron Firestore")
                }
            } catch (e: Exception) {
                onComplete?.invoke(false, e.localizedMessage ?: "Error sinkronisasi")
            }
        }
    }

    fun restoreFromFirestore(onComplete: ((Boolean, String) -> Unit)? = null) {
        val uid = firebaseAuth.uid ?: run {
            onComplete?.invoke(false, "Silakan login terlebih dahulu untuk memulihkan dari Firestore")
            return
        }
        viewModelScope.launch {
            try {
                val result = firestoreSync.fetchAndRestoreFromFirestore(uid, repository)
                if (result.isSuccess) {
                    val summary = result.getOrNull()
                    val msg = "Berhasil memulihkan: ${summary?.restoredProducts ?: 0} produk, ${summary?.restoredTransactions ?: 0} transaksi"
                    onComplete?.invoke(true, msg)
                } else {
                    onComplete?.invoke(false, result.exceptionOrNull()?.localizedMessage ?: "Gagal memulihkan dari Firestore")
                }
            } catch (e: Exception) {
                onComplete?.invoke(false, e.localizedMessage ?: "Error pemulihan Firestore")
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

    private fun triggerAutoSync(forceOverwriteRemote: Boolean = false) {
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

    fun addCategory(name: String) = viewModelScope.launch(Dispatchers.IO) { repository.insertCategory(
        CategoryEntity(name = name)
    ) }
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
    }

    fun updateProcurementPlan(plan: ProcurementPlanEntity, items: List<ProcurementItemEntity>) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateProcurementPlanWithItems(plan, items)
    }

    fun deleteProcurementPlan(plan: ProcurementPlanEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteItemsByPlanId(plan.id)
        repository.deleteProcurementPlan(plan)
    }

    fun toggleProcurementItemStatus(item: ProcurementItemEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateProcurementItem(item.copy(isBought = !item.isBought))
    }

    fun removeItemFromPlan(item: ProcurementItemEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteProcurementItem(item)
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
            lastUpdated = System.currentTimeMillis(),
            deletedTransactions = getDeletedTransactionKeys().toList()
        )
        
        return adapter.toJson(syncBundle)
    }

    fun exportBackupToDevice(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val jsonData = generateBackupJson()
            val fileName = "eco_tobacco_sync_data.json"
            
            // Simpan ke direktori app internal & external files folder aplikasi (tanpa perlu izin all files)
            val appFilesDir = context.getExternalFilesDir("backups") ?: File(context.filesDir, "backups")
            if (!appFilesDir.exists()) {
                appFilesDir.mkdirs()
            }
            val file = File(appFilesDir, fileName)
            file.writeText(jsonData)
            
            AppLogger.d("Backup", "Backup otomatis tersimpan di folder aplikasi: ${file.absolutePath}")
        } catch (e: Exception) {
            AppLogger.e("Backup", "Gagal auto-backup ke internal storage", e)
        }
    }

    fun writeBackupToUri(context: Context, uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val jsonData = generateBackupJson()
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(jsonData.toByteArray(Charsets.UTF_8))
                stream.flush()
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

    fun restoreBackupFromDevice(context: Context, uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val jsonData = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            if (jsonData.isNullOrBlank()) return@launch
            
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(SyncDataBundle::class.java)
            val remoteBundle = adapter.fromJson(jsonData) ?: return@launch
            
            val localTransactionsData = repository.getAllTransactionsDirect()
            val localProductsData = repository.getAllProductsDirect()
            
            val combinedDeleted = (getDeletedTransactionKeys() + (remoteBundle.deletedTransactions ?: emptyList())).toSet()
            addDeletedTransactionKeys(combinedDeleted)
            
            val activeLocal = localTransactionsData.filter { tx ->
                val key = getTransactionKey(tx.date, tx.productName, tx.quantity, tx.unitPrice, tx.supplier)
                if (combinedDeleted.contains(key)) { repository.deleteTransactionById(tx.id); false } else true
            }
            val activeRemote = remoteBundle.transactions.filter { r -> !combinedDeleted.contains(getTransactionKey(r.date, r.productName, r.quantity, r.unitPrice, r.supplier ?: "")) }

            val mergedTransactions = mergeTransactions(activeLocal, activeRemote)
            val mergedProducts = mergeProducts(localProductsData, remoteBundle.products)
            
            if (mergedTransactions.inserted.isNotEmpty()) repository.insertTransactions(mergedTransactions.inserted)
            if (mergedProducts.inserted.isNotEmpty()) repository.insertProducts(mergedProducts.inserted)
            
            // Restore Procurement Plans
            val existingPlans = repository.getAllProcurementPlansDirect()
            remoteBundle.procurementPlans?.forEach { planSync ->
                // Check if plan already exists (simple check by date and supplier)
                val isDuplicate = existingPlans.any { it.plan.date == planSync.date && it.plan.supplierName == planSync.supplierName }
                if (!isDuplicate) {
                    val planEntity = ProcurementPlanEntity(
                        date = planSync.date,
                        supplierName = planSync.supplierName
                    )
                    val items = planSync.items.map { itemSync ->
                        ProcurementItemEntity(
                            planId = 0, // Will be set by repository
                            productName = itemSync.productName,
                            targetQuantity = itemSync.targetQuantity,
                            estimatedUnitPrice = itemSync.estimatedUnitPrice,
                            unit = itemSync.unit,
                            isBought = itemSync.isBought
                        )
                    }
                    repository.insertProcurementPlanWithItems(planEntity, items)
                }
            }
            
            withContext(Dispatchers.Main) { Toast.makeText(context, "Restore berhasil!", Toast.LENGTH_SHORT).show() }
            triggerAutoSync(forceOverwriteRemote = true)
        } catch (e: Exception) {
            AppLogger.e("Restore", "Gagal restore dari device", e)
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
