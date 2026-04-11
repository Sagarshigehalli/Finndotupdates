import com.finndot.parser.core.TransactionType
import com.finndot.parser.core.bank.HDFCBankParser
import com.finndot.parser.core.test.ExpectedTransaction
import com.finndot.parser.core.test.ParserTestCase
import com.finndot.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SalaryCategorizationTest {

    @Test
    fun `salary categorization works for generic banks`() {
        // HDFC parser uses default extractMerchant for most cases
        val parser = HDFCBankParser()

        ParserTestUtils.printTestHeader(
            parserName = "HDFC Bank (Salary Test)",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val testCases = listOf(
            ParserTestCase(
                name = "Salary Credit via NEFT",
                message = "Rs. 50,000.00 credited to a/c XX1234 on 01-Nov-25 by NEFT/UTR123456/ACME CORP PRIVATE LIMITED. Bal: Rs. 1,00,000.00",
                sender = "HDFCBK",
                expected = ExpectedTransaction(
                    amount = BigDecimal("50000.00"),
                    currency = "INR",
                    type = TransactionType.INCOME,
                    merchant = "Salary - ACME CORP PRIVATE LIMITED",
                    accountLast4 = "1234",
                    balance = BigDecimal("100000.00")
                )
            )
        )

        val handleChecks = listOf(
            "HDFCBK" to true
        )

        val result = ParserTestUtils.runTestSuite(
            parser = parser,
            testCases = testCases,
            handleCases = handleChecks,
            suiteName = "Salary Categorization Test"
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
