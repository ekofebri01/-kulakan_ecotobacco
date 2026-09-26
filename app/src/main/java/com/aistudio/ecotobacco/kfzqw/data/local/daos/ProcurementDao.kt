package com.aistudio.ecotobacco.kfzqw.data.local.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProcurementItemEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProcurementPlanEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProcurementPlanWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcurementDao {
    @Transaction
    @Query("SELECT * FROM procurement_plans ORDER BY date DESC")
    fun getAllPlansWithItems(): Flow<List<ProcurementPlanWithItems>>

    @Transaction
    @Query("SELECT * FROM procurement_plans ORDER BY date DESC")
    suspend fun getAllPlansWithItemsDirect(): List<ProcurementPlanWithItems>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: ProcurementPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ProcurementItemEntity>)

    @Update
    suspend fun updatePlan(plan: ProcurementPlanEntity)

    @Update
    suspend fun updateItem(item: ProcurementItemEntity)

    @Delete
    suspend fun deletePlan(plan: ProcurementPlanEntity)

    @Delete
    suspend fun deleteItem(item: ProcurementItemEntity)

    @Query("DELETE FROM procurement_items WHERE planId = :planId")
    suspend fun deleteItemsByPlanId(planId: Int)

    @Query("DELETE FROM procurement_plans")
    suspend fun deleteAllPlans()

    @Query("DELETE FROM procurement_items")
    suspend fun deleteAllItems()
}
