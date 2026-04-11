package com.anomapro.finndot.data.auth

import javax.inject.Inject
import javax.inject.Singleton

/**
 * No-op implementation used when cloud profile sync is not available (e.g. F-Droid build).
 */
@Singleton
class NoOpCloudUserProfileService @Inject constructor() : CloudUserProfileService {
    override suspend fun syncProfile(profile: UserProfile): Result<Unit> = Result.success(Unit)
}
