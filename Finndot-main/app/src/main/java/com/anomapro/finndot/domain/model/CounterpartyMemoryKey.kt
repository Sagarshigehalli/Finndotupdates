package com.anomapro.finndot.domain.model

import com.anomapro.finndot.data.database.entity.TransactionEntity
import java.util.Locale

/**
 * Stable key for "ask once" counterparty memory: same bank + account leg + merchant label
 * should match future SMS parsed into the same shape.
 */
object CounterpartyMemoryKey {

    private val unknownMerchants = setOf(
        "unknown",
        "unknown merchant",
        "na",
        "n/a",
        "not available",
        "null",
        "-",
        "",
    )

    /**
     * Returns null when we cannot build a meaningful key (blank / unknown merchant).
     */
    fun fromEntity(entity: TransactionEntity): String? {
        val merchantRaw = (entity.normalizedMerchantName ?: entity.merchantName).trim()
        if (merchantRaw.isBlank()) return null
        val lower = merchantRaw.lowercase(Locale.ROOT)
        if (lower in unknownMerchants) return null

        val bank = entity.bankName?.trim()?.lowercase(Locale.ROOT).orEmpty()
        val digits = entity.accountNumber?.filter { it.isDigit() }.orEmpty()
        val last4 = digits.takeLast(4)

        return "v1|$bank|$last4|$lower"
    }
}
