package com.anomapro.finndot.domain.service

import com.anomapro.finndot.data.database.entity.TransactionEntity
import com.anomapro.finndot.data.database.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class TransferLikeSmsClassifierTest {

    private val classifier = TransferLikeSmsClassifier()
    private val t0 = LocalDateTime.of(2025, 7, 1, 10, 0, 0)

    private fun baseExpense(smsBody: String) = TransactionEntity(
        amount = BigDecimal("5000"),
        merchantName = "FT",
        category = "Banking",
        transactionType = TransactionType.EXPENSE,
        dateTime = t0,
        transactionHash = "h-neft",
        bankName = "SBI",
        accountNumber = "1234",
        smsBody = smsBody
    )

    @Test
    fun neftWithoutMerchant_promotesToTransfer() {
        val sms = "Rs.5000 debited from A/c XX1234 towards NEFT txn. Ref 123456789. IFSC HDFC0000123 beneficiary NAME"
        val out = classifier.applyAfterRules(baseExpense(sms), sms)
        assertEquals(TransactionType.TRANSFER, out.transactionType)
        assertEquals(TransferLikeSmsClassifier.HEURISTIC_TRANSFER_CATEGORY, out.category)
    }

    @Test
    fun impsCredit_promotesToTransfer() {
        val sms = "Your A/c XX5678 credited with Rs 2500.00 on 01-Jul-25 IMPS from XX9999-SBI Ref 987654321"
        val inc = TransactionEntity(
            amount = BigDecimal("2500"),
            merchantName = "IMPS",
            category = "Income",
            transactionType = TransactionType.INCOME,
            dateTime = t0,
            transactionHash = "h-imps",
            bankName = "HDFC Bank",
            accountNumber = "5678",
            smsBody = sms
        )
        val out = classifier.applyAfterRules(inc, sms)
        assertEquals(TransactionType.TRANSFER, out.transactionType)
    }

    @Test
    fun posWithNeftKeywordInMerchantName_staysExpense() {
        val sms = "Rs.500 debited at POS SWIGGY NEFT settlement terminal Mumbai"
        val out = classifier.applyAfterRules(baseExpense(sms), sms)
        assertEquals(TransactionType.EXPENSE, out.transactionType)
    }

    @Test
    fun amazonPurchase_staysExpense() {
        val sms = "Rs.899 debited to A/c XX1234 on Amazon.in txn ref 998877"
        val out = classifier.applyAfterRules(baseExpense(sms), sms)
        assertEquals(TransactionType.EXPENSE, out.transactionType)
    }

    @Test
    fun recurring_notChanged() {
        val sms = "Rs.5000 debited NEFT transfer"
        val entity = baseExpense(sms).copy(isRecurring = true)
        val out = classifier.applyAfterRules(entity, sms)
        assertEquals(TransactionType.EXPENSE, out.transactionType)
    }

    @Test
    fun creditCardSpend_notChanged() {
        val sms = "Rs.5000 debited NEFT transfer"
        val entity = baseExpense(sms).copy(transactionType = TransactionType.CREDIT)
        val out = classifier.applyAfterRules(entity, sms)
        assertEquals(TransactionType.CREDIT, out.transactionType)
    }
}
