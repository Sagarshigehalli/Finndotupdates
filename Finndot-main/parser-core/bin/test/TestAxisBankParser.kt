package com.finndot.parser.core.bank

import com.finndot.parser.core.test.ParserTestUtils
import com.finndot.parser.core.test.ParserTestCase
import com.finndot.parser.core.test.ExpectedTransaction
import com.finndot.parser.core.TransactionType
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class AxisBankParserTest {
    @Test
    fun `test Axis Bank Parser comprehensive test suite`() {
        val parser = AxisBankParser()

        ParserTestUtils.printTestHeader(
            parserName = "Axis Bank",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val testCases = listOf(
            ParserTestCase(
                name = "Credit Card - Swiggy",
                message = "INR 131.00 debited from Credit Card XX0818 on Swiggy 02-12-2025 20:38:23 IST. Avl Limit: INR 217162.72. Not you? SMS BLOCK 0818 to 919951860002",
                sender = "JD-AXISBK-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("131.00"),
                    currency = "INR",
                    type = TransactionType.CREDIT,
                    merchant = "Swiggy",
                    accountLast4 = "0818",
                    isFromCard = true,
                    creditLimit = BigDecimal("217162.72")
                )
            ),
            ParserTestCase(
                name = "Credit Card - Amazon",
                message = "INR 1299.00 debited from Credit Card XX5678 on Amazon 02-12-2025 20:38:23 IST. Avl Limit: INR 50000.00. Not you? SMS BLOCK 5678 to 919951860002",
                sender = "JD-AXISBK-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1299.00"),
                    currency = "INR",
                    type = TransactionType.CREDIT,
                    merchant = "Amazon",
                    accountLast4 = "5678",
                    isFromCard = true,
                    creditLimit = BigDecimal("50000.00")
                )
            ),
            ParserTestCase(
                name = "Credit Card - AVENUE",
                message = "INR 562.00 debited from Credit Card XX7441 on AVENUE 02-12-2025 20:38:23 IST. Avl Limit: INR 5120.87. Not you? SMS BLOCK 7441 to 919951860002, if not you - Axis Bank",
                sender = "JD-AXISBK-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("562.00"),
                    currency = "INR",
                    type = TransactionType.CREDIT,
                    merchant = "AVENUE",
                    accountLast4 = "7441",
                    isFromCard = true,
                    creditLimit = BigDecimal("5120.87")
                )
            ),
            ParserTestCase(
                name = "Credit Card - Blinkit",
                message = "INR 174.00 debited from Credit Card XX7441 on Blinkit 02-12-2025 20:38:23 IST. Avl Limit: INR 6652.78. Not you? SMS BLOCK 7441 to 919951860002",
                sender = "JD-AXISBK-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("174.00"),
                    currency = "INR",
                    type = TransactionType.CREDIT,
                    merchant = "Blinkit",
                    accountLast4 = "7441",
                    isFromCard = true,
                    creditLimit = BigDecimal("6652.78")
                )
            ),
            ParserTestCase(
                name = "Credit Card - Blinkit 2",
                message = "INR 207.00 debited from Credit Card XX7441 on Blinkit 02-12-2025 20:38:23 IST. Avl Lmt INR 4632.87. Not you? SMS BLOCK 7441 to 919951860002, if not you - Axis Bank",
                sender = "JD-AXISBK-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("207.00"),
                    currency = "INR",
                    type = TransactionType.CREDIT,
                    merchant = "Blinkit",
                    accountLast4 = "7441",
                    isFromCard = true,
                    creditLimit = BigDecimal("4632.87")
                )
            ),
            ParserTestCase(
                name = "Credit Card - BPCL ARUNAA",
                message = "INR 500.00 debited from Credit Card XX6018 on BPCL ARUNAA 02-12-2025 20:38:23 IST. Avl Limit: INR 17131.47. Not you? SMS BLOCK 6018 to 919951860002",
                sender = "JD-AXISBK-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("500.00"),
                    currency = "INR",
                    type = TransactionType.CREDIT,
                    merchant = "BPCL ARUNAA",
                    accountLast4 = "6018",
                    isFromCard = true,
                    creditLimit = BigDecimal("17131.47")
                )
            ),
            ParserTestCase(
                name = "Credit Card - JSK FUEL ST",
                message = "INR 500.00 debited from Credit Card XX6018 on JSK FUEL ST 02-12-2025 20:38:23 IST. Avl Limit: INR 6826.78. Not you? SMS BLOCK 6018 to 919951860002",
                sender = "JD-AXISBK-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("500.00"),
                    currency = "INR",
                    type = TransactionType.CREDIT,
                    merchant = "JSK FUEL ST",
                    accountLast4 = "6018",
                    isFromCard = true,
                    creditLimit = BigDecimal("6826.78")
                )
            ),
            ParserTestCase(
                name = "Debit Card - Restaurant",
                message = "INR 1028.00 debited from A/c no. XX1234 on RESTAURANT XY 02-12-2025 20:38:23 IST. Avl bal: INR xxxxxxx. Not you? SMS BLOCKCARD XX0023 to +919951860002 - Axis Bank",
                sender = "JD-AXISBK-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1028.00"),
                    currency = "INR",
                    type = TransactionType.EXPENSE,
                    merchant = "RESTAURANT XY",
                    accountLast4 = "1234"
                )
            ),
            ParserTestCase(
                name = "Debit Card - Numeric Account Pattern",
                message = "INR 500.00 debited from A/c no. XX312225 on MERCHANT ABC 02-12-2025 20:38:23 IST. Avl bal: INR 10000.00. Not you? SMS BLOCKCARD XX0023 to +919951860002 - Axis Bank",
                sender = "JD-AXISBK-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("500.00"),
                    currency = "INR",
                    type = TransactionType.EXPENSE,
                    merchant = "MERCHANT ABC",
                    accountLast4 = "2225",
                    balance = BigDecimal("10000.00")
                )
            )
        )

        val handleCases: List<Pair<String, Boolean>> = listOf(
            "AXISBK" to true,
            "AXISBANK" to true,
            "JD-AXISBK-S" to true,
            "HDFC" to false
        )

        val result = ParserTestUtils.runTestSuite(parser, testCases, handleCases, "Axis Bank Parser Tests")
        ParserTestUtils.printTestSummary(
            totalTests = result.totalTests,
            passedTests = result.passedTests,
            failedTests = result.failedTests,
            failureDetails = result.failureDetails
        )
    }
}
