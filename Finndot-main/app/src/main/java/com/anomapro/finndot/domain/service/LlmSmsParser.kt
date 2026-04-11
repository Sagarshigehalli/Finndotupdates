package com.anomapro.finndot.domain.service

import android.util.Log
import com.finndot.parser.core.ParsedTransaction
import com.finndot.parser.core.TransactionType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmSmsParser @Inject constructor(
    private val llmService: LlmService
) {
    companion object {
        private const val TAG = "LlmSmsParser"
        private val jsonParser = Json { 
            ignoreUnknownKeys = true 
            isLenient = true 
        }
    }
    
    private val mutex = kotlinx.coroutines.sync.Mutex()

    @Serializable
    private data class LlmResponse(
        val amount: String? = null,
        val merchant: String? = null,
        val type: String? = null,
        val account: String? = null,
        val balance: String? = null,
        val category: String? = null
    )
    
    fun isReady(): Boolean = llmService.isInitialized()

    suspend fun parse(sms: String, sender: String, timestamp: Long): ParsedTransaction? {
        if (!isReady()) {
            return null
        }

        return mutex.withLock {
            try {
                val prompt = buildPrompt(sms, sender)
                val responseResult = llmService.generateResponse(prompt)

                if (responseResult.isSuccess) {
                    val jsonResponse = responseResult.getOrNull() ?: return null
                    parseJsonResponse(jsonResponse, sms, sender, timestamp)
                } else {
                    Log.e(TAG, "LLM generation failed: ${responseResult.exceptionOrNull()?.message}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during LLM parsing", e)
                null
            }
        }
    }

    private fun buildPrompt(sms: String, sender: String): String {
        return """
            You are a financial transaction parser. Extract details from this SMS sent by "$sender":
            "$sms"
            
            Return ONLY a JSON object with these fields:
            - amount: number (no currency symbols)
            - merchant: string (payee name or source)
            - type: one of [INCOME, EXPENSE, CREDIT, TRANSFER, INVESTMENT]
            - account: string (last 4 digits only, or null)
            - balance: number (or null)
            - category: string (e.g. Salary, Food, Travel, Shopping, etc.)
            
            If not a transaction, return empty JSON {}.
            Do not include markdown formatting or explanations.
        """.trimIndent()
    }

    private fun parseJsonResponse(
        jsonString: String,
        smsBody: String,
        sender: String,
        timestamp: Long
    ): ParsedTransaction? {
        try {
            // Clean up potential markdown code blocks
            val cleanJson = jsonString.replace("```json", "").replace("```", "").trim()
            if (cleanJson == "{}" || cleanJson.isEmpty()) return null

            val response = jsonParser.decodeFromString<LlmResponse>(cleanJson)
            
            if (response.amount == null || response.type == null) return null

            val amountStr = response.amount.replace(",", "")
            val amount = BigDecimal(amountStr)
            
            val typeStr = response.type.uppercase()
            val type = try {
                TransactionType.valueOf(typeStr)
            } catch (e: IllegalArgumentException) {
                // Fallback mapping
                when {
                    typeStr.contains("DEBIT") -> TransactionType.EXPENSE
                    typeStr.contains("CREDIT") -> TransactionType.INCOME
                    else -> TransactionType.EXPENSE
                }
            }

            val merchant = response.merchant
            val account = response.account?.takeIf { it.isNotEmpty() && it != "null" }
            val balanceStr = response.balance?.takeIf { it.isNotEmpty() && it != "null" }?.replace(",", "")
            val balance = balanceStr?.let { BigDecimal(it) }
            val category = response.category?.takeIf { it.isNotEmpty() && it != "null" }

            return ParsedTransaction(
                amount = amount,
                type = type,
                merchant = merchant,
                reference = null,
                accountLast4 = account,
                balance = balance,
                smsBody = smsBody,
                sender = sender,
                timestamp = timestamp,
                bankName = sender, // Use sender as bank name for now
                isFromCard = type == TransactionType.CREDIT,
                category = category
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON response: $jsonString", e)
            return null
        }
    }
}
