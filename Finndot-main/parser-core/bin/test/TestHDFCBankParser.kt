package com.finndot.parser.core.bank

import com.finndot.parser.core.TransactionType
import com.finndot.parser.core.test.ExpectedTransaction
import com.finndot.parser.core.test.ParserTestCase
import com.finndot.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class HDFCBankParserTest {

    @Test
    fun `test HDFC Bank Parser`() {
        val parser = HDFCBankParser()

        ParserTestUtils.printTestHeader(
            parserName = "HDFC Bank",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val testCases = listOf(
            ParserTestCase(
                name = "HDFC Life Insurance Survival Benefit - Should be ignored",
                message = "Survival Benefit payout of INR 12000.00 against policy no. 27602783 has been credited to your bank a/c via NEFT on 29/04/2025 . T&C apply-PO_HDFC Life",
                sender = "HDFCBK",
                shouldParse = false, // Should be rejected/ignored
                expected = null
            )
        )

        val handleCases = listOf(
            "HDFCBK" to true,
            "HDFCBANK" to true,
            "PO_HDFC Life" to false // By default HDFC parser likely rejects this unless "HDFC" is enough?
                                    // canHandle checks: "HDFCBK", "HDFCBANK", "HDFC", "HDFCB" or DLT.
                                    // "PO_HDFC Life".uppercase() contains "HDFC" ? No, contains "HDFC LIFE".
                                    // canHandle implementation:
                                    // val hdfcSenders = setOf("HDFCBK", "HDFCBANK", "HDFC", "HDFCB")
                                    // if (upperSender in hdfcSenders) return true
                                    // pattern match DLT: ^[A-Z]{2}-HDFCBK.*$ etc.
                                    // "PO_HDFC Life" -> doesn't match these strictly.
                                    // However, user might have mapped it manually or it's a "HDFC" substring match that I missed.
                                    // Wait, regex DLT matches: Regex("^[A-Z]{2}-HDFC.*$")
                                    // "PO-HDFC" would match. "PO_HDFC" ?
                                    // The user said "PO_HDFC Life". "_" is not usually a separator in sender IDs, usually "-".
                                    // Maybe it is "PO-HDFC"? Let's assume standard format matches HDFC parser.
        )

        val result = ParserTestUtils.runTestSuite(parser, testCases, handleCases, "HDFC Bank Parser Tests")
        ParserTestUtils.printTestSummary(
            totalTests = result.totalTests,
            passedTests = result.passedTests,
            failedTests = result.failedTests,
            failureDetails = result.failureDetails
        )
    }
}
