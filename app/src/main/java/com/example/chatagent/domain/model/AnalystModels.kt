package com.example.chatagent.domain.model

enum class AnalystFileType(val extensions: List<String>, val displayName: String) {
    CSV(listOf("csv"), "CSV"),
    JSON(listOf("json"), "JSON"),
    LOG(listOf("log", "txt"), "Log File")
}

data class AnalystFile(
    val fileName: String,
    val fileType: AnalystFileType,
    val rawSize: Long,
    val parsedContent: String,
    val tokenEstimate: Int,
    val wasTruncated: Boolean,
    val metadata: AnalystFileMetadata
)

sealed class AnalystFileMetadata {
    data class CsvMetadata(
        val headers: List<String>,
        val rowCount: Int,
        val includedRows: Int
    ) : AnalystFileMetadata()

    data class JsonMetadata(
        val topLevelType: String,
        val elementCount: Int?,
        val topLevelKeys: List<String>?
    ) : AnalystFileMetadata()

    data class LogMetadata(
        val lineCount: Int,
        val includedLines: Int
    ) : AnalystFileMetadata()
}
