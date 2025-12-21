package com.example.chatagent.domain.model


data class McpServerInfo(
    val name: String,
    val url: String,
    val description: String,
    val requiresAuth: Boolean = false,
    val protocol: String = "HTTP" // HTTP или SSE
)

object McpServers {
    val allServers = listOf(
        McpServerInfo(
            name = "Kiwi.com Flight Search",
            url = "https://mcp.kiwi.com",
            description = "✅ FREE - Search and book flights using Kiwi.com search engine",
            requiresAuth = false,
            protocol = "SSE"
        ),
        McpServerInfo(
            name = "Cloudflare Demo Day MCP",
            url = "https://demo-day.mcp.cloudflare.com/sse",
            description = "Cloudflare public MCP server (may be offline)",
            requiresAuth = false,
            protocol = "SSE"
        ),
        McpServerInfo(
            name = "PayPal MCP Server",
            url = "https://mcp.paypal.com/sse",
            description = "PayPal MCP server (requires authentication)",
            requiresAuth = false,
            protocol = "SSE"
        ),
        McpServerInfo(
            name = "Localhost Reminder Server (LAN)",
            url = "http://192.168.0.100:3000/mcp",
            description = "✅ Local Reminder Server - Run: python mcp_reminder_server.py",
            requiresAuth = false,
            protocol = "HTTP"
        ),
        McpServerInfo(
            name = "Localhost Reminder (Emulator)",
            url = "http://10.0.2.2:3000/mcp",
            description = "For Android Emulator only - Run: python mcp_reminder_server.py",
            requiresAuth = false,
            protocol = "HTTP"
        ),
        McpServerInfo(
            name = "Localhost Port 8080",
            url = "http://10.0.2.2:8080/mcp",
            description = "Alternative local port - use if 3000 is busy",
            requiresAuth = false,
            protocol = "HTTP"
        ),
        McpServerInfo(
            name = "Microsoft Learn MCP",
            url = "https://learn.microsoft.com/api/mcp",
            description = "Official Microsoft server - uses SSE",
            requiresAuth = false,
            protocol = "SSE"
        ),
        McpServerInfo(
            name = "GitHub Copilot MCP",
            url = "https://api.githubcopilot.com/mcp/",
            description = "Requires GitHub authentication",
            requiresAuth = true,
            protocol = "HTTP"
        )
    )

    fun getPublicServers() = allServers.filter { !it.requiresAuth }

    fun getHttpServers() = allServers.filter { !it.requiresAuth && it.protocol == "HTTP" }
}