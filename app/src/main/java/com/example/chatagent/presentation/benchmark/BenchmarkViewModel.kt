package com.example.chatagent.presentation.benchmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatagent.domain.model.PredefinedModels
import com.example.chatagent.domain.usecase.RunBenchmarkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BenchmarkViewModel @Inject constructor(
    private val runBenchmarkUseCase: RunBenchmarkUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BenchmarkUiState())
    val uiState: StateFlow<BenchmarkUiState> = _uiState.asStateFlow()

    fun onPromptChanged(prompt: String) {
        _uiState.update { it.copy(prompt = prompt) }
    }

    fun runBenchmark() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isEmpty()) {
            _uiState.update { it.copy(error = "Please enter a prompt") }
            return
        }

        _uiState.update {
            it.copy(
                isRunning = true,
                error = null,
                comparison = null
            )
        }

        viewModelScope.launch {
            runBenchmarkUseCase(PredefinedModels.allModels, prompt)
                .onSuccess { comparison ->
                    _uiState.update {
                        it.copy(
                            isRunning = false,
                            comparison = comparison
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isRunning = false,
                            error = exception.message ?: "Unknown error occurred"
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}