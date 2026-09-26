package com.aistudio.ecotobacco.kfzqw.data.repository

import com.aistudio.ecotobacco.kfzqw.data.local.daos.ProcurementDao
import com.aistudio.ecotobacco.kfzqw.data.local.daos.ProductDao
import com.aistudio.ecotobacco.kfzqw.data.local.daos.TransactionDao
import com.aistudio.ecotobacco.kfzqw.data.local.entities.CategoryEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProcurementItemEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProcurementPlanEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProcurementPlanWithItems
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProductEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

class TobaccoRepository(
    private val productDao: ProductDao,
    private val transactionDao: TransactionDao,
    private val procurementDao: ProcurementDao
) {
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allProcurementPlans: Flow<List<ProcurementPlanWithItems>> = procurementDao.getAllPlansWithItems()

    suspend fun insertProduct(product: ProductEntity) = productDao.insertProduct(product)
    suspend fun insertProducts(products: List<ProductEntity>) = productDao.insertProducts(products)
    suspend fun updateProduct(product: ProductEntity) = productDao.updateProduct(product)
    suspend fun deleteProductById(id: Int) = productDao.deleteProductById(id)
    suspend fun deleteAllProducts() = productDao.deleteAllProducts()

    suspend fun getAllProductsDirect() = productDao.getAllProductsDirect()
    suspend fun getAllTransactionsDirect() = transactionDao.getAllTransactionsDirect()
    suspend fun getAllProcurementPlansDirect() = procurementDao.getAllPlansWithItemsDirect()

    suspend fun insertTransaction(transaction: TransactionEntity) = transactionDao.insertTransaction(transaction)
    suspend fun insertTransactions(transactions: List<TransactionEntity>) = transactionDao.insertTransactions(transactions)
    suspend fun updateTransaction(transaction: TransactionEntity) = transactionDao.updateTransaction(transaction)
    suspend fun deleteTransactionById(id: Int) = transactionDao.deleteTransactionById(id)
    suspend fun deleteTransactionsByIds(ids: List<Int>) = transactionDao.deleteTransactionsByIds(ids)
    suspend fun deleteAllTransactions() = transactionDao.deleteAllTransactions()
    suspend fun getTransactionById(id: Int) = transactionDao.getTransactionById(id)

    suspend fun insertProcurementPlanWithItems(plan: ProcurementPlanEntity, items: List<ProcurementItemEntity>) {
        val planId = procurementDao.insertPlan(plan).toInt()
        val itemsWithId = items.map { it.copy(planId = planId) }
        procurementDao.insertItems(itemsWithId)
    }

    suspend fun updateProcurementPlanWithItems(plan: ProcurementPlanEntity, items: List<ProcurementItemEntity>) {
        procurementDao.updatePlan(plan)
        procurementDao.deleteItemsByPlanId(plan.id)
        val itemsWithId = items.map { it.copy(planId = plan.id) }
        procurementDao.insertItems(itemsWithId)
    }

    suspend fun deleteProcurementPlan(plan: ProcurementPlanEntity) = procurementDao.deletePlan(plan)
    suspend fun updateProcurementItem(item: ProcurementItemEntity) = procurementDao.updateItem(item)
    suspend fun deleteProcurementItem(item: ProcurementItemEntity) = procurementDao.deleteItem(item)
    suspend fun deleteItemsByPlanId(planId: Int) = procurementDao.deleteItemsByPlanId(planId)
    suspend fun deleteAllProcurement() {
        procurementDao.deleteAllPlans()
        procurementDao.deleteAllItems()
    }

    fun getAllCategories(): Flow<List<CategoryEntity>> = transactionDao.getAllCategories()
    suspend fun insertCategory(category: CategoryEntity) = transactionDao.insertCategory(category)
}
