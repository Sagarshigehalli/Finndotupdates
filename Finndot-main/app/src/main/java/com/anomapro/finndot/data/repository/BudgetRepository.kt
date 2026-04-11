package com.anomapro.finndot.data.repository

import com.anomapro.finndot.data.database.dao.BudgetDao
import com.anomapro.finndot.data.database.entity.BudgetEntity
import android.util.Log
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao
) {
    
    fun getBudgetsForMonth(yearMonth: YearMonth, currency: String): Flow<List<BudgetEntity>> {
        return budgetDao.getBudgetsForMonth(yearMonth, currency)
    }
    
    suspend fun getBudgetForCategory(yearMonth: YearMonth, category: String, currency: String): BudgetEntity? {
        return budgetDao.getBudgetForCategory(yearMonth, category, currency)
    }
    
    suspend fun getTotalBudgetForMonth(yearMonth: YearMonth, currency: String): BudgetEntity? {
        return try {
            budgetDao.getTotalBudgetForMonth(yearMonth, currency)
        } catch (e: Exception) {
            android.util.Log.e("BudgetRepository", "Error getting total budget", e)
            null
        }
    }
    
    suspend fun getTotalBudgetAmount(yearMonth: YearMonth, currency: String): BigDecimal {
        return budgetDao.getTotalBudgetAmount(yearMonth, currency)
    }
    
    suspend fun createOrUpdateBudget(
        category: String,
        amount: BigDecimal,
        yearMonth: YearMonth,
        currency: String
    ): Long {
        val existing = budgetDao.getBudgetForCategory(yearMonth, category, currency)
        val budget = if (existing != null) {
            existing.copy(
                amount = amount,
                updatedAt = LocalDateTime.now()
            )
        } else {
            BudgetEntity(
                category = category,
                amount = amount,
                yearMonth = yearMonth,
                currency = currency
            )
        }
        return budgetDao.insertBudget(budget)
    }
    
    suspend fun deleteBudget(budgetId: Long) {
        budgetDao.deleteBudget(budgetId)
    }
    
    suspend fun deleteBudgetForCategory(yearMonth: YearMonth, category: String, currency: String) {
        budgetDao.deleteBudgetForCategory(yearMonth, category, currency)
    }
    
    fun getAllBudgets(): Flow<List<BudgetEntity>> {
        return budgetDao.getAllBudgets()
    }
}

