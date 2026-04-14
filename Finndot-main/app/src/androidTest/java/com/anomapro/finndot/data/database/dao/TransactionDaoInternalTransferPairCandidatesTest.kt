package com.anomapro.finndot.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anomapro.finndot.data.database.FinndotDatabase
import com.anomapro.finndot.data.database.entity.TransactionEntity
import com.anomapro.finndot.data.database.entity.TransactionType
import com.anomapro.finndot.data.repository.TransactionRepository
import com.anomapro.finndot.data.regret.RegretAutoTagger
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class TransactionDaoInternalTransferPairCandidatesTest {

    private lateinit var db: FinndotDatabase
    private lateinit var dao: TransactionDao
    private lateinit var repository: TransactionRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FinndotDatabase::class.java)
            .fallbackToDestructiveMigration()
            .build()
        dao = db.transactionDao()
        repository = TransactionRepository(dao, RegretAutoTagger())
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun findInternalTransferPairCandidates_returnsIncomeOnDifferentAccount() = runBlocking {
        val t0 = LocalDateTime.of(2025, 6, 1, 12, 0, 0)
        val expense = TransactionEntity(
            amount = BigDecimal("5000.00"),
            merchantName = "Transfer",
            category = "Banking",
            transactionType = TransactionType.EXPENSE,
            dateTime = t0,
            transactionHash = "pair-test-expense-1",
            bankName = "State Bank of India",
            accountNumber = "1234"
        )
        val income = TransactionEntity(
            amount = BigDecimal("5000.00"),
            merchantName = "Transfer",
            category = "Banking",
            transactionType = TransactionType.INCOME,
            dateTime = t0.plusMinutes(2),
            transactionHash = "pair-test-income-1",
            bankName = "HDFC Bank",
            accountNumber = "5678"
        )
        val expenseId = dao.insertTransaction(expense)
        val incomeId = dao.insertTransaction(income)
        assertTrue(expenseId > 0)
        assertTrue(incomeId > 0)

        val anchor = dao.getTransactionById(expenseId)!!
        val candidates = dao.findInternalTransferPairCandidates(
            excludeId = expenseId,
            amount = BigDecimal("5000.00"),
            currency = "INR",
            oppositeType = TransactionType.INCOME,
            windowStart = t0.minusMinutes(30),
            windowEnd = t0.plusMinutes(30),
            sourceBankName = anchor.bankName,
            sourceAccountNumber = anchor.accountNumber,
            limit = 10
        )
        assertEquals(1, candidates.size)
        assertEquals(incomeId, candidates[0].id)
    }

    @Test
    fun findInternalTransferPairCandidates_excludesSameBankAndAccount() = runBlocking {
        val t0 = LocalDateTime.of(2025, 6, 2, 10, 0, 0)
        val expense = TransactionEntity(
            amount = BigDecimal("100"),
            merchantName = "A",
            category = "Banking",
            transactionType = TransactionType.EXPENSE,
            dateTime = t0,
            transactionHash = "pair-test-expense-same-acct",
            bankName = "Same Bank",
            accountNumber = "9999"
        )
        val bogusIncomeSameAccount = TransactionEntity(
            amount = BigDecimal("100"),
            merchantName = "B",
            category = "Banking",
            transactionType = TransactionType.INCOME,
            dateTime = t0.plusMinutes(1),
            transactionHash = "pair-test-income-same-acct",
            bankName = "Same Bank",
            accountNumber = "9999"
        )
        val expenseId = dao.insertTransaction(expense)
        dao.insertTransaction(bogusIncomeSameAccount)

        val candidates = dao.findInternalTransferPairCandidates(
            excludeId = expenseId,
            amount = BigDecimal("100"),
            currency = "INR",
            oppositeType = TransactionType.INCOME,
            windowStart = t0.minusMinutes(10),
            windowEnd = t0.plusMinutes(10),
            sourceBankName = "Same Bank",
            sourceAccountNumber = "9999",
            limit = 10
        )
        assertTrue(candidates.isEmpty())
    }

    @Test
    fun findInternalTransferPairCandidatesForAnchor_delegatesThroughRepository() = runBlocking {
        val t0 = LocalDateTime.of(2025, 6, 3, 15, 30, 0)
        val expense = TransactionEntity(
            amount = BigDecimal("2500"),
            merchantName = "IMPS",
            category = "Banking",
            transactionType = TransactionType.EXPENSE,
            dateTime = t0,
            transactionHash = "pair-test-repo-exp",
            bankName = "ICICI Bank",
            accountNumber = "1111"
        )
        val income = TransactionEntity(
            amount = BigDecimal("2500"),
            merchantName = "IMPS",
            category = "Banking",
            transactionType = TransactionType.INCOME,
            dateTime = t0.plusMinutes(5),
            transactionHash = "pair-test-repo-inc",
            bankName = "Axis Bank",
            accountNumber = "2222"
        )
        val expenseId = dao.insertTransaction(expense)
        dao.insertTransaction(income)

        val anchor = dao.getTransactionById(expenseId)!!
        val fromRepo = repository.findInternalTransferPairCandidatesForAnchor(
            anchor = anchor,
            oppositeType = TransactionType.INCOME,
            windowStart = t0.minusHours(1),
            windowEnd = t0.plusHours(1),
            limit = 5
        )
        assertEquals(1, fromRepo.size)
        assertEquals(TransactionType.INCOME, fromRepo[0].transactionType)
        assertEquals(BigDecimal("2500"), fromRepo[0].amount)
    }
}
