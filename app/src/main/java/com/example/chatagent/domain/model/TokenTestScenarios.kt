package com.example.chatagent.domain.model

/**
 * Test scenarios for token counting demonstration
 * Day 8: Working with tokens
 *
 * These scenarios help demonstrate:
 * 1. How token counting works
 * 2. Differences between short, medium, and long requests
 * 3. How to monitor token usage
 */
object TokenTestScenarios {

    /**
     * SHORT REQUEST (typically ~10-30 tokens)
     * Use case: Simple questions, quick interactions
     * Expected behavior: Low token usage, fast response
     */
    val shortRequest = "What is AI?"

    /**
     * MEDIUM REQUEST (typically ~50-150 tokens)
     * Use case: Standard conversations, moderate detail requests
     * Expected behavior: Moderate token usage, balanced response
     */
    val mediumRequest = """
        Can you explain how neural networks work?
        I'm interested in understanding the basic concepts like layers,
        weights, and activation functions. Please keep it at a beginner level.
    """.trimIndent()

    /**
     * LONG REQUEST (typically ~200-500 tokens)
     * Use case: Detailed questions, complex scenarios, context-rich requests
     * Expected behavior: Higher token usage, comprehensive response
     */
    val longRequest = """
        I'm building an Android application using Jetpack Compose and I need help with state management.

        Here's my situation:
        - I have a chat interface where users send messages to an AI
        - I need to track conversation history
        - I want to display token usage statistics
        - The app should handle errors gracefully
        - I'm using MVVM architecture with ViewModels and repositories
        - I'm using Retrofit for API calls to Claude's Anthropic API

        Can you provide best practices for:
        1. Managing conversation state
        2. Handling API response errors
        3. Implementing proper loading states
        4. Optimizing for performance
        5. Testing the implementation

        Please provide detailed explanations with code examples where appropriate.
    """.trimIndent()

    /**
     * VERY LONG REQUEST (typically ~800-1500 tokens)
     * Use case: Maximum context, detailed technical discussions
     * Expected behavior: Highest token usage, may approach model limits
     *
     * Note: Claude Sonnet 4.5 has a context window of 200k tokens for input,
     * but maxTokens for output is typically limited (default 1024-4096)
     */
    val veryLongRequest = """
        I'm developing a comprehensive Android chat application with the following requirements:

        ARCHITECTURE:
        - Clean Architecture with Domain, Data, and Presentation layers
        - MVVM pattern for UI components
        - Dependency injection with Hilt
        - Repository pattern for data access
        - Use cases for business logic

        FEATURES:
        - Real-time chat with AI (Claude Anthropic API)
        - Message history with pagination
        - Token usage tracking and display
        - Multiple AI personalities/system prompts
        - Temperature control for response creativity
        - Conversation export functionality
        - Offline message queueing
        - Message search and filtering
        - Custom themes (light/dark mode)
        - Accessibility support

        TECHNICAL STACK:
        - Kotlin with Coroutines and Flow
        - Jetpack Compose for UI
        - Retrofit + OkHttp for networking
        - Room database for local storage
        - DataStore for preferences
        - Material 3 design system
        - Coil for image loading
        - JUnit + Mockito for testing

        API INTEGRATION:
        - Anthropic Claude API v1
        - Support for claude-sonnet-4-5 model
        - Handle rate limiting
        - Implement retry logic with exponential backoff
        - Stream responses for better UX
        - Cache responses when appropriate

        QUESTIONS:
        1. How should I structure the data layer to efficiently manage conversation history?
        2. What's the best way to implement token counting at both request and response level?
        3. How can I optimize the app to handle very long conversations (100+ messages)?
        4. Should I implement message chunking for very long inputs that might exceed token limits?
        5. What's the best approach for implementing streaming responses in Jetpack Compose?
        6. How can I implement proper error handling with user-friendly messages?
        7. What testing strategy would you recommend for this architecture?
        8. How should I handle API key management securely?
        9. What's the best way to implement offline support with message queueing?
        10. How can I ensure the app remains responsive during long API calls?

        PERFORMANCE CONSIDERATIONS:
        - Messages can contain markdown formatting
        - Need to support code syntax highlighting
        - Images and attachments in future versions
        - Must work smoothly on low-end devices
        - Battery optimization is important

        SECURITY REQUIREMENTS:
        - Secure API key storage
        - Encrypted local database
        - HTTPS only communication
        - Certificate pinning
        - No logging of sensitive data

        Please provide comprehensive guidance covering architecture decisions,
        implementation patterns, code examples, testing strategies, and performance optimization techniques.
        Include specific recommendations for handling edge cases and potential issues.
    """.trimIndent()

    /**
     * Helper function to get all test scenarios with metadata
     */
    data class TestScenario(
        val name: String,
        val description: String,
        val prompt: String,
        val estimatedInputTokens: String,
        val expectedBehavior: String
    )

    val allScenarios = listOf(
        TestScenario(
            name = "Short Request",
            description = "Simple question, minimal context",
            prompt = shortRequest,
            estimatedInputTokens = "10-30 tokens",
            expectedBehavior = "Fast response, low cost"
        ),
        TestScenario(
            name = "Medium Request",
            description = "Standard conversation with moderate detail",
            prompt = mediumRequest,
            estimatedInputTokens = "50-150 tokens",
            expectedBehavior = "Balanced response, moderate cost"
        ),
        TestScenario(
            name = "Long Request",
            description = "Detailed question with context",
            prompt = longRequest,
            estimatedInputTokens = "200-500 tokens",
            expectedBehavior = "Comprehensive response, higher cost"
        ),
        TestScenario(
            name = "Very Long Request",
            description = "Maximum context, complex requirements",
            prompt = veryLongRequest,
            estimatedInputTokens = "800-1500 tokens",
            expectedBehavior = "Detailed response, highest cost"
        )
    )

    /**
     * Instructions for testing token counting:
     *
     * 1. Clear conversation history before each test
     * 2. Send one of the test requests above
     * 3. Observe the token display showing:
     *    - Input tokens (prompt + conversation history)
     *    - Output tokens (response from Claude)
     *    - Total tokens (cumulative for session)
     * 4. Compare actual token counts with estimates
     * 5. Note how token counts increase with:
     *    - Longer prompts
     *    - Conversation history accumulation
     *    - More detailed responses
     *
     * COST ESTIMATION (as of 2024, check current pricing):
     * Claude Sonnet 4.5:
     * - Input: ~$3 per million tokens
     * - Output: ~$15 per million tokens
     *
     * Example calculation for a 500 token input + 1000 token output:
     * - Input cost: 500 * ($3 / 1,000,000) = $0.0015
     * - Output cost: 1000 * ($15 / 1,000,000) = $0.015
     * - Total: $0.0165 per interaction
     *
     * TOKEN LIMITS:
     * - Claude Sonnet 4.5: 200k token context window
     * - Typical max_tokens for output: 1024-4096 tokens
     * - Conversation history counts toward input tokens
     * - System prompts count toward input tokens
     */
}