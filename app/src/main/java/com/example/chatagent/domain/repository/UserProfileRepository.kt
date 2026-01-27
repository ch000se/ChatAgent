package com.example.chatagent.domain.repository

import com.example.chatagent.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository for managing user profile and personalization settings.
 */
interface UserProfileRepository {
    /**
     * Get the current user profile as a StateFlow.
     */
    fun getUserProfile(): StateFlow<UserProfile>

    /**
     * Update the user profile.
     */
    suspend fun updateProfile(profile: UserProfile)

    /**
     * Load profile from storage (assets or SharedPreferences).
     */
    suspend fun loadProfile()

    /**
     * Save profile to persistent storage.
     */
    suspend fun saveProfile()

    /**
     * Generate a personalization context string for system prompts.
     * This string contains relevant user information that can be injected into prompts.
     */
    fun generatePersonalizationContext(): String

    /**
     * Reset profile to default values.
     */
    suspend fun resetProfile()

    /**
     * Check if profile has been customized.
     */
    fun isProfileCustomized(): Boolean
}
