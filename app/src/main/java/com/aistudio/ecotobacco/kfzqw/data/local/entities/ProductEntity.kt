package com.aistudio.ecotobacco.kfzqw.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val category: String,       // Tembakau, Alat, Papir, Tembakau Kemasan, dll
    val subCategory: String,    // Sub-kategori e.g. Garangan, Mesin Inject, dll
    val stock: Int = 0,
    val unit: String = "Kg / Pcs",
    val buyPrice: Double = 0.0,
    val sellPrice: Double = 0.0
)
