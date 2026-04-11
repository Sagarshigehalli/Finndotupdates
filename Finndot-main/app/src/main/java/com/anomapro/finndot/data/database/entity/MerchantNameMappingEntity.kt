package com.anomapro.finndot.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Entity for storing user-defined merchant name mappings.
 * Maps original merchant names (as extracted from SMS) to normalized merchant names.
 * This allows users to combine different variations of the same merchant into one.
 * 
 * Example:
 * - "AMAZON PAY" -> "Amazon Pay"
 * - "AMAZONPAY" -> "Amazon Pay"
 * - "AMAZON PAYMENTS" -> "Amazon Pay"
 */
@Entity(
    tableName = "merchant_name_mappings",
    indices = [
        Index(value = ["original_name"], unique = true),
        Index(value = ["normalized_name"])
    ]
)
data class MerchantNameMappingEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    /**
     * The original merchant name as extracted from SMS (case-sensitive for matching)
     */
    @ColumnInfo(name = "original_name")
    val originalName: String,
    
    /**
     * The normalized merchant name that should be used for display and grouping
     */
    @ColumnInfo(name = "normalized_name")
    val normalizedName: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime
)
