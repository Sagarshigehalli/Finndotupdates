package com.anomapro.finndot.data.preferences

import kotlinx.serialization.Serializable

@Serializable
data class CounterpartyMemoryEntry(
    val transactionType: String,
    val category: String,
    val updatedAtEpochMs: Long,
)

@Serializable
data class CounterpartyMemoryStore(
    val entries: Map<String, CounterpartyMemoryEntry> = emptyMap(),
)
