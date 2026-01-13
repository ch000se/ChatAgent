package com.example.chatagent.domain.util

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectDocumentScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "ProjectDocumentScanner"

    data class ProjectDocument(
        val fileName: String,
        val content: String,
        val relativePath: String,
        val fileType: ProjectDocumentType
    )

    enum class ProjectDocumentType {
        README,
        BUILD_INSTRUCTIONS,
        DOCUMENTATION,
        CHANGELOG
    }

    /**
     * Scans assets/docs/ folder for project documentation files
     * In production, Android apps bundle docs in assets/
     */
    suspend fun scanProjectDocuments(): List<ProjectDocument> {
        val documents = mutableListOf<ProjectDocument>()

        try {
            // Scan assets/docs/ folder
            val assetManager = context.assets
            val docFiles = assetManager.list("docs") ?: emptyArray()

            docFiles.filter { it.endsWith(".md") || it.endsWith(".txt") }
                .forEach { fileName ->
                    try {
                        val content = assetManager.open("docs/$fileName")
                            .bufferedReader()
                            .use { it.readText() }

                        documents.add(
                            ProjectDocument(
                                fileName = fileName,
                                content = content,
                                relativePath = "docs/$fileName",
                                fileType = classifyDocument(fileName)
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to read $fileName", e)
                    }
                }

            Log.d(TAG, "Found ${documents.size} project documents")
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning project documents", e)
        }

        return documents
    }

    private fun classifyDocument(fileName: String): ProjectDocumentType {
        return when {
            fileName.equals("README.md", ignoreCase = true) ->
                ProjectDocumentType.README
            fileName.contains("BUILD", ignoreCase = true) ->
                ProjectDocumentType.BUILD_INSTRUCTIONS
            fileName.contains("CHANGELOG", ignoreCase = true) ->
                ProjectDocumentType.CHANGELOG
            else -> ProjectDocumentType.DOCUMENTATION
        }
    }
}
