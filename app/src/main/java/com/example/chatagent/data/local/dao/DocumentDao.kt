package com.example.chatagent.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.chatagent.data.local.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents ORDER BY uploadedAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :documentId")
    suspend fun getDocumentById(documentId: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE fileName = :fileName LIMIT 1")
    suspend fun getDocumentByFileName(fileName: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE indexed = 1 ORDER BY indexedAt DESC")
    fun getIndexedDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE indexed = 0 ORDER BY uploadedAt DESC")
    fun getNotIndexedDocuments(): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<DocumentEntity>)

    @Query("UPDATE documents SET indexed = :indexed, indexedAt = :indexedAt, chunkCount = :chunkCount WHERE id = :documentId")
    suspend fun updateIndexStatus(documentId: String, indexed: Boolean, indexedAt: Long?, chunkCount: Int)

    @Query("DELETE FROM documents WHERE id = :documentId")
    suspend fun deleteDocument(documentId: String)

    @Query("DELETE FROM documents")
    suspend fun deleteAllDocuments()

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun getDocumentsCount(): Int

    @Query("SELECT COUNT(*) FROM documents WHERE indexed = 1")
    suspend fun getIndexedDocumentsCount(): Int
}
