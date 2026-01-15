package com.example.chatagent.domain.model

data class SupportTicket(
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val category: SupportCategory,
    val status: TicketStatus,
    val priority: TicketPriority,
    val createdAt: Long,
    val updatedAt: Long,
    val resolvedAt: Long? = null,
    val assignedTo: String? = null,
    val tags: List<String> = emptyList(),
    val relatedIssues: List<String> = emptyList()
)

data class SupportUser(
    val id: String,
    val email: String,
    val name: String,
    val subscription: SubscriptionType,
    val registeredAt: Long,
    val lastActiveAt: Long,
    val totalTickets: Int = 0,
    val resolvedTickets: Int = 0,
    val metadata: Map<String, String> = emptyMap()
)

enum class SupportCategory {
    AUTHENTICATION,
    RAG_SEARCH,
    MCP_INTEGRATION,
    COMMANDS,
    PERFORMANCE,
    DATABASE,
    PR_REVIEW,
    CONFIGURATION,
    BUG_REPORT,
    FEATURE_REQUEST,
    OTHER
}

enum class TicketStatus {
    NEW,
    OPEN,
    IN_PROGRESS,
    WAITING_FOR_USER,
    RESOLVED,
    CLOSED,
    REOPENED
}

enum class TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class SubscriptionType {
    FREE,
    BASIC,
    PRO,
    ENTERPRISE
}

data class SupportResponse(
    val ticketId: String,
    val answer: String,
    val suggestedSolutions: List<String>,
    val relevantDocs: List<DocumentSearchResult>,
    val confidence: Float,
    val requiresHumanSupport: Boolean,
    val estimatedResolutionTime: String? = null,
    val relatedTickets: List<SupportTicket> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
)

data class SupportQuery(
    val query: String,
    val userId: String? = null,
    val ticketId: String? = null,
    val category: SupportCategory? = null,
    val includeHistory: Boolean = true,
    val maxResults: Int = 5
)

data class SupportStats(
    val totalTickets: Int,
    val openTickets: Int,
    val resolvedTickets: Int,
    val averageResolutionTime: Long,
    val ticketsByCategory: Map<SupportCategory, Int>,
    val ticketsByPriority: Map<TicketPriority, Int>,
    val topIssues: List<String>
)
