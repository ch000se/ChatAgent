package com.example.chatagent.domain.model

sealed class Command {
    abstract val rawInput: String

    data class Help(
        override val rawInput: String,
        val query: String
    ) : Command()

    data class Code(
        override val rawInput: String,
        val query: String
    ) : Command()

    data class Docs(
        override val rawInput: String,
        val query: String
    ) : Command()

    data class Git(
        override val rawInput: String,
        val subcommand: GitSubcommand = GitSubcommand.Status
    ) : Command()

    data class ProjectInfo(
        override val rawInput: String
    ) : Command()

    data class Support(
        override val rawInput: String,
        val ticketIdOrQuery: String
    ) : Command()

    data class Unknown(
        override val rawInput: String
    ) : Command()
}

enum class GitSubcommand {
    Status,
    Log,
    Diff,
    Branch
}

data class CommandResult(
    val command: Command,
    val content: String,
    val success: Boolean,
    val metadata: CommandMetadata? = null,
    val error: String? = null
)

data class CommandMetadata(
    val sources: List<DocumentSearchResult>? = null,
    val executionTimeMs: Long? = null,
    val commandType: String,
    val matchCount: Int? = null
)
