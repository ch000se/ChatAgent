package com.example.chatagent.data.local.converters

import androidx.room.TypeConverter
import com.example.chatagent.domain.model.AgentJsonResponse
import com.example.chatagent.domain.model.TokenUsage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromAgentJsonResponse(value: AgentJsonResponse?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toAgentJsonResponse(value: String?): AgentJsonResponse? {
        return value?.let {
            val type = object : TypeToken<AgentJsonResponse>() {}.type
            gson.fromJson(it, type)
        }
    }

    @TypeConverter
    fun fromTokenUsage(value: TokenUsage?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toTokenUsage(value: String?): TokenUsage? {
        return value?.let {
            val type = object : TypeToken<TokenUsage>() {}.type
            gson.fromJson(it, type)
        }
    }
}
