package com.example.chatagent.domain.model

/**
 * User profile model for personalization.
 * Contains personal information, preferences, habits, and interests
 * that the AI agent uses to provide personalized responses.
 */
data class UserProfile(
    val name: String = "",
    val nickname: String? = null,
    val age: Int? = null,
    val occupation: String? = null,
    val location: String? = null,
    val timezone: String? = null,
    val language: String = "en",

    // Communication preferences
    val communicationStyle: CommunicationStyle = CommunicationStyle.BALANCED,
    val preferredResponseLength: ResponseLength = ResponseLength.MEDIUM,
    val useEmojis: Boolean = false,

    // Personal interests and hobbies
    val interests: List<String> = emptyList(),
    val hobbies: List<String> = emptyList(),
    val favoriteTopics: List<String> = emptyList(),

    // Professional context
    val skills: List<String> = emptyList(),
    val currentProjects: List<String> = emptyList(),
    val learningGoals: List<String> = emptyList(),

    // Daily habits and routines
    val workSchedule: WorkSchedule? = null,
    val productivityPeakTime: String? = null,
    val dailyRoutines: List<String> = emptyList(),

    // Preferences
    val musicPreferences: List<String> = emptyList(),
    val foodPreferences: List<String> = emptyList(),
    val dietaryRestrictions: List<String> = emptyList(),

    // Important dates
    val birthday: String? = null,
    val importantDates: Map<String, String> = emptyMap(),

    // Personal notes
    val notes: String? = null,
    val reminders: List<String> = emptyList(),

    // Custom fields for extensibility
    val customFields: Map<String, String> = emptyMap()
)

enum class CommunicationStyle {
    FORMAL,      // Professional, structured responses
    BALANCED,    // Mix of professional and friendly
    CASUAL,      // Relaxed, conversational
    FRIENDLY     // Warm, enthusiastic, like a friend
}

enum class ResponseLength {
    CONCISE,     // Brief, to the point
    MEDIUM,      // Balanced detail
    DETAILED     // Comprehensive explanations
}

data class WorkSchedule(
    val workDays: List<String> = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday"),
    val workStartTime: String = "09:00",
    val workEndTime: String = "18:00",
    val breakTime: String? = "13:00"
)
