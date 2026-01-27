package com.example.chatagent.presentation.personalagent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chatagent.domain.model.CommunicationStyle
import com.example.chatagent.domain.model.Message
import com.example.chatagent.domain.model.ResponseLength
import com.example.chatagent.domain.model.UserProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalAgentScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: PersonalAgentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Personal Assistant")
                        if (profile.name.isNotEmpty()) {
                            Text(
                                text = "Hello, ${profile.nickname ?: profile.name}!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleProfileEditor() }) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Edit Profile",
                            tint = if (uiState.showProfileEditor)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { viewModel.clearConversation() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Chat")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Chat messages
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.messages.isEmpty()) {
                    item {
                        WelcomeCard(profile = profile)
                    }
                }

                items(uiState.messages) { message ->
                    MessageBubble(message = message)
                }

                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Error message
            uiState.error?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss")
                        }
                    }
                }
            }

            // Input field
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.inputText,
                        onValueChange = { viewModel.onInputTextChanged(it) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask your personal assistant...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = { viewModel.sendMessage() },
                        enabled = uiState.inputText.isNotBlank() && !uiState.isLoading
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }

        // Profile Editor Bottom Sheet
        if (uiState.showProfileEditor) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.toggleProfileEditor() },
                sheetState = sheetState
            ) {
                ProfileEditor(
                    profile = profile,
                    onUpdateName = viewModel::updateName,
                    onUpdateNickname = viewModel::updateNickname,
                    onUpdateAge = viewModel::updateAge,
                    onUpdateOccupation = viewModel::updateOccupation,
                    onUpdateLocation = viewModel::updateLocation,
                    onUpdateLanguage = viewModel::updateLanguage,
                    onUpdateCommunicationStyle = viewModel::updateCommunicationStyle,
                    onUpdateResponseLength = viewModel::updateResponseLength,
                    onUpdateUseEmojis = viewModel::updateUseEmojis,
                    onUpdateInterests = viewModel::updateInterests,
                    onUpdateHobbies = viewModel::updateHobbies,
                    onUpdateSkills = viewModel::updateSkills,
                    onUpdateLearningGoals = viewModel::updateLearningGoals,
                    onUpdateNotes = viewModel::updateNotes,
                    onResetProfile = viewModel::resetProfile
                )
            }
        }
    }
}

@Composable
private fun WelcomeCard(profile: UserProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = if (profile.name.isNotEmpty())
                    "Welcome, ${profile.nickname ?: profile.name}!"
                else
                    "Welcome!",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "I'm your personal AI assistant. I know your preferences and can help you with tasks tailored to your needs.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap the person icon to customize your profile and make our conversations even more personalized.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val isUser = message.isFromUser

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Card(
            modifier = Modifier.widthIn(max = 320.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            )
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProfileEditor(
    profile: UserProfile,
    onUpdateName: (String) -> Unit,
    onUpdateNickname: (String) -> Unit,
    onUpdateAge: (Int?) -> Unit,
    onUpdateOccupation: (String) -> Unit,
    onUpdateLocation: (String) -> Unit,
    onUpdateLanguage: (String) -> Unit,
    onUpdateCommunicationStyle: (CommunicationStyle) -> Unit,
    onUpdateResponseLength: (ResponseLength) -> Unit,
    onUpdateUseEmojis: (Boolean) -> Unit,
    onUpdateInterests: (List<String>) -> Unit,
    onUpdateHobbies: (List<String>) -> Unit,
    onUpdateSkills: (List<String>) -> Unit,
    onUpdateLearningGoals: (List<String>) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onResetProfile: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Edit Your Profile",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Help your assistant know you better",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Basic Info Section
        SectionHeader("Basic Information")

        OutlinedTextField(
            value = profile.name,
            onValueChange = onUpdateName,
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = profile.nickname ?: "",
            onValueChange = onUpdateNickname,
            label = { Text("Nickname (how you want to be called)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = profile.age?.toString() ?: "",
                onValueChange = { onUpdateAge(it.toIntOrNull()) },
                label = { Text("Age") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = profile.language,
                onValueChange = onUpdateLanguage,
                label = { Text("Language") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = profile.occupation ?: "",
            onValueChange = onUpdateOccupation,
            label = { Text("Occupation") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = profile.location ?: "",
            onValueChange = onUpdateLocation,
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Communication Preferences Section
        SectionHeader("Communication Preferences")

        Text(
            text = "Communication Style",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CommunicationStyle.entries.forEach { style ->
                FilterChip(
                    selected = profile.communicationStyle == style,
                    onClick = { onUpdateCommunicationStyle(style) },
                    label = { Text(style.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Response Length",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ResponseLength.entries.forEach { length ->
                FilterChip(
                    selected = profile.preferredResponseLength == length,
                    onClick = { onUpdateResponseLength(length) },
                    label = { Text(length.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Use emojis in responses")
            Switch(
                checked = profile.useEmojis,
                onCheckedChange = onUpdateUseEmojis
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Interests Section
        SectionHeader("Interests & Hobbies")

        ChipListEditor(
            label = "Interests",
            items = profile.interests,
            onItemsChange = onUpdateInterests
        )

        Spacer(modifier = Modifier.height(16.dp))

        ChipListEditor(
            label = "Hobbies",
            items = profile.hobbies,
            onItemsChange = onUpdateHobbies
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Professional Section
        SectionHeader("Professional")

        ChipListEditor(
            label = "Skills",
            items = profile.skills,
            onItemsChange = onUpdateSkills
        )

        Spacer(modifier = Modifier.height(16.dp))

        ChipListEditor(
            label = "Learning Goals",
            items = profile.learningGoals,
            onItemsChange = onUpdateLearningGoals
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Notes Section
        SectionHeader("Personal Notes")

        OutlinedTextField(
            value = profile.notes ?: "",
            onValueChange = onUpdateNotes,
            label = { Text("Notes (anything else you want me to know)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Reset Button
        TextButton(
            onClick = { showResetDialog = true },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reset to Defaults")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Profile?") },
            text = { Text("This will reset all your profile settings to defaults. This action cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    onResetProfile()
                    showResetDialog = false
                }) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipListEditor(
    label: String,
    items: List<String>,
    onItemsChange: (List<String>) -> Unit
) {
    var newItemText by remember { mutableStateOf("") }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { item ->
                InputChip(
                    selected = false,
                    onClick = { },
                    label = { Text(item) },
                    trailingIcon = {
                        IconButton(
                            onClick = { onItemsChange(items - item) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newItemText,
                onValueChange = { newItemText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add $label...") },
                singleLine = true,
                trailingIcon = {
                    if (newItemText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                if (newItemText.isNotBlank() && newItemText !in items) {
                                    onItemsChange(items + newItemText.trim())
                                    newItemText = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                }
            )
        }
    }
}
