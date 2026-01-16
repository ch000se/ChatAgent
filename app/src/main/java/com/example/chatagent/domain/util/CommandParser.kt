package com.example.chatagent.domain.util

import com.example.chatagent.domain.model.Command
import com.example.chatagent.domain.model.GitSubcommand
import com.example.chatagent.domain.model.TeamAction

object CommandParser {

    private val COMMAND_REGEX = Regex("^/([a-z]+)(?:\\s+(.*))?$", RegexOption.IGNORE_CASE)

    /**
     * Parses user input into a Command object
     * Returns null if input is not a command (doesn't start with /)
     */
    fun parse(input: String): Command? {
        val trimmed = input.trim()
        if (!trimmed.startsWith("/")) return null

        val matchResult = COMMAND_REGEX.matchEntire(trimmed)
            ?: return Command.Unknown(trimmed)

        val commandName = matchResult.groupValues[1].lowercase()
        val args = matchResult.groupValues.getOrNull(2)?.trim() ?: ""

        return when (commandName) {
            "help" -> Command.Help(
                rawInput = trimmed,
                query = args.ifEmpty { "project overview" }
            )

            "code" -> {
                if (args.isEmpty()) {
                    Command.Unknown(trimmed) // /code requires a query
                } else {
                    Command.Code(rawInput = trimmed, query = args)
                }
            }

            "docs" -> {
                if (args.isEmpty()) {
                    Command.Unknown(trimmed)
                } else {
                    Command.Docs(rawInput = trimmed, query = args)
                }
            }

            "git" -> {
                val subcommand = parseGitSubcommand(args)
                Command.Git(rawInput = trimmed, subcommand = subcommand)
            }

            "project", "info" -> Command.ProjectInfo(rawInput = trimmed)

            "support" -> {
                if (args.isEmpty()) {
                    Command.Unknown(trimmed) // /support requires ticket ID or query
                } else {
                    Command.Support(rawInput = trimmed, ticketIdOrQuery = args)
                }
            }

            "team" -> {
                val (action, params) = parseTeamAction(args)
                Command.Team(rawInput = trimmed, action = action, params = params)
            }

            else -> Command.Unknown(trimmed)
        }
    }

    private fun parseGitSubcommand(args: String): GitSubcommand {
        return when (args.lowercase().trim()) {
            "log", "history" -> GitSubcommand.Log
            "diff", "changes" -> GitSubcommand.Diff
            "branch", "branches" -> GitSubcommand.Branch
            else -> GitSubcommand.Status // default
        }
    }

    private fun parseTeamAction(args: String): Pair<TeamAction, String> {
        val parts = args.trim().split(Regex("\\s+"), limit = 2)
        val actionStr = parts.getOrNull(0)?.lowercase() ?: ""
        val params = parts.getOrNull(1) ?: ""

        val action = when (actionStr) {
            "status", "stat", "overview" -> TeamAction.STATUS
            "tasks", "task", "list" -> TeamAction.TASKS
            "priority", "priorities", "prio" -> TeamAction.PRIORITY
            "create", "add", "new" -> TeamAction.CREATE
            "update", "edit", "modify" -> TeamAction.UPDATE
            "roadmap", "milestones", "plan" -> TeamAction.ROADMAP
            "blockers", "blocked", "blocks" -> TeamAction.BLOCKERS
            "deadlines", "due", "upcoming" -> TeamAction.DEADLINES
            "workload", "load", "capacity" -> TeamAction.WORKLOAD
            "stats", "statistics", "metrics" -> TeamAction.STATS
            "help", "?" -> TeamAction.HELP
            else -> {
                // If no action specified, treat entire args as params for default status
                if (actionStr.isEmpty()) {
                    return Pair(TeamAction.STATUS, "")
                }
                // If action is not recognized, might be a natural language query
                return Pair(TeamAction.TASKS, args)
            }
        }

        return Pair(action, params)
    }

    /**
     * Checks if input is a command without full parsing
     */
    fun isCommand(input: String): Boolean {
        return input.trim().startsWith("/")
    }

    /**
     * Returns list of available commands for autocomplete/help
     */
    fun getAvailableCommands(): List<CommandInfo> {
        return listOf(
            CommandInfo(
                name = "/help",
                description = "Search project documentation and get help",
                usage = "/help [query]",
                examples = listOf(
                    "/help",
                    "/help how to build",
                    "/help RAG implementation"
                )
            ),
            CommandInfo(
                name = "/code",
                description = "Search code fragments and implementations",
                usage = "/code <query>",
                examples = listOf(
                    "/code chat repository",
                    "/code mcp client",
                    "/code rag search"
                )
            ),
            CommandInfo(
                name = "/docs",
                description = "Search documentation files only",
                usage = "/docs <query>",
                examples = listOf(
                    "/docs build instructions",
                    "/docs RAG quickstart"
                )
            ),
            CommandInfo(
                name = "/git",
                description = "Show git repository information",
                usage = "/git [status|log|diff|branch]",
                examples = listOf(
                    "/git",
                    "/git log",
                    "/git diff"
                )
            ),
            CommandInfo(
                name = "/project",
                description = "Show project overview and architecture info",
                usage = "/project",
                examples = listOf(
                    "/project"
                )
            ),
            CommandInfo(
                name = "/support",
                description = "Get support assistance with intelligent context-aware responses",
                usage = "/support <ticket-id|query>",
                examples = listOf(
                    "/support ticket-001",
                    "/support Why doesn't authentication work?",
                    "/support RAG search issues"
                )
            ),
            CommandInfo(
                name = "/team",
                description = "Team assistant for task management, project status, and priority recommendations",
                usage = "/team <action> [params]",
                examples = listOf(
                    "/team status",
                    "/team tasks priority high",
                    "/team priority",
                    "/team create Implement user authentication",
                    "/team blockers",
                    "/team roadmap",
                    "/team deadlines",
                    "/team workload",
                    "/team stats"
                )
            )
        )
    }
}

data class CommandInfo(
    val name: String,
    val description: String,
    val usage: String,
    val examples: List<String>
)
