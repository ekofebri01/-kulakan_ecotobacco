package com.aistudio.ecotobacco.kfzqw.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import com.aistudio.ecotobacco.kfzqw.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TobaccoDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products")
    suspend fun getAllProductsDirect(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: Long)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsDirect(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun deleteTransactionsByIds(ids: List<Long>)

    @Transaction
    @Query("SELECT * FROM procurement_plans ORDER BY createdAt DESC")
    fun getAllProcurementPlans(): Flow<List<ProcurementPlanWithItems>>

    @Transaction
    @Query("SELECT * FROM procurement_plans")
    suspend fun getAllProcurementPlansDirect(): List<ProcurementPlanWithItems>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProcurementPlan(plan: ProcurementPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProcurementItem(item: ProcurementItemEntity)

    @Transaction
    suspend fun insertProcurementPlanWithItems(plan: ProcurementPlanEntity, items: List<ProcurementItemEntity>): Long {
        val planId = insertProcurementPlan(plan)
        items.forEach { item ->
            insertProcurementItem(item.copy(planId = planId))
        }
        return planId
    }

    @Transaction
    suspend fun updateProcurementPlanWithItems(plan: ProcurementPlanEntity, items: List<ProcurementItemEntity>) {
        insertProcurementPlan(plan)
        deleteItemsByPlanId(plan.id)
        items.forEach { item ->
            insertProcurementItem(item.copy(planId = plan.id))
        }
    }

    @Query("DELETE FROM procurement_items WHERE planId = :planId")
    suspend fun deleteItemsByPlanId(planId: Long)

    @Delete
    suspend fun deleteProcurementPlan(plan: ProcurementPlanEntity)

    @Update
    suspend fun updateProcurementItem(item: ProcurementItemEntity)

    @Delete
    suspend fun deleteProcurementItem(item: ProcurementItemEntity)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategoriesDirect(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM procurement_plans")
    suspend fun deleteAllProcurementPlans()

    @Query("DELETE FROM procurement_items")
    suspend fun deleteAllProcurementItems()

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @Transaction
    suspend fun deleteAllProcurement() {
        deleteAllProcurementPlans()
        deleteAllProcurementItems()
    }
}

@Database(entities = [ProductEntity::class, TransactionEntity::class, ProcurementPlanEntity::class, ProcurementItemEntity::class, CategoryEntity::class], version = 1, exportSchema = false)
abstract class TobaccoDatabase : RoomDatabase() {
    abstract fun tobaccoDao(): TobaccoDao

    companion object {
        @Volatile
        private var INSTANCE: TobaccoDatabase? = null

        fun getDatabase(context: Context): TobaccoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TobaccoDatabase::class.java,
                    "eco_tobacco_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
