package com.anomapro.finndot.data.mapper

import com.finndot.parser.core.ParsedTransaction
import com.anomapro.finndot.core.Constants
import com.anomapro.finndot.data.database.entity.TransactionEntity
import com.anomapro.finndot.data.database.entity.TransactionType
import com.anomapro.finndot.ui.icons.CategoryMapping
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Maps ParsedTransaction from parser-core to TransactionEntity
 */
fun ParsedTransaction.toEntity(): TransactionEntity {
    val dateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(timestamp),
        ZoneId.systemDefault()
    )

    // Normalize merchant name to proper case
    val normalizedMerchant = sanitizeMerchantName(merchant)?.let { normalizeMerchantName(it) }

    // Map TransactionType from parser-core to database entity
    val entityType = when (type) {
        com.finndot.parser.core.TransactionType.INCOME -> TransactionType.INCOME
        com.finndot.parser.core.TransactionType.EXPENSE -> TransactionType.EXPENSE
        com.finndot.parser.core.TransactionType.CREDIT -> TransactionType.CREDIT
        com.finndot.parser.core.TransactionType.TRANSFER -> TransactionType.TRANSFER
        com.finndot.parser.core.TransactionType.INVESTMENT -> TransactionType.INVESTMENT
    }

    return TransactionEntity(
        id = 0, // Auto-generated
        amount = amount,
        merchantName = normalizedMerchant ?: "Unknown Merchant",
        category = category ?: determineCategory(merchant, entityType),
        transactionType = entityType,
        dateTime = dateTime,
        description = null,
        smsBody = smsBody,
        bankName = bankName,
        smsSender = sender,
        accountNumber = accountLast4,
        balanceAfter = balance,
        transactionHash = transactionHash?.takeIf { it.isNotBlank() } ?: generateTransactionId(),
        isRecurring = false, // Will be determined later
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        currency = currency,
        fromAccount = fromAccount,
        toAccount = toAccount
    )
}

/**
 * Normalizes merchant name to consistent format.
 * Converts all-caps to proper case, preserves already mixed case.
 */
private fun normalizeMerchantName(name: String): String {
    val trimmed = name.trim()

    // If it's all uppercase, convert to proper case
    return if (trimmed == trimmed.uppercase()) {
        trimmed.lowercase().split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
    } else {
        // Already has mixed case, keep as is
        trimmed
    }
}

/**
 * Returns null for blank or placeholder merchant values.
 */
private fun sanitizeMerchantName(name: String?): String? {
    if (name == null) return null
    val trimmed = name.trim()
    if (trimmed.isBlank()) return null

    val normalized = trimmed.lowercase()
    return if (normalized in setOf("unknown", "unknown merchant", "na", "n/a", "not available", "null", "-")) {
        null
    } else {
        trimmed
    }
}

/**
 * Determines the category based on merchant name and transaction type.
 */
private fun determineCategory(merchant: String?, type: TransactionType): String {
    val merchantName = merchant ?: return "Others"

    // Special handling for income transactions
    if (type == TransactionType.INCOME) {
        val merchantLower = merchantName.lowercase()
        return when {
            merchantLower.contains("salary") -> "Salary"
            merchantLower.contains("refund") -> "Refunds"
            merchantLower.contains("cashback") -> "Cashback"
            merchantLower.contains("interest") -> "Interest"
            merchantLower.contains("dividend") -> "Dividends"
            else -> "Income"
        }
    }

    // Use unified category mapping for expenses
    return CategoryMapping.getCategory(merchantName)
}

/**
 * Extension to map parser-core TransactionType to database entity TransactionType
 */
fun com.finndot.parser.core.TransactionType.toEntityType(): TransactionType {
    return when (this) {
        com.finndot.parser.core.TransactionType.INCOME -> TransactionType.INCOME
        com.finndot.parser.core.TransactionType.EXPENSE -> TransactionType.EXPENSE
        com.finndot.parser.core.TransactionType.CREDIT -> TransactionType.CREDIT
        com.finndot.parser.core.TransactionType.TRANSFER -> TransactionType.TRANSFER
        com.finndot.parser.core.TransactionType.INVESTMENT -> TransactionType.INVESTMENT
    }
}