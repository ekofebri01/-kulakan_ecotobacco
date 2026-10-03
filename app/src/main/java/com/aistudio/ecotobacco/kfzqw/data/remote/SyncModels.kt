package com.aistudio.ecotobacco.kfzqw.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SyncDataBundle(
    val transactions: List<TransactionSyncModel>,
    val products: List<ProductSyncModel>,
    val lastUpdated: Long,
    val deletedTransactions: List<String>? = null,
    val procurementPlans: List<ProcurementPlanSyncModel>? = null,
    val appSettings: AppSettingsSyncModel? = null
)

@JsonClass(generateAdapter = true)
data class AppSettingsSyncModel(
    val themeMode: String = "SYSTEM",
    val customColor: Int = 0xFF10B981.toInt(),
    val customUnits: List<String> = listOf("kg", "ons", "pcs"),
    val fontSizeScale: Float = 1.0f,
    val paddingScale: Float = 1.0f,
    val buttonHeight: Int = 56,
    val cornerRadius: Int = 16,
    val customTextColor: Int = 0
)

@JsonClass(generateAdapter = true)
data class TransactionSyncModel(
    val id: Int,
    val date: Long,
    val productName: String,
    val quantity: Double,
    val unitPrice: Double,
    val total: Double,
    val supplier: String? = "",
    val unit: String? = "kg",
    val isDeleted: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class ProductSyncModel(
    val id: Int,
    val name: String,
    val price: Double,
    val unit: String? = "kg",
    val sellingPrice: Double? = 0.0,
    val sellingUnit: String? = "pcs",
    val contentQuantity: Double? = 1.0
)

@JsonClass(generateAdapter = true)
data class ProcurementPlanSyncModel(
    val id: Int,
    val date: Long,
    val supplierName: String,
    val items: List<ProcurementItemSyncModel>
)

@JsonClass(generateAdapter = true)
data class ProcurementItemSyncModel(
    val id: Int,
    val productName: String,
    val targetQuantity: Double,
    val estimatedUnitPrice: Double,
    val unit: String = "kg",
    val isBought: Boolean = false
)
