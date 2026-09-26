package com.aistudio.ecotobacco.kfzqw.data.local.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "procurement_plans")
data class ProcurementPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val supplierName: String = ""
)

@Entity(tableName = "procurement_items")
data class ProcurementItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val planId: Int,
    val productName: String,
    val targetQuantity: Double,
    val estimatedUnitPrice: Double,
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
