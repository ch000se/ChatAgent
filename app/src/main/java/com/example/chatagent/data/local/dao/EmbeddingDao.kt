package com.example.chatagent.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chatagent.data.local.entity.EmbeddingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmbeddingDao {

    @Query("SELECT * FROM embeddings WHERE documentId = :documentId ORDER BY chunkIndex ASC")
    suspend fun getEmbeddingsByDocumentId(documentId: String): List<EmbeddingEntity>

    @Query("SELECT * FROM embeddings WHERE id = :embeddingId")
    suspend fun getEmbeddingById(embeddingId: String): EmbeddingEntity?

    @Query("SELECT * FROM embeddings ORDER BY createdAt DESC")
    fun getAllEmbeddings(): Flow<List<EmbeddingEntity>>

    @Query("SELECT * FROM embeddings ORDER BY createdAt DESC")
    suspend fun getAllEmbeddingsList(): List<EmbeddingEntity>

    @Query("SELECT * FROM embeddings ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentEmbeddings(limit: Int): List<EmbeddingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbedding(embedding: EmbeddingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbeddings(embeddings: List<EmbeddingEntity>)

    @Query("DELETE FROM embeddings WHERE documentId = :documentId")
    suspend fun deleteEmbeddingsByDocumentId(documentId: String)

    @Query("DELETE FROM embeddings WHERE id = :embeddingId")
    suspend fun deleteEmbedding(embeddingId: String)

    @Query("DELETE FROM embeddings")
    suspend fun deleteAllEmbeddings()

    @Query("SELECT COUNT(*) FROM embeddings")
    suspend fun getEmbeddingsCount(): Int

    @Query("SELECT COUNT(*) FROM embeddings WHERE documentId = :documentId")
    suspend fun getEmbeddingsCountByDocument(documentId: String): Int
}
