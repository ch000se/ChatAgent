package com.example.chatagent.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.chatagent.data.local.converters.Converters
import com.example.chatagent.data.local.dao.DocumentDao
import com.example.chatagent.data.local.dao.EmbeddingDao
import com.example.chatagent.data.local.dao.MessageDao
import com.example.chatagent.data.local.entity.DocumentEntity
import com.example.chatagent.data.local.entity.EmbeddingEntity
import com.example.chatagent.data.local.entity.MessageEntity

@Database(
    entities = [
        MessageEntity::class,
        DocumentEntity::class,
        EmbeddingEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun documentDao(): DocumentDao
    abstract fun embeddingDao(): EmbeddingDao
}
