import com.finndot.parser.core.TransactionType
import com.finndot.parser.core.bank.SliceParser
import com.finndot.parser.core.test.ExpectedTransaction
import com.finndot.parser.core.test.ParserTestCase
import com.finndot.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SliceParserTest {

    @Test
    fun `slice parser handles fixed deposit creation`() {
        val parser = SliceParser()

        ParserTestUtils.printTestHeader(
            parserName = "Slice",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val testCases = listOf(
            ParserTestCase(
                name = "Fixed Deposit Creation",
                message = "Your fixed deposit of Rs. 1,60,000 has been created on 12-Nov-2025. For queries, call 080-4832-9999 - slice",
                sender = "JK-SLICEIT",
                expected = ExpectedTransaction(
                    amount = BigDecimal("160000"),
                    currency = "INR",
                    type = TransactionType.INVESTMENT,
                    merchant = "Slice Fixed Deposit"
                )
            )
        )

        val handleChecks = listOf(
            "JK-SLICEIT" to true,
            "SLICE" to true,
            "HDFC" to false
        )

        val result = ParserTestUtils.runTestSuite(
            parser = parser,
            testCases = testCases,
            handleCases = handleChecks,
            suiteName = "Slice Parser"
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
