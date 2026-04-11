package com.anomapro.finndot.data.database.dao

import androidx.room.*
import com.anomapro.finndot.data.database.entity.MerchantNameMappingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantNameMappingDao {
    
    /**
     * Gets the normalized name for an original merchant name.
     * Returns null if no mapping exists.
     */
    @Query("SELECT normalized_name FROM merchant_name_mappings WHERE original_name = :originalName LIMIT 1")
    suspend fun getNormalizedName(originalName: String): String?
    
    /**
     * Gets all mappings for a normalized name (reverse lookup).
     * Useful for showing all variations of a merchant.
     */
    @Query("SELECT * FROM merchant_name_mappings WHERE normalized_name = :normalizedName ORDER BY original_name ASC")
    fun getMappingsForNormalizedName(normalizedName: String): Flow<List<MerchantNameMappingEntity>>
    
    /**
     * Gets all mappings ordered by normalized name.
     */
    @Query("SELECT * FROM merchant_name_mappings ORDER BY normalized_name ASC, original_name ASC")
    fun getAllMappings(): Flow<List<MerchantNameMappingEntity>>
    
    /**
     * Gets a specific mapping by original name.
     */
    @Query("SELECT * FROM merchant_name_mappings WHERE original_name = :originalName LIMIT 1")
    suspend fun getMapping(originalName: String): MerchantNameMappingEntity?
    
    /**
     * Inserts or updates a mapping.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMapping(mapping: MerchantNameMappingEntity)
    
    /**
     * Deletes a mapping by original name.
     */
    @Query("DELETE FROM merchant_name_mappings WHERE original_name = :originalName")
    suspend fun deleteMapping(originalName: String)
    
    /**
     * Deletes all mappings for a normalized name.
     * Useful when merging merchants.
     */
    @Query("DELETE FROM merchant_name_mappings WHERE normalized_name = :normalizedName")
    suspend fun deleteMappingsForNormalizedName(normalizedName: String)
    
    /**
     * Gets count of mappings.
     */
    @Query("SELECT COUNT(*) FROM merchant_name_mappings")
    suspend fun getMappingCount(): Int
    
    /**
     * Gets all distinct normalized names.
     */
    @Query("SELECT DISTINCT normalized_name FROM merchant_name_mappings ORDER BY normalized_name ASC")
    fun getAllNormalizedNames(): Flow<List<String>>
    
    /**
     * Checks if a mapping exists for the given original name.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM merchant_name_mappings WHERE original_name = :originalName)")
    suspend fun mappingExists(originalName: String): Boolean

    @Query("DELETE FROM merchant_name_mappings")
    suspend fun deleteAllMappings()
}
