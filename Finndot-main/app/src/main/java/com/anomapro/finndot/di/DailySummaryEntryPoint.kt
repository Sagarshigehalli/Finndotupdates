package com.anomapro.finndot.di

import com.anomapro.finndot.data.manager.DailySummaryNotificationManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DailySummaryEntryPoint {
    fun dailySummaryNotificationManager(): DailySummaryNotificationManager
}
