package com.finndot.parser.core.bank

import com.finndot.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Parser for RBL Bank credit card SMS messages
 */
class RBLBankParser : BankParser() {

    override fun getBankName() = "RBL Bank"

    override fun canHandle(sender: String): Boolean {
        val s = sender.uppercase()
        // Common DLT sender IDs usually contain RBL or RBLBNK
        return s.contains("RBL") ||
               s.contains("RBLBNK") ||
               s.matches(Regex("^[A-Z]{2}-RBL[A-Z]*-[STPG]$"))
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lower = message.lowercase()
        if (lower.contains("otp") || lower.contains("one time password")) return false
        return listOf("spent", "debited", "purchase", "txn", "transaction").any { lower.contains(it) }
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lower = message.lowercase()
        return when {
            lower.contains("spent") || lower.contains("debited") || lower.contains("purchase") -> TransactionType.EXPENSE
            lower.contains("credited") || lower.contains("refund") -> TransactionType.INCOME
            else -> super.extractTransactionType(message)
        }
    }

    override fun extractAmount(message: String): BigDecimal? {
        // Prefer explicit INR amounts preceding spent/transaction wording
        val patterns = listOf(
            Regex("""INR\s*([0-9,]+(?:\.\d{2})?)\s*spent""", RegexOption.IGNORE_CASE),
            Regex("""spent\s+INR\s*([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        )
        for (p in patterns) {
            p.find(message)?.let {
                return runCatching { BigDecimal(it.groupValues[1].replace(",", "")) }.getOrNull()
            }
        }
        return super.extractAmount(message)
    }

    override fun extractMerchant(message: String, sender: String): String? {
        // Pattern: "spent at <merchant> on RBL Bank credit card"
        val p = Regex("""spent\s+at\s+(.+?)\s+on\s+RBL\s+Bank\s+credit\s+card""", RegexOption.IGNORE_CASE)
        p.find(message)?.let { m ->
            val merchant = cleanMerchantName(m.groupValues[1])
            if (isValidMerchantName(merchant)) return merchant
        }
        return super.extractMerchant(message, sender)
    }

    override fun extractAccountLast4(message: String): String? {
        // Card last 4 inside parentheses e.g. (0859)
        val p = Regex("""\((\d{4})\)""")
        p.find(message)?.let { return it.groupValues[1] }
        return super.extractAccountLast4(message)
    }

    override fun extractAvailableLimit(message: String): BigDecimal? {
        // "AVL limit- INR110,590.04" or "AVL limit: INR 110,590.04"
        val patterns = listOf(
            Regex("""AVL\s+limit[-:\s]+INR\s*([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""Available\s+limit[-:\s]+INR\s*([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        )
        for (p in patterns) {
            p.find(message)?.let {
                return runCatching { BigDecimal(it.groupValues[1].replace(",", "")) }.getOrNull()
            }
        }
        return super.extractAvailableLimit(message)
    }
}


