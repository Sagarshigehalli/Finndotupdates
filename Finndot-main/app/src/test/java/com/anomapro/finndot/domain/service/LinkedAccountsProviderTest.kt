package com.anomapro.finndot.domain.service

import com.anomapro.finndot.data.database.entity.AccountBalanceEntity
import com.anomapro.finndot.data.database.entity.CardEntity
import com.anomapro.finndot.data.database.entity.CardType
import com.anomapro.finndot.data.database.entity.TransactionEntity
import com.anomapro.finndot.data.database.entity.TransactionType
import com.anomapro.finndot.data.repository.AccountBalanceRepository
import com.anomapro.finndot.data.repository.CardRepository
import com.anomapro.finndot.domain.model.LinkedAccountKey
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDateTime

class LinkedAccountsProviderTest {

    private val t0 = LocalDateTime.of(2025, 1, 1, 12, 0)

    @Test
    fun loadOwnedAccountKeys_includesLatestBalancesAndDebitAccountAndCardLast4() = runBlocking {
        val balanceRepo = mock<AccountBalanceRepository>()
        val cardRepo = mock<CardRepository>()
        whenever(balanceRepo.getAllLatestBalances()).thenReturn(
            flowOf(
                listOf(
                    AccountBalanceEntity(
                        bankName = "HDFC Bank",
                        accountLast4 = "5678",
                        balance = BigDecimal("1000"),
                        timestamp = t0
                    )
                )
            )
        )
        whenever(cardRepo.getAllActiveCards()).thenReturn(
            flowOf(
                listOf(
                    CardEntity(
                        cardLast4 = "9999",
                        cardType = CardType.DEBIT,
                        bankName = "ICICI Bank",
                        accountLast4 = "2222"
                    )
                )
            )
        )

        val provider = LinkedAccountsProvider(balanceRepo, cardRepo)
        val keys = provider.loadOwnedAccountKeys()

        assertTrue(LinkedAccountKey.fromRaw("hdfc bank", "5678") in keys)
        assertTrue(LinkedAccountKey.fromRaw("icici bank", "2222") in keys)
        assertTrue(LinkedAccountKey.fromRaw("icici bank", "9999") in keys)
    }

    @Test
    fun loadOwnedAccountKeys_creditCardAddsBankAndCardLast4Only() = runBlocking {
        val balanceRepo = mock<AccountBalanceRepository>()
        val cardRepo = mock<CardRepository>()
        whenever(balanceRepo.getAllLatestBalances()).thenReturn(flowOf(emptyList()))
        whenever(cardRepo.getAllActiveCards()).thenReturn(
            flowOf(
                listOf(
                    CardEntity(
                        cardLast4 = "4242",
                        cardType = CardType.CREDIT,
                        bankName = "Axis Bank",
                        accountLast4 = null
                    )
                )
            )
        )

        val keys = LinkedAccountsProvider(balanceRepo, cardRepo).loadOwnedAccountKeys()
        assertTrue(LinkedAccountKey.fromRaw("axis bank", "4242") in keys)
        assertTrue(keys.size == 1)
    }

    @Test
    fun areBothLegsOwned_requiresBothKeysPresent() = runBlocking {
        val balanceRepo = mock<AccountBalanceRepository>()
        val cardRepo = mock<CardRepository>()
        whenever(balanceRepo.getAllLatestBalances()).thenReturn(
            flowOf(
                listOf(
                    AccountBalanceEntity(
                        bankName = "SBI",
                        accountLast4 = "1111",
                        balance = BigDecimal.ONE,
                        timestamp = t0
                    ),
                    AccountBalanceEntity(
                        bankName = "PNB",
                        accountLast4 = "2222",
                        balance = BigDecimal.TEN,
                        timestamp = t0
                    )
                )
            )
        )
        whenever(cardRepo.getAllActiveCards()).thenReturn(flowOf(emptyList()))

        val provider = LinkedAccountsProvider(balanceRepo, cardRepo)
        val keys = provider.loadOwnedAccountKeys()

        val out = TransactionEntity(
            amount = BigDecimal("50"),
            merchantName = "FT",
            category = "Banking",
            transactionType = TransactionType.EXPENSE,
            dateTime = t0,
            transactionHash = "h1",
            bankName = "SBI",
            accountNumber = "1111"
        )
        val inc = TransactionEntity(
            amount = BigDecimal("50"),
            merchantName = "FT",
            category = "Banking",
            transactionType = TransactionType.INCOME,
            dateTime = t0,
            transactionHash = "h2",
            bankName = "PNB",
            accountNumber = "2222"
        )
        assertTrue(provider.areBothLegsOwned(out, inc, keys))
    }

    @Test
    fun isLegOwned_falseWhenBankOrAccountMissing() = runBlocking {
        val balanceRepo = mock<AccountBalanceRepository>()
        val cardRepo = mock<CardRepository>()
        whenever(balanceRepo.getAllLatestBalances()).thenReturn(
            flowOf(
                listOf(
                    AccountBalanceEntity(
                        bankName = "SBI",
                        accountLast4 = "1111",
                        balance = BigDecimal.ONE,
                        timestamp = t0
                    )
                )
            )
        )
        whenever(cardRepo.getAllActiveCards()).thenReturn(flowOf(emptyList()))

        val provider = LinkedAccountsProvider(balanceRepo, cardRepo)
        val keys = provider.loadOwnedAccountKeys()
        val missingAccount = TransactionEntity(
            amount = BigDecimal.ONE,
            merchantName = "X",
            category = "Banking",
            transactionType = TransactionType.EXPENSE,
            dateTime = t0,
            transactionHash = "h3",
            bankName = "SBI",
            accountNumber = null
        )
        assertFalse(provider.isLegOwned(missingAccount, keys))
    }
}
