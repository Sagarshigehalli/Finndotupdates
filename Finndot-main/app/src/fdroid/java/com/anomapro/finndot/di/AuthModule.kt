package com.anomapro.finndot.di

import com.anomapro.finndot.data.auth.AuthRepository
import com.anomapro.finndot.data.auth.CloudUserProfileService
import com.anomapro.finndot.data.auth.GoogleSignInHelper
import com.anomapro.finndot.data.auth.NoOpAuthRepository
import com.anomapro.finndot.data.auth.NoOpCloudUserProfileService
import com.anomapro.finndot.data.auth.NoOpGoogleSignInHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideGoogleSignInHelper(noOp: NoOpGoogleSignInHelper): GoogleSignInHelper = noOp

    @Provides
    @Singleton
    fun provideAuthRepository(noOp: NoOpAuthRepository): AuthRepository = noOp

    @Provides
    @Singleton
    fun provideCloudUserProfileService(
        noOp: NoOpCloudUserProfileService
    ): CloudUserProfileService = noOp
}
