package com.example.chatagent.domain.command

import android.content.Context
import android.util.Log
import com.example.chatagent.data.local.dao.SupportTicketDao
import com.example.chatagent.data.local.dao.SupportUserDao
import com.example.chatagent.data.mapper.SupportMapper
import com.example.chatagent.data.remote.api.ChatApiService
import com.example.chatagent.data.remote.dto.ChatRequest
import com.example.chatagent.data.remote.dto.MessageDto
import com.example.chatagent.domain.model.*
import com.example.chatagent.domain.usecase.SearchDocumentsUseCase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class SupportCommandHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val searchDocumentsUseCase: SearchDocumentsUseCase,
    private val chatApiService: ChatApiService,
    private val supportTicketDao: SupportTicketDao,
    private val supportUserDao: SupportUserDao
) : CommandHandler<Command.Support> {

    companion object {
        private const val TAG = "SupportCommandHandler"
        private const val CRM_DATA_FILE = "support_crm_data.json"
        private const val FAQ_SEARCH_QUERY_PREFIX = "support faq"
    }

    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override suspend fun handle(command: Command.Support): CommandResult {
        val startTime = System.currentTimeMillis()

        return try {
            Log.d(TAG, "Processing /support command: '${command.ticketIdOrQuery}'")

            // Load CRM data from JSON (simulates external CRM system)
            val crmData = loadCrmData()

            // Determine if input is ticket ID or general query
            val isTicketId = command.ticketIdOrQuery.startsWith("ticket-")

            val supportContext = if (isTicketId) {
                // Load specific ticket and user context
                buildTicketContext(command.ticketIdOrQuery, crmData)
            } else {
                // General support query
                buildGeneralQueryContext(command.ticketIdOrQuery, crmData)
            }

            // Search FAQ documentation via RAG
            val faqResults = searchDocumentsUseCase(
                query = "$FAQ_SEARCH_QUERY_PREFIX ${command.ticketIdOrQuery}",
                topK = 3
            ).getOrNull() ?: emptyList()

            Log.d(TAG, "Found ${faqResults.size} FAQ results")

            // Build documentation context from RAG
            val faqContext = buildFaqContext(faqResults)

            // Call Claude API with support assistant prompt
            val aiResponse = callSupportAssistantAI(
                query = command.ticketIdOrQuery,
                supportContext = supportContext,
                faqContext = faqContext
            )

            Log.d(TAG, "Support response generated successfully")

            CommandResult(
                command = command,
                content = aiResponse,
                success = true,
                metadata = CommandMetadata(
                    sources = faqResults,
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    commandType = "support",
                    matchCount = faqResults.size
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in support command", e)
            CommandResult(
                command = command,
                content = buildErrorResponse(e),
                success = false,
                error = e.message,
                metadata = CommandMetadata(
                    sources = null,
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    commandType = "support",
                    matchCount = 0
                )
            )
        }
    }

    private fun loadCrmData(): CrmData {
        return try {
            val jsonString = context.assets.open(CRM_DATA_FILE)
                .bufferedReader()
                .use { it.readText() }

            gson.fromJson(jsonString, CrmData::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load CRM data", e)
            // Return empty CRM data if file not found
            CrmData(
                users = emptyList(),
                tickets = emptyList(),
                metadata = CrmMetadata(
                    version = "1.0.0",
                    lastUpdated = System.currentTimeMillis(),
                    totalUsers = 0,
                    totalTickets = 0,
                    openTickets = 0,
                    inProgressTickets = 0,
                    resolvedTickets = 0
                )
            )
        }
    }

    private fun buildTicketContext(ticketId: String, crmData: CrmData): String {
        val ticket = crmData.tickets.find { it.id == ticketId }
            ?: return "⚠️ Ticket $ticketId not found in CRM system."

        val user = crmData.users.find { it.id == ticket.userId }
        val relatedTickets = ticket.relatedIssues.mapNotNull { relatedId ->
            crmData.tickets.find { it.id == relatedId }
        }

        val builder = StringBuilder()
        builder.append("=== TICKET CONTEXT (from CRM) ===\n\n")

        // Ticket Details
        builder.append("📋 Ticket Information:\n")
        builder.append("  ID: ${ticket.id}\n")
        builder.append("  Title: ${ticket.title}\n")
        builder.append("  Description: ${ticket.description}\n")
        builder.append("  Category: ${ticket.category}\n")
        builder.append("  Status: ${ticket.status}\n")
        builder.append("  Priority: ${ticket.priority}\n")
        builder.append("  Created: ${dateFormat.format(Date(ticket.createdAt))}\n")
        builder.append("  Updated: ${dateFormat.format(Date(ticket.updatedAt))}\n")

        if (ticket.resolvedAt != null) {
            builder.append("  Resolved: ${dateFormat.format(Date(ticket.resolvedAt))}\n")
        }

        if (ticket.assignedTo != null) {
            builder.append("  Assigned to: ${ticket.assignedTo}\n")
        }

        if (ticket.tags.isNotEmpty()) {
            builder.append("  Tags: ${ticket.tags.joinToString(", ")}\n")
        }

        builder.append("\n")

        // User Information
        if (user != null) {
            builder.append("👤 User Information:\n")
            builder.append("  Name: ${user.name}\n")
            builder.append("  Email: ${user.email}\n")
            builder.append("  Subscription: ${user.subscription}\n")
            builder.append("  Total Tickets: ${user.totalTickets}\n")
            builder.append("  Resolved Tickets: ${user.resolvedTickets}\n")
            builder.append("  Registered: ${dateFormat.format(Date(user.registeredAt))}\n")

            if (user.metadata.isNotEmpty()) {
                builder.append("  Additional Info:\n")
                user.metadata.forEach { (key, value) ->
                    builder.append("    - $key: $value\n")
                }
            }
            builder.append("\n")
        }

        // Related Issues
        if (relatedTickets.isNotEmpty()) {
            builder.append("🔗 Related Tickets:\n")
            relatedTickets.forEach { related ->
                builder.append("  - ${related.id}: ${related.title} [${related.status}]\n")
            }
            builder.append("\n")
        }

        builder.append("=== END OF TICKET CONTEXT ===\n")
        return builder.toString()
    }

    private fun buildGeneralQueryContext(query: String, crmData: CrmData): String {
        // Find similar tickets based on keywords
        val keywords = query.lowercase().split(" ").filter { it.length > 3 }
        val similarTickets = crmData.tickets.filter { ticket ->
            keywords.any { keyword ->
                ticket.title.lowercase().contains(keyword) ||
                ticket.description.lowercase().contains(keyword) ||
                ticket.tags.any { tag -> tag.lowercase().contains(keyword) }
            }
        }.take(3)

        val builder = StringBuilder()
        builder.append("=== SUPPORT CONTEXT (from CRM) ===\n\n")

        builder.append("📊 CRM Statistics:\n")
        builder.append("  Total Tickets: ${crmData.metadata.totalTickets}\n")
        builder.append("  Open Tickets: ${crmData.metadata.openTickets}\n")
        builder.append("  In Progress: ${crmData.metadata.inProgressTickets}\n")
        builder.append("  Resolved: ${crmData.metadata.resolvedTickets}\n")
        builder.append("  Total Users: ${crmData.metadata.totalUsers}\n")
        builder.append("\n")

        if (similarTickets.isNotEmpty()) {
            builder.append("🔍 Similar Reported Issues:\n")
            similarTickets.forEach { ticket ->
                builder.append("  - ${ticket.id}: ${ticket.title}\n")
                builder.append("    Status: ${ticket.status} | Priority: ${ticket.priority}\n")
                builder.append("    Category: ${ticket.category}\n")
                if (ticket.status == "RESOLVED") {
                    builder.append("    ✓ Resolved on ${dateFormat.format(Date(ticket.resolvedAt!!))}\n")
                }
                builder.append("\n")
            }
        }

        builder.append("=== END OF SUPPORT CONTEXT ===\n")
        return builder.toString()
    }

    private fun buildFaqContext(results: List<DocumentSearchResult>): String {
        if (results.isEmpty()) {
            return "⚠️ No FAQ documentation found. Using general knowledge only."
        }

        val builder = StringBuilder()
        builder.append("=== FAQ DOCUMENTATION (from RAG search) ===\n\n")

        results.forEachIndexed { index, result ->
            val similarity = (result.similarity * 100).toInt()
            builder.append("--- FAQ ${index + 1} (relevance: $similarity%) ---\n")
            builder.append("${result.chunk.text}\n\n")
        }

        builder.append("=== END OF FAQ DOCUMENTATION ===\n")
        return builder.toString()
    }

    private suspend fun callSupportAssistantAI(
        query: String,
        supportContext: String,
        faqContext: String
    ): String {
        val userMessage = """
            User Query/Ticket: $query

            $supportContext

            $faqContext

            Please provide a comprehensive support response following the format:
            🎫 Ticket Context
            📊 User Information (if available)
            💡 Solution
            📚 Related Documentation
            ⚠️ Prevention Tips
            🔗 Related Issues (if any)
        """.trimIndent()

        val request = ChatRequest(
            model = "claude-sonnet-4-5-20250929",
            system = SystemPrompts.SUPPORT_ASSISTANT.prompt,
            messages = listOf(
                MessageDto(role = "user", content = userMessage)
            ),
            maxTokens = 2048,
            temperature = 0.4 // Balanced between factual and helpful
        )

        val response = chatApiService.sendMessage(request)
        return response.content.firstOrNull()?.text
            ?: "❌ Failed to generate support response. Please try again."
    }

    override fun canHandle(command: Command): Boolean {
        return command is Command.Support
    }

    private fun buildErrorResponse(error: Exception): String {
        return """
            ❌ Support System Error

            An error occurred while processing your support request:
            ${error.message}

            💡 What you can do:
            1. Try rephrasing your question
            2. Use a specific ticket ID: /support ticket-001
            3. Search FAQ directly: /docs support
            4. Check your network connection

            If the problem persists, please contact support at: support@example.com

            Available commands:
            - /support <ticket-id> - Get info about specific ticket
            - /support <question> - Ask general support question
            - /docs support - Search support documentation
            - /help - Get general help
        """.trimIndent()
    }
}

// Data classes for CRM JSON parsing
private data class CrmData(
    val users: List<CrmUser>,
    val tickets: List<CrmTicket>,
    val metadata: CrmMetadata
)

private data class CrmUser(
    val id: String,
    val email: String,
    val name: String,
    val subscription: String,
    val registeredAt: Long,
    val lastActiveAt: Long,
    val totalTickets: Int,
    val resolvedTickets: Int,
    val metadata: Map<String, String>
)

private data class CrmTicket(
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val category: String,
    val status: String,
    val priority: String,
    val createdAt: Long,
    val updatedAt: Long,
    val resolvedAt: Long?,
    val assignedTo: String?,
    val tags: List<String>,
    val relatedIssues: List<String>
)

private data class CrmMetadata(
    val version: String,
    val lastUpdated: Long,
    val totalUsers: Int,
    val totalTickets: Int,
    val openTickets: Int,
    val inProgressTickets: Int,
    val resolvedTickets: Int
)
