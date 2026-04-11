package com.anomapro.finndot.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["category", "year_month", "currency"], unique = true),
        Index(value = ["year_month"]),
        Index(value = ["category"])
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "category")
    val category: String, // "All" for total budget, or specific category name
    
    @ColumnInfo(name = "amount")
    val amount: BigDecimal,
    
    @ColumnInfo(name = "year_month")
    val yearMonth: YearMonth, // e.g., "2024-01" for January 2024
    
    @ColumnInfo(name = "currency", defaultValue = "INR")
    val currency: String = "INR",
    
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

