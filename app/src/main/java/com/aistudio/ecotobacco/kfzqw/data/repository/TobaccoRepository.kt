package com.aistudio.ecotobacco.kfzqw.data.repository

import com.aistudio.ecotobacco.kfzqw.data.local.TobaccoDao
import com.aistudio.ecotobacco.kfzqw.data.local.entities.*
import kotlinx.coroutines.flow.Flow

class TobaccoRepository(private val dao: TobaccoDao) {
    val allProducts: Flow<List<ProductEntity>> = dao.getAllProducts()
    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val allProcurementPlans: Flow<List<ProcurementPlanWithItems>> = dao.getAllProcurementPlans()
    val allCategories: Flow<List<CategoryEntity>> = dao.getAllCategories()

    suspend fun getAllProductsDirect(): List<ProductEntity> = dao.getAllProductsDirect()
    suspend fun getProductById(id: Long): ProductEntity? = dao.getProductById(id)
    suspend fun insertProduct(product: ProductEntity) = dao.insertProduct(product)
    suspend fun insertProducts(products: List<ProductEntity>) = dao.insertProducts(products)
    suspend fun updateProduct(product: ProductEntity) = dao.updateProduct(product)
    suspend fun deleteProduct(product: ProductEntity) = dao.deleteProduct(product)
    suspend fun deleteProductById(id: Long) = dao.deleteProductById(id)

    suspend fun getAllTransactionsDirect(): List<TransactionEntity> = dao.getAllTransactionsDirect()
    suspend fun getTransactionById(id: Long): TransactionEntity? = dao.getTransactionById(id)
    suspend fun insertTransaction(transaction: TransactionEntity) = dao.insertTransaction(transaction)
    suspend fun insertTransactions(transactions: List<TransactionEntity>) = dao.insertTransactions(transactions)
    suspend fun updateTransaction(transaction: TransactionEntity) = dao.updateTransaction(transaction)
    suspend fun deleteTransactionById(id: Long) = dao.deleteTransactionById(id)
    suspend fun deleteTransactionsByIds(ids: List<Long>) = dao.deleteTransactionsByIds(ids)

    suspend fun getAllProcurementPlansDirect(): List<ProcurementPlanWithItems> = dao.getAllProcurementPlansDirect()
    suspend fun insertProcurementPlan(plan: ProcurementPlanEntity): Long = dao.insertProcurementPlan(plan)
    suspend fun insertProcurementItem(item: ProcurementItemEntity) = dao.insertProcurementItem(item)
    suspend fun insertProcurementPlanWithItems(plan: ProcurementPlanEntity, items: List<ProcurementItemEntity>): Long = dao.insertProcurementPlanWithItems(plan, items)
    suspend fun updateProcurementPlanWithItems(plan: ProcurementPlanEntity, items: List<ProcurementItemEntity>) = dao.updateProcurementPlanWithItems(plan, items)
    suspend fun deleteItemsByPlanId(planId: Long) = dao.deleteItemsByPlanId(planId)
    suspend fun deleteProcurementPlan(plan: ProcurementPlanEntity) = dao.deleteProcurementPlan(plan)
    suspend fun updateProcurementItem(item: ProcurementItemEntity) = dao.updateProcurementItem(item)
    suspend fun deleteProcurementItem(item: ProcurementItemEntity) = dao.deleteProcurementItem(item)

    suspend fun getAllCategoriesDirect(): List<CategoryEntity> = dao.getAllCategoriesDirect()
    suspend fun getAllCategories(): List<CategoryEntity> = dao.getAllCategoriesDirect()
    suspend fun insertCategory(category: CategoryEntity) = dao.insertCategory(category)

    suspend fun deleteAllProducts() = dao.deleteAllProducts()
    suspend fun deleteAllTransactions() = dao.deleteAllTransactions()
    suspend fun deleteAllProcurement() = dao.deleteAllProcurement()

    suspend fun clearAllData() {
        dao.deleteAllProducts()
        dao.deleteAllTransactions()
        dao.deleteAllProcurementPlans()
        dao.deleteAllProcurementItems()
        dao.deleteAllCategories()
    }
}
