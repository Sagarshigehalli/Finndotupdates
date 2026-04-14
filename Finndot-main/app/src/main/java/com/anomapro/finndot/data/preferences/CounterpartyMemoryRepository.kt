package com.anomapro.finndot.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anomapro.finndot.data.database.entity.TransactionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.counterpartyMemoryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "counterparty_memory",
)

private val ENTRIES_JSON = stringPreferencesKey("counterparty_memory_entries")

private const val MAX_ENTRIES = 500

@Singleton
class CounterpartyMemoryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun get(key: String): CounterpartyMemoryEntry? {
        val prefs = context.counterpartyMemoryDataStore.data.first()
        return decodeStore(prefs[ENTRIES_JSON])?.entries?.get(key)
    }

    suspend fun put(key: String, transactionType: TransactionType, category: String) {
        context.counterpartyMemoryDataStore.edit { prefs ->
            val current = decodeStore(prefs[ENTRIES_JSON])?.entries?.toMutableMap() ?: mutableMapOf()
            current[key] = CounterpartyMemoryEntry(
                transactionType = transactionType.name,
                category = category,
                updatedAtEpochMs = System.currentTimeMillis(),
            )
            val pruned = if (current.size > MAX_ENTRIES) {
                current.entries
                    .sortedByDescending { it.value.updatedAtEpochMs }
                    .take(MAX_ENTRIES)
                    .associate { it.key to it.value }
            } else {
                current.toMap()
            }
            prefs[ENTRIES_JSON] = json.encodeToString(
                CounterpartyMemoryStore.serializer(),
                CounterpartyMemoryStore(pruned),
            )
        }
    }

    fun observeHasKey(key: String) = context.counterpartyMemoryDataStore.data.map { prefs ->
        decodeStore(prefs[ENTRIES_JSON])?.entries?.containsKey(key) == true
    }

    private fun decodeStore(raw: String?): CounterpartyMemoryStore? {
        if (raw.isNullOrBlank()) return null
        return try {
            json.decodeFromString(CounterpartyMemoryStore.serializer(), raw)
        } catch (_: Exception) {
            null
        }
    }
}
