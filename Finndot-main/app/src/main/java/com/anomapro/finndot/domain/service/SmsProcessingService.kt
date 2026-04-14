package com.anomapro.finndot.domain.service

import android.util.Log
import com.anomapro.finndot.data.database.entity.UnrecognizedSmsEntity
import com.anomapro.finndot.data.mapper.toEntity
import com.anomapro.finndot.data.repository.AccountBalanceRepository
import com.anomapro.finndot.data.repository.CardRepository
import com.anomapro.finndot.data.repository.MerchantMappingRepository
import com.anomapro.finndot.data.repository.MerchantNameMappingRepository
import com.anomapro.finndot.data.repository.SubscriptionRepository
import com.anomapro.finndot.data.repository.TransactionRepository
import com.anomapro.finndot.data.repository.UnrecognizedSmsRepository
import com.anomapro.finndot.domain.repository.RuleRepository
import com.finndot.parser.core.ParsedTransaction
import com.finndot.parser.core.bank.BankParserFactory
import com.finndot.parser.core.bank.FederalBankParser
import com.finndot.parser.core.bank.SBIBankParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for processing incoming SMS messages in real-time.
 * Handles parsing, transaction storage, balance updates, and subscription notifications.
 */
@Singleton
class SmsProcessingService @Inject constructor(
    private val llmSmsParser: LlmSmsParser,
    private val transactionRepository: TransactionRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val cardRepository: CardRepository,
    private val merchantMappingRepository: MerchantMappingRepository,
    private val unrecognizedSmsRepository: UnrecognizedSmsRepository,
    private val ruleRepository: RuleRepository,
    private val ruleEngine: RuleEngine,
    private val internalTransferPairingService: InternalTransferPairingService,
    private val transferLikeSmsClassifier: TransferLikeSmsClassifier,
    private val merchantNameMappingRepository: MerchantNameMappingRepository,
    private val counterpartyMemoryApplier: CounterpartyMemoryApplier,
) {
    companion object {
        private const val TAG = "SmsProcessingService"
    }

    /**
     * Process a single SMS message in real-time.
     * Attempts LLM parsing first, falls back to bank parser, then handles unrecognized SMS.
     */
    suspend fun processSms(sender: String, body: String, timestamp: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Processing SMS from $sender")

            // Skip promotional and government messages
            val senderUpper = sender.uppercase()
            if (senderUpper.endsWith("-P") || senderUpper.endsWith("-G")) {
                Log.d(TAG, "Skipping promotional/government message from $sender")
                return@withContext false
            }

            // Try LLM parsing first
            val llmParsedTransaction = llmSmsParser.parse(body, sender, timestamp)
            if (llmParsedTransaction != null) {
                Log.d(TAG, "LLM successfully parsed SMS from $sender")
                return@withContext saveParsedTransaction(llmParsedTransaction, sender, body, timestamp)
            }

            // LLM failed, try bank parser
            val parser = BankParserFactory.getParser(sender)
            if (parser != null) {
                Log.d(TAG, "Processing SMS with bank parser from ${parser.getBankName()}")
                
                // Check for subscription notifications
                val smsDateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(timestamp),
                    ZoneId.systemDefault()
                )
                val thirtyDaysAgo = LocalDateTime.now().minusDays(30)
                val isRecentMessage = smsDateTime.isAfter(thirtyDaysAgo)

                // Process subscription notifications
                val subscriptionProcessed = processSubscriptionNotifications(
                    parser, sender, body, timestamp, smsDateTime, isRecentMessage
                )
                
                if (subscriptionProcessed) {
                    Log.d(TAG, "Processed as subscription notification")
                    return@withContext true
                }

                // Parse as regular transaction
                val parsedTransaction = parser.parse(body, sender, timestamp)
                if (parsedTransaction != null) {
                    Log.d(TAG, "Bank parser successfully parsed SMS from $sender")
                    return@withContext saveParsedTransaction(parsedTransaction, sender, body, timestamp)
                } else {
                    Log.d(TAG, "Bank parser failed to parse SMS from $sender")
                }
            }

            // Both LLM and bank parser failed, handle as unrecognized
            processUnrecognizedSms(sender, body, timestamp)
            return@withContext false

        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS from $sender: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Save a parsed transaction to the database with balance updates and rule applications.
     */
    private suspend fun saveParsedTransaction(
        parsedTransaction: ParsedTransaction,
        sender: String,
        body: String,
        timestamp: Long
    ): Boolean {
        return try {
            // Convert to entity and save
            val entity = parsedTransaction.toEntity()

            // Check if this transaction was previously deleted by the user
            val existingTransaction = transactionRepository.getTransactionByHash(entity.transactionHash)
            if (existingTransaction != null) {
                if (existingTransaction.isDeleted) {
                    Log.d(TAG, "Transaction was previously deleted by user, skipping: ${entity.transactionHash}")
                    return false
                }
                Log.d(TAG, "Transaction already exists (duplicate), skipping: ${entity.transactionHash}")
                return false
            }

            val normalizedMerchantName = merchantNameMappingRepository.getNormalizedName(entity.merchantName)
            val entityWithNormalizedName = if (normalizedMerchantName != entity.merchantName) {
                entity.copy(normalizedMerchantName = normalizedMerchantName)
            } else {
                entity
            }
            val customCategory = merchantMappingRepository.getCategoryForMerchant(normalizedMerchantName)
            val entityWithMapping = if (customCategory != null) {
                entityWithNormalizedName.copy(category = customCategory)
            } else {
                entityWithNormalizedName
            }
            val finalEntity = counterpartyMemoryApplier.applyAfterMerchantMapping(entityWithMapping)

            // Save transaction
            val rowId = transactionRepository.insertTransaction(finalEntity)
            if (rowId > 0) {
                val savedEntity = finalEntity.copy(id = rowId)
                Log.d(TAG, "Saved transaction: ${savedEntity.transactionHash}")

                // Apply rules
                try {
                    val rules = ruleRepository.getActiveRules()
                    val (modifiedTransaction, ruleApplications) = ruleEngine.evaluateRules(savedEntity, body, rules)

                    if (ruleApplications.isNotEmpty()) {
                        ruleRepository.saveRuleApplications(ruleApplications)
                        Log.d(TAG, "Saved ${ruleApplications.size} rule applications for transaction: ${savedEntity.id}")
                        if (modifiedTransaction != savedEntity) {
                            transactionRepository.updateTransaction(modifiedTransaction)
                            Log.d(TAG, "Updated transaction with rule modifications")
                        }
                    }

                    // After rules: SMS transfer heuristic (rules win for fields they already set)
                    val afterRulesWithId = modifiedTransaction.copy(id = rowId)
                    var txForDownstream = transferLikeSmsClassifier.applyAfterRules(modifiedTransaction, body)
                        .copy(id = rowId)
                    if (txForDownstream != afterRulesWithId) {
                        transactionRepository.updateTransaction(txForDownstream)
                        Log.d(TAG, "Applied transfer-like SMS heuristic for transaction id=$rowId")
                    }

                    processBalanceUpdate(parsedTransaction, txForDownstream, rowId)
                    internalTransferPairingService.tryPairAfterInsert(txForDownstream)
                    return true
                } catch (e: Exception) {
                    Log.e(TAG, "Error applying rules: ${e.message}", e)
                    val txAfterHeuristic = transferLikeSmsClassifier.applyAfterRules(savedEntity, body)
                        .copy(id = rowId)
                    if (txAfterHeuristic != savedEntity) {
                        transactionRepository.updateTransaction(txAfterHeuristic)
                    }
                    processBalanceUpdate(parsedTransaction, txAfterHeuristic, rowId)
                    internalTransferPairingService.tryPairAfterInsert(txAfterHeuristic)
                    return true
                }
            }

            false
        } catch (e: Exception) {
            Log.e(TAG, "Error saving transaction: ${e.message}", e)
            false
        }
    }

    /**
     * Process balance updates for the transaction.
     */
    private suspend fun processBalanceUpdate(
        parsedTransaction: ParsedTransaction,
        entity: com.anomapro.finndot.data.database.entity.TransactionEntity,
        rowId: Long
    ) {
        if (parsedTransaction.accountLast4 != null) {
            val isFromCard = parsedTransaction.isFromCard

            Log.d(TAG, """
                Processing transaction:
                - Bank: ${parsedTransaction.bankName}
                - Number: **${parsedTransaction.accountLast4}
                - Is From Card: $isFromCard
                - Balance: ${parsedTransaction.balance}
                - Credit Limit: ${parsedTransaction.creditLimit}
            """.trimIndent())

            // Determine target account number
            val accountLast4 = parsedTransaction.accountLast4
            val finalAccountLast4 = if (isFromCard && accountLast4 != null) {
                val card = cardRepository.getCard(
                    parsedTransaction.bankName,
                    accountLast4
                )
                card?.accountLast4 ?: accountLast4
            } else {
                accountLast4
            }

            // Skip if we don't have an account number
            if (finalAccountLast4 == null) {
                return
            }

            // Check if this is a credit card
            val isCreditCard = isFromCard && accountLast4 != null &&
                cardRepository.getCard(
                    parsedTransaction.bankName,
                    accountLast4
                )?.cardType == com.anomapro.finndot.data.database.entity.CardType.CREDIT

            val existingAccount = accountBalanceRepository.getLatestBalance(
                parsedTransaction.bankName,
                finalAccountLast4
            )

            val newBalance = when {
                isCreditCard -> {
                    val currentBalance = existingAccount?.balance ?: BigDecimal.ZERO
                    currentBalance + parsedTransaction.amount
                }
                existingAccount?.isCreditCard == true && parsedTransaction.balance != null -> {
                    parsedTransaction.balance
                }
                parsedTransaction.balance != null -> {
                    parsedTransaction.balance
                }
                else -> null
            }

            if (newBalance != null) {
                val balanceEntity = com.anomapro.finndot.data.database.entity.AccountBalanceEntity(
                    bankName = parsedTransaction.bankName,
                    accountLast4 = finalAccountLast4,
                    balance = newBalance,
                    timestamp = entity.dateTime,
                    transactionId = rowId,
                    isCreditCard = isCreditCard
                )

                accountBalanceRepository.insertBalance(balanceEntity)
                Log.d(TAG, "Saved balance update: ${balanceEntity.bankName} **${balanceEntity.accountLast4} = ${balanceEntity.balance}")
            }
        }
    }

    /**
     * Process subscription notifications (UPI mandates, future debits, etc.)
     */
    private suspend fun processSubscriptionNotifications(
        parser: com.finndot.parser.core.bank.BankParser,
        sender: String,
        body: String,
        timestamp: Long,
        smsDateTime: LocalDateTime,
        isRecentMessage: Boolean
    ): Boolean {
        return when (parser) {
            is SBIBankParser -> {
                if (parser.isUPIMandateNotification(body)) {
                    if (!isRecentMessage) {
                        Log.d(TAG, "Skipping old SBI UPI-Mandate from ${smsDateTime.toLocalDate()}")
                        return false
                    }
                    val upiMandate = parser.parseUPIMandateSubscription(body)
                    if (upiMandate != null) {
                        val subscriptionEntity = com.anomapro.finndot.data.database.entity.SubscriptionEntity(
                            merchantName = upiMandate.merchant,
                            amount = upiMandate.amount,
                            nextPaymentDate = null, // SBI doesn't provide next payment date
                            state = com.anomapro.finndot.data.database.entity.SubscriptionState.ACTIVE,
                            bankName = "State Bank of India",
                            umn = upiMandate.umn,
                            smsBody = body,
                            createdAt = smsDateTime
                        )
                        subscriptionRepository.insertSubscription(subscriptionEntity)
                        Log.d(TAG, "Saved SBI UPI-Mandate subscription: ${upiMandate.merchant}")
                        return true
                    }
                }
                false
            }
            is FederalBankParser -> {
                // Try future debit notification first
                val futureDebit = parser.parseFutureDebit(body)
                if (futureDebit != null) {
                    val nextDeductionDate = futureDebit.nextDeductionDate
                    val isFuturePayment = try {
                        nextDeductionDate != null && 
                        LocalDateTime.parse(nextDeductionDate, java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                            .isAfter(LocalDateTime.now())
                    } catch (e: Exception) {
                        false
                    }

                    if (!isFuturePayment) {
                        Log.d(TAG, "Skipping old Federal Bank Future Debit from ${smsDateTime.toLocalDate()}")
                        return false
                    }

                    if (!isRecentMessage && isFuturePayment) {
                        Log.d(TAG, "Processing old Federal Bank Future Debit with future payment date")
                    }

                    // Create subscription entity from EMandateInfo
                    val subscriptionEntity = com.anomapro.finndot.data.database.entity.SubscriptionEntity(
                        merchantName = futureDebit.merchant,
                        amount = futureDebit.amount,
                        nextPaymentDate = try {
                            nextDeductionDate?.let {
                                LocalDate.parse(it, java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                            }
                        } catch (e: Exception) {
                            null
                        },
                        state = com.anomapro.finndot.data.database.entity.SubscriptionState.ACTIVE,
                        bankName = "Federal Bank",
                        umn = futureDebit.umn,
                        smsBody = body,
                        createdAt = smsDateTime
                    )
                    
                    subscriptionRepository.insertSubscription(subscriptionEntity)
                    Log.d(TAG, "Saved Federal Bank Future Debit subscription: ${futureDebit.merchant}")
                    return true
                }
                
                // Try e-mandate creation notification
                val eMandate = parser.parseEMandateSubscription(body)
                if (eMandate != null) {
                    val subscriptionEntity = com.anomapro.finndot.data.database.entity.SubscriptionEntity(
                        merchantName = eMandate.merchant,
                        amount = eMandate.amount,
                        nextPaymentDate = try {
                            eMandate.nextDeductionDate?.let {
                                LocalDate.parse(it, java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                            }
                        } catch (e: Exception) {
                            null
                        },
                        state = com.anomapro.finndot.data.database.entity.SubscriptionState.ACTIVE,
                        bankName = "Federal Bank",
                        umn = eMandate.umn,
                        smsBody = body,
                        createdAt = smsDateTime
                    )
                    
                    subscriptionRepository.insertSubscription(subscriptionEntity)
                    Log.d(TAG, "Saved Federal Bank E-Mandate subscription: ${eMandate.merchant}")
                    return true
                }
                
                false
            }
            else -> false
        }
    }

    /**
     * Store unrecognized SMS for later analysis.
     */
    private suspend fun processUnrecognizedSms(sender: String, body: String, timestamp: Long) {
        val upperSender = sender.uppercase()
        if (upperSender.endsWith("-T") || upperSender.endsWith("-S")) {
            try {
                val alreadyExists = unrecognizedSmsRepository.exists(sender, body)
                if (!alreadyExists) {
                    val unrecognizedSms = UnrecognizedSmsEntity(
                        sender = sender,
                        smsBody = body,
                        receivedAt = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(timestamp),
                            ZoneId.systemDefault()
                        )
                    )
                    unrecognizedSmsRepository.insert(unrecognizedSms)
                    Log.d(TAG, "Stored unrecognized SMS from $sender")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error storing unrecognized SMS: ${e.message}", e)
            }
        }
    }
}
