package com.aistudio.ecotobacco.kfzqw.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val productName: String,
    val quantity: Double,
    val unitPrice: Double,
    val total: Double,
    val supplier: String = "",
    val unit: String = "kg",
    val isDeleted: Int = 0
)
