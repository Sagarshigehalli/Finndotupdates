package com.anomapro.finndot.data.database.dao

import androidx.room.*
import com.anomapro.finndot.data.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.YearMonth

@Dao
interface BudgetDao {
    
    @Query("SELECT * FROM budgets WHERE year_month = :yearMonth AND currency = :currency ORDER BY category ASC")
    fun getBudgetsForMonth(yearMonth: YearMonth, currency: String): Flow<List<BudgetEntity>>
    
    @Query("SELECT * FROM budgets WHERE year_month = :yearMonth AND currency = :currency AND category = :category LIMIT 1")
    suspend fun getBudgetForCategory(yearMonth: YearMonth, category: String, currency: String): BudgetEntity?
    
    @Query("SELECT * FROM budgets WHERE year_month = :yearMonth AND currency = :currency AND category = 'All' LIMIT 1")
    suspend fun getTotalBudgetForMonth(yearMonth: YearMonth, currency: String): BudgetEntity?
    
    @Query("SELECT COALESCE(SUM(amount), 0) FROM budgets WHERE year_month = :yearMonth AND currency = :currency")
    suspend fun getTotalBudgetAmount(yearMonth: YearMonth, currency: String): BigDecimal
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)
    
    @Update
    suspend fun updateBudget(budget: BudgetEntity)
    
    @Query("DELETE FROM budgets WHERE id = :budgetId")
    suspend fun deleteBudget(budgetId: Long)
    
    @Query("DELETE FROM budgets WHERE year_month = :yearMonth AND category = :category AND currency = :currency")
    suspend fun deleteBudgetForCategory(yearMonth: YearMonth, category: String, currency: String)
    
    @Query("SELECT * FROM budgets ORDER BY year_month DESC, category ASC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()
}

