package com.example.chatagent.data.remote.client

import com.google.gson.Gson
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class SseResponseConverterFactory(
    private val gson: Gson
) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *> {
        return SseResponseConverter(gson, type)
    }

    private class SseResponseConverter(
        private val gson: Gson,
        private val type: Type
    ) : Converter<ResponseBody, Any?> {

        override fun convert(value: ResponseBody): Any? {
            val responseString = value.string()

            // Check if this is an SSE response
            val json = if (responseString.startsWith("event:") || responseString.contains("\nevent:")) {
                // Parse SSE format
                parseSseResponse(responseString)
            } else {
                // Regular JSON response
                responseString
            }

            return gson.fromJson(json, type)
        }

        private fun parseSseResponse(sseResponse: String): String {
            // SSE format:
            // event: message
            // data: {"json":"here"}
            //
            // OR just:
            // data: {"json":"here"}

            val lines = sseResponse.lines()
            val dataLines = mutableListOf<String>()

            for (line in lines) {
                when {
                    line.startsWith("data:") -> {
                        // Extract data content (after "data: ")
                        val data = line.substring(5).trim()
                        if (data.isNotEmpty()) {
                            dataLines.add(data)
                        }
                    }
                    // Ignore event:, id:, retry:, and empty lines
                }
            }

            // Join all data lines (in case of multi-line data)
            return dataLines.joinToString("\n")
        }
    }
}
