package com.anomapro.finndot.data.database.dao

import androidx.room.*
import com.anomapro.finndot.data.database.entity.TransactionEntity
import com.anomapro.finndot.data.database.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface TransactionDao {
    
    @Query("SELECT * FROM transactions WHERE is_deleted = 0 ORDER BY date_time DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: Long): TransactionEntity?
    
    @Query("""
        SELECT * FROM transactions 
        WHERE is_deleted = 0 
        AND date_time BETWEEN :startDate AND :endDate 
        ORDER BY date_time DESC
    """)
    fun getTransactionsBetweenDates(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<TransactionEntity>>
    
    @Query("""
        SELECT * FROM transactions 
        WHERE is_deleted = 0 
        AND transaction_type = :type 
        ORDER BY date_time DESC
    """)
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>>
    
    @Query("""
        SELECT * FROM transactions 
        WHERE is_deleted = 0 
        AND category = :category 
        ORDER BY date_time DESC
    """)
    fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>>
    
    @Query("""
        SELECT * FROM transactions 
        WHERE is_deleted = 0 
        AND (merchant_name LIKE '%' || :searchQuery || '%' 
        OR description LIKE '%' || :searchQuery || '%'
        OR sms_body LIKE '%' || :searchQuery || '%') 
        ORDER BY date_time DESC
    """)
    fun searchTransactions(searchQuery: String): Flow<List<TransactionEntity>>
    
    @Query("SELECT DISTINCT category FROM transactions WHERE is_deleted = 0 ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Query("""
        SELECT category FROM transactions
        WHERE is_deleted = 0
        GROUP BY category
        ORDER BY COUNT(*) DESC
        LIMIT :limit
    """)
    suspend fun getTopCategoriesByUsage(limit: Int = 3): List<String>
    
    @Query("SELECT DISTINCT merchant_name FROM transactions WHERE is_deleted = 0 ORDER BY merchant_name ASC")
    fun getAllMerchants(): Flow<List<String>>
    
    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE is_deleted = 0 
        AND transaction_type = :type 
        AND date_time BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalAmountByTypeAndPeriod(
        type: TransactionType,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Double?
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)
    
    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)
    
    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)
    
    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: Long)
    
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
    
    @Query("UPDATE transactions SET category = :newCategory WHERE merchant_name = :merchantName")
    suspend fun updateCategoryForMerchant(merchantName: String, newCategory: String)

    @Query("SELECT COUNT(*) FROM transactions WHERE merchant_name = :merchantName AND id != :excludeId")
    suspend fun getTransactionCountForMerchant(merchantName: String, excludeId: Long): Int

    @Query("SELECT DISTINCT currency FROM transactions WHERE is_deleted = 0 ORDER BY currency")
    fun getAllCurrencies(): Flow<List<String>>

    @Query("SELECT DISTINCT currency FROM transactions WHERE is_deleted = 0 AND date_time BETWEEN :startDate AND :endDate ORDER BY currency")
    fun getCurrenciesForPeriod(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<String>>

    // Soft delete methods
    @Query("UPDATE transactions SET is_deleted = 1 WHERE id = :transactionId")
    suspend fun softDeleteTransaction(transactionId: Long)

    @Query("UPDATE transactions SET is_deleted = 1 WHERE transaction_hash = :transactionHash")
    suspend fun softDeleteByHash(transactionHash: String)
    
    // Regret methods
    @Query("UPDATE transactions SET is_regret = :isRegret WHERE id = :transactionId")
    suspend fun updateRegretStatus(transactionId: Long, isRegret: Boolean)

    // Method to check if transaction exists by hash (including deleted)
    @Query("SELECT * FROM transactions WHERE transaction_hash = :transactionHash LIMIT 1")
    suspend fun getTransactionByHash(transactionHash: String): TransactionEntity?
    
    @Query("""
        SELECT * FROM transactions 
        WHERE is_deleted = 0 
        AND date_time BETWEEN :startDate AND :endDate 
        ORDER BY date_time DESC
    """)
    suspend fun getTransactionsBetweenDatesList(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<TransactionEntity>
    
    @Query("""
        SELECT * FROM transactions
        WHERE is_deleted = 0
        AND bank_name = :bankName
        AND (account_number = :accountLast4 OR account_number IS NULL)
        ORDER BY date_time DESC
    """)
    fun getTransactionsByAccount(
        bankName: String,
        accountLast4: String
    ): Flow<List<TransactionEntity>>
    
    @Query("""
        SELECT * FROM transactions 
        WHERE is_deleted = 0 
        AND bank_name = :bankName 
        AND account_number = :accountLast4
        AND date_time BETWEEN :startDate AND :endDate
        ORDER BY date_time DESC
    """)
    fun getTransactionsByAccountAndDateRange(
        bankName: String,
        accountLast4: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<TransactionEntity>>
    
    /**
     * Get all transactions that have SMS data (either smsBody or smsSender is not null)
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE is_deleted = 0 
        AND (sms_body IS NOT NULL OR sms_sender IS NOT NULL)
        ORDER BY date_time DESC
    """)
    suspend fun getTransactionsWithSms(): List<TransactionEntity>
}