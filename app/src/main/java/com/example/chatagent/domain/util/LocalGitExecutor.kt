package com.example.chatagent.domain.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes git commands locally without MCP
 * Fallback when MCP is not available
 */
@Singleton
class LocalGitExecutor @Inject constructor() {

    companion object {
        private const val TAG = "LocalGitExecutor"
    }

    /**
     * Execute git command and return output
     */
    suspend fun executeGitCommand(
        vararg args: String,
        workingDir: File? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val command = listOf("git") + args
            Log.d(TAG, "Executing: ${command.joinToString(" ")}")

            val processBuilder = ProcessBuilder(command)

            if (workingDir != null && workingDir.exists()) {
                processBuilder.directory(workingDir)
            }

            processBuilder.redirectErrorStream(true)

            val process = processBuilder.start()
            val output = StringBuilder()

            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
            }

            val exitCode = process.waitFor()

            if (exitCode == 0) {
                val result = output.toString().trim()
                Log.d(TAG, "Command succeeded: $result")
                Result.success(result)
            } else {
                val error = "Git command failed with exit code $exitCode: ${output.toString().trim()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing git command", e)
            Result.failure(e)
        }
    }

    /**
     * Check if git is available on the system
     */
    suspend fun isGitAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("git", "--version").start()
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Git not available", e)
            false
        }
    }

    /**
     * Get current git branch
     */
    suspend fun getCurrentBranch(workingDir: File? = null): Result<String> {
        return executeGitCommand("rev-parse", "--abbrev-ref", "HEAD", workingDir = workingDir)
    }

    /**
     * Get git status
     */
    suspend fun getStatus(workingDir: File? = null): Result<String> {
        return executeGitCommand("status", "--short", "--branch", workingDir = workingDir)
    }

    /**
     * Get git log
     */
    suspend fun getLog(count: Int = 10, workingDir: File? = null): Result<String> {
        return executeGitCommand("log", "--oneline", "-$count", workingDir = workingDir)
    }

    /**
     * Get git diff
     */
    suspend fun getDiff(workingDir: File? = null): Result<String> {
        return executeGitCommand("diff", "--stat", workingDir = workingDir)
    }

    /**
     * Get git branches
     */
    suspend fun getBranches(workingDir: File? = null): Result<String> {
        return executeGitCommand("branch", "-a", workingDir = workingDir)
    }

    /**
     * Get remote info
     */
    suspend fun getRemote(workingDir: File? = null): Result<String> {
        return executeGitCommand("remote", "-v", workingDir = workingDir)
    }
}
