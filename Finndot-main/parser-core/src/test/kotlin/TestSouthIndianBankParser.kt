import com.finndot.parser.core.TransactionType
import com.finndot.parser.core.bank.SouthIndianBankParser
import com.finndot.parser.core.test.ExpectedTransaction
import com.finndot.parser.core.test.ParserTestCase
import com.finndot.parser.core.test.ParserTestUtils
import com.finndot.parser.core.test.SimpleTestCase
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SouthIndianBankParserTest {

    @Test
    fun `south indian bank parser basic flows`() {
        val parser = SouthIndianBankParser()

        ParserTestUtils.printTestHeader(
            parserName = "South Indian Bank",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val testCases = listOf(
            ParserTestCase(
                name = "IMPS credit with reference",
                message = "Dear Customer, Your A/c X7377 is credited with Rs.792.02 Info: IMPS/FDRL/528005821348/EPIFI ACCOUN. Final balance is Rs.793.02-South Indian Bank",
                sender = "SIBSMS",
                expected = ExpectedTransaction(
                    amount = BigDecimal("792.02"),
                    currency = "INR",
                    type = TransactionType.INCOME,
                    merchant = "EPIFI ACCOUN",
                    accountLast4 = "7377",
                    balance = BigDecimal("793.02"),
                    reference = "528005821348"
                )
            )
        )

        val handleChecks = listOf(
            "SIBSMS" to true,
            "AD-SIBSMS" to true,
            "CP-SIBSMS" to true,
            "AD-SIBSMS-S" to true,
            "SIBBANK" to true,
            "AX-HDFC-S" to false
        )

        val result = ParserTestUtils.runTestSuite(
            parser = parser,
            testCases = testCases,
            handleCases = handleChecks,
            suiteName = "South Indian Bank Parser"
        )

        ParserTestUtils.printTestSummary(
            totalTests = result.totalTests,
            passedTests = result.passedTests,
            failedTests = result.failedTests,
            failureDetails = result.failureDetails
        )
    }

    @Test
    fun `factory resolves south indian bank`() {
        val cases = listOf(
            SimpleTestCase(
                bankName = "South Indian Bank",
                sender = "SIBSMS",
                currency = "INR",
                message = "Dear Customer, Your A/c X7377 is credited with Rs.792.02 Info: IMPS/FDRL/528005821348/EPIFI ACCOUN. Final balance is Rs.793.02-South Indian Bank",
                expected = ExpectedTransaction(
                    amount = BigDecimal("792.02"),
                    currency = "INR",
                    type = TransactionType.INCOME
                ),
                shouldHandle = true
            )
        )

        val suite = ParserTestUtils.runFactoryTestSuite(cases, "Factory smoke tests - South Indian Bank")
        ParserTestUtils.printTestSummary(
            totalTests = suite.totalTests,
            passedTests = suite.passedTests,
            failedTests = suite.failedTests,
            failureDetails = suite.failureDetails
        )
    }
}
