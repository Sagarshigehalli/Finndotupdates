package com.finndot.parser.core.bank

import com.finndot.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Parser for EPFO (Employees' Provident Fund Organization) messages.
 */
class EpfoParser : BankParser() {
    
    override fun getBankName() = "EPFO"
    
    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase()
        return normalizedSender.contains("EPFOHO") || 
               normalizedSender.contains("EPF")
    }
    
    override fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()
        return lowerMessage.contains("contribution") && 
               lowerMessage.contains("received")
    }
    
    override fun extractAmount(message: String): BigDecimal? {
        // Look for "Contribution of Rs. X"
        val contributionPattern = Regex("""Contribution\s+of\s+(?:Rs\.?|INR)\s*([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        contributionPattern.find(message)?.let { match ->
            val amountStr = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amountStr)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        return super.extractAmount(message)
    }
    
    override fun extractBalance(message: String): BigDecimal? {
        // Look for "balance ... is Rs. X"
        val balancePattern = Regex("""balance.*(?:is|:)\s*(?:Rs\.?|INR)\s*([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        balancePattern.find(message)?.let { match ->
            val amountStr = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amountStr)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        return super.extractBalance(message)
    }
    
    override fun extractTransactionType(message: String): TransactionType? {
        return TransactionType.INVESTMENT
    }
    
    override fun extractMerchant(message: String, sender: String): String? {
        var merchant = "EPF Contribution"
        
        // Look for "due month Mon-YY"
        val monthPattern = Regex("""due\s+month\s+([A-Za-z]{3}-\d{2})""", RegexOption.IGNORE_CASE)
        monthPattern.find(message)?.let { match ->
            merchant += " ${match.groupValues[1]}"
        }
        
        return merchant
    }
    
    override fun extractAccountLast4(message: String): String? {
        // Look for "Dear XXXXXXXX6289"
        val accountPattern = Regex("""Dear\s+(?:X+)?(\d{4})""", RegexOption.IGNORE_CASE)
        accountPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        return super.extractAccountLast4(message)
    }
}
