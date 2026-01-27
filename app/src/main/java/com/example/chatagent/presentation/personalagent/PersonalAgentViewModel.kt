package com.example.chatagent.presentation.personalagent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatagent.domain.model.CommunicationStyle
import com.example.chatagent.domain.model.Message
import com.example.chatagent.domain.model.ResponseLength
import com.example.chatagent.domain.model.SystemPrompts
import com.example.chatagent.domain.model.UserProfile
import com.example.chatagent.domain.model.WorkSchedule
import com.example.chatagent.domain.repository.ChatRepository
import com.example.chatagent.domain.repository.UserProfileRepository
import com.example.chatagent.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PersonalAgentViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val chatRepository: ChatRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonalAgentUiState())
    val uiState: StateFlow<PersonalAgentUiState> = _uiState.asStateFlow()

    val userProfile: StateFlow<UserProfile> = userProfileRepository.getUserProfile()

    init {
        // Observe user profile changes
        viewModelScope.launch {
            userProfile.collect { profile ->
                _uiState.update { it.copy(profile = profile) }
                // Update system prompt with personalization context
                updatePersonalizedSystemPrompt()
            }
        }

        // Set initial system prompt
        updatePersonalizedSystemPrompt()
    }

    private fun updatePersonalizedSystemPrompt() {
        val basePrompt = SystemPrompts.PERSONAL_ASSISTANT.prompt
        val personalizationContext = userProfileRepository.generatePersonalizationContext()
        val fullPrompt = """
            $basePrompt

            --- USER PROFILE START ---
            $personalizationContext
            --- USER PROFILE END ---
        """.trimIndent()
        chatRepository.setSystemPrompt(fullPrompt)
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val messageText = _uiState.value.inputText.trim()
        if (messageText.isEmpty()) return

        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            content = messageText,
            isFromUser = true
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            sendMessageUseCase(messageText)
                .onSuccess { agentMessage ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            messages = currentState.messages + agentMessage,
                            isLoading = false
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Unknown error occurred"
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearConversation() {
        chatRepository.clearConversationHistory()
        _uiState.update { it.copy(messages = emptyList()) }
    }

    fun toggleProfileEditor() {
        _uiState.update { it.copy(showProfileEditor = !it.showProfileEditor) }
    }

    // Profile editing functions
    fun updateName(name: String) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(name = name)
            userProfileRepository.updateProfile(updated)
        }
    }

    fun updateNickname(nickname: String) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(nickname = nickname.ifEmpty { null })
            userProfileRepository.updateProfile(updated)
        }
    }

    fun updateAge(age: Int?) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(age = age)
            userProfileRepository.updateProfile(updated)
        }
    }

    fun updateOccupation(occupation: String) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(occupation = occupation.ifEmpty { null })
            userProfileRepository.updateProfile(updated)
        }
    }

    fun updateLocation(location: String) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(location = location.ifEmpty { null })
            userProfileRepository.updateProfile(updated)
        }
    }

    fun updateTimezone(timezone: String) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(timezone = timezone.ifEmpty { null })
            userProfileRepository.updateProfile(updated)
        }
    }

    fun updateLanguage(language: String) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(language = language)
            userProfileRepository.updateProfile(updated)
        }
    }

    fun updateCommunicationStyle(style: CommunicationStyle) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(communicationStyle = style)
            userProfileRepository.updateProfile(updated)
        }
    }

    fun updateResponseLength(length: ResponseLength) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(preferredResponseLength = length)
            userProfileRepository.updateProfile(updated)
        }
    }

    fun updateUseEmojis(useEmojis: Boolean) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(useEmojis = useEmojis)
            userProfileRepository.updateProfile(updated)
        }
    }

    fun updateInterests(interests: List<String>) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(interests = interests)
            userProfileRepository.updateProfile(updated)
        }
    }

    fun updateHobbies(hobbies: List<String>) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(hobbies = hobbies)
            userProfileRepository.updateProfile(updated)
        }
    }

    fun updateSkills(skills: List<String>) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(skills = skills)
            userProfileRepository.updateProfile(updated)
        }
    }

    fun updateCurrentProjects(projects: List<String>) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(currentProjects = projects)
            userProfileRepository.updateProfile(updated)
        }
    }

    fun updateLearningGoals(goals: List<String>) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(learningGoals = goals)
            userProfileRepository.updateProfile(updated)
        }
    }

    fun updateNotes(notes: String) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(notes = notes.ifEmpty { null })
            userProfileRepository.updateProfile(updated)
        }
    }

    fun updateWorkSchedule(schedule: WorkSchedule?) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(workSchedule = schedule)
            userProfileRepository.updateProfile(updated)
        }
    }

    fun resetProfile() {
        viewModelScope.launch {
            userProfileRepository.resetProfile()
        }
    }
}

data class PersonalAgentUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val inputText: String = "",
    val profile: UserProfile = UserProfile(),
    val showProfileEditor: Boolean = false
)
