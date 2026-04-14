package com.anomapro.finndot.domain.service

import com.anomapro.finndot.data.database.entity.CardType
import com.anomapro.finndot.data.database.entity.TransactionEntity
import com.anomapro.finndot.data.repository.AccountBalanceRepository
import com.anomapro.finndot.data.repository.CardRepository
import com.anomapro.finndot.domain.model.LinkedAccountKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the set of (bank, last4) identities the app treats as the user's own, without
 * new database tables. Sources:
 *
 * 1. **[AccountBalanceRepository.getAllLatestBalances]** — one latest row per distinct
 *    (bank_name, account_last4). Covers savings/current (and credit-card balance rows
 *    where we stored card last4 as account_last4).
 * 2. **[CardRepository.getAllActiveCards]** — active cards:
 *    - **Debit + linked** [com.anomapro.finndot.data.database.entity.CardEntity.accountLast4]:
 *      adds the **account** last4 (SMS often resolves here after processing).
 *    - **Debit (any)**: also adds **(bank, cardLast4)** so legs that still reference the
 *      card number match (e.g. orphaned debit not linked to an account yet).
 *    - **Credit**: adds **(bank, cardLast4)**; savings↔credit internal flows use card last4
 *      on the card side.
 *
 * ## Edge cases (conservative behaviour)
 *
 * - **NULL [TransactionEntity.bankName] or [TransactionEntity.accountNumber]**: cannot build
 *   a key → [isLegOwned] is false (unknown leg).
 * - **No balances and no cards**: empty set → no leg is "owned".
 * - **Bank name mismatch**: parser vs card label may differ; we only normalize trim +
 *   lowercase — very different spellings ("SBI" vs "State Bank of India") may not match
 *   until naming is aligned elsewhere.
 * - **Same last4, different banks**: keys include bank → treated as different identities.
 */
@Singleton
class LinkedAccountsProvider @Inject constructor(
    private val accountBalanceRepository: AccountBalanceRepository,
    private val cardRepository: CardRepository
) {

    /**
     * Snapshot of all owned keys right now. Call when pairing; cache in caller if batching.
     */
    suspend fun loadOwnedAccountKeys(): Set<LinkedAccountKey> {
        val keys = mutableSetOf<LinkedAccountKey>()

        val latestBalances = accountBalanceRepository.getAllLatestBalances().first()
        for (row in latestBalances) {
            LinkedAccountKey.fromRaw(row.bankName, row.accountLast4)?.let { keys.add(it) }
        }

        val cards = cardRepository.getAllActiveCards().first()
        for (card in cards) {
            when (card.cardType) {
                CardType.DEBIT -> {
                    if (card.accountLast4 != null) {
                        LinkedAccountKey.fromRaw(card.bankName, card.accountLast4)?.let { keys.add(it) }
                    }
                    LinkedAccountKey.fromRaw(card.bankName, card.cardLast4)?.let { keys.add(it) }
                }
                CardType.CREDIT -> {
                    LinkedAccountKey.fromRaw(card.bankName, card.cardLast4)?.let { keys.add(it) }
                }
            }
        }

        return keys
    }

    fun isLegOwned(transaction: TransactionEntity, ownedKeys: Set<LinkedAccountKey>): Boolean {
        val key = LinkedAccountKey.fromTransactionLeg(transaction) ?: return false
        return key in ownedKeys
    }

    fun areBothLegsOwned(
        first: TransactionEntity,
        second: TransactionEntity,
        ownedKeys: Set<LinkedAccountKey>
    ): Boolean = isLegOwned(first, ownedKeys) && isLegOwned(second, ownedKeys)
}
