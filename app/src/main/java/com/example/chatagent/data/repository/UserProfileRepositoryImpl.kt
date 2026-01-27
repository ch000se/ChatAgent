package com.example.chatagent.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.chatagent.domain.model.CommunicationStyle
import com.example.chatagent.domain.model.ResponseLength
import com.example.chatagent.domain.model.UserProfile
import com.example.chatagent.domain.model.WorkSchedule
import com.example.chatagent.domain.repository.UserProfileRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) : UserProfileRepository {

    private val TAG = "UserProfileRepository"
    private val PREFS_NAME = "user_profile_prefs"
    private val KEY_PROFILE = "user_profile_json"
    private val KEY_IS_CUSTOMIZED = "is_profile_customized"

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(UserProfile())

    init {
        repositoryScope.launch {
            loadProfile()
        }
    }

    override fun getUserProfile(): StateFlow<UserProfile> = _userProfile.asStateFlow()

    override suspend fun updateProfile(profile: UserProfile) {
        _userProfile.value = profile
        saveProfile()
    }

    override suspend fun loadProfile() = withContext(Dispatchers.IO) {
        try {
            // First, try to load from SharedPreferences (user customized profile)
            val savedJson = prefs.getString(KEY_PROFILE, null)
            if (savedJson != null) {
                val profile = parseProfileJson(savedJson)
                if (profile != null) {
                    _userProfile.value = profile
                    Log.d(TAG, "Loaded profile from SharedPreferences: ${profile.name}")
                    return@withContext
                }
            }

            // If no saved profile, load default from assets
            loadDefaultProfile()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading profile", e)
            _userProfile.value = UserProfile()
        }
    }

    private suspend fun loadDefaultProfile() = withContext(Dispatchers.IO) {
        try {
            val json = context.assets.open("user_profile.json").bufferedReader().use { it.readText() }
            val profile = parseProfileJson(json)
            if (profile != null) {
                _userProfile.value = profile
                Log.d(TAG, "Loaded default profile from assets: ${profile.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading default profile from assets", e)
            _userProfile.value = UserProfile()
        }
    }

    private fun parseProfileJson(json: String): UserProfile? {
        return try {
            val mapType = object : TypeToken<Map<String, Any?>>() {}.type
            val map: Map<String, Any?> = gson.fromJson(json, mapType)

            UserProfile(
                name = map["name"] as? String ?: "",
                nickname = map["nickname"] as? String,
                age = (map["age"] as? Double)?.toInt(),
                occupation = map["occupation"] as? String,
                location = map["location"] as? String,
                timezone = map["timezone"] as? String,
                language = map["language"] as? String ?: "en",
                communicationStyle = parseCommunicationStyle(map["communicationStyle"] as? String),
                preferredResponseLength = parseResponseLength(map["preferredResponseLength"] as? String),
                useEmojis = map["useEmojis"] as? Boolean ?: false,
                interests = parseStringList(map["interests"]),
                hobbies = parseStringList(map["hobbies"]),
                favoriteTopics = parseStringList(map["favoriteTopics"]),
                skills = parseStringList(map["skills"]),
                currentProjects = parseStringList(map["currentProjects"]),
                learningGoals = parseStringList(map["learningGoals"]),
                workSchedule = parseWorkSchedule(map["workSchedule"]),
                productivityPeakTime = map["productivityPeakTime"] as? String,
                dailyRoutines = parseStringList(map["dailyRoutines"]),
                musicPreferences = parseStringList(map["musicPreferences"]),
                foodPreferences = parseStringList(map["foodPreferences"]),
                dietaryRestrictions = parseStringList(map["dietaryRestrictions"]),
                birthday = map["birthday"] as? String,
                importantDates = parseStringMap(map["importantDates"]),
                notes = map["notes"] as? String,
                reminders = parseStringList(map["reminders"]),
                customFields = parseStringMap(map["customFields"])
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing profile JSON", e)
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseStringList(value: Any?): List<String> {
        return (value as? List<*>)?.filterIsInstance<String>() ?: emptyList()
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseStringMap(value: Any?): Map<String, String> {
        return (value as? Map<*, *>)
            ?.filterKeys { it is String }
            ?.filterValues { it is String }
            ?.map { (k, v) -> k as String to v as String }
            ?.toMap() ?: emptyMap()
    }

    private fun parseCommunicationStyle(value: String?): CommunicationStyle {
        return try {
            value?.let { CommunicationStyle.valueOf(it) } ?: CommunicationStyle.BALANCED
        } catch (e: Exception) {
            CommunicationStyle.BALANCED
        }
    }

    private fun parseResponseLength(value: String?): ResponseLength {
        return try {
            value?.let { ResponseLength.valueOf(it) } ?: ResponseLength.MEDIUM
        } catch (e: Exception) {
            ResponseLength.MEDIUM
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseWorkSchedule(value: Any?): WorkSchedule? {
        val map = value as? Map<*, *> ?: return null
        return try {
            WorkSchedule(
                workDays = (map["workDays"] as? List<*>)?.filterIsInstance<String>()
                    ?: listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday"),
                workStartTime = map["workStartTime"] as? String ?: "09:00",
                workEndTime = map["workEndTime"] as? String ?: "18:00",
                breakTime = map["breakTime"] as? String
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveProfile() {
        withContext(Dispatchers.IO) {
            try {
                val profile = _userProfile.value
                val json = gson.toJson(profileToMap(profile))
                prefs.edit()
                    .putString(KEY_PROFILE, json)
                    .putBoolean(KEY_IS_CUSTOMIZED, true)
                    .apply()
                Log.d(TAG, "Profile saved successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving profile", e)
            }
        }
    }

    private fun profileToMap(profile: UserProfile): Map<String, Any?> {
        return mapOf(
            "name" to profile.name,
            "nickname" to profile.nickname,
            "age" to profile.age,
            "occupation" to profile.occupation,
            "location" to profile.location,
            "timezone" to profile.timezone,
            "language" to profile.language,
            "communicationStyle" to profile.communicationStyle.name,
            "preferredResponseLength" to profile.preferredResponseLength.name,
            "useEmojis" to profile.useEmojis,
            "interests" to profile.interests,
            "hobbies" to profile.hobbies,
            "favoriteTopics" to profile.favoriteTopics,
            "skills" to profile.skills,
            "currentProjects" to profile.currentProjects,
            "learningGoals" to profile.learningGoals,
            "workSchedule" to profile.workSchedule?.let {
                mapOf(
                    "workDays" to it.workDays,
                    "workStartTime" to it.workStartTime,
                    "workEndTime" to it.workEndTime,
                    "breakTime" to it.breakTime
                )
            },
            "productivityPeakTime" to profile.productivityPeakTime,
            "dailyRoutines" to profile.dailyRoutines,
            "musicPreferences" to profile.musicPreferences,
            "foodPreferences" to profile.foodPreferences,
            "dietaryRestrictions" to profile.dietaryRestrictions,
            "birthday" to profile.birthday,
            "importantDates" to profile.importantDates,
            "notes" to profile.notes,
            "reminders" to profile.reminders,
            "customFields" to profile.customFields
        )
    }

    override fun generatePersonalizationContext(): String {
        val profile = _userProfile.value
        val builder = StringBuilder()

        builder.appendLine("=== USER PROFILE ===")

        // Basic info
        if (profile.name.isNotEmpty()) {
            builder.appendLine("Name: ${profile.name}")
        }
        profile.nickname?.let { builder.appendLine("Preferred name: $it") }
        profile.age?.let { builder.appendLine("Age: $it") }
        profile.occupation?.let { builder.appendLine("Occupation: $it") }
        profile.location?.let { builder.appendLine("Location: $it") }
        profile.timezone?.let { builder.appendLine("Timezone: $it") }
        builder.appendLine("Preferred language: ${profile.language}")

        // Communication preferences
        builder.appendLine()
        builder.appendLine("=== COMMUNICATION PREFERENCES ===")
        builder.appendLine("Style: ${formatCommunicationStyle(profile.communicationStyle)}")
        builder.appendLine("Response length: ${formatResponseLength(profile.preferredResponseLength)}")
        if (profile.useEmojis) {
            builder.appendLine("Uses emojis: Yes, feel free to use emojis in responses")
        }

        // Interests and hobbies
        if (profile.interests.isNotEmpty() || profile.hobbies.isNotEmpty() || profile.favoriteTopics.isNotEmpty()) {
            builder.appendLine()
            builder.appendLine("=== INTERESTS & HOBBIES ===")
            if (profile.interests.isNotEmpty()) {
                builder.appendLine("Interests: ${profile.interests.joinToString(", ")}")
            }
            if (profile.hobbies.isNotEmpty()) {
                builder.appendLine("Hobbies: ${profile.hobbies.joinToString(", ")}")
            }
            if (profile.favoriteTopics.isNotEmpty()) {
                builder.appendLine("Favorite topics: ${profile.favoriteTopics.joinToString(", ")}")
            }
        }

        // Professional context
        if (profile.skills.isNotEmpty() || profile.currentProjects.isNotEmpty() || profile.learningGoals.isNotEmpty()) {
            builder.appendLine()
            builder.appendLine("=== PROFESSIONAL CONTEXT ===")
            if (profile.skills.isNotEmpty()) {
                builder.appendLine("Skills: ${profile.skills.joinToString(", ")}")
            }
            if (profile.currentProjects.isNotEmpty()) {
                builder.appendLine("Current projects: ${profile.currentProjects.joinToString(", ")}")
            }
            if (profile.learningGoals.isNotEmpty()) {
                builder.appendLine("Learning goals: ${profile.learningGoals.joinToString(", ")}")
            }
        }

        // Work schedule
        profile.workSchedule?.let { schedule ->
            builder.appendLine()
            builder.appendLine("=== WORK SCHEDULE ===")
            builder.appendLine("Work days: ${schedule.workDays.joinToString(", ")}")
            builder.appendLine("Work hours: ${schedule.workStartTime} - ${schedule.workEndTime}")
            schedule.breakTime?.let { builder.appendLine("Break time: $it") }
        }
        profile.productivityPeakTime?.let { builder.appendLine("Most productive: $it") }

        // Daily routines
        if (profile.dailyRoutines.isNotEmpty()) {
            builder.appendLine()
            builder.appendLine("=== DAILY ROUTINES ===")
            profile.dailyRoutines.forEach { builder.appendLine("- $it") }
        }

        // Preferences
        if (profile.musicPreferences.isNotEmpty() || profile.foodPreferences.isNotEmpty()) {
            builder.appendLine()
            builder.appendLine("=== PREFERENCES ===")
            if (profile.musicPreferences.isNotEmpty()) {
                builder.appendLine("Music: ${profile.musicPreferences.joinToString(", ")}")
            }
            if (profile.foodPreferences.isNotEmpty()) {
                builder.appendLine("Food: ${profile.foodPreferences.joinToString(", ")}")
            }
            if (profile.dietaryRestrictions.isNotEmpty()) {
                builder.appendLine("Dietary restrictions: ${profile.dietaryRestrictions.joinToString(", ")}")
            }
        }

        // Important dates
        profile.birthday?.let {
            builder.appendLine()
            builder.appendLine("=== IMPORTANT DATES ===")
            builder.appendLine("Birthday: $it")
        }
        if (profile.importantDates.isNotEmpty()) {
            if (profile.birthday == null) {
                builder.appendLine()
                builder.appendLine("=== IMPORTANT DATES ===")
            }
            profile.importantDates.forEach { (date, desc) ->
                builder.appendLine("$date: $desc")
            }
        }

        // Reminders
        if (profile.reminders.isNotEmpty()) {
            builder.appendLine()
            builder.appendLine("=== ACTIVE REMINDERS ===")
            profile.reminders.forEach { builder.appendLine("- $it") }
        }

        // Notes
        profile.notes?.let {
            builder.appendLine()
            builder.appendLine("=== PERSONAL NOTES ===")
            builder.appendLine(it)
        }

        // Custom fields
        if (profile.customFields.isNotEmpty()) {
            builder.appendLine()
            builder.appendLine("=== ADDITIONAL INFO ===")
            profile.customFields.forEach { (key, value) ->
                builder.appendLine("$key: $value")
            }
        }

        builder.appendLine()
        builder.appendLine("=== END OF PROFILE ===")

        return builder.toString()
    }

    private fun formatCommunicationStyle(style: CommunicationStyle): String {
        return when (style) {
            CommunicationStyle.FORMAL -> "Formal and professional"
            CommunicationStyle.BALANCED -> "Balanced (professional but approachable)"
            CommunicationStyle.CASUAL -> "Casual and relaxed"
            CommunicationStyle.FRIENDLY -> "Warm and friendly, like talking to a friend"
        }
    }

    private fun formatResponseLength(length: ResponseLength): String {
        return when (length) {
            ResponseLength.CONCISE -> "Brief and to the point"
            ResponseLength.MEDIUM -> "Balanced detail"
            ResponseLength.DETAILED -> "Comprehensive with full explanations"
        }
    }

    override suspend fun resetProfile() {
        withContext(Dispatchers.IO) {
            prefs.edit()
                .remove(KEY_PROFILE)
                .remove(KEY_IS_CUSTOMIZED)
                .apply()
            loadDefaultProfile()
            Log.d(TAG, "Profile reset to defaults")
        }
    }

    override fun isProfileCustomized(): Boolean {
        return prefs.getBoolean(KEY_IS_CUSTOMIZED, false)
    }
}
