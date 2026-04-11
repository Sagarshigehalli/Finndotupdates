package com.anomapro.finndot.di

import android.content.Context
import androidx.credentials.CredentialManager
import com.anomapro.finndot.data.auth.AuthRepository
import com.anomapro.finndot.data.auth.CloudUserProfileService
import com.anomapro.finndot.data.auth.CredentialManagerGoogleSignInHelper
import com.anomapro.finndot.data.auth.FirestoreCloudUserProfileService
import com.anomapro.finndot.data.auth.FirebaseSignInService
import com.anomapro.finndot.data.auth.GoogleSignInHelper
import com.anomapro.finndot.data.auth.StandardAuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideCredentialManager(@ApplicationContext context: Context): CredentialManager =
        CredentialManager.create(context)

    @Provides
    @Singleton
    fun provideGoogleSignInHelper(credentialManager: CredentialManager): GoogleSignInHelper =
        CredentialManagerGoogleSignInHelper(credentialManager)

    @Provides
    @Singleton
    fun provideCloudUserProfileService(
        firestore: FirebaseFirestore
    ): CloudUserProfileService = FirestoreCloudUserProfileService(firestore)

    @Provides
    @Singleton
    fun provideAuthRepository(
        standardAuthRepository: StandardAuthRepository
    ): AuthRepository = standardAuthRepository
}
