package com.anomapro.finndot.domain.service

import com.anomapro.finndot.data.database.entity.TransactionEntity
import com.anomapro.finndot.data.database.entity.TransactionType
import com.anomapro.finndot.data.repository.TransactionRepository
import com.anomapro.finndot.domain.model.LinkedAccountKey
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDateTime

class InternalTransferPairingServiceTest {

    private val t0 = LocalDateTime.of(2025, 6, 10, 14, 0, 0)

    @Test
    fun tryPairAfterInsert_updatesBothLegsToTransferWhenMatchAndOwned() = runBlocking {
        val transactionRepository = mock<TransactionRepository>()
        val linkedAccountsProvider = mock<LinkedAccountsProvider>()
        val service = InternalTransferPairingService(transactionRepository, linkedAccountsProvider)

        val owned = setOf(
            LinkedAccountKey.fromRaw("state bank of india", "1234")!!,
            LinkedAccountKey.fromRaw("hdfc bank", "5678")!!
        )
        whenever(linkedAccountsProvider.loadOwnedAccountKeys()).thenReturn(owned)
        whenever(linkedAccountsProvider.isLegOwned(any(), eq(owned))).thenAnswer { inv ->
            val t = inv.getArgument<TransactionEntity>(0)
            LinkedAccountKey.fromTransactionLeg(t) in owned
        }
        whenever(linkedAccountsProvider.areBothLegsOwned(any(), any(), eq(owned))).thenReturn(true)

        val anchor = TransactionEntity(
            id = 2L,
            amount = BigDecimal("5000.00"),
            merchantName = "FT",
            category = "Banking",
            transactionType = TransactionType.INCOME,
            dateTime = t0,
            transactionHash = "inc-1",
            bankName = "HDFC Bank",
            accountNumber = "5678"
        )
        val expenseLeg = TransactionEntity(
            id = 1L,
            amount = BigDecimal("5000.00"),
            merchantName = "FT",
            category = "Banking",
            transactionType = TransactionType.EXPENSE,
            dateTime = t0.minusMinutes(1),
            transactionHash = "exp-1",
            bankName = "State Bank of India",
            accountNumber = "1234"
        )
        whenever(
            transactionRepository.findInternalTransferPairCandidatesForAnchor(
                eq(anchor),
                eq(TransactionType.EXPENSE),
                any(),
                any(),
                any()
            )
        ).thenReturn(listOf(expenseLeg))

        service.tryPairAfterInsert(anchor)

        val captor = argumentCaptor<TransactionEntity>()
        verify(transactionRepository, times(2)).updateTransaction(captor.capture())
        val updated = captor.allValues
        assertEquals(2, updated.size)
        assertEquals(TransactionType.TRANSFER, updated[0].transactionType)
        assertEquals(TransactionType.TRANSFER, updated[1].transactionType)
        assertEquals("State Bank of India **1234", updated[0].fromAccount)
        assertEquals("HDFC Bank **5678", updated[0].toAccount)
    }
}
