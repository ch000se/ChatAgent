package com.example.chatagent.domain.command

import com.example.chatagent.domain.model.Command
import com.example.chatagent.domain.model.CommandMetadata
import com.example.chatagent.domain.model.CommandResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandDispatcher @Inject constructor(
    private val helpHandler: HelpCommandHandler,
    private val codeHandler: CodeSearchCommandHandler,
    private val docsHandler: DocsCommandHandler,
    private val gitHandler: GitCommandHandler,
    private val projectInfoHandler: ProjectInfoCommandHandler,
    private val supportHandler: SupportCommandHandler,
    private val teamHandler: TeamCommandHandler
) {

    suspend fun dispatch(command: Command): CommandResult {
        return when (command) {
            is Command.Help -> helpHandler.handle(command)
            is Command.Code -> codeHandler.handle(command)
            is Command.Docs -> docsHandler.handle(command)
            is Command.Git -> gitHandler.handle(command)
            is Command.ProjectInfo -> projectInfoHandler.handle(command)
            is Command.Support -> supportHandler.handle(command)
            is Command.Team -> teamHandler.handle(command)
            is Command.Unknown -> handleUnknownCommand(command)
        }
    }

    private fun handleUnknownCommand(command: Command.Unknown): CommandResult {
        return CommandResult(
            command = command,
            content = """
                ## Невідома команда: ${command.rawInput}

                📋 Доступні команди:
                - `/help [запит]` - AI асистент з RAG пошуком документації
                - `/code <запит>` - Пошук фрагментів коду
                - `/docs <запит>` - Пошук тільки в документації
                - `/git [status|log|diff|branch]` - Git операції через MCP
                - `/project` - Інформація про проект та архітектуру
                - `/support <ticket-id|запит>` - AI асистент підтримки з контекстом
                - `/team <action> [params]` - Командний асистент для управління задачами

                💡 Приклади:
                - `/help як працює RAG`
                - `/code ChatRepository`
                - `/docs quickstart`
                - `/git status`
                - `/project`
                - `/support ticket-001`
                - `/team status`
                - `/team tasks priority high`
                - `/team priority`
            """.trimIndent(),
            success = false,
            error = "Unknown command",
            metadata = CommandMetadata(
                sources = null,
                executionTimeMs = 0,
                commandType = "unknown",
                matchCount = 0
            )
        )
    }
}
