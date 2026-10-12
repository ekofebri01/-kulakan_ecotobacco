package com.aistudio.ecotobacco.kfzqw.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
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
        CategoryEntity::class,
        TransactionEntity::class,
        ProcurementPlanEntity::class,
        ProcurementItemEntity::class
    ],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun procurementDao(): ProcurementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "eco_tobacco_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getDatabase(context: Context): AppDatabase = getInstance(context)
    }
}
