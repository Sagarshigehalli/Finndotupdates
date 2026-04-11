package com.anomapro.finndot.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.anomapro.finndot.data.database.FinndotDatabase
import com.anomapro.finndot.data.database.dao.AccountBalanceDao
import com.anomapro.finndot.data.database.dao.CardDao
import com.anomapro.finndot.data.database.dao.CategoryDao
import com.anomapro.finndot.data.database.dao.ChatDao
import com.anomapro.finndot.data.database.dao.ExchangeRateDao
import com.anomapro.finndot.data.database.dao.MerchantMappingDao
import com.anomapro.finndot.data.database.dao.MerchantNameMappingDao
import com.anomapro.finndot.data.database.dao.RuleApplicationDao
import com.anomapro.finndot.data.database.dao.RuleDao
import com.anomapro.finndot.data.database.dao.SubscriptionDao
import com.anomapro.finndot.data.database.dao.TransactionDao
import com.anomapro.finndot.data.database.dao.UnrecognizedSmsDao
import com.anomapro.finndot.data.database.dao.BudgetDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

/**
 * Hilt module that provides database-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Provides the singleton instance of FinndotDatabase.
     * 
     * @param context Application context
     * @return Configured Room database instance
     */
    @Provides
    @Singleton
    fun provideFinndotDatabase(
        @ApplicationContext context: Context
    ): FinndotDatabase {
        val database = Room.databaseBuilder(
            context,
            FinndotDatabase::class.java,
            FinndotDatabase.DATABASE_NAME
        )
            // Add manual migrations here when needed
            .addMigrations(
                FinndotDatabase.MIGRATION_12_14,
                FinndotDatabase.MIGRATION_13_14,
                FinndotDatabase.MIGRATION_14_15,
                FinndotDatabase.MIGRATION_20_21,
                FinndotDatabase.MIGRATION_21_22,
                FinndotDatabase.MIGRATION_22_23,
                FinndotDatabase.MIGRATION_29_30
            )
            
            // Enable auto-migrations
            // Room will automatically detect schema changes between versions
            
            // Add callback to seed default data on first creation
            .addCallback(DatabaseCallback())
            
            .build()
        
        // Set the singleton instance so BroadcastReceivers can access it
        FinndotDatabase.setInstance(database)
        
        return database
    }
    
    /**
     * Provides the TransactionDao from the database.
     * 
     * @param database The FinndotDatabase instance
     * @return TransactionDao for accessing transaction data
     */
    @Provides
    @Singleton
    fun provideTransactionDao(database: FinndotDatabase): TransactionDao {
        return database.transactionDao()
    }
    
    /**
     * Provides the SubscriptionDao from the database.
     * 
     * @param database The FinndotDatabase instance
     * @return SubscriptionDao for accessing subscription data
     */
    @Provides
    @Singleton
    fun provideSubscriptionDao(database: FinndotDatabase): SubscriptionDao {
        return database.subscriptionDao()
    }
    
    /**
     * Provides the ChatDao from the database.
     * 
     * @param database The FinndotDatabase instance
     * @return ChatDao for accessing chat message data
     */
    @Provides
    @Singleton
    fun provideChatDao(database: FinndotDatabase): ChatDao {
        return database.chatDao()
    }
    
    /**
     * Provides the MerchantMappingDao from the database.
     * 
     * @param database The FinndotDatabase instance
     * @return MerchantMappingDao for accessing merchant mapping data
     */
    @Provides
    @Singleton
    fun provideMerchantMappingDao(database: FinndotDatabase): MerchantMappingDao {
        return database.merchantMappingDao()
    }
    
    /**
     * Provides the MerchantNameMappingDao from the database.
     * 
     * @param database The FinndotDatabase instance
     * @return MerchantNameMappingDao for accessing merchant name mapping data
     */
    @Provides
    @Singleton
    fun provideMerchantNameMappingDao(database: FinndotDatabase): MerchantNameMappingDao {
        return database.merchantNameMappingDao()
    }
    
    /**
     * Provides the CategoryDao from the database.
     * 
     * @param database The FinndotDatabase instance
     * @return CategoryDao for accessing category data
     */
    @Provides
    @Singleton
    fun provideCategoryDao(database: FinndotDatabase): CategoryDao {
        return database.categoryDao()
    }
    
    /**
     * Provides the AccountBalanceDao from the database.
     * 
     * @param database The FinndotDatabase instance
     * @return AccountBalanceDao for accessing account balance data
     */
    @Provides
    @Singleton
    fun provideAccountBalanceDao(database: FinndotDatabase): AccountBalanceDao {
        return database.accountBalanceDao()
    }
    
    /**
     * Provides the UnrecognizedSmsDao from the database.
     * 
     * @param database The FinndotDatabase instance
     * @return UnrecognizedSmsDao for accessing unrecognized SMS data
     */
    @Provides
    @Singleton
    fun provideUnrecognizedSmsDao(database: FinndotDatabase): UnrecognizedSmsDao {
        return database.unrecognizedSmsDao()
    }
    
    /**
     * Provides the CardDao from the database.
     *
     * @param database The FinndotDatabase instance
     * @return CardDao for accessing card data
     */
    @Provides
    @Singleton
    fun provideCardDao(database: FinndotDatabase): CardDao {
        return database.cardDao()
    }

    /**
     * Provides the RuleDao from the database.
     *
     * @param database The FinndotDatabase instance
     * @return RuleDao for accessing rule data
     */
    @Provides
    @Singleton
    fun provideRuleDao(database: FinndotDatabase): RuleDao {
        return database.ruleDao()
    }

    /**
     * Provides the RuleApplicationDao from the database.
     *
     * @param database The FinndotDatabase instance
     * @return RuleApplicationDao for accessing rule application data
     */
    @Provides
    @Singleton
    fun provideRuleApplicationDao(database: FinndotDatabase): RuleApplicationDao {
        return database.ruleApplicationDao()
    }

    /**
     * Provides the ExchangeRateDao from the database.
     *
     * @param database The FinndotDatabase instance
     * @return ExchangeRateDao for accessing exchange rate data
     */
    @Provides
    @Singleton
    fun provideExchangeRateDao(database: FinndotDatabase): ExchangeRateDao {
        return database.exchangeRateDao()
    }
    
    /**
     * Provides the BudgetDao from the database.
     *
     * @param database The FinndotDatabase instance
     * @return BudgetDao for accessing budget data
     */
    @Provides
    @Singleton
    fun provideBudgetDao(database: FinndotDatabase): BudgetDao {
        return database.budgetDao()
    }
}

/**
 * Database callback to seed initial data when database is first created
 */
class DatabaseCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        
        // Seed default categories for new installations
        CoroutineScope(Dispatchers.IO).launch {
            seedCategories(db)
        }
    }
    
    private fun seedCategories(db: SupportSQLiteDatabase) {
        val categories = listOf(
            Triple("Food & Dining", "#FC8019", false),
            Triple("Groceries", "#5AC85A", false),
            Triple("Transportation", "#000000", false),
            Triple("Shopping", "#FF9900", false),
            Triple("Bills & Utilities", "#4CAF50", false),
            Triple("Entertainment", "#E50914", false),
            Triple("Healthcare", "#10847E", false),
            Triple("Investments", "#00D09C", false),
            Triple("Banking", "#004C8F", false),
            Triple("Personal Care", "#6A4C93", false),
            Triple("Education", "#673AB7", false),
            Triple("Mobile", "#2A3890", false),
            Triple("Fitness", "#FF3278", false),
            Triple("Insurance", "#0066CC", false),
            Triple("Travel", "#00BCD4", false),
            Triple("Salary", "#4CAF50", true),
            Triple("Income", "#4CAF50", true),
            Triple("Others", "#757575", false)
        )
        
        categories.forEachIndexed { index, (name, color, isIncome) ->
            db.execSQL("""
                INSERT OR IGNORE INTO categories (name, color, is_system, is_income, display_order, created_at, updated_at)
                VALUES (?, ?, 1, ?, ?, datetime('now'), datetime('now'))
            """.trimIndent(), arrayOf<Any>(name, color, if (isIncome) 1 else 0, index + 1))
        }
    }
}
