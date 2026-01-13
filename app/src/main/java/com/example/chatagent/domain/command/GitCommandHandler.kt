package com.example.chatagent.domain.command

import android.content.Context
import com.example.chatagent.data.remote.client.McpClient
import com.example.chatagent.domain.model.Command
import com.example.chatagent.domain.model.CommandMetadata
import com.example.chatagent.domain.model.CommandResult
import com.example.chatagent.domain.model.GitSubcommand
import com.example.chatagent.domain.util.LocalGitExecutor
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class GitCommandHandler @Inject constructor(
    private val mcpClient: McpClient,
    private val localGitExecutor: LocalGitExecutor,
    @ApplicationContext private val context: Context
) : CommandHandler<Command.Git> {

    override suspend fun handle(command: Command.Git): CommandResult {
        val startTime = System.currentTimeMillis()

        // Try local git first (fallback when MCP not available)
        val connectionState = mcpClient.connectionState.value
        val useMcp = connectionState is McpClient.ConnectionState.Connected

        return try {
            val gitOutput = when (command.subcommand) {
                GitSubcommand.Status -> if (useMcp) executeGitStatus() else executeLocalGitStatus()
                GitSubcommand.Log -> if (useMcp) executeGitLog() else executeLocalGitLog()
                GitSubcommand.Diff -> if (useMcp) executeGitDiff() else executeLocalGitDiff()
                GitSubcommand.Branch -> if (useMcp) executeGitBranch() else executeLocalGitBranch()
            }

            CommandResult(
                command = command,
                content = gitOutput,
                success = true,
                metadata = CommandMetadata(
                    sources = null,
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    commandType = "git",
                    matchCount = null
                )
            )
        } catch (e: Exception) {
            CommandResult(
                command = command,
                content = "❌ Помилка виконання git команди: ${e.message}\n\n" +
                        "💡 Переконайтеся що:\n" +
                        "- Git встановлено в системі\n" +
                        "- Ви знаходитесь в git репозиторії\n" +
                        "- MCP server запущено (опціонально)",
                success = false,
                error = e.message,
                metadata = CommandMetadata(
                    sources = null,
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    commandType = "git",
                    matchCount = 0
                )
            )
        }
    }

    override fun canHandle(command: Command): Boolean {
        return command is Command.Git
    }

    private suspend fun executeGitStatus(): String {
        // Get detailed status including branch and remote info
        val statusResult = mcpClient.callTool(
            toolName = "execute_command",
            arguments = mapOf(
                "command" to "git",
                "args" to listOf("status", "--short", "--branch")
            )
        )

        val branchResult = mcpClient.callTool(
            toolName = "execute_command",
            arguments = mapOf(
                "command" to "git",
                "args" to listOf("rev-parse", "--abbrev-ref", "HEAD")
            )
        )

        val remoteResult = mcpClient.callTool(
            toolName = "execute_command",
            arguments = mapOf(
                "command" to "git",
                "args" to listOf("remote", "-v")
            )
        )

        return statusResult.fold(
            onSuccess = { callResult ->
                val output = callResult.content.joinToString("\n") { it.text ?: "" }
                val branch = branchResult.getOrNull()?.content?.firstOrNull()?.text?.trim() ?: "unknown"
                val remote = remoteResult.getOrNull()?.content?.firstOrNull()?.text?.trim() ?: "none"
                formatGitStatus(output, branch, remote)
            },
            onFailure = { error ->
                "Git error: ${error.message}\n\nMake sure the MCP server supports command execution."
            }
        )
    }

    private suspend fun executeGitLog(): String {
        val result = mcpClient.callTool(
            toolName = "execute_command",
            arguments = mapOf(
                "command" to "git",
                "args" to listOf("log", "--oneline", "-10")
            )
        )

        return result.fold(
            onSuccess = { callResult ->
                val output = callResult.content.joinToString("\n") { it.text ?: "" }
                formatGitLog(output)
            },
            onFailure = { error ->
                "Git error: ${error.message}"
            }
        )
    }

    private suspend fun executeGitDiff(): String {
        val result = mcpClient.callTool(
            toolName = "execute_command",
            arguments = mapOf(
                "command" to "git",
                "args" to listOf("diff", "--stat")
            )
        )

        return result.fold(
            onSuccess = { callResult ->
                val output = callResult.content.joinToString("\n") { it.text ?: "" }
                formatGitDiff(output)
            },
            onFailure = { error ->
                "Git error: ${error.message}"
            }
        )
    }

    private suspend fun executeGitBranch(): String {
        val result = mcpClient.callTool(
            toolName = "execute_command",
            arguments = mapOf(
                "command" to "git",
                "args" to listOf("branch", "-a")
            )
        )

        return result.fold(
            onSuccess = { callResult ->
                val output = callResult.content.joinToString("\n") { it.text ?: "" }
                formatGitBranch(output)
            },
            onFailure = { error ->
                "Git error: ${error.message}"
            }
        )
    }

    private fun formatGitStatus(output: String, branch: String, remote: String): String {
        val status = if (output.isBlank()) "Working tree is clean" else output

        return """
            ## 🔀 Git Status

            **Поточна гілка:** `$branch`

            ${if (remote != "none") "**Remote:** `${remote.lines().firstOrNull() ?: "none"}`\n" else ""}
            **Статус:**
            ```
            $status
            ```
        """.trimIndent()
    }

    private fun formatGitLog(output: String): String {
        if (output.isBlank()) {
            return "## Recent Commits\n\nNo commits found."
        }

        return """
            ## Recent Commits

            ```
            $output
            ```
        """.trimIndent()
    }

    private fun formatGitDiff(output: String): String {
        if (output.isBlank()) {
            return "## Git Diff\n\nNo changes to show."
        }

        return """
            ## Git Diff (Summary)

            ```
            $output
            ```
        """.trimIndent()
    }

    private fun formatGitBranch(output: String): String {
        if (output.isBlank()) {
            return "## Git Branches\n\nNo branches found."
        }

        return """
            ## Git Branches

            ```
            $output
            ```
        """.trimIndent()
    }

    // ===== LOCAL GIT METHODS (without MCP) =====

    private suspend fun executeLocalGitStatus(): String {
        val workingDir = getGitWorkingDir()

        val statusResult = localGitExecutor.getStatus(workingDir)
        val branchResult = localGitExecutor.getCurrentBranch(workingDir)
        val remoteResult = localGitExecutor.getRemote(workingDir)

        return statusResult.fold(
            onSuccess = { output ->
                val branch = branchResult.getOrNull() ?: "unknown"
                val remote = remoteResult.getOrNull() ?: "none"
                formatGitStatus(output, branch, remote) + "\n\n💻 **Mode:** Local Git (без MCP)"
            },
            onFailure = { error ->
                buildGitUnavailableMessage()
            }
        )
    }

    private suspend fun executeLocalGitLog(): String {
        val workingDir = getGitWorkingDir()

        return localGitExecutor.getLog(10, workingDir).fold(
            onSuccess = { output ->
                formatGitLog(output) + "\n\n💻 **Mode:** Local Git (без MCP)"
            },
            onFailure = { error ->
                "❌ Помилка git log: ${error.message}"
            }
        )
    }

    private suspend fun executeLocalGitDiff(): String {
        val workingDir = getGitWorkingDir()

        return localGitExecutor.getDiff(workingDir).fold(
            onSuccess = { output ->
                formatGitDiff(output) + "\n\n💻 **Mode:** Local Git (без MCP)"
            },
            onFailure = { error ->
                "❌ Помилка git diff: ${error.message}"
            }
        )
    }

    private suspend fun executeLocalGitBranch(): String {
        val workingDir = getGitWorkingDir()

        return localGitExecutor.getBranches(workingDir).fold(
            onSuccess = { output ->
                formatGitBranch(output) + "\n\n💻 **Mode:** Local Git (без MCP)"
            },
            onFailure = { error ->
                "❌ Помилка git branch: ${error.message}"
            }
        )
    }

    /**
     * Try to find git working directory
     * On Android, external storage path might contain the repo
     */
    private fun getGitWorkingDir(): File? {
        // Try to use /sdcard/AndroidStudioProjects/ChatAgent or similar
        // On Android emulator/device this might not work, returns null for default
        val possiblePaths = listOf(
            File("/sdcard/AndroidStudioProjects/ChatAgent"),
            File("/storage/emulated/0/AndroidStudioProjects/ChatAgent"),
            File(context.getExternalFilesDir(null), "../../..").canonicalFile
        )

        return possiblePaths.firstOrNull { it.exists() && File(it, ".git").exists() }
    }

    private fun buildGitUnavailableMessage(): String {
        return """
            ## 🔀 Git команди недоступні

            Git не встановлено на цьому пристрої (Android).

            **Рішення:**
            1. Запустіть Git MCP Server на вашому ПК:
               ```
               cd mcp_servers
               start_git.bat   (Windows)
               python git_server.py   (Linux/Mac)
               ```

            2. В додатку підключіться до MCP:
               - Меню → MCP Tools
               - Emulator: `http://10.0.2.2:3002`
               - Device: `http://YOUR_PC_IP:3002`

            3. Спробуйте команду знову

            **Альтернатива:**
            Використовуйте інші команди (працюють без Git):
            - `/help` - AI асистент з RAG
            - `/code` - пошук в коді
            - `/docs` - пошук в документації
            - `/project` - огляд проекту

            📖 Детальна інструкція: START_HERE.md
        """.trimIndent()
    }

    private fun buildMcpNotConnectedResponse(): String {
        return """
            ## Git commands unavailable

            MCP client is not connected. Git operations require MCP server integration.

            Please connect to an MCP server that supports command execution:
            1. Go to MCP screen
            2. Connect to a server (e.g., filesystem server)
            3. Try the /git command again
        """.trimIndent()
    }
}
