package com.finndot.parser.core.bank

import com.finndot.parser.core.TransactionType
import com.finndot.parser.core.test.ExpectedTransaction
import com.finndot.parser.core.test.ParserTestCase
import com.finndot.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class CanaraBankParserTest {

    @Test
    fun `test Canara Bank Parser`() {
        val parser = CanaraBankParser()

        ParserTestUtils.printTestHeader(
            parserName = "Canara Bank",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val testCases = listOf(
            ParserTestCase(
                name = "Credit with Sender Account Issue",
                message = "An amount of INR 18,333.00 has been credited to XXXX7122 on 21/11/2023 towards NEFT by Sender CHARA TECHNOLOGIES PRIVATE LIM, IFSC IDFB0010204, Sender A/c XXXX8266, IDFC BANK LIMITED, CMS HUB, UTR IDFBH23325958209, Total Avail. Bal INR 38412.7- Canara Bank",
                sender = "CANBNK",
                expected = ExpectedTransaction(
                    amount = BigDecimal("18333.00"),
                    currency = "INR",
                    type = TransactionType.INCOME,
                    merchant = "NEFT by Sender CHARA TECHNOLOGIES PRIVATE LIM", // Or similar extraction
                    accountLast4 = "7122",
                    balance = BigDecimal("38412.7")
                )
            )
        )

        val handleCases = listOf(
            "CANBNK" to true,
            "CANARA" to true,
            "HDFC" to false
        )

        val result = ParserTestUtils.runTestSuite(parser, testCases, handleCases, "Canara Bank Parser Tests")
        ParserTestUtils.printTestSummary(
            totalTests = result.totalTests,
            passedTests = result.passedTests,
            failedTests = result.failedTests,
            failureDetails = result.failureDetails
        )
    }
}
