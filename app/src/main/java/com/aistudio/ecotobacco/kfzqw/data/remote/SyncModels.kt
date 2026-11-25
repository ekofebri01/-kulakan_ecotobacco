package com.aistudio.ecotobacco.kfzqw.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DriveBackupFile(
    val id: String = "",
    val name: String = "",
    val sizeBytes: Long = 0L,
    val timestamp: Long = 0L,
    val modifiedTime: String = "",
    val source: String = "Cloud",
    val itemCountText: String = "0 items",
    val jsonData: String? = "",
    val summary: String = ""
)

enum class SyncStatus {
    IDLE, SYNCING, SUCCESS, ERROR, DISCONNECTED, OFFLINE, SYNCED
}

@JsonClass(generateAdapter = true)
data class SyncDataBundle(
    val transactions: List<TransactionSyncModel>,
    val products: List<ProductSyncModel>,
    val lastUpdated: Long,
    val deletedTransactions: List<String>? = null,
    val deletedProducts: List<String>? = null,
    val procurementPlans: List<ProcurementPlanSyncModel>? = null,
    val appSettings: AppSettingsSyncModel? = null,
    val categories: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class AutoBackupInfo(
    val lastBackupTime: Long = 0L,
    val fileSizeFormatted: String = "0 B",
    val productCount: Int = 0,
    val transactionCount: Int = 0,
    val planCount: Int = 0,
    val filePath: String = "",
    val isAutoBackupActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class AppSettingsSyncModel(
    val themeMode: String = "SYSTEM",
    val customColor: Int = 0xFF10B981.toInt(),
    val customUnits: List<String> = listOf("kg", "ons", "pcs"),
    val customSubCategories: List<String> = listOf("Tembakau", "Alat", "Papir", "Kemasan", "Umum"),
    val fontSizeScale: Float = 1.0f,
    val paddingScale: Float = 1.0f,
    val buttonHeight: Int = 56,
    val cornerRadius: Int = 16,
    val customTextColor: Int = 0
)

@JsonClass(generateAdapter = true)
data class TransactionSyncModel(
    val id: Long = 0L,
    val date: Long = System.currentTimeMillis(),
    val productName: String = "",
    val quantity: Double = 0.0,
    val unitPrice: Double = 0.0,
    val total: Double = 0.0,
    val supplier: String? = "",
    val unit: String? = "kg",
    val isDeleted: Boolean? = false,
    val invoiceNumber: String = "",
    val totalAmount: Double = 0.0,
    val paymentMethod: String = "Tunai",
    val cashierName: String = "Kasir",
    val itemsJson: String = ""
)

@JsonClass(generateAdapter = true)
data class ProductSyncModel(
    val id: Long = 0L,
    val name: String = "",
    val price: Double = 0.0,
    val unit: String? = "kg",
    val sellingPrice: Double? = 0.0,
    val sellingUnit: String? = "pcs",
    val contentQuantity: Double? = 1.0,
    val category: String? = "",
    val subCategory: String? = "Umum",
    val stock: Int = 0,
    val barcode: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class ProcurementPlanSyncModel(
    val id: Long = 0L,
    val date: Long = System.currentTimeMillis(),
    val supplierName: String = "",
    val title: String = "",
    val status: String = "Pending",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val items: List<ProcurementItemSyncModel> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ProcurementItemSyncModel(
    val id: Long = 0L,
    val planId: Long = 0L,
    val productName: String = "",
    val targetQuantity: Double = 0.0,
    val estimatedUnitPrice: Double = 0.0,
    val quantity: Int = 0,
    val estimatedPrice: Double = 0.0,
    val unit: String = "kg",
    val isBought: Boolean = false
)
