package com.aistudio.ecotobacco.kfzqw.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String = "",
    val price: Double = 0.0,
    val unit: String = "Pcs",
    val sellingPrice: Double = 0.0,
    val sellingUnit: String = "pcs",
    val contentQuantity: Double = 1.0,
    val category: String = "Umum",
    val subCategory: String = "Umum",
    val stock: Int = 0,
    val barcode: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: Long = System.currentTimeMillis(),
    val productName: String = "",
    val quantity: Double = 0.0,
    val unitPrice: Double = 0.0,
    val total: Double = 0.0,
    val supplier: String = "",
    val unit: String = "kg",
    val isDeleted: Boolean = false,
    val invoiceNumber: String = "",
    val totalAmount: Double = 0.0,
    val paymentMethod: String = "Tunai",
    val cashierName: String = "Kasir",
    val itemsJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "procurement_plans")
data class ProcurementPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: Long = System.currentTimeMillis(),
    val supplierName: String = "",
    val title: String = "",
    val status: String = "Pending",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "procurement_items")
data class ProcurementItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val planId: Long = 0L,
    val productName: String = "",
    val targetQuantity: Double = 0.0,
    val estimatedUnitPrice: Double = 0.0,
    val quantity: Int = 0,
    val estimatedPrice: Double = 0.0,
    val unit: String = "kg",
    val isBought: Boolean = false
)

data class ProcurementPlanWithItems(
    @Embedded val plan: ProcurementPlanEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "planId"
    )
    val items: List<ProcurementItemEntity>
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String = ""
)
