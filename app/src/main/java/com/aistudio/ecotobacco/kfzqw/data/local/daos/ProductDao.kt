package com.aistudio.ecotobacco.kfzqw.data.local.daos

import androidx.room.*
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProductEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.SubCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY category ASC, name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("SELECT * FROM sub_categories ORDER BY category ASC, name ASC")
    fun getAllSubCategories(): Flow<List<SubCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubCategory(subCategory: SubCategoryEntity): Long

    @Delete
    suspend fun deleteSubCategory(subCategory: SubCategoryEntity)

    @Update
    suspend fun updateSubCategory(subCategory: SubCategoryEntity)
}
