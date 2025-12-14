package com.example.chatagent.data.mapper

import com.example.chatagent.data.local.entity.MessageEntity
import com.example.chatagent.domain.model.AgentJsonResponse
import com.example.chatagent.domain.model.Message
import com.example.chatagent.domain.model.TokenUsage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private val gson = Gson()

fun MessageEntity.toDomain(): Message {
    return Message(
        id = id,
        content = content,
        isFromUser = isFromUser,
        timestamp = timestamp,
        jsonResponse = jsonResponseJson?.let {
            try {
                val type = object : TypeToken<AgentJsonResponse>() {}.type
                gson.fromJson(it, type)
            } catch (e: Exception) {
                null
            }
        },
        tokenUsage = tokenUsageJson?.let {
            try {
                val type = object : TypeToken<TokenUsage>() {}.type
                gson.fromJson(it, type)
            } catch (e: Exception) {
                null
            }
        },
        isSummary = isSummary,
        summarizedMessageCount = summarizedMessageCount,
        originalTokenCount = originalTokenCount
    )
}

fun Message.toEntity(): MessageEntity {
    return MessageEntity(
        id = id,
        content = content,
        isFromUser = isFromUser,
        timestamp = timestamp,
        jsonResponseJson = jsonResponse?.let { gson.toJson(it) },
        tokenUsageJson = tokenUsage?.let { gson.toJson(it) },
        isSummary = isSummary,
        summarizedMessageCount = summarizedMessageCount,
        originalTokenCount = originalTokenCount
    )
}
