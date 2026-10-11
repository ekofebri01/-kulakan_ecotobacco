package com.aistudio.ecotobacco.kfzqw.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: String, // "MASUK" (Kulakan) atau "KELUAR" (Penjualan)
    val productId: Long,
    val productName: String,
    val category: String,
    val subCategory: String,
    val quantity: Int,
    val totalPrice: Double,
    val timestamp: Long = System.currentTimeMillis()
)
