package com.anomapro.finndot.domain.service

import com.anomapro.finndot.data.database.entity.TransactionType
import com.anomapro.finndot.domain.model.rule.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleTemplateService @Inject constructor() {

    fun getDefaultRuleTemplates(): List<TransactionRule> {
        return listOf(
            createSmallPaymentsToFoodRule(),
            // SMS_TEXT templates: major Indian banks — inactive until the user enables them.
            createSbiSmsRailTransferTemplate(),
            createHdfcSmsRailTransferTemplate(),
            createIciciSmsRailTransferTemplate(),
            createAxisSmsRailTransferTemplate(),
            createKotakSmsRailTransferTemplate(),
            createPnbSmsRailTransferTemplate(),
            createBankOfBarodaSmsRailTransferTemplate(),
            createGenericIndianSmsRailTransferTemplate(),
        )
    }

    /**
     * [RuleEngine] uses [String.matches], so patterns must match the **entire** SMS body.
     * [smsMerchantExclusionPrefix] drops obvious POS / e‑commerce phrasing (aligned with
     * [CompiledPatterns.TransferHeuristic.MERCHANT_OR_POS_BLOCK]).
     */
    private fun smsMerchantExclusionPrefix(): String =
        """(?is)(?!.*(?:Amazon|AMZN|Flipkart|FKRT|Swiggy|Zomato|Point\s+of\s+Sale|\bPOS\b|""" +
            """Card\s+purchase|Debit\s+purchase|Online\s+payment|Tap\s*(?:&|and)?\s*Pay))"""

    private fun smsTransferRailsSubpattern(): String =
        """(?:\bNEFT\b|\bIMPS\b|\bRTGS\b|Fund\s+Transfer|Beneficiary|\bIFSC\b|\bUTR\b|""" +
            """INB[-/\s]*(?:FT|NEFT|IMPS|RTGS)|INF[-/\s]*(?:FT|NEFT|IMPS|RTGS))"""

    private fun smsBankRailTransferRule(
        name: String,
        description: String,
        priority: Int,
        bankSubpattern: String,
    ): TransactionRule {
        val regex =
            smsMerchantExclusionPrefix() +
                """.*(?=.*""" + bankSubpattern + """)(?=.*""" + smsTransferRailsSubpattern() + """).*"""
        return TransactionRule(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            priority = priority,
            conditions = listOf(
                RuleCondition(
                    field = TransactionField.TYPE,
                    operator = ConditionOperator.IN,
                    value = "EXPENSE,INCOME",
                    logicalOperator = LogicalOperator.AND,
                ),
                RuleCondition(
                    field = TransactionField.SMS_TEXT,
                    operator = ConditionOperator.REGEX_MATCHES,
                    value = regex,
                    logicalOperator = LogicalOperator.AND,
                ),
            ),
            actions = listOf(
                RuleAction(
                    field = TransactionField.TYPE,
                    actionType = ActionType.SET,
                    value = TransactionType.TRANSFER.name,
                ),
                RuleAction(
                    field = TransactionField.CATEGORY,
                    actionType = ActionType.SET,
                    value = TransferLikeSmsClassifier.HEURISTIC_TRANSFER_CATEGORY,
                ),
            ),
            isActive = false,
            isSystemTemplate = true,
        )
    }

    private fun createSbiSmsRailTransferTemplate(): TransactionRule =
        smsBankRailTransferRule(
            name = "Template: SBI — NEFT/IMPS/RTGS (SMS)",
            description = "State Bank of India SMS mentioning NEFT/IMPS/RTGS, IFSC, UTR, or fund transfer",
            priority = 22,
            bankSubpattern = """(?:\bSBI\b|SBIN|STATE\s+BANK\s+(?:OF\s+)?INDIA|YONO\s+SBI)""",
        )

    private fun createHdfcSmsRailTransferTemplate(): TransactionRule =
        smsBankRailTransferRule(
            name = "Template: HDFC — NEFT/IMPS/RTGS (SMS)",
            description = "HDFC Bank SMS with rail keywords (NEFT/IMPS/RTGS, IFSC, INB-FT, etc.)",
            priority = 24,
            bankSubpattern = """(?:HDFC|HDFCBK|HDFC\s+Bank)""",
        )

    private fun createIciciSmsRailTransferTemplate(): TransactionRule =
        smsBankRailTransferRule(
            name = "Template: ICICI — NEFT/IMPS/RTGS (SMS)",
            description = "ICICI Bank SMS with rail keywords",
            priority = 26,
            bankSubpattern = """(?:ICICI|ICICIB|ICICI\s+Bank)""",
        )

    private fun createAxisSmsRailTransferTemplate(): TransactionRule =
        smsBankRailTransferRule(
            name = "Template: Axis — NEFT/IMPS/RTGS (SMS)",
            description = "Axis Bank SMS with rail keywords",
            priority = 28,
            bankSubpattern = """(?:\bAXIS\b|UTIB|Axis\s+Bank)""",
        )

    private fun createKotakSmsRailTransferTemplate(): TransactionRule =
        smsBankRailTransferRule(
            name = "Template: Kotak — NEFT/IMPS/RTGS (SMS)",
            description = "Kotak Mahindra Bank SMS with rail keywords",
            priority = 30,
            bankSubpattern = """(?:KOTAK|KKBK|Kotak\s+Mahindra)""",
        )

    private fun createPnbSmsRailTransferTemplate(): TransactionRule =
        smsBankRailTransferRule(
            name = "Template: PNB — NEFT/IMPS/RTGS (SMS)",
            description = "Punjab National Bank SMS with rail keywords",
            priority = 32,
            bankSubpattern = """(?:\bPNB\b|PUNB|Punjab\s+National)""",
        )

    private fun createBankOfBarodaSmsRailTransferTemplate(): TransactionRule =
        smsBankRailTransferRule(
            name = "Template: Bank of Baroda — NEFT/IMPS/RTGS (SMS)",
            description = "Bank of Baroda SMS with rail keywords",
            priority = 34,
            bankSubpattern = """(?:\bBOB\b|BARB|Bank\s+of\s+Baroda)""",
        )

    private fun createGenericIndianSmsRailTransferTemplate(): TransactionRule {
        val rails = smsTransferRailsSubpattern()
        val extraAnchor = """(?:IFSC|UTR|Beneficiary)"""
        val regex =
            smsMerchantExclusionPrefix() +
                """.*(?=.*""" + rails + """)(?=.*""" + extraAnchor + """).*"""
        return TransactionRule(
            id = UUID.randomUUID().toString(),
            name = "Template: Any bank — NEFT/IMPS/RTGS + IFSC/UTR (SMS)",
            description = "Non‑merchant SMS mentioning NEFT/IMPS/RTGS (or equivalent) and IFSC, UTR, or beneficiary",
            priority = 50,
            conditions = listOf(
                RuleCondition(
                    field = TransactionField.TYPE,
                    operator = ConditionOperator.IN,
                    value = "EXPENSE,INCOME",
                    logicalOperator = LogicalOperator.AND,
                ),
                RuleCondition(
                    field = TransactionField.SMS_TEXT,
                    operator = ConditionOperator.REGEX_MATCHES,
                    value = regex,
                    logicalOperator = LogicalOperator.AND,
                ),
            ),
            actions = listOf(
                RuleAction(
                    field = TransactionField.TYPE,
                    actionType = ActionType.SET,
                    value = TransactionType.TRANSFER.name,
                ),
                RuleAction(
                    field = TransactionField.CATEGORY,
                    actionType = ActionType.SET,
                    value = TransferLikeSmsClassifier.HEURISTIC_TRANSFER_CATEGORY,
                ),
            ),
            isActive = false,
            isSystemTemplate = true,
        )
    }

    private fun createSmallPaymentsToFoodRule(): TransactionRule {
        return TransactionRule(
            id = UUID.randomUUID().toString(),
            name = "Small Payments to Food",
            description = "Categorize small expense payments (under ₹200) as Food & Dining",
            priority = 100,
            conditions = listOf(
                RuleCondition(
                    field = TransactionField.AMOUNT,
                    operator = ConditionOperator.LESS_THAN,
                    value = "200",
                    logicalOperator = LogicalOperator.AND
                ),
                RuleCondition(
                    field = TransactionField.TYPE,
                    operator = ConditionOperator.EQUALS,
                    value = "EXPENSE",
                    logicalOperator = LogicalOperator.AND
                )
            ),
            actions = listOf(
                RuleAction(
                    field = TransactionField.CATEGORY,
                    actionType = ActionType.SET,
                    value = "Food & Dining"
                )
            ),
            isActive = false, // Users can enable this
            isSystemTemplate = true
        )
    }

    private fun createUpiCashbackRule(): TransactionRule {
        return TransactionRule(
            id = UUID.randomUUID().toString(),
            name = "UPI Cashback",
            description = "Identify small UPI receipts (under ₹10) from NPCI as cashback",
            priority = 50,
            conditions = listOf(
                RuleCondition(
                    field = TransactionField.AMOUNT,
                    operator = ConditionOperator.LESS_THAN,
                    value = "10",
                    logicalOperator = LogicalOperator.AND
                ),
                RuleCondition(
                    field = TransactionField.TYPE,
                    operator = ConditionOperator.EQUALS,
                    value = "INCOME",
                    logicalOperator = LogicalOperator.AND
                ),
                RuleCondition(
                    field = TransactionField.SMS_TEXT,
                    operator = ConditionOperator.CONTAINS,
                    value = "NPCI",
                    logicalOperator = LogicalOperator.AND
                )
            ),
            actions = listOf(
                RuleAction(
                    field = TransactionField.CATEGORY,
                    actionType = ActionType.SET,
                    value = "Cashback"
                )
            ),
            isActive = false
        )
    }

    private fun createSalaryDetectionRule(): TransactionRule {
        return TransactionRule(
            id = UUID.randomUUID().toString(),
            name = "Salary Detection",
            description = "Detect salary credits based on keywords",
            priority = 75,
            conditions = listOf(
                RuleCondition(
                    field = TransactionField.TYPE,
                    operator = ConditionOperator.EQUALS,
                    value = "INCOME",
                    logicalOperator = LogicalOperator.AND
                ),
                RuleCondition(
                    field = TransactionField.SMS_TEXT,
                    operator = ConditionOperator.REGEX_MATCHES,
                    value = "(?i)(salary|sal|stipend|wages|payroll)",
                    logicalOperator = LogicalOperator.OR
                ),
                RuleCondition(
                    field = TransactionField.NARRATION,
                    operator = ConditionOperator.REGEX_MATCHES,
                    value = "(?i)(salary|sal|stipend|wages|payroll)",
                    logicalOperator = LogicalOperator.AND
                )
            ),
            actions = listOf(
                RuleAction(
                    field = TransactionField.CATEGORY,
                    actionType = ActionType.SET,
                    value = "Salary"
                )
            ),
            isActive = false
        )
    }

    private fun createRentPaymentRule(): TransactionRule {
        return TransactionRule(
            id = UUID.randomUUID().toString(),
            name = "Rent Payment Detection",
            description = "Identify rent payments based on keywords and amount patterns",
            priority = 80,
            conditions = listOf(
                RuleCondition(
                    field = TransactionField.TYPE,
                    operator = ConditionOperator.EQUALS,
                    value = "EXPENSE",
                    logicalOperator = LogicalOperator.AND
                ),
                RuleCondition(
                    field = TransactionField.SMS_TEXT,
                    operator = ConditionOperator.REGEX_MATCHES,
                    value = "(?i)(rent|landlord|house owner|flat|apartment)",
                    logicalOperator = LogicalOperator.OR
                ),
                RuleCondition(
                    field = TransactionField.NARRATION,
                    operator = ConditionOperator.REGEX_MATCHES,
                    value = "(?i)(rent|landlord|house owner)",
                    logicalOperator = LogicalOperator.AND
                )
            ),
            actions = listOf(
                RuleAction(
                    field = TransactionField.CATEGORY,
                    actionType = ActionType.SET,
                    value = "Housing"
                )
            ),
            isActive = false
        )
    }

    private fun createInvestmentDetectionRule(): TransactionRule {
        return TransactionRule(
            id = UUID.randomUUID().toString(),
            name = "Investment Detection",
            description = "Categorize mutual funds, stocks, and other investments",
            priority = 85,
            conditions = listOf(
                RuleCondition(
                    field = TransactionField.SMS_TEXT,
                    operator = ConditionOperator.REGEX_MATCHES,
                    value = "(?i)(mutual fund|mf|sip|zerodha|groww|upstox|paytm money|kuvera|et money|stocks|shares|demat)",
                    logicalOperator = LogicalOperator.OR
                ),
                RuleCondition(
                    field = TransactionField.MERCHANT,
                    operator = ConditionOperator.REGEX_MATCHES,
                    value = "(?i)(zerodha|groww|upstox|paytm money|kuvera|et money|hdfc securities|icici direct)",
                    logicalOperator = LogicalOperator.AND
                )
            ),
            actions = listOf(
                RuleAction(
                    field = TransactionField.CATEGORY,
                    actionType = ActionType.SET,
                    value = "Investments"
                )
            ),
            isActive = false
        )
    }

    private fun createEmiDetectionRule(): TransactionRule {
        return TransactionRule(
            id = UUID.randomUUID().toString(),
            name = "EMI Detection",
            description = "Identify EMI payments",
            priority = 90,
            conditions = listOf(
                RuleCondition(
                    field = TransactionField.TYPE,
                    operator = ConditionOperator.EQUALS,
                    value = "EXPENSE",
                    logicalOperator = LogicalOperator.AND
                ),
                RuleCondition(
                    field = TransactionField.SMS_TEXT,
                    operator = ConditionOperator.REGEX_MATCHES,
                    value = "(?i)(emi|equated monthly|installment|loan)",
                    logicalOperator = LogicalOperator.OR
                ),
                RuleCondition(
                    field = TransactionField.NARRATION,
                    operator = ConditionOperator.CONTAINS,
                    value = "EMI",
                    logicalOperator = LogicalOperator.AND
                )
            ),
            actions = listOf(
                RuleAction(
                    field = TransactionField.CATEGORY,
                    actionType = ActionType.SET,
                    value = "EMI"
                )
            ),
            isActive = false
        )
    }

    private fun createTransferCategorizationRule(): TransactionRule {
        return TransactionRule(
            id = UUID.randomUUID().toString(),
            name = "Transfer Detection",
            description = "Mark contra and transfer transactions",
            priority = 95,
            conditions = listOf(
                RuleCondition(
                    field = TransactionField.NARRATION,
                    operator = ConditionOperator.REGEX_MATCHES,
                    value = "(?i)(contra|transfer|trf|self)",
                    logicalOperator = LogicalOperator.OR
                ),
                RuleCondition(
                    field = TransactionField.SMS_TEXT,
                    operator = ConditionOperator.REGEX_MATCHES,
                    value = "(?i)(transfer to self|own account|linked account)",
                    logicalOperator = LogicalOperator.AND
                )
            ),
            actions = listOf(
                RuleAction(
                    field = TransactionField.TYPE,
                    actionType = ActionType.SET,
                    value = TransactionType.TRANSFER.name
                ),
                RuleAction(
                    field = TransactionField.CATEGORY,
                    actionType = ActionType.SET,
                    value = TransferLikeSmsClassifier.HEURISTIC_TRANSFER_CATEGORY
                )
            ),
            isActive = false
        )
    }

    private fun createSubscriptionDetectionRule(): TransactionRule {
        return TransactionRule(
            id = UUID.randomUUID().toString(),
            name = "Subscription Detection",
            description = "Identify recurring subscriptions like Netflix, Spotify, etc.",
            priority = 105,
            conditions = listOf(
                RuleCondition(
                    field = TransactionField.MERCHANT,
                    operator = ConditionOperator.REGEX_MATCHES,
                    value = "(?i)(netflix|spotify|amazon prime|hotstar|youtube|apple|google|microsoft|adobe)",
                    logicalOperator = LogicalOperator.OR
                ),
                RuleCondition(
                    field = TransactionField.SMS_TEXT,
                    operator = ConditionOperator.REGEX_MATCHES,
                    value = "(?i)(subscription|recurring|auto-debit|mandate)",
                    logicalOperator = LogicalOperator.AND
                )
            ),
            actions = listOf(
                RuleAction(
                    field = TransactionField.CATEGORY,
                    actionType = ActionType.SET,
                    value = "Entertainment"
                )
            ),
            isActive = false
        )
    }

    private fun createFuelDetectionRule(): TransactionRule {
        return TransactionRule(
            id = UUID.randomUUID().toString(),
            name = "Fuel/Petrol Detection",
            description = "Categorize fuel and petrol pump transactions",
            priority = 110,
            conditions = listOf(
                RuleCondition(
                    field = TransactionField.MERCHANT,
                    operator = ConditionOperator.REGEX_MATCHES,
                    value = "(?i)(indian oil|bharat petroleum|hp|hindustan petroleum|bpcl|iocl|shell|essar|reliance|petrol|diesel|fuel|pump)",
                    logicalOperator = LogicalOperator.OR
                ),
                RuleCondition(
                    field = TransactionField.SMS_TEXT,
                    operator = ConditionOperator.REGEX_MATCHES,
                    value = "(?i)(petrol|diesel|fuel|pump|filling station)",
                    logicalOperator = LogicalOperator.AND
                )
            ),
            actions = listOf(
                RuleAction(
                    field = TransactionField.CATEGORY,
                    actionType = ActionType.SET,
                    value = "Transportation"
                )
            ),
            isActive = false
        )
    }

    private fun createHealthcareDetectionRule(): TransactionRule {
        return TransactionRule(
            id = UUID.randomUUID().toString(),
            name = "Healthcare Detection",
            description = "Identify medical and healthcare expenses",
            priority = 115,
            conditions = listOf(
                RuleCondition(
                    field = TransactionField.MERCHANT,
                    operator = ConditionOperator.REGEX_MATCHES,
                    value = "(?i)(apollo|fortis|max|medanta|aiims|hospital|clinic|pharmacy|medical|pharma|netmeds|1mg|pharmeasy)",
                    logicalOperator = LogicalOperator.OR
                ),
                RuleCondition(
                    field = TransactionField.SMS_TEXT,
                    operator = ConditionOperator.REGEX_MATCHES,
                    value = "(?i)(hospital|doctor|medical|medicine|pharmacy|health|diagnostic|lab|test)",
                    logicalOperator = LogicalOperator.AND
                )
            ),
            actions = listOf(
                RuleAction(
                    field = TransactionField.CATEGORY,
                    actionType = ActionType.SET,
                    value = "Healthcare"
                )
            ),
            isActive = false
        )
    }
}