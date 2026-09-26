package com.aistudio.ecotobacco.kfzqw.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aistudio.ecotobacco.kfzqw.data.local.daos.ProcurementDao
import com.aistudio.ecotobacco.kfzqw.data.local.daos.ProductDao
import com.aistudio.ecotobacco.kfzqw.data.local.daos.TransactionDao
import com.aistudio.ecotobacco.kfzqw.data.local.entities.CategoryEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProcurementItemEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProcurementPlanEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProductEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.TransactionEntity

@Database(
    entities = [
        ProductEntity::class,
        TransactionEntity::class,
        ProcurementPlanEntity::class,
        ProcurementItemEntity::class,
        CategoryEntity::class
    ],
    version = 10,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun procurementDao(): ProcurementDao
}
