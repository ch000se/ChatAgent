package com.example.chatagent.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.chatagent.data.remote.api.ChatApiService
import com.example.chatagent.data.remote.client.McpClient
import com.example.chatagent.data.remote.dto.ChatRequest
import com.example.chatagent.data.remote.dto.ContentDto
import com.example.chatagent.data.remote.dto.MessageDto
import com.example.chatagent.notification.NotificationHelper
import com.example.chatagent.notification.NotificationPushChannel
import com.example.chatagent.notification.PushChannelManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class DailySummaryWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val mcpClient: McpClient,
    private val chatApiService: ChatApiService
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "DailySummaryWorker"
        const val WORK_NAME = "daily_summary_work"

        // Auto-connect to this MCP server (localhost reminder server)
        // Для емулятора: 10.0.2.2 або IP комп'ютера (192.168.0.100)
        // Для реального пристрою: IP комп'ютера в локальній мережі
        private const val MCP_SERVER_URL = "http://192.168.0.100:3000/mcp"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting daily summary generation...")

            // Auto-connect to MCP if not connected
            val connectionState = mcpClient.connectionState.first()
            if (connectionState !is McpClient.ConnectionState.Connected) {
                Log.d(TAG, "MCP not connected, attempting auto-connect...")

                // Try to connect to localhost MCP server
                val connectResult = mcpClient.connect(MCP_SERVER_URL)

                if (connectResult.isFailure) {
                    Log.e(TAG, "Failed to connect to MCP: ${connectResult.exceptionOrNull()?.message}")
                    return Result.success() // Don't retry - will try again tomorrow
                }

                Log.d(TAG, "✅ Auto-connected to MCP server")

                // Fetch tools list after connecting
                val toolsResult = mcpClient.listTools()
                if (toolsResult.isFailure) {
                    Log.e(TAG, "Failed to fetch tools: ${toolsResult.exceptionOrNull()?.message}")
                    return Result.success()
                }

                Log.d(TAG, "✅ Fetched ${toolsResult.getOrNull()?.size ?: 0} MCP tools")
            }

            // Step 1: Call MCP tool 'get_summary' directly to get raw data
            Log.d(TAG, "Calling MCP tool: get_summary")
            val summaryResult = mcpClient.callTool("get_summary", emptyMap())

            if (summaryResult.isFailure) {
                Log.e(TAG, "Failed to get summary from MCP: ${summaryResult.exceptionOrNull()?.message}")
                return Result.success()
            }

            // Extract text from MCP response
            val rawSummaryData = summaryResult.getOrNull()?.content?.firstOrNull()?.text ?: ""
            if (rawSummaryData.isBlank()) {
                Log.d(TAG, "No data from MCP summary")
                return Result.success()
            }

            Log.d(TAG, "Got raw summary data from MCP (${rawSummaryData.length} chars)")

            // Step 2: Send to Claude WITHOUT tools, just for formatting
            val formattingPrompt = """
Here's my reminder data:

$rawSummaryData

Create a beautiful and detailed summary with emojis.
Include:
- Overall statistics (total, completed, pending)
- Important tasks
- Overdue tasks
- Insights and recommendations

The response should be formatted as a text report with emojis for Android notification.
Keep it concise but informative.
""".trimIndent()

            Log.d(TAG, "Asking Claude to format the summary...")

            // Create request to Claude (NO tools this time - just formatting)
            val request = ChatRequest(
                system = null,
                messages = listOf(
                    MessageDto(
                        role = "user",
                        content = formattingPrompt
                    )
                ),
                maxTokens = 2048,
                temperature = 0.7,
                tools = null  // No tools - we already have the data
            )

            // Call Claude API
            val response = chatApiService.sendMessage(request)

            // Extract text response (after potential tool use)
            val summaryText = extractTextFromResponse(response.content)

            if (summaryText.isNotBlank()) {
                Log.d(TAG, "Summary generated: ${summaryText.take(100)}...")

                // Send to all configured push channels
                val pushManager = createPushChannelManager()
                val results = pushManager.sendToAll(summaryText)

                // Log results
                results.forEach { (channel, result) ->
                    if (result.isSuccess) {
                        Log.d(TAG, "✅ Successfully sent to $channel")
                    } else {
                        Log.e(TAG, "❌ Failed to send to $channel: ${result.exceptionOrNull()?.message}")
                    }
                }

                Log.d(TAG, "Daily summary sent to ${results.filter { it.value.isSuccess }.size}/${results.size} channels")
            } else {
                Log.d(TAG, "No summary generated")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Daily summary error: ${e.message}", e)
            Result.failure()
        }
    }

    private fun extractTextFromResponse(content: Any): String {
        return when (content) {
            is String -> content
            is List<*> -> {
                content.mapNotNull { block ->
                    // ContentDto objects
                    if (block is ContentDto) {
                        if (block.type == "text") {
                            block.text
                        } else null
                    } else {
                        // Fallback for other types
                        block.toString()
                    }
                }.joinToString("\n")
            }
            else -> content.toString()
        }
    }

    private fun createPushChannelManager(): PushChannelManager {
        val channels = listOf(
            // Always enabled: Android Notification
            NotificationPushChannel(context),

            // Optional: Email (configure in app settings)
            // EmailPushChannel(
            //     emailAddress = "your@email.com",
            //     smtpConfig = EmailConfig(...)
            // ),

            // Optional: Telegram (configure bot token and chat ID)
            // TelegramPushChannel(
            //     botToken = "YOUR_BOT_TOKEN",
            //     chatId = "YOUR_CHAT_ID"
            // ),

            // Optional: Webhook
            // WebhookPushChannel(
            //     webhookUrl = "https://your-webhook-url.com/summary"
            // )
        )

        return PushChannelManager(channels)
    }
}
