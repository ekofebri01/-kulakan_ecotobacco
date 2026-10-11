package com.aistudio.ecotobacco.kfzqw.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aistudio.ecotobacco.kfzqw.data.local.daos.ProductDao
import com.aistudio.ecotobacco.kfzqw.data.local.daos.TransactionDao
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProductEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.SubCategoryEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.TransactionEntity

@Database(entities = [ProductEntity::class, SubCategoryEntity::class, TransactionEntity::class], version = 12, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
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
    }
}
