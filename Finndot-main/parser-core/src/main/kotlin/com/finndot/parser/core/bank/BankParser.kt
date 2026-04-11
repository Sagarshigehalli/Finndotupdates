package com.finndot.parser.core.bank

import com.finndot.parser.core.CompiledPatterns
import com.finndot.parser.core.Constants
import com.finndot.parser.core.TransactionType
import com.finndot.parser.core.ParsedTransaction
import java.math.BigDecimal

/**
 * Base class for bank-specific message parsers.
 * Each bank should extend this class and implement its specific parsing logic.
 */
abstract class BankParser {
    
    /**
     * Returns the name of the bank this parser handles.
     */
    abstract fun getBankName(): String
    
    /**
     * Checks if this parser can handle messages from the given sender.
     */
    abstract fun canHandle(sender: String): Boolean

    /**
     * Returns the currency used by this bank.
     * Defaults to INR for Indian banks. International banks should override this.
     */
    open fun getCurrency(): String = "INR"
    
    /**
     * Parses an SMS message and extracts transaction information.
     * Returns null if the message cannot be parsed.
     */
    open fun parse(smsBody: String, sender: String, timestamp: Long): ParsedTransaction? {
        // Skip non-transaction messages
        if (!isTransactionMessage(smsBody)) {
            return null
        }
        
        val amount = extractAmount(smsBody)
        if (amount == null) {
            return null
        }
        
        val type = extractTransactionType(smsBody)
        if (type == null) {
            return null
        }
        
        // Extract available limit for credit card transactions
        val isCard = detectIsCard(smsBody)
        val availableLimit = if (isCard) {
            extractAvailableLimit(smsBody)
        } else {
            null
        }
        
        return ParsedTransaction(
            amount = amount,
            type = type,
            merchant = extractMerchant(smsBody, sender),
            reference = extractReference(smsBody),
            accountLast4 = extractAccountLast4(smsBody),
            balance = extractBalance(smsBody),
            creditLimit = availableLimit,  // TODO: This is actually available limit, will be fixed in SmsReaderWorker
            smsBody = smsBody,
            sender = sender,
            timestamp = timestamp,
            bankName = getBankName(),
            isFromCard = isCard,
            currency = getCurrency()
        )
    }
    
    /**
     * Checks if the message is a transaction message (not OTP, promotional, etc.)
     */
    protected open fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()
        
        // Skip OTP messages
        if (lowerMessage.contains("otp") || 
            lowerMessage.contains("one time password") ||
            lowerMessage.contains("verification code")) {
            return false
        }
        
        // Skip promotional messages
        if (lowerMessage.contains("offer") || 
            lowerMessage.contains("discount") ||
            lowerMessage.contains("cashback offer") ||
            lowerMessage.contains("win ")) {
            return false
        }
        
        // Skip payment request messages (common across banks)
        if (lowerMessage.contains("has requested") || 
            lowerMessage.contains("payment request") ||
            lowerMessage.contains("collect request") ||
            lowerMessage.contains("requesting payment") ||
            lowerMessage.contains("requests rs") ||
            lowerMessage.contains("ignore if already paid")) {
            return false
        }
        
        // Skip merchant payment acknowledgments
        if (lowerMessage.contains("have received payment")) {
            return false
        }
        
        // Skip payment reminder/due messages
        if (lowerMessage.contains("is due") ||
            lowerMessage.contains("min amount due") ||
            lowerMessage.contains("minimum amount due") ||
            lowerMessage.contains("in arrears") ||
            lowerMessage.contains("is overdue") ||
            lowerMessage.contains("ignore if paid") ||
            (lowerMessage.contains("pls pay") && lowerMessage.contains("min of"))) {
            return false
        }
        
        // Must contain transaction keywords
        val transactionKeywords = listOf(
            "debited", "credited", "withdrawn", "deposited",
            "spent", "received", "transferred", "paid"
        )
        
        return transactionKeywords.any { lowerMessage.contains(it) }
    }
    
    /**
     * Extracts the transaction currency from the message.
     * Can be overridden by specific bank parsers for custom logic.
     */
    protected open fun extractCurrency(message: String): String? {
        // Default implementation - try to find currency pattern
        val currencyPattern = Regex("""([A-Z]{3})\s*[0-9,]+(?:\.\d{2})?""", RegexOption.IGNORE_CASE)
        currencyPattern.find(message)?.let { match ->
            return match.groupValues[1].uppercase()
        }
        return null
    }

    /**
     * Extracts the transaction amount from the message.
     */
    protected open fun extractAmount(message: String): BigDecimal? {
        for (pattern in CompiledPatterns.Amount.ALL_PATTERNS) {
            pattern.find(message)?.let { match ->
                val amountStr = match.groupValues[1].replace(",", "")
                return try {
                    BigDecimal(amountStr)
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }
        
        return null
    }
    
    /**
     * Extracts the transaction type (INCOME/EXPENSE/INVESTMENT).
     */
    protected open fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()
        
        // Check for investment transactions first (highest priority)
        if (isInvestmentTransaction(lowerMessage)) {
            return TransactionType.INVESTMENT
        }
        
        return when {
            lowerMessage.contains("debited") -> TransactionType.EXPENSE
            lowerMessage.contains("withdrawn") -> TransactionType.EXPENSE
            lowerMessage.contains("spent") -> TransactionType.EXPENSE
            lowerMessage.contains("charged") -> TransactionType.EXPENSE
            lowerMessage.contains("paid") -> TransactionType.EXPENSE
            lowerMessage.contains("purchase") -> TransactionType.EXPENSE
            lowerMessage.contains("deducted") -> TransactionType.EXPENSE
            
            lowerMessage.contains("credited") -> TransactionType.INCOME
            lowerMessage.contains("deposited") -> TransactionType.INCOME
            lowerMessage.contains("received") -> TransactionType.INCOME
            lowerMessage.contains("refund") -> TransactionType.INCOME
            lowerMessage.contains("cashback") && !lowerMessage.contains("earn cashback") -> TransactionType.INCOME
            
            else -> null
        }
    }
    
    /**
     * Checks if the message is for an investment transaction.
     * Can be overridden by specific bank parsers for custom logic.
     */
    protected open fun isInvestmentTransaction(lowerMessage: String): Boolean {
        val investmentKeywords = listOf(
            // Clearing corporations
            "iccl",                         // Indian Clearing Corporation Limited
            "indian clearing corporation",
            "nsccl",                        // NSE Clearing Corporation
            "nse clearing",
            "clearing corporation",
            
            // Auto-pay indicators (excluding mandate/UMRN to avoid subscription false positives)
            "nach",                         // National Automated Clearing House
            "ach",                          // Automated Clearing House
            "ecs",                          // Electronic Clearing Service
            
            // Investment platforms
            "groww",
            "zerodha",
            "upstox",
            "kite",
            "kuvera",
            "paytm money",
            "etmoney",
            "coin by zerodha",
            "smallcase",
            "angel one",
            "angel broking",
            "5paisa",
            "icici securities",
            "icici direct",
            "hdfc securities",
            "kotak securities",
            "motilal oswal",
            "sharekhan",
            "edelweiss",
            "axis direct",
            "sbi securities",
            
            // Investment types
            "mutual fund",
            "sip",                          // Systematic Investment Plan
            "elss",                         // Tax saving funds
            "ipo",                          // Initial Public Offering
            "folio",                        // Mutual fund folio
            "demat",
            "stockbroker",
            
            // Stock exchanges
            "nse",                          // National Stock Exchange
            "bse",                          // Bombay Stock Exchange
            "cdsl",                         // Central Depository Services
            "nsdl"                          // National Securities Depository
        )
        
        return investmentKeywords.any { lowerMessage.contains(it) }
    }
    
    /**
     * Extracts merchant/payee information with enhanced fallback strategies.
     * Uses multi-level extraction to reduce "Unknown Merchant" cases.
     */
    protected open fun extractMerchant(message: String, sender: String): String? {
        // Level 1: Check for salary first (highest priority)
        extractSalaryMerchant(message)?.let { return it }

        // Level 2: Primary patterns (existing regex patterns)
        for (pattern in CompiledPatterns.Merchant.ALL_PATTERNS) {
            pattern.find(message)?.let { match ->
                val merchant = cleanMerchantName(match.groupValues[1].trim())
                if (isValidMerchantName(merchant)) {
                    return merchant
                }
            }
        }
        
        // Level 3: Secondary patterns (more aggressive extraction)
        extractMerchantSecondary(message)?.let { return it }
        
        // Level 4: SMS structure analysis (line-based extraction)
        extractMerchantFromStructure(message)?.let { return it }
        
        // Level 5: Parentheses extraction
        extractMerchantFromParentheses(message)?.let { return it }
        
        // Level 6: UPPERCASE line extraction (common in bank SMS)
        extractMerchantFromUppercaseLines(message)?.let { return it }
        
        // Level 7: Location code-based extraction
        extractMerchantFromLocationCodes(message)?.let { return it }
        
        // Level 8: Final fallback - extract meaningful text
        extractMerchantFallback(message)?.let { return it }
        
        return null
    }
    
    /**
     * Secondary extraction patterns - more aggressive matching.
     */
    protected open fun extractMerchantSecondary(message: String): String? {
        val secondaryPatterns = listOf(
            // Pattern: "sent to NAME" or "sent NAME"
            Regex("""sent\s+(?:to\s+)?([A-Z][A-Z0-9\s]+?)(?:\s+\(|\s+on|\s+at|\s+via|$)""", RegexOption.IGNORE_CASE),
            // Pattern: "paid to NAME" or "paid NAME"
            Regex("""paid\s+(?:to\s+)?([A-Z][A-Z0-9\s]+?)(?:\s+\(|\s+on|\s+at|\s+via|$)""", RegexOption.IGNORE_CASE),
            // Pattern: "transfer to NAME"
            Regex("""transfer(?:red)?\s+to\s+([A-Z][A-Z0-9\s]+?)(?:\s+\(|\s+on|\s+at|$)""", RegexOption.IGNORE_CASE),
            // Pattern: "purchase at NAME"
            Regex("""purchase\s+at\s+([A-Z][A-Z0-9\s]+?)(?:\s+on|\s+at|$)""", RegexOption.IGNORE_CASE),
            // Pattern: "with NAME" (for UPI transfers)
            Regex("""with\s+([A-Z][A-Z0-9\s]+?)(?:\s+\(|\s+on|\s+at|$)""", RegexOption.IGNORE_CASE),
            // Pattern: "via NAME" (for UPI)
            Regex("""via\s+([A-Z][A-Z0-9\s]+?)(?:\s+\(|\s+on|\s+at|$)""", RegexOption.IGNORE_CASE),
            // Pattern: "by NAME" (common in some banks)
            Regex("""by\s+([A-Z][A-Z0-9\s]+?)(?:\s+\(|\s+on|\s+at|\s+\.|$)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in secondaryPatterns) {
            pattern.find(message)?.let { match ->
                val merchant = cleanMerchantName(match.groupValues[1].trim())
                if (isValidMerchantName(merchant)) {
                    return merchant
                }
            }
        }
        
        return null
    }
    
    /**
     * Extracts merchant from SMS structure (line-based analysis).
     * Many bank SMS have merchant names on specific lines.
     */
    protected open fun extractMerchantFromStructure(message: String): String? {
        val lines = message.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        
        if (lines.isEmpty()) return null
        
        // Strategy 1: Find lines with location codes (often contain merchant names)
        val locationCodes = setOf("DUBAI", "SHARJAH", "BANGKOK", "ABU DHABI", "AJMAN", 
            "FUJAIRAH", "AL AIN", "AE", "US", "TH", "SG", "GB", "IN")
        
        for (line in lines) {
            val upperLine = line.uppercase()
            if (locationCodes.any { upperLine.contains(it) } && line.length > 15) {
                // Extract merchant name (usually before location code)
                val parts = line.split(Regex("""\s+(?:DUBAI|SHARJAH|BANGKOK|ABU DHABI|AJMAN|FUJAIRAH|AL AIN|AE|US|TH|SG|GB|IN)\s+""", RegexOption.IGNORE_CASE))
                if (parts.isNotEmpty()) {
                    val merchant = cleanMerchantName(parts[0].trim())
                    if (isValidMerchantName(merchant)) {
                        return merchant
                    }
                }
            }
        }
        
        // Strategy 2: Find longest line with mostly uppercase letters (common for merchant names)
        val uppercaseLines = lines.filter { line ->
            val letterCount = line.count { it.isLetter() }
            val upperCount = line.count { it.isUpperCase() }
            letterCount > 0 && (upperCount.toDouble() / letterCount) > 0.7 && letterCount >= 5
        }
        
        if (uppercaseLines.isNotEmpty()) {
            // Prefer lines that are not dates, amounts, or account numbers
            val candidate = uppercaseLines.maxByOrNull { it.length }
            candidate?.let {
                // Exclude if it looks like a date, amount, or account
                if (!it.matches(Regex("""\d{2}[/-]\d{2}[/-]\d{2,4}""")) &&
                    !it.matches(Regex("""(?:Rs\.?|INR|AED|USD|THB)\s*[\d,]+""", RegexOption.IGNORE_CASE)) &&
                    !it.matches(Regex("""Account\s+[X\d]+""", RegexOption.IGNORE_CASE))) {
                    val merchant = cleanMerchantName(it.trim())
                    if (isValidMerchantName(merchant)) {
                        return merchant
                    }
                }
            }
        }
        
        // Strategy 3: Find line after amount/date patterns (merchant often appears after)
        for (i in lines.indices) {
            val line = lines[i]
            // Check if current line has amount or date
            if (line.contains(Regex("""(?:Rs\.?|INR|AED|USD|THB)\s*[\d,]+""", RegexOption.IGNORE_CASE)) ||
                line.contains(Regex("""\d{2}[/-]\d{2}[/-]\d{2,4}"""))) {
                // Next line might be merchant
                if (i + 1 < lines.size) {
                    val nextLine = lines[i + 1]
                    val merchant = cleanMerchantName(nextLine.trim())
                    if (isValidMerchantName(merchant) && merchant.length >= 3) {
                        return merchant
                    }
                }
            }
        }
        
        return null
    }
    
    /**
     * Extracts merchant name from parentheses.
     * Many SMS have merchant names in parentheses.
     */
    protected open fun extractMerchantFromParentheses(message: String): String? {
        val parenthesesPattern = Regex("""\(([^)]+)\)""")
        val matches = parenthesesPattern.findAll(message).toList()
        
        for (match in matches) {
            val content = match.groupValues[1].trim()
            // Skip if it's a phone number, date, or reference
            if (!content.matches(Regex("""\d{10,}""")) &&
                !content.matches(Regex("""\d{2}[/-]\d{2}[/-]\d{2,4}""")) &&
                !content.matches(Regex("""Ref\s*:?\s*\d+""", RegexOption.IGNORE_CASE)) &&
                content.length >= 3 &&
                content.any { it.isLetter() }) {
                val merchant = cleanMerchantName(content)
                if (isValidMerchantName(merchant)) {
                    return merchant
                }
            }
        }
        
        return null
    }
    
    /**
     * Extracts merchant from lines that are mostly UPPERCASE.
     * Bank SMS often have merchant names in all caps.
     */
    protected open fun extractMerchantFromUppercaseLines(message: String): String? {
        val lines = message.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        
        for (line in lines) {
            // Check if line is mostly uppercase and has meaningful length
            val letterCount = line.count { it.isLetter() }
            val upperCount = line.count { it.isUpperCase() }
            
            if (letterCount >= 5 && 
                letterCount <= 50 && 
                (upperCount.toDouble() / letterCount) > 0.8) {
                // Exclude common non-merchant patterns
                if (!line.matches(Regex("""^(?:DEBIT|CREDIT|ACCOUNT|BALANCE|AVAILABLE|LIMIT).*""", RegexOption.IGNORE_CASE)) &&
                    !line.contains(Regex("""\d{2}[/-]\d{2}[/-]\d{2,4}""")) &&
                    !line.contains(Regex("""(?:Rs\.?|INR|AED|USD|THB)\s*[\d,]+""", RegexOption.IGNORE_CASE))) {
                    val merchant = cleanMerchantName(line.trim())
                    if (isValidMerchantName(merchant)) {
                        return merchant
                    }
                }
            }
        }
        
        return null
    }
    
    /**
     * Extracts merchant from lines containing location codes.
     * Many international bank SMS have merchant names with location codes.
     */
    protected open fun extractMerchantFromLocationCodes(message: String): String? {
        val locationPattern = Regex("""([A-Z][A-Z0-9\s]{5,}?)\s+(?:DUBAI|SHARJAH|BANGKOK|ABU DHABI|AJMAN|FUJAIRAH|AL AIN)\s+[A-Z]{2}""", RegexOption.IGNORE_CASE)
        locationPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }
        
        return null
    }
    
    /**
     * Final fallback extraction - tries to extract any meaningful merchant name.
     */
    protected open fun extractMerchantFallback(message: String): String? {
        val lines = message.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        
        // Find the longest line that looks like a merchant name
        val candidates = lines.filter { line ->
            val letterCount = line.count { it.isLetter() }
            letterCount >= 3 && 
            letterCount <= 50 &&
            !line.matches(Regex("""^\d+$""")) && // Not just numbers
            !line.contains("@") && // Not email/UPI
            !line.matches(Regex("""\d{2}[/-]\d{2}[/-]\d{2,4}""")) && // Not date
            !line.contains(Regex("""(?:Rs\.?|INR|AED|USD|THB)\s*[\d,]+""", RegexOption.IGNORE_CASE)) && // Not amount
            !line.matches(Regex("""Account\s+[X\d]+""", RegexOption.IGNORE_CASE)) && // Not account
            !line.matches(Regex("""Ref\s*:?\s*\d+""", RegexOption.IGNORE_CASE)) && // Not reference
            !line.contains(Regex("""(?:debited|credited|withdrawn|deposited)""", RegexOption.IGNORE_CASE)) // Not transaction verb
        }
        
        if (candidates.isNotEmpty()) {
            // Prefer lines with more letters and less numbers
            val bestCandidate = candidates.maxByOrNull { line ->
                val letterCount = line.count { it.isLetter() }
                val digitCount = line.count { it.isDigit() }
                letterCount - (digitCount * 2) // Penalize digits
            }
            
            bestCandidate?.let {
                val merchant = cleanMerchantName(it.trim())
                if (isValidMerchantName(merchant)) {
                    return merchant
                }
            }
        }
        
        return null
    }

    /**
     * Checks for salary transactions based on common patterns.
     * Returns appropriate salary merchant name based on SMS format.
     */
    protected open fun extractSalaryMerchant(message: String): String? {
        val lowerMessage = message.lowercase()
        
        // Must be an income transaction (credited/received/deposited)
        val isIncome = lowerMessage.contains("credited") || 
                       lowerMessage.contains("received") || 
                       lowerMessage.contains("deposited")
        
        if (!isIncome) {
            return null
        }
        
        // Pattern 1: NEFT + PRIVATE LIMITED (strong salary indicator, even without "salary" keyword)
        // This pattern is very common for salary credits
        if (message.contains("NEFT", ignoreCase = true) && 
            (message.contains("PRIVATE LIMITED", ignoreCase = true) || 
             message.contains("PVT LTD", ignoreCase = true))) {
            // Extract company name from NEFT string: NEFT/UTR/COMPANY NAME or NEFT Cr-...-COMPANY NAME
            // Matches: NEFT/ICIN430458878409/CHARA TECHNOLOGIES PRIVATE LIMITED
            // Matches: NEFT Cr-ICIC0SF0002-SCIENAPTIC SYSTEMS PRIVATE LIMITED-...
            val neftPattern1 = Regex("""NEFT/[A-Z0-9]+/([A-Z0-9\s]+(?:PRIVATE LIMITED|PVT LTD))""", RegexOption.IGNORE_CASE)
            neftPattern1.find(message)?.let { match ->
                return "Salary - ${match.groupValues[1].trim()}"
            }
            
            // Pattern for "NEFT Cr-...-COMPANY PRIVATE LIMITED-..."
            val neftPattern2 = Regex("""NEFT\s+Cr-[^-]+-([A-Z0-9\s]+(?:PRIVATE LIMITED|PVT LTD))""", RegexOption.IGNORE_CASE)
            neftPattern2.find(message)?.let { match ->
                return "Salary - ${match.groupValues[1].trim()}"
            }
            
            // Generic NEFT with PRIVATE LIMITED
            return "Salary - NEFT Transfer"
        }
        
        // Check for explicit salary keywords (for other salary patterns)
        val hasSalary = lowerMessage.contains("salary", ignoreCase = true)
        if (!hasSalary) {
            return null
        }
        
        // Pattern 1: "For: Salary Payment/Monthly Salary,COMPANY" 
        // Priority: Prefer "Monthly Salary" over extracting company
        // Example: "For: Salary Payment/Monthly Salary,UJJ SH" → "Monthly Salary"
        val forMonthlySalaryPattern = Regex("""For:\s*Salary\s+Payment/(Monthly\s+Salary)[,\s]+([A-Z][A-Z0-9\s]+)""", RegexOption.IGNORE_CASE)
        forMonthlySalaryPattern.find(message)?.let { match ->
            val monthlySalary = match.groupValues[1].trim()
            return monthlySalary
        }
        
        // Pattern 2: "For: Monthly Salary,COMPANY" - extract company if meaningful
        val forMonthlyPattern = Regex("""For:\s*(?:Monthly\s+)?Salary[,\s]+([A-Z][A-Z0-9\s]+)""", RegexOption.IGNORE_CASE)
        forMonthlyPattern.find(message)?.let { match ->
            val company = match.groupValues[1].trim()
            // Exclude common non-company words
            if (isValidMerchantName(company) && 
                company.length >= 3 &&
                !company.equals("Payment", ignoreCase = true) &&
                !company.equals("Credit", ignoreCase = true)) {
                return "Salary - ${cleanMerchantName(company)}"
            }
        }
        
        // Pattern 3: NEFT + PRIVATE LIMITED - extract company name
        if (message.contains("NEFT", ignoreCase = true) && 
            message.contains("PRIVATE LIMITED", ignoreCase = true)) {
            val neftPattern = Regex("""NEFT/[A-Z0-9]+/([A-Z0-9\s]+(?:PRIVATE LIMITED|PVT LTD))""", RegexOption.IGNORE_CASE)
            neftPattern.find(message)?.let { match ->
                return "Salary - ${match.groupValues[1].trim()}"
            }
            return "Salary - NEFT Transfer"
        }
        
        // Pattern 4: HDFC specific "for XXXXX-ABC-XYZ MONTH SALARY-COMPANY NAME"
        val hdfcSalaryPattern = Regex("""for\s+[^-]+-[^-]+-[^-]+\s+[A-Z]+\s+SALARY-([^\.\n]+)""", RegexOption.IGNORE_CASE)
        hdfcSalaryPattern.find(message)?.let { match ->
            val company = match.groupValues[1].trim()
            if (isValidMerchantName(company)) {
                return "Salary - ${cleanMerchantName(company)}"
            }
        }
        
        // Pattern 5: "SALARY CREDIT\n-COMPANY" - extract company if present
        val salaryCreditPattern = Regex("""SALARY\s+CREDIT\s*[-\n]+\s*([A-Z][A-Z0-9\s]+)""", RegexOption.IGNORE_CASE)
        salaryCreditPattern.find(message)?.let { match ->
            val company = match.groupValues[1].trim()
            if (isValidMerchantName(company) && company.length >= 3) {
                return "Salary - ${cleanMerchantName(company)}"
            }
        }
        
        // Pattern 6: "from COMPANY via Salary" - extract company
        val fromCompanyViaSalaryPattern = Regex("""from\s+([A-Z][A-Z0-9\s]+?)\s+via\s+Salary""", RegexOption.IGNORE_CASE)
        fromCompanyViaSalaryPattern.find(message)?.let { match ->
            val company = match.groupValues[1].trim()
            if (isValidMerchantName(company) && 
                !company.equals("Salary", ignoreCase = true) &&
                !company.equals("Salary Payment", ignoreCase = true)) {
                return "Salary - ${cleanMerchantName(company)}"
            }
        }
        
        // Pattern 7: Extract company from lines with "SALARY" and company patterns
        val lines = message.split("\n")
        for (line in lines) {
            if (line.contains("SALARY", ignoreCase = true)) {
                // Look for company name patterns (PRIVATE LIMITED, LTD, etc.)
                val companyPattern = Regex("""([A-Z][A-Z0-9\s]{5,}(?:PRIVATE LIMITED|PVT LTD|LTD|INC|CORP))""", RegexOption.IGNORE_CASE)
                companyPattern.find(line)?.let { match ->
                    val company = match.groupValues[1].trim()
                    if (isValidMerchantName(company)) {
                        return "Salary - ${cleanMerchantName(company)}"
                    }
                }
            }
        }
        
        // Pattern 8: "SALARY-COMPANY" (simple pattern, exclude common words)
        val simpleSalaryPattern = Regex("""SALARY[- ]([^\.\n]+?)(?:\s+Info|\s+on|\s+at|$)""", RegexOption.IGNORE_CASE)
        simpleSalaryPattern.find(message)?.let { match ->
            val company = match.groupValues[1].trim()
            if (isValidMerchantName(company) && 
                !company.equals("Payment", ignoreCase = true) &&
                !company.equals("Credit", ignoreCase = true) &&
                company.length >= 3) {
                return "Salary - ${cleanMerchantName(company)}"
            }
        }
        
        // Pattern 9: "from Salary Payment" or generic salary - return "Salary"
        // This handles cases where "Salary Payment" is the description, not a company
        if (lowerMessage.contains("from salary payment", ignoreCase = true) ||
            lowerMessage.contains("salary payment", ignoreCase = true)) {
            return "Salary"
        }
        
        // Default: return "Salary" if salary keyword found
        return "Salary"
    }
    
    /**
     * Extracts transaction reference number.
     */
    protected open fun extractReference(message: String): String? {
        for (pattern in CompiledPatterns.Reference.ALL_PATTERNS) {
            pattern.find(message)?.let { match ->
                return match.groupValues[1].trim()
            }
        }
        
        return null
    }
    
    /**
     * Extracts last 4 digits of account number.
     */
    protected open fun extractAccountLast4(message: String): String? {
        for (pattern in CompiledPatterns.Account.ALL_PATTERNS) {
            pattern.find(message)?.let { match ->
                return match.groupValues[1]
            }
        }
        
        return null
    }
    
    /**
     * Extracts balance after transaction.
     */
    protected open fun extractBalance(message: String): BigDecimal? {
        for (pattern in CompiledPatterns.Balance.ALL_PATTERNS) {
            pattern.find(message)?.let { match ->
                val balanceStr = match.groupValues[1].replace(",", "")
                return try {
                    BigDecimal(balanceStr)
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }
        
        return null
    }
    
    /**
     * Extracts credit card available limit from the message.
     * This is the remaining credit available to spend, NOT the total credit limit.
     */
    protected open fun extractAvailableLimit(message: String): BigDecimal? {
        
        // Common patterns for credit limit across banks
        val creditLimitPatterns = listOf(
            // "Available limit Rs.111,111.89" - Federal Bank format (no space after Rs.)
            Regex("""Available\s+limit\s+Rs\.([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
            // "Available limit Rs. 111,111.89" or "Available limit: Rs 111,111.89"
            Regex("""Available\s+limit:?\s*Rs\.?\s*([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
            // "Avl Lmt Rs.111,111.89" or "Avl Lmt: Rs 111,111.89" (ICICI and others)
            Regex("""Avl\s+Lmt:?\s*Rs\.?\s*([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
            // "Avail Limit Rs.111,111.89"
            Regex("""Avail\s+Limit:?\s*Rs\.?\s*([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
            // "Available Credit Limit: Rs.111,111.89"
            Regex("""Available\s+Credit\s+Limit:?\s*Rs\.?\s*([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
            // "Limit: Rs.111,111.89" (generic, but only for credit card messages)
            Regex("""(?:^|\s)Limit:?\s*Rs\.?\s*([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        )
        
        for ((index, pattern) in creditLimitPatterns.withIndex()) {
            pattern.find(message)?.let { match ->
                val limitStr = match.groupValues[1].replace(",", "")
                return try {
                    val limit = BigDecimal(limitStr)
                    limit
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }
        
        return null
    }
    
    /**
     * Detects if the transaction is from a card (credit/debit) based on message patterns.
     * First excludes account-related patterns, then checks for actual card patterns.
     */
    protected open fun detectIsCard(message: String): Boolean {
        val lowerMessage = message.lowercase()
        
        // FIRST: Explicitly exclude account-related patterns - these are NOT cards
        val accountPatterns = listOf(
            "a/c",           // Account abbreviation (e.g., "from HDFC Bank A/c 120092")
            "account",       // Full word account (e.g., "from HDFC Bank Account XX0093")
            "ac ",           // Account abbreviation with space
            "acc ",          // Account abbreviation
            "saving account",
            "current account",
            "savings a/c",
            "current a/c"
        )
        
        // If message contains account patterns, it's NOT a card transaction
        for (pattern in accountPatterns) {
            if (lowerMessage.contains(pattern)) {
                return false
            }
        }
        
        // SECOND: Check for actual card-specific patterns
        val cardPatterns = listOf(
            "card ending",
            "card xx",
            "debit card",
            "credit card",
            "card no.",
            "card number",
            "card *",
            "card x"
        )
        
        // Check for card patterns
        for (pattern in cardPatterns) {
            if (lowerMessage.contains(pattern)) {
                return true
            }
        }
        
        // Check for masked card number patterns (e.g., "XXXX1234", "*1234", "ending 1234")
        // BUT only if we haven't already excluded it as an account transaction
        val maskedCardRegex = Regex("""(?:xx|XX|\*{2,})?\d{4}""")
        if (lowerMessage.contains("ending") && maskedCardRegex.containsMatchIn(message)) {
            return true
        }
        
        return false
    }
    
    /**
     * Cleans merchant name by removing common suffixes and noise.
     * Enhanced to handle phone numbers and trailing numbers.
     */
    protected open fun cleanMerchantName(merchant: String): String {
        var cleaned = merchant
            .replace(CompiledPatterns.Cleaning.TRAILING_PARENTHESES, "")
            .replace(CompiledPatterns.Cleaning.REF_NUMBER_SUFFIX, "")
            .replace(CompiledPatterns.Cleaning.DATE_SUFFIX, "")
            .replace(CompiledPatterns.Cleaning.UPI_SUFFIX, "")
            .replace(CompiledPatterns.Cleaning.TIME_SUFFIX, "")
            .replace(CompiledPatterns.Cleaning.TRAILING_DASH, "")
            .replace(CompiledPatterns.Cleaning.PVT_LTD, "")
            .replace(CompiledPatterns.Cleaning.LTD, "")
            .trim()
        
        // Remove trailing numbers (e.g., "arun bhat 65" -> "arun bhat")
        cleaned = cleaned.replace(Regex("""\s+\d+$"""), "")
        
        // Remove common suffixes that might appear after cleaning
        val commonSuffixes = listOf("mbl", "mobile", "phone", "no", "number", "mob")
        for (suffix in commonSuffixes) {
            val suffixPattern = Regex("""\s+$suffix\s*$""", RegexOption.IGNORE_CASE)
            cleaned = cleaned.replace(suffixPattern, "")
        }
        
        return cleaned.trim()
    }
    
    /**
     * Validates if the extracted merchant name is valid.
     */
    protected open fun isValidMerchantName(name: String): Boolean {
        val commonWords = setOf("USING", "VIA", "THROUGH", "BY", "WITH", "FOR", "TO", "FROM", "AT", "THE")
        
        return name.length >= Constants.Parsing.MIN_MERCHANT_NAME_LENGTH && 
               name.any { it.isLetter() } && 
               name.uppercase() !in commonWords &&
               !name.all { it.isDigit() } &&
               !name.contains("@") // Not a UPI ID
    }
}
