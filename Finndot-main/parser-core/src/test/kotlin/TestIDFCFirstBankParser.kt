package com.finndot.parser.core.bank

import com.finndot.parser.core.TransactionType
import com.finndot.parser.core.test.ExpectedTransaction
import com.finndot.parser.core.test.ParserTestCase
import com.finndot.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class IDFCFirstBankParserTest {

    @Test
    fun `test IDFC First Bank Parser`() {
        val parser = IDFCFirstBankParser()

        ParserTestUtils.printTestHeader(
            parserName = "IDFC First Bank",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val testCases = listOf(
            ParserTestCase(
                name = "No account number, IMPS ref confusion",
                message = "Your a/c no. TxnToAccount is credited by Rs. 4441.40 on 15-Jul-25 by a/c linked to mobile XXXXXXXXX898 (IMPS Ref no 519612115474 ). Team IDFC FIRST Bank",
                sender = "IDFCBK",
                expected = ExpectedTransaction(
                    amount = BigDecimal("4441.40"),
                    currency = "INR",
                    type = TransactionType.INCOME,
                    merchant = "IMPS Transfer - Mobile XXX898",
                    accountLast4 = null, // Should be null, not 5474
                    reference = "519612115474"
                )
            )
        )

        val handleCases = listOf(
            "IDFCBK" to true,
            "IDFCFB" to true,
            "HDFC" to false
        )

        val result = ParserTestUtils.runTestSuite(parser, testCases, handleCases, "IDFC First Bank Parser Tests")
        ParserTestUtils.printTestSummary(
            totalTests = result.totalTests,
            passedTests = result.passedTests,
            failedTests = result.failedTests,
            failureDetails = result.failureDetails
        )
    }
}
