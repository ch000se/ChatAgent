package com.example.chatagent.domain.usecase

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.example.chatagent.domain.model.AnalystFile
import com.example.chatagent.domain.model.AnalystFileMetadata
import com.example.chatagent.domain.model.AnalystFileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import javax.inject.Inject

class ParseAnalystFileUseCase @Inject constructor() {

    suspend fun fromRawText(
        rawText: String,
        fileName: String,
        maxChars: Int
    ): Result<AnalystFile> = withContext(Dispatchers.IO) {
        try {
            val fileType = detectFileType(fileName)
            val (parsedContent, metadata, wasTruncated) = when (fileType) {
                AnalystFileType.CSV -> parseCsv(rawText, maxChars)
                AnalystFileType.JSON -> parseJson(rawText, maxChars)
                AnalystFileType.LOG -> parseLog(rawText, maxChars)
            }
            Result.success(
                AnalystFile(
                    fileName = fileName,
                    fileType = fileType,
                    rawSize = rawText.length.toLong(),
                    parsedContent = parsedContent,
                    tokenEstimate = parsedContent.length / 4,
                    wasTruncated = wasTruncated,
                    metadata = metadata
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend operator fun invoke(
        uri: Uri,
        resolver: ContentResolver,
        maxChars: Int
    ): Result<AnalystFile> = withContext(Dispatchers.IO) {
        try {
            val (fileName, fileSize) = queryFileInfo(uri, resolver)
            val rawText = readFileContent(uri, resolver, maxChars * 2)
            val fileType = detectFileType(fileName)

            val (parsedContent, metadata, wasTruncated) = when (fileType) {
                AnalystFileType.CSV -> parseCsv(rawText, maxChars)
                AnalystFileType.JSON -> parseJson(rawText, maxChars)
                AnalystFileType.LOG -> parseLog(rawText, maxChars)
            }

            Result.success(
                AnalystFile(
                    fileName = fileName,
                    fileType = fileType,
                    rawSize = fileSize,
                    parsedContent = parsedContent,
                    tokenEstimate = parsedContent.length / 4,
                    wasTruncated = wasTruncated,
                    metadata = metadata
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun queryFileInfo(uri: Uri, resolver: ContentResolver): Pair<String, Long> {
        var name = "unknown"
        var size = 0L
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: "unknown"
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }
        return name to size
    }

    private fun readFileContent(uri: Uri, resolver: ContentResolver, maxBytes: Int): String {
        return resolver.openInputStream(uri)?.use { stream ->
            val buffer = ByteArray(minOf(maxBytes, 2 * 1024 * 1024)) // cap at 2MB read
            val bytesRead = stream.read(buffer)
            if (bytesRead > 0) String(buffer, 0, bytesRead, Charsets.UTF_8) else ""
        } ?: throw IllegalStateException("Cannot open file")
    }

    private fun detectFileType(fileName: String): AnalystFileType {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return AnalystFileType.entries.find { ext in it.extensions } ?: AnalystFileType.LOG
    }

    private fun parseCsv(rawText: String, maxChars: Int): Triple<String, AnalystFileMetadata, Boolean> {
        val lines = rawText.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return Triple("(empty file)", AnalystFileMetadata.CsvMetadata(emptyList(), 0, 0), false)
        }

        val headers = lines.first().split(",").map { it.trim() }
        val dataLines = lines.drop(1)
        val totalRows = dataLines.size

        val result = StringBuilder()
        result.appendLine(headers.joinToString(" | "))
        result.appendLine("-".repeat(minOf(headers.joinToString(" | ").length, 80)))

        var includedRows = 0
        var wasTruncated = false

        for (line in dataLines) {
            val row = line.split(",").map { it.trim() }.joinToString(" | ")
            if (result.length + row.length + 1 > maxChars) {
                wasTruncated = true
                break
            }
            result.appendLine(row)
            includedRows++
        }

        val metadata = AnalystFileMetadata.CsvMetadata(
            headers = headers,
            rowCount = totalRows,
            includedRows = includedRows
        )

        return Triple(result.toString(), metadata, wasTruncated)
    }

    private fun parseJson(rawText: String, maxChars: Int): Triple<String, AnalystFileMetadata, Boolean> {
        return try {
            val token = JSONTokener(rawText).nextValue()
            when (token) {
                is JSONArray -> parseJsonArray(token, maxChars)
                is JSONObject -> parseJsonObject(token, maxChars)
                else -> Triple(
                    rawText.take(maxChars),
                    AnalystFileMetadata.JsonMetadata("primitive", null, null),
                    rawText.length > maxChars
                )
            }
        } catch (e: Exception) {
            // If JSON parsing fails, treat as raw text
            val content = rawText.take(maxChars)
            Triple(
                content,
                AnalystFileMetadata.JsonMetadata("invalid", null, null),
                rawText.length > maxChars
            )
        }
    }

    private fun parseJsonArray(array: JSONArray, maxChars: Int): Triple<String, AnalystFileMetadata, Boolean> {
        val totalElements = array.length()
        val result = StringBuilder()
        result.appendLine("[")

        var includedElements = 0
        var wasTruncated = false

        for (i in 0 until totalElements) {
            val element = array.get(i).toString()
            val formatted = "  $element"
            if (result.length + formatted.length + 3 > maxChars) {
                wasTruncated = true
                break
            }
            if (i > 0) result.appendLine(",")
            result.append(formatted)
            includedElements++
        }

        result.appendLine()
        result.appendLine("]")

        val topKeys = if (totalElements > 0) {
            try {
                val firstObj = array.getJSONObject(0)
                firstObj.keys().asSequence().toList()
            } catch (e: Exception) { null }
        } else null

        val metadata = AnalystFileMetadata.JsonMetadata(
            topLevelType = "array",
            elementCount = totalElements,
            topLevelKeys = topKeys
        )

        return Triple(result.toString(), metadata, wasTruncated)
    }

    private fun parseJsonObject(obj: JSONObject, maxChars: Int): Triple<String, AnalystFileMetadata, Boolean> {
        val keys = obj.keys().asSequence().toList()
        val prettyPrinted = obj.toString(2)
        val wasTruncated = prettyPrinted.length > maxChars
        val content = prettyPrinted.take(maxChars)

        val metadata = AnalystFileMetadata.JsonMetadata(
            topLevelType = "object",
            elementCount = null,
            topLevelKeys = keys
        )

        return Triple(content, metadata, wasTruncated)
    }

    private fun parseLog(rawText: String, maxChars: Int): Triple<String, AnalystFileMetadata, Boolean> {
        val lines = rawText.lines()
        val totalLines = lines.size

        // For logs, keep the last N lines (most recent are more relevant)
        val result = StringBuilder()
        val reversedLines = lines.reversed()
        val includedLines = mutableListOf<String>()

        for (line in reversedLines) {
            val numberedLine = "${totalLines - includedLines.size}: $line"
            if (result.length + numberedLine.length + 1 > maxChars) {
                break
            }
            includedLines.add(numberedLine)
        }

        includedLines.reverse()
        val content = includedLines.joinToString("\n")

        val metadata = AnalystFileMetadata.LogMetadata(
            lineCount = totalLines,
            includedLines = includedLines.size
        )

        return Triple(content, metadata, includedLines.size < totalLines)
    }
}
