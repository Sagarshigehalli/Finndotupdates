package com.anomapro.finndot.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.anomapro.finndot.domain.service.SmsProcessingService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver for intercepting incoming SMS messages in real-time.
 * Processes transaction-related SMS immediately upon arrival.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var smsProcessingService: SmsProcessingService

    companion object {
        private const val TAG = "SmsReceiver"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        try {
            // Extract SMS messages from intent
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) {
                Log.w(TAG, "No SMS messages found in intent")
                return
            }

            // Process each SMS message
            for (smsMessage in messages) {
                val sender = smsMessage.originatingAddress ?: continue
                val body = smsMessage.messageBody ?: continue
                val timestamp = smsMessage.timestampMillis

                Log.d(TAG, "Received SMS from $sender at $timestamp")

                // Filter for potential transaction messages
                if (isTransactionSender(sender)) {
                    // Process asynchronously to avoid blocking the broadcast
                    scope.launch {
                        try {
                            val success = smsProcessingService.processSms(sender, body, timestamp)
                            if (success) {
                                Log.d(TAG, "Successfully processed SMS from $sender")
                            } else {
                                Log.d(TAG, "Failed to process SMS from $sender")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing SMS from $sender: ${e.message}", e)
                        }
                    }
                } else {
                    Log.d(TAG, "Skipping non-transaction SMS from $sender")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onReceive: ${e.message}", e)
        }
    }

    /**
     * Filter for transaction-related SMS senders.
     * Includes bank senders and excludes promotional/government messages.
     */
    private fun isTransactionSender(sender: String): Boolean {
        val upperSender = sender.uppercase()
        
        // Exclude promotional and government messages
        if (upperSender.endsWith("-P") || upperSender.endsWith("-G")) {
            return false
        }

        // Include transactional and service messages
        // -T = Transactional, -S = Service
        if (upperSender.endsWith("-T") || upperSender.endsWith("-S")) {
            return true
        }

        // Include messages from known bank patterns
        // DLT format: XX-BANKNAME-T/S (e.g., AX-HDFCBK-S)
        val dltPattern = Regex("^[A-Z]{2}-[A-Z0-9]+-[TS]$")
        if (dltPattern.matches(upperSender)) {
            return true
        }

        // Include numeric senders (often used by banks)
        if (sender.all { it.isDigit() } && sender.length >= 4) {
            return true
        }

        // Include known bank keywords
        val bankKeywords = listOf(
            "BANK", "HDFC", "ICICI", "SBI", "AXIS", "KOTAK", "FEDERAL", "IDFC",
            "PAYTM", "PHONEPE", "GPAY", "AMAZON", "FLIPKART", "SLICE", "CRED",
            "UPI", "NEFT", "IMPS", "RTGS", "EPFO"
        )
        
        return bankKeywords.any { upperSender.contains(it) }
    }
}
