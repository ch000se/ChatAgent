package com.example.chatagent.domain.command

import com.example.chatagent.domain.model.Command
import com.example.chatagent.domain.model.CommandResult

interface CommandHandler<T : Command> {
    suspend fun handle(command: T): CommandResult
    fun canHandle(command: Command): Boolean
}
