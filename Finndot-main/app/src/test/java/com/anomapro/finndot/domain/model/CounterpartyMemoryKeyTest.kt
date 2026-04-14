package com.anomapro.finndot.domain.model

import com.anomapro.finndot.data.database.entity.TransactionEntity
import com.anomapro.finndot.data.database.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class CounterpartyMemoryKeyTest {

    @Test
    fun fromEntity_sameBankMerchantAndLast4_producesSameKey() {
        val a = base().copy(
            bankName = "HDFC Bank",
            accountNumber = "9012",
            merchantName = "Acme Corp",
            normalizedMerchantName = null,
        )
        val b = base().copy(
            bankName = "hdfc bank",
            accountNumber = "xx9012",
            merchantName = "ACME CORP",
            normalizedMerchantName = null,
        )
        assertEquals(CounterpartyMemoryKey.fromEntity(a), CounterpartyMemoryKey.fromEntity(b))
    }

    @Test
    fun fromEntity_unknownMerchant_returnsNull() {
        val t = base().copy(merchantName = "Unknown Merchant", bankName = "SBI")
        assertNull(CounterpartyMemoryKey.fromEntity(t))
    }

    private fun base() = TransactionEntity(
        amount = BigDecimal.ONE,
        merchantName = "x",
        category = "Others",
        transactionType = TransactionType.EXPENSE,
        dateTime = LocalDateTime.now(),
        transactionHash = "h",
    )
}
