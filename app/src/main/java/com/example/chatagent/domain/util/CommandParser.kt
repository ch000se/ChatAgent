package com.example.chatagent.domain.util

import com.example.chatagent.domain.model.Command
import com.example.chatagent.domain.model.GitSubcommand

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
