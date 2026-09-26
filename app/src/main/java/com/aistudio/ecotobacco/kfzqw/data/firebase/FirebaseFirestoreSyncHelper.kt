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
    val timestamp: Long
)

class FirebaseFirestoreSyncHelper(private val context: Context) {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            ensureFirebaseInitialized(context)
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w("FirestoreSyncHelper", "Firestore unavailable: ${e.message}")
            null
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
                        .setApplicationId("1:863851997609:android:com_aistudio_ecotobacco")
                        .setApiKey("AIzaSyB_Fallback_EcoTobacco_Key")
                        .setProjectId("ais-ecotobacco")
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
        categories: List<CategoryEntity>
    ): Result<FirestoreSyncResult> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User ID is empty"))
        }
        val fs = firestore ?: return@withContext Result.failure(IllegalStateException("Cloud Firestore tidak tersedia"))

        _isSyncing.value = true
        _syncMessage.value = "Mengunggah data ke Cloud Firestore..."

        try {
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

            // 2. Batch write products
            val productBatches = products.chunked(400)
            for (batchList in productBatches) {
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
            }

            // 3. Batch write transactions
            val txBatches = transactions.chunked(400)
            for (batchList in txBatches) {
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
            }

            // 4. Batch write procurement plans
            if (plans.isNotEmpty()) {
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

            // 5. Save preferences
            prefs.edit().putLong("last_firestore_sync_time", now).apply()
            _lastSyncedTime.value = now
            _isSyncing.value = false
            _syncMessage.value = "Sinkronisasi Cloud Firestore berhasil!"

            val result = FirestoreSyncResult(
                uploadedProducts = products.size,
                uploadedTransactions = transactions.size,
                uploadedPlans = plans.size,
                timestamp = now
            )
            Result.success(result)
        } catch (e: Exception) {
            Log.w("FirestoreSyncHelper", "Sync to Firestore notice: ${e.message}")
            _isSyncing.value = false
            _syncMessage.value = "Gagal sinkron Firestore: ${e.localizedMessage}"
            Result.failure(e)
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

        _isSyncing.value = true
        _syncMessage.value = "Mengunduh data dari Cloud Firestore..."

        try {
            val userDoc = fs.collection("users").document(userId)

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
                        id = doc.getLong("id")?.toInt() ?: 0,
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
                val isDeletedInt = doc.getLong("isDeleted")?.toInt()
                    ?: if (doc.getBoolean("isDeleted") == true) 1 else 0

                repository.insertTransaction(
                    TransactionEntity(
                        id = doc.getLong("id")?.toInt() ?: 0,
                        date = date,
                        productName = productName,
                        quantity = quantity,
                        unitPrice = unitPrice,
                        total = total,
                        supplier = supplier,
                        unit = unit,
                        isDeleted = isDeletedInt
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
                    id = doc.getLong("id")?.toInt() ?: 0,
                    date = date,
                    supplierName = supplierName
                )

                @Suppress("UNCHECKED_CAST")
                val itemsData = doc.get("items") as? List<Map<String, Any>>
                val items = itemsData?.map { itemMap ->
                    ProcurementItemEntity(
                        id = (itemMap["id"] as? Number)?.toInt() ?: 0,
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
            _syncMessage.value = "Berhasil memulihkan $restoredProducts produk & $restoredTransactions transaksi dari Firestore!"

            Result.success(
                FirestoreRestoreResult(
                    restoredProducts = restoredProducts,
                    restoredTransactions = restoredTransactions,
                    restoredPlans = restoredPlans,
                    timestamp = now
                )
            )
        } catch (e: Exception) {
            Log.w("FirestoreSyncHelper", "Restore from Firestore notice: ${e.message}")
            _isSyncing.value = false
            _syncMessage.value = "Gagal memulihkan Firestore: ${e.localizedMessage}"
            Result.failure(e)
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
}
