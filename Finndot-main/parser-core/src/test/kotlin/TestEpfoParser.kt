import com.finndot.parser.core.TransactionType
import com.finndot.parser.core.bank.EpfoParser
import com.finndot.parser.core.test.ExpectedTransaction
import com.finndot.parser.core.test.ParserTestCase
import com.finndot.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class EpfoParserTest {

    @Test
    fun `epfo parser handles contribution messages`() {
        val parser = EpfoParser()

        ParserTestUtils.printTestHeader(
            parserName = "EPFO",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val testCases = listOf(
            ParserTestCase(
                name = "EPF Contribution",
                message = "Dear XXXXXXXX6289, your passbook balance against PYBOM******0068 is Rs. 73,724/-. Contribution of Rs. 3,600/- for due month Sep-25 has been received.",
                sender = "EPFOHO",
                expected = ExpectedTransaction(
                    amount = BigDecimal("3600"),
                    currency = "INR",
                    type = TransactionType.INVESTMENT,
                    merchant = "EPF Contribution Sep-25",
                    balance = BigDecimal("73724"),
                    accountLast4 = "6289"
                )
            )
        )

        val handleChecks = listOf(
            "EPFOHO" to true,
            "XX-EPFOHO" to true,
            "HDFC" to false
        )

        val result = ParserTestUtils.runTestSuite(
            parser = parser,
            testCases = testCases,
            handleCases = handleChecks,
            suiteName = "EPFO Parser"
        )

        ParserTestUtils.printTestSummary(
            totalTests = result.totalTests,
            passedTests = result.passedTests,
            failedTests = result.failedTests,
            failureDetails = result.failureDetails
        )
        
        if (result.failedTests > 0) {
            throw AssertionError("Tests failed: ${result.failureDetails}")
        }
    }
}
