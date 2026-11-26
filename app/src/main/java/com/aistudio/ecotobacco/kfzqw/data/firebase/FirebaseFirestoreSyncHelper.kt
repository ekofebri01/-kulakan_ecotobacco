package com.aistudio.ecotobacco.kfzqw.data.firebase

import android.content.Context
import android.util.Log
import com.aistudio.ecotobacco.kfzqw.data.local.entities.CategoryEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProcurementItemEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProcurementPlanEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProcurementPlanWithItems
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProductEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.TransactionEntity
import com.aistudio.ecotobacco.kfzqw.data.repository.TobaccoRepository
import com.aistudio.ecotobacco.kfzqw.data.remote.DriveBackupFile
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class FirestoreSyncStatus {
    IDLE,
    SYNCING,
    SYNCED,
    ERROR
}

data class FirestoreSyncResult(
    val uploadedProducts: Int,
    val uploadedTransactions: Int,
    val uploadedPlans: Int,
    val timestamp: Long
)

data class FirestoreRestoreResult(
    val restoredProducts: Int,
    val restoredTransactions: Int,
    val restoredPlans: Int,
    val timestamp: Long,
    val backupJson: String? = null
)

class FirebaseFirestoreSyncHelper(private val context: Context) {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            ensureFirebaseInitialized(context)
            val dbIdRes = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
            val dbId = if (dbIdRes != 0) context.getString(dbIdRes) else "ai-studio-android-ecotobac-7baa973e-7c16-4d74-8a86-4a1ef92fd4a0"
            FirebaseFirestore.getInstance(FirebaseApp.getInstance(), dbId)
        } catch (e: Exception) {
            Log.w("FirestoreSyncHelper", "Firestore named db init notice: ${e.message}")
            try {
                FirebaseFirestore.getInstance()
            } catch (ex: Exception) {
                null
            }
        }
    }

    private fun ensureFirebaseInitialized(ctx: Context) {
        try {
            if (FirebaseApp.getApps(ctx).isEmpty()) {
                val resId = ctx.resources.getIdentifier("google_app_id", "string", ctx.packageName)
                if (resId != 0) {
                    FirebaseApp.initializeApp(ctx.applicationContext)
                } else {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:680872469505:android:983bf0e74d3ab7cd998c00")
                        .setApiKey("AIzaSyCfVEKVF9LLUWYxvyr5OKzccq1FJz_4-6I")
                        .setProjectId("project-0be2da66-9971-458e-bfe")
                        .setStorageBucket("project-0be2da66-9971-458e-bfe.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(ctx.applicationContext, options)
                }
            }
        } catch (e: Exception) {
            Log.w("FirestoreSyncHelper", "FirebaseApp init: ${e.message}")
        }
    }

    private val prefs = context.getSharedPreferences("firebase_sync_prefs", Context.MODE_PRIVATE)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatus = MutableStateFlow<FirestoreSyncStatus>(
        if (prefs.getLong("last_firestore_sync_time", -1L) > 0) FirestoreSyncStatus.SYNCED else FirestoreSyncStatus.IDLE
    )
    val syncStatus: StateFlow<FirestoreSyncStatus> = _syncStatus.asStateFlow()

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

    private val _syncProgress = MutableStateFlow(0f)
    val syncProgress: StateFlow<Float> = _syncProgress.asStateFlow()

    private val _lastSyncedTime = MutableStateFlow<Long?>(
        prefs.getLong("last_firestore_sync_time", -1L).takeIf { it > 0 }
    )
    val lastSyncedTime: StateFlow<Long?> = _lastSyncedTime.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    /**
     * Uploads all current Room local data to Cloud Firestore under the user's document
     */
    suspend fun syncAllToFirestore(
        userId: String,
        products: List<ProductEntity>,
        transactions: List<TransactionEntity>,
        plans: List<ProcurementPlanWithItems>,
        categories: List<CategoryEntity>,
        backupJson: String? = null
    ): Result<FirestoreSyncResult> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User ID is empty"))
        }
        val fs = firestore ?: return@withContext Result.failure(IllegalStateException("Cloud Firestore tidak tersedia"))

        _syncStatus.value = FirestoreSyncStatus.SYNCING
        _isSyncing.value = true
        _lastErrorMessage.value = null
        _syncProgress.value = 0.05f
        _syncMessage.value = "Menyiapkan data stok & transaksi..."

        try {
            kotlinx.coroutines.withTimeout(45000) {
                val userDoc = fs.collection("users").document(userId)
                val now = System.currentTimeMillis()

                // 1. Update metadata
                val metadata = hashMapOf(
                    "appName" to "ECO TOBACCO",
                    "lastSyncTimestamp" to now,
                    "totalProducts" to products.size,
                    "totalTransactions" to transactions.size,
                    "totalPlans" to plans.size,
                    "totalCategories" to categories.size,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
                userDoc.collection("data").document("metadata")
                    .set(metadata, SetOptions.merge()).await()
                _syncProgress.value = 0.15f

            // 2. Batch write products (Stock Data)
            if (products.isNotEmpty()) {
                val productBatches = products.chunked(200)
                val totalProductBatches = productBatches.size
                productBatches.forEachIndexed { index, batchList ->
                    val uploadedCount = (index * 200) + batchList.size
                    _syncMessage.value = "Mengunggah data stok ($uploadedCount/${products.size} barang)..."
                    val batch = fs.batch()
                    for (p in batchList) {
                        val pDoc = userDoc.collection("products").document(p.id.toString())
                        val pData = hashMapOf(
                            "id" to p.id,
                            "name" to p.name,
                            "price" to p.price,
                            "unit" to p.unit,
                            "sellingPrice" to p.sellingPrice,
                            "sellingUnit" to p.sellingUnit,
                            "contentQuantity" to p.contentQuantity,
                            "updatedAt" to now
                        )
                        batch.set(pDoc, pData, SetOptions.merge())
                    }
                    batch.commit().await()
                    val batchProgress = 0.15f + ((index + 1).toFloat() / totalProductBatches) * 0.40f
                    _syncProgress.value = batchProgress
                }
            } else {
                _syncProgress.value = 0.55f
            }

            // 3. Batch write transactions
            if (transactions.isNotEmpty()) {
                val txBatches = transactions.chunked(200)
                val totalTxBatches = txBatches.size
                txBatches.forEachIndexed { index, batchList ->
                    val uploadedTx = (index * 200) + batchList.size
                    _syncMessage.value = "Mengunggah riwayat nota ($uploadedTx/${transactions.size})..."
                    val batch = fs.batch()
                    for (tx in batchList) {
                        val txDoc = userDoc.collection("transactions").document(tx.id.toString())
                        val txData = hashMapOf(
                            "id" to tx.id,
                            "date" to tx.date,
                            "productName" to tx.productName,
                            "quantity" to tx.quantity,
                            "unitPrice" to tx.unitPrice,
                            "total" to tx.total,
                            "supplier" to (tx.supplier ?: ""),
                            "unit" to (tx.unit ?: "kg"),
                            "isDeleted" to tx.isDeleted,
                            "updatedAt" to now
                        )
                        batch.set(txDoc, txData, SetOptions.merge())
                    }
                    batch.commit().await()
                    val batchProgress = 0.55f + ((index + 1).toFloat() / totalTxBatches) * 0.30f
                    _syncProgress.value = batchProgress
                }
            } else {
                _syncProgress.value = 0.85f
            }

            // 4. Batch write procurement plans
            if (plans.isNotEmpty()) {
                _syncMessage.value = "Mengunggah rencana kulakan..."
                val planBatch = fs.batch()
                for (planWithItems in plans) {
                    val planDoc = userDoc.collection("procurement_plans").document(planWithItems.plan.id.toString())
                    val itemsList = planWithItems.items.map { item ->
                        hashMapOf(
                            "id" to item.id,
                            "planId" to item.planId,
                            "productName" to item.productName,
                            "targetQuantity" to item.targetQuantity,
                            "estimatedUnitPrice" to item.estimatedUnitPrice,
                            "unit" to item.unit,
                            "isBought" to item.isBought
                        )
                    }
                    val planData = hashMapOf(
                        "id" to planWithItems.plan.id,
                        "date" to planWithItems.plan.date,
                        "supplierName" to planWithItems.plan.supplierName,
                        "items" to itemsList,
                        "updatedAt" to now
                    )
                    planBatch.set(planDoc, planData, SetOptions.merge())
                }
                planBatch.commit().await()
            }
            _syncProgress.value = 0.95f

            // 5. Simpan Snapshot File Backup ke Collection 'backups' di Firestore
            try {
                val backupId = "cloud_backup_main"
                val backupName = "eco_tobacco_cloud_backup.json"
                val backupMap = hashMapOf(
                    "id" to backupId,
                    "name" to backupName,
                    "timestamp" to now,
                    "productCount" to products.size,
                    "transactionCount" to transactions.size,
                    "planCount" to plans.size,
                    "source" to "Cloud Firestore",
                    "jsonData" to (backupJson ?: "")
                )
                userDoc.collection("backups").document(backupId)
                    .set(backupMap, SetOptions.merge()).await()

                if (!backupJson.isNullOrBlank()) {
                    userDoc.collection("data").document("latest_backup")
                        .set(hashMapOf("jsonData" to backupJson, "timestamp" to now), SetOptions.merge()).await()
                }
            } catch (e: Exception) {
                Log.w("FirestoreSyncHelper", "Snapshot backup doc notice: ${e.message}")
            }

            // 6. Save preferences
            prefs.edit().putLong("last_firestore_sync_time", now).apply()
            _lastSyncedTime.value = now
            _syncProgress.value = 1.0f
            _syncMessage.value = "Sinkronisasi selesai! ${products.size} stok & ${transactions.size} transaksi tersimpan di Firestore."
            _syncStatus.value = FirestoreSyncStatus.SYNCED
            _lastErrorMessage.value = null
            _isSyncing.value = false

            val result = FirestoreSyncResult(
                uploadedProducts = products.size,
                uploadedTransactions = transactions.size,
                uploadedPlans = plans.size,
                timestamp = now
            )
            return@withTimeout Result.success(result)
          }
        } catch (e: Exception) {
            Log.w("FirestoreSyncHelper", "Sync to Firestore error: ${e.message}")
            _isSyncing.value = false
            _syncStatus.value = FirestoreSyncStatus.ERROR
            val errMsg = e.localizedMessage ?: "Gagal terhubung ke Cloud Firestore"
            _lastErrorMessage.value = errMsg
            _syncMessage.value = "Gagal sinkron Cloud: $errMsg"
            return@withContext Result.failure(e)
        }
    }

    /**
     * Lists all cloud backup snapshots stored in Firestore for the given user.
     * Avoids duplicate/synthetic entries so each snapshot appears once.
     */
    suspend fun listCloudBackups(userId: String): List<DriveBackupFile> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext emptyList()
        val fs = firestore ?: return@withContext emptyList()
        val list = mutableListOf<DriveBackupFile>()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val userDoc = fs.collection("users").document(userId)

        // 1. Query 'backups' subcollection (canonical backup snapshots)
        try {
            val snapshot = userDoc.collection("backups").limit(50).get().await()
            val sortedDocs = snapshot.documents.sortedByDescending { it.getLong("timestamp") ?: 0L }
            for (doc in sortedDocs) {
                val id = doc.getString("id") ?: doc.id
                val name = doc.getString("name") ?: "Backup Cloud ($id)"
                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                val prodCount = doc.getLong("productCount")?.toInt() ?: 0
                val txCount = doc.getLong("transactionCount")?.toInt() ?: 0
                val planCount = doc.getLong("planCount")?.toInt() ?: 0
                val jsonData = doc.getString("jsonData")

                val countText = "$prodCount Produk • $txCount Transaksi" + (if (planCount > 0) " • $planCount Rencana" else "")

                list.add(
                    DriveBackupFile(
                        id = id,
                        name = name,
                        timestamp = timestamp,
                        modifiedTime = sdf.format(Date(timestamp)),
                        source = "Cloud Firestore",
                        itemCountText = countText,
                        jsonData = jsonData,
                        ownerUid = userId
                    )
                )
            }
        } catch (e: Exception) {
            Log.w("FirestoreSyncHelper", "Query backups subcollection notice: ${e.message}")
        }

        // 2. Fallback to 'latest_backup' ONLY when 'backups' subcollection is empty
        if (list.isEmpty()) {
            try {
                val latestBackupDoc = userDoc.collection("data").document("latest_backup").get().await()
                val json = if (latestBackupDoc.exists()) latestBackupDoc.getString("jsonData") else null
                if (!json.isNullOrBlank()) {
                    val timestamp = latestBackupDoc.getLong("timestamp") ?: System.currentTimeMillis()
                    var summaryText = "Snapshot Cadangan Cloud Terkini"
                    try {
                        val obj = org.json.JSONObject(json)
                        val p = obj.optJSONArray("products")?.length() ?: 0
                        val t = obj.optJSONArray("transactions")?.length() ?: 0
                        summaryText = "$p Produk • $t Transaksi (Cadangan Cloud)"
                    } catch (e: Exception) { /* ignore */ }
                    list.add(
                        DriveBackupFile(
                            id = "firestore_latest_backup",
                            name = "eco_tobacco_snapshot_terkini.json",
                            timestamp = timestamp,
                            modifiedTime = sdf.format(Date(timestamp)),
                            source = "Cloud Firestore",
                            itemCountText = summaryText,
                            jsonData = json,
                            ownerUid = userId
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w("FirestoreSyncHelper", "Query latest_backup doc notice: ${e.message}")
            }
        }

        list
    }

    /**
     * Gets JSON content of a specific backup snapshot from Firestore
     */
    suspend fun getCloudBackupJson(userId: String, backupId: String): String? = withContext(Dispatchers.IO) {
        if (userId.isBlank() || backupId.isBlank()) return@withContext null
        val fs = firestore ?: return@withContext null
        try {
            val userDoc = fs.collection("users").document(userId)

            if (backupId == "firestore_latest_backup") {
                val doc = userDoc.collection("data").document("latest_backup").get().await()
                val json = doc.getString("jsonData")
                if (!json.isNullOrBlank()) return@withContext json
            }

            // Try subcollection backups
            val doc = userDoc.collection("backups").document(backupId).get().await()
            val json = doc.getString("jsonData")
            if (!json.isNullOrBlank()) return@withContext json

            // Fallback to latest_backup
            val fallbackDoc = userDoc.collection("data").document("latest_backup").get().await()
            fallbackDoc.getString("jsonData")
        } catch (e: Exception) {
            Log.w("FirestoreSyncHelper", "getCloudBackupJson notice: ${e.message}")
            null
        }
    }

    /**
     * Deletes a cloud backup snapshot (or all legacy duplicates tied to it) from Firestore
     */
    suspend fun deleteCloudBackup(userId: String, backupId: String): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank() || backupId.isBlank()) return@withContext false
        val fs = firestore ?: return@withContext false
        try {
            val userDoc = fs.collection("users").document(userId)
            when (backupId) {
                "firestore_latest_sync" -> {
                    userDoc.collection("data").document("metadata").delete().await()
                    userDoc.collection("data").document("latest_backup").delete().await()
                }
                "firestore_latest_backup" -> {
                    userDoc.collection("data").document("latest_backup").delete().await()
                    userDoc.collection("backups").document("cloud_backup_main").delete().await()
                }
                "cloud_backup_main" -> {
                    userDoc.collection("backups").document("cloud_backup_main").delete().await()
                    userDoc.collection("data").document("latest_backup").delete().await()
                }
                else -> {
                    userDoc.collection("backups").document(backupId).delete().await()
                    // Also clean any matching doc whose stored "id" field equals backupId
                    val snap = userDoc.collection("backups").whereEqualTo("id", backupId).get().await()
                    for (d in snap.documents) {
                        d.reference.delete().await()
                    }
                    // If no snapshots remain in backups, remove latest_backup too so it doesn't reappear
                    val remaining = userDoc.collection("backups").limit(1).get().await()
                    if (remaining.isEmpty) {
                        userDoc.collection("data").document("latest_backup").delete().await()
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.w("FirestoreSyncHelper", "deleteCloudBackup error: ${e.message}")
            false
        }
    }

    /**
     * Deletes all backup snapshots in Firestore for the given user.
     */
    suspend fun deleteAllCloudBackups(userId: String): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext false
        val fs = firestore ?: return@withContext false
        try {
            val userDoc = fs.collection("users").document(userId)
            val snapshot = userDoc.collection("backups").get().await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
            userDoc.collection("data").document("latest_backup").delete().await()
            true
        } catch (e: Exception) {
            Log.w("FirestoreSyncHelper", "deleteAllCloudBackups error: ${e.message}")
            false
        }
    }

    /**
     * Restores data from Cloud Firestore into the local Room database
     */
    suspend fun fetchAndRestoreFromFirestore(
        userId: String,
        repository: TobaccoRepository
    ): Result<FirestoreRestoreResult> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User ID is empty"))
        }
        val fs = firestore ?: return@withContext Result.failure(IllegalStateException("Cloud Firestore tidak tersedia"))

        _syncStatus.value = FirestoreSyncStatus.SYNCING
        _isSyncing.value = true
        _lastErrorMessage.value = null
        _syncMessage.value = "Mengunduh data dari Cloud Firestore..."

        try {
            val userDoc = fs.collection("users").document(userId)

            // 0. Fetch backup JSON with settings if available
            var restoredBackupJson: String? = null
            try {
                val backupDoc = userDoc.collection("data").document("latest_backup").get().await()
                if (backupDoc.exists()) {
                    restoredBackupJson = backupDoc.getString("jsonData")
                }
            } catch (e: Exception) {
                Log.w("FirestoreSyncHelper", "Fetch latest_backup notice: ${e.message}")
            }

            // 1. Fetch products
            val productsSnapshot = userDoc.collection("products").get().await()
            var restoredProducts = 0
            for (doc in productsSnapshot.documents) {
                val name = doc.getString("name") ?: continue
                val price = doc.getDouble("price") ?: 0.0
                val unit = doc.getString("unit") ?: "kg"
                val sellingPrice = doc.getDouble("sellingPrice") ?: 0.0
                val sellingUnit = doc.getString("sellingUnit") ?: "pcs"
                val contentQuantity = doc.getDouble("contentQuantity") ?: 1.0

                repository.insertProduct(
                    ProductEntity(
                        id = doc.getLong("id") ?: 0L,
                        name = name,
                        price = price,
                        unit = unit,
                        sellingPrice = sellingPrice,
                        sellingUnit = sellingUnit,
                        contentQuantity = contentQuantity
                    )
                )
                restoredProducts++
            }

            // 2. Fetch transactions
            val txSnapshot = userDoc.collection("transactions").get().await()
            var restoredTransactions = 0
            for (doc in txSnapshot.documents) {
                val productName = doc.getString("productName") ?: continue
                val quantity = doc.getDouble("quantity") ?: 0.0
                val unitPrice = doc.getDouble("unitPrice") ?: 0.0
                val total = doc.getDouble("total") ?: (quantity * unitPrice)
                val date = doc.getLong("date") ?: System.currentTimeMillis()
                val supplier = doc.getString("supplier") ?: ""
                val unit = doc.getString("unit") ?: "kg"
                val isDeletedBool = doc.getBoolean("isDeleted") ?: false

                repository.insertTransaction(
                    TransactionEntity(
                        id = doc.getLong("id") ?: 0L,
                        date = date,
                        productName = productName,
                        quantity = quantity,
                        unitPrice = unitPrice,
                        total = total,
                        supplier = supplier,
                        unit = unit,
                        isDeleted = isDeletedBool
                    )
                )
                restoredTransactions++
            }

            // 3. Fetch procurement plans
            val plansSnapshot = userDoc.collection("procurement_plans").get().await()
            var restoredPlans = 0
            for (doc in plansSnapshot.documents) {
                val supplierName = doc.getString("supplierName") ?: ""
                val date = doc.getLong("date") ?: System.currentTimeMillis()
                val plan = ProcurementPlanEntity(
                    id = doc.getLong("id") ?: 0L,
                    date = date,
                    supplierName = supplierName
                )

                @Suppress("UNCHECKED_CAST")
                val itemsData = doc.get("items") as? List<Map<String, Any>>
                val items = itemsData?.map { itemMap ->
                    ProcurementItemEntity(
                        id = (itemMap["id"] as? Number)?.toLong() ?: 0L,
                        planId = plan.id,
                        productName = (itemMap["productName"] as? String) ?: "",
                        targetQuantity = (itemMap["targetQuantity"] as? Number)?.toDouble() ?: 0.0,
                        estimatedUnitPrice = (itemMap["estimatedUnitPrice"] as? Number)?.toDouble() ?: 0.0,
                        unit = (itemMap["unit"] as? String) ?: "kg",
                        isBought = (itemMap["isBought"] as? Boolean) ?: false
                    )
                } ?: emptyList()

                repository.insertProcurementPlanWithItems(plan, items)
                restoredPlans++
            }

            val now = System.currentTimeMillis()
            prefs.edit().putLong("last_firestore_sync_time", now).apply()
            _lastSyncedTime.value = now
            _isSyncing.value = false
            _syncStatus.value = FirestoreSyncStatus.SYNCED
            _lastErrorMessage.value = null
            _syncMessage.value = "Berhasil memulihkan $restoredProducts produk & $restoredTransactions transaksi dari Firestore!"

            Result.success(
                FirestoreRestoreResult(
                    restoredProducts = restoredProducts,
                    restoredTransactions = restoredTransactions,
                    restoredPlans = restoredPlans,
                    timestamp = now,
                    backupJson = restoredBackupJson
                )
            )
        } catch (e: Exception) {
            Log.w("FirestoreSyncHelper", "Restore from Firestore notice: ${e.message}")
            _isSyncing.value = false
            _syncStatus.value = FirestoreSyncStatus.ERROR
            val errStr = e.message ?: ""
            val userMsg = if (errStr.contains("PERMISSION_DENIED") || errStr.contains("administrator only") || errStr.contains("Missing or insufficient permissions")) {
                "Cloud Firestore dibatasi aturan akses. Data Anda aman tersimpan secara lokal (Room Database) & Google Drive Backup."
            } else {
                "Gagal memulihkan Firestore: ${e.localizedMessage}"
            }
            _lastErrorMessage.value = userMsg
            _syncMessage.value = userMsg
            Result.failure(Exception(userMsg))
        }
    }

    /**
     * Persists a single product immediately to Firestore when modified
     */
    suspend fun persistProduct(userId: String, product: ProductEntity) = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext
        val fs = firestore ?: return@withContext
        try {
            val userDoc = fs.collection("users").document(userId)
            val pDoc = userDoc.collection("products").document(product.id.toString())
            val pData = hashMapOf(
                "id" to product.id,
                "name" to product.name,
                "price" to product.price,
                "unit" to product.unit,
                "sellingPrice" to product.sellingPrice,
                "sellingUnit" to product.sellingUnit,
                "contentQuantity" to product.contentQuantity,
                "updatedAt" to System.currentTimeMillis()
            )
            pDoc.set(pData, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w("FirestoreSyncHelper", "Persist product notice: ${e.message}")
        }
    }

    /**
     * Persists a single transaction immediately to Firestore when created
     */
    suspend fun persistTransaction(userId: String, tx: TransactionEntity) = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext
        val fs = firestore ?: return@withContext
        try {
            val userDoc = fs.collection("users").document(userId)
            val txDoc = userDoc.collection("transactions").document(tx.id.toString())
            val txData = hashMapOf(
                "id" to tx.id,
                "date" to tx.date,
                "productName" to tx.productName,
                "quantity" to tx.quantity,
                "unitPrice" to tx.unitPrice,
                "total" to tx.total,
                "supplier" to (tx.supplier ?: ""),
                "unit" to (tx.unit ?: "kg"),
                "isDeleted" to tx.isDeleted,
                "updatedAt" to System.currentTimeMillis()
            )
            txDoc.set(txData, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w("FirestoreSyncHelper", "Persist transaction notice: ${e.message}")
        }
    }

    suspend fun deleteProduct(userId: String, productId: Long) = withContext(Dispatchers.IO) {
        if (userId.isBlank() || productId <= 0L) return@withContext
        val fs = firestore ?: return@withContext
        try {
            val userDoc = fs.collection("users").document(userId)
            userDoc.collection("products").document(productId.toString()).delete().await()
        } catch (e: Exception) {
            Log.w("FirestoreSyncHelper", "Delete product notice: ${e.message}")
        }
    }

    suspend fun deleteTransaction(userId: String, txId: Long) = withContext(Dispatchers.IO) {
        if (userId.isBlank() || txId <= 0L) return@withContext
        val fs = firestore ?: return@withContext
        try {
            val userDoc = fs.collection("users").document(userId)
            val txDoc = userDoc.collection("transactions").document(txId.toString())
            txDoc.update("isDeleted", true).await()
        } catch (e: Exception) {
            Log.w("FirestoreSyncHelper", "Delete transaction notice: ${e.message}")
        }
    }
}
