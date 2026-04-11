package com.anomapro.finndot.data.repository

import com.anomapro.finndot.data.database.dao.MerchantNameMappingDao
import com.anomapro.finndot.data.database.entity.MerchantNameMappingEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerchantNameMappingRepository @Inject constructor(
    private val merchantNameMappingDao: MerchantNameMappingDao
) {
    
    /**
     * Gets the normalized name for an original merchant name.
     * Returns the original name if no mapping exists.
     */
    suspend fun getNormalizedName(originalName: String): String {
        return merchantNameMappingDao.getNormalizedName(originalName) ?: originalName
    }
    
    /**
     * Creates or updates a mapping from original name to normalized name.
     */
    suspend fun setMapping(originalName: String, normalizedName: String) {
        val existing = merchantNameMappingDao.getMapping(originalName)
        val mapping = existing?.copy(
            normalizedName = normalizedName,
            updatedAt = LocalDateTime.now()
        ) ?: MerchantNameMappingEntity(
            originalName = originalName,
            normalizedName = normalizedName,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        merchantNameMappingDao.insertOrUpdateMapping(mapping)
    }
    
    /**
     * Removes a mapping for an original name.
     */
    suspend fun removeMapping(originalName: String) {
        merchantNameMappingDao.deleteMapping(originalName)
    }
    
    /**
     * Gets all mappings for a normalized name.
     */
    fun getMappingsForNormalizedName(normalizedName: String): Flow<List<MerchantNameMappingEntity>> {
        return merchantNameMappingDao.getMappingsForNormalizedName(normalizedName)
    }
    
    /**
     * Gets all mappings.
     */
    fun getAllMappings(): Flow<List<MerchantNameMappingEntity>> {
        return merchantNameMappingDao.getAllMappings()
    }
    
    /**
     * Gets a specific mapping.
     */
    suspend fun getMapping(originalName: String): MerchantNameMappingEntity? {
        return merchantNameMappingDao.getMapping(originalName)
    }
    
    /**
     * Checks if a mapping exists.
     */
    suspend fun mappingExists(originalName: String): Boolean {
        return merchantNameMappingDao.mappingExists(originalName)
    }
    
    /**
     * Gets all distinct normalized names.
     */
    fun getAllNormalizedNames(): Flow<List<String>> {
        return merchantNameMappingDao.getAllNormalizedNames()
    }
    
    /**
     * Merges multiple original names into one normalized name.
     * Useful for combining different variations of the same merchant.
     */
    suspend fun mergeMerchants(originalNames: List<String>, normalizedName: String) {
        originalNames.forEach { originalName ->
            setMapping(originalName, normalizedName)
        }
    }
    
    /**
     * Deletes all mappings for a normalized name.
     */
    suspend fun deleteMappingsForNormalizedName(normalizedName: String) {
        merchantNameMappingDao.deleteMappingsForNormalizedName(normalizedName)
    }
    
    /**
     * Gets the mapping count.
     */
    suspend fun getMappingCount(): Int {
        return merchantNameMappingDao.getMappingCount()
    }
}
