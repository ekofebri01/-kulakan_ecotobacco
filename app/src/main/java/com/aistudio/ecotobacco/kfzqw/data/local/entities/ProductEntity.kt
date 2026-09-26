package com.aistudio.ecotobacco.kfzqw.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val price: Double,
    val unit: String = "kg",
    val sellingPrice: Double = 0.0,
    val sellingUnit: String = "pcs",
    val contentQuantity: Double = 1.0
)
