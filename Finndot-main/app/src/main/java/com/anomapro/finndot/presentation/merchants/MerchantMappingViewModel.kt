package com.anomapro.finndot.presentation.merchants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anomapro.finndot.data.database.entity.MerchantNameMappingEntity
import com.anomapro.finndot.data.database.entity.TransactionEntity
import com.anomapro.finndot.data.repository.MerchantNameMappingRepository
import com.anomapro.finndot.data.repository.TransactionRepository
import com.anomapro.finndot.utils.MerchantNameUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MerchantMappingUiState(
    val mappings: List<MerchantNameMappingEntity> = emptyList(),
    val allMerchants: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showMergeDialog: Boolean = false,
    val selectedMerchants: List<String> = emptyList(),
    val mergeTargetName: String = "",
    val suggestedNormalizedName: String = "",
    val showUpdateExistingDialog: Boolean = false,
    val updateExistingTransactions: Boolean = false,
    val showMapDialog: Boolean = false,
    val mapOriginalName: String = "",
    val mapTargetName: String = ""
)

@HiltViewModel
class MerchantMappingViewModel @Inject constructor(
    private val merchantNameMappingRepository: MerchantNameMappingRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MerchantMappingUiState())
    val uiState: StateFlow<MerchantMappingUiState> = _uiState.asStateFlow()
    
    init {
        loadMappings()
        loadAllMerchants()
    }
    
    private fun loadMappings() {
        viewModelScope.launch {
            merchantNameMappingRepository.getAllMappings()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = e.message,
                        isLoading = false
                    )
                }
                .collect { mappings ->
                    _uiState.value = _uiState.value.copy(
                        mappings = mappings,
                        isLoading = false
                    )
                }
        }
    }
    
    private fun loadAllMerchants() {
        viewModelScope.launch {
            transactionRepository.getAllMerchants()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = e.message
                    )
                }
                .collect { merchants ->
                    _uiState.value = _uiState.value.copy(
                        allMerchants = merchants
                    )
                }
        }
    }
    
    fun showMapDialog(originalName: String) {
        val suggested = getSuggestedNormalizedName(originalName)
        _uiState.value = _uiState.value.copy(
            showMapDialog = true,
            mapOriginalName = originalName,
            mapTargetName = suggested
        )
    }
    
    fun hideMapDialog() {
        _uiState.value = _uiState.value.copy(
            showMapDialog = false,
            mapOriginalName = "",
            mapTargetName = ""
        )
    }
    
    fun updateMapTargetName(name: String) {
        _uiState.value = _uiState.value.copy(mapTargetName = name)
    }
    
    fun createMapping(originalName: String, normalizedName: String, updateExisting: Boolean = false) {
        viewModelScope.launch {
            try {
                if (normalizedName.isBlank()) {
                    _uiState.value = _uiState.value.copy(errorMessage = "Normalized name cannot be empty")
                    return@launch
                }
                
                merchantNameMappingRepository.setMapping(originalName, normalizedName)
                
                // Update existing transactions if requested
                if (updateExisting) {
                    updateExistingTransactionsForMerchant(originalName, normalizedName)
                }
                
                hideMapDialog()
                _uiState.value = _uiState.value.copy(errorMessage = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    private suspend fun updateExistingTransactionsForMerchant(
        originalName: String,
        normalizedName: String
    ) {
        // Get all transactions with this merchant name
        val allTransactions = transactionRepository.getAllTransactions().first()
        val transactionsToUpdate = allTransactions.filter { 
            it.merchantName == originalName && it.normalizedMerchantName != normalizedName
        }
        
        // Update each transaction
        transactionsToUpdate.forEach { transaction ->
            val updated = transaction.copy(normalizedMerchantName = normalizedName)
            transactionRepository.updateTransaction(updated)
        }
    }
    
    fun deleteMapping(originalName: String) {
        viewModelScope.launch {
            try {
                merchantNameMappingRepository.removeMapping(originalName)
                _uiState.value = _uiState.value.copy(errorMessage = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    fun showMergeDialog(merchants: List<String>) {
        // Suggest normalized name based on the first merchant
        val suggestedName = merchants.firstOrNull()?.let { 
            MerchantNameUtils.suggestNormalizedName(it)
        } ?: ""
        
        _uiState.value = _uiState.value.copy(
            showMergeDialog = true,
            selectedMerchants = merchants,
            mergeTargetName = suggestedName,
            suggestedNormalizedName = suggestedName
        )
    }
    
    fun hideMergeDialog() {
        _uiState.value = _uiState.value.copy(
            showMergeDialog = false,
            selectedMerchants = emptyList(),
            mergeTargetName = ""
        )
    }
    
    fun updateMergeTargetName(name: String) {
        _uiState.value = _uiState.value.copy(mergeTargetName = name)
    }
    
    fun mergeMerchants(updateExisting: Boolean = false) {
        val selected = _uiState.value.selectedMerchants
        val targetName = _uiState.value.mergeTargetName.trim()
        
        if (selected.isEmpty() || targetName.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select merchants and enter a target name")
            return
        }
        
        viewModelScope.launch {
            try {
                merchantNameMappingRepository.mergeMerchants(selected, targetName)
                
                // Update existing transactions if requested
                if (updateExisting) {
                    updateTransactionsWithNormalizedName(selected, targetName)
                }
                
                hideMergeDialog()
                _uiState.value = _uiState.value.copy(errorMessage = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    private suspend fun updateTransactionsWithNormalizedName(
        originalNames: List<String>,
        normalizedName: String
    ) {
        // Get all transactions with these merchant names
        val allTransactions = transactionRepository.getAllTransactions().first()
        val transactionsToUpdate = allTransactions.filter { 
            it.merchantName in originalNames && it.normalizedMerchantName != normalizedName
        }
        
        // Update each transaction
        transactionsToUpdate.forEach { transaction ->
            val updated = transaction.copy(normalizedMerchantName = normalizedName)
            transactionRepository.updateTransaction(updated)
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * Finds similar merchant names using advanced algorithms.
     * Returns merchants sorted by similarity score.
     */
    fun findSimilarMerchants(merchantName: String): List<String> {
        val allMerchants = _uiState.value.allMerchants
        
        val similarMerchants = MerchantNameUtils.findSimilarMerchants(
            targetMerchant = merchantName,
            candidateMerchants = allMerchants,
            maxResults = 10,
            minSimilarity = 0.6
        )
        
        return similarMerchants.map { (merchant, _) -> merchant }
    }
    
    /**
     * Gets suggested normalized name for a merchant.
     */
    fun getSuggestedNormalizedName(merchantName: String): String {
        return MerchantNameUtils.suggestNormalizedName(merchantName)
    }
    
    /**
     * Checks if two merchants are likely the same.
     */
    fun areMerchantsLikelySame(merchant1: String, merchant2: String): Boolean {
        return MerchantNameUtils.areLikelySame(merchant1, merchant2)
    }
    
    /**
     * Finds potential duplicate merchants that should be merged.
     * Uses transaction patterns and name similarity.
     * Returns a Flow that emits the list of potential duplicates.
     */
    fun findPotentialDuplicates(): Flow<List<Pair<String, List<String>>>> {
        return combine(
            transactionRepository.getAllTransactions(),
            merchantNameMappingRepository.getAllMappings()
        ) { allTransactions, mappings ->
            val mappingMap = mappings.associateBy { it.originalName }
            val merchantGroups = allTransactions.groupBy { transaction ->
                // Use normalized name if available, otherwise original
                transaction.normalizedMerchantName
                    ?: mappingMap[transaction.merchantName]?.normalizedName
                    ?: transaction.merchantName
            }
            
            val duplicates = mutableListOf<Pair<String, List<String>>>()
            val processed = mutableSetOf<String>()
            
            merchantGroups.forEach { (merchant1, _) ->
                if (merchant1 in processed) return@forEach
                
                val similarMerchants = merchantGroups.keys.filter { merchant2 ->
                    merchant2 != merchant1 && 
                    merchant2 !in processed &&
                    MerchantNameUtils.areLikelySame(merchant1, merchant2)
                }
                
                if (similarMerchants.isNotEmpty()) {
                    duplicates.add(merchant1 to similarMerchants)
                    processed.add(merchant1)
                    processed.addAll(similarMerchants)
                }
            }
            
            duplicates
        }
    }
}
