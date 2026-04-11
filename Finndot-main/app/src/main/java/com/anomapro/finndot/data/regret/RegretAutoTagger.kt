package com.anomapro.finndot.data.regret

import com.anomapro.finndot.data.database.entity.TransactionEntity
import com.anomapro.finndot.data.database.entity.TransactionType
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auto-tags transactions as "regret" only when multiple signals suggest impulse spending.
 * User can always override via the existing regret toggle.
 *
 * Strategy (all must apply where relevant):
 * - Only EXPENSE and CREDIT; never INCOME, TRANSFER, INVESTMENT.
 * - Never tag recurring (subscriptions / planned recurring).
 * - Never tag large amounts (above planned threshold = likely intentional).
 * - Regret-prone categories (Shopping, Entertainment, Personal Care): tag only if
 *   amount is small (impulse range) OR transaction is late night (22:00–02:00).
 * - "Others" category: tag only when amount is very small (unclassified impulse).
 */
@Singleton
class RegretAutoTagger @Inject constructor() {

    private val regretProneCategories: Set<String> = setOf(
        "Shopping",
        "Entertainment",
        "Personal Care"
    )

    /** Above this amount we never auto-tag (likely planned). */
    private val plannedSpendThreshold: BigDecimal = BigDecimal("5000")

    /** In regret-prone categories, only tag if amount <= this (impulse range). */
    private val impulseCategoryThreshold: BigDecimal = BigDecimal("1500")

    /** "Others" with amount <= this treated as small unclassified impulse. */
    private val othersImpulseThreshold: BigDecimal = BigDecimal("500")

    private val lateNightStartHour = 22
    private val lateNightEndHour = 2

    fun shouldAutoTagAsRegret(transaction: TransactionEntity): Boolean {
        if (transaction.transactionType != TransactionType.EXPENSE &&
            transaction.transactionType != TransactionType.CREDIT
        ) {
            return false
        }
        if (transaction.isRecurring) return false

        val amount = transaction.amount.abs()
        if (amount > plannedSpendThreshold) return false

        val category = transaction.category?.trim() ?: "Others"
        val isLateNight = isLateNight(transaction.dateTime)

        return when {
            regretProneCategories.contains(category) ->
                amount <= impulseCategoryThreshold || isLateNight
            category == "Others" ->
                amount <= othersImpulseThreshold
            else -> false
        }
    }

    private fun isLateNight(dateTime: LocalDateTime): Boolean {
        val hour = dateTime.hour
        return hour >= lateNightStartHour || hour < lateNightEndHour
    }
}
