package com.example.chatagent.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.chatagent.data.local.converters.Converters
import com.example.chatagent.data.local.dao.DocumentDao
import com.example.chatagent.data.local.dao.EmbeddingDao
import com.example.chatagent.data.local.dao.EpicDao
import com.example.chatagent.data.local.dao.MessageDao
import com.example.chatagent.data.local.dao.SprintDao
import com.example.chatagent.data.local.dao.SupportTicketDao
import com.example.chatagent.data.local.dao.SupportUserDao
import com.example.chatagent.data.local.dao.TaskDao
import com.example.chatagent.data.local.dao.TeamMemberDao
import com.example.chatagent.data.local.entity.DocumentEntity
import com.example.chatagent.data.local.entity.EmbeddingEntity
import com.example.chatagent.data.local.entity.EpicEntity
import com.example.chatagent.data.local.entity.MessageEntity
import com.example.chatagent.data.local.entity.SprintEntity
import com.example.chatagent.data.local.entity.SupportTicketEntity
import com.example.chatagent.data.local.entity.SupportUserEntity
import com.example.chatagent.data.local.entity.TaskEntity
import com.example.chatagent.data.local.entity.TeamMemberEntity

@Database(
    entities = [
        MessageEntity::class,
        DocumentEntity::class,
        EmbeddingEntity::class,
        SupportTicketEntity::class,
        SupportUserEntity::class,
        // Team Assistant entities
        TaskEntity::class,
        TeamMemberEntity::class,
        SprintEntity::class,
        EpicEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun documentDao(): DocumentDao
    abstract fun embeddingDao(): EmbeddingDao
    abstract fun supportTicketDao(): SupportTicketDao
    abstract fun supportUserDao(): SupportUserDao

    // Team Assistant DAOs
    abstract fun taskDao(): TaskDao
    abstract fun teamMemberDao(): TeamMemberDao
    abstract fun sprintDao(): SprintDao
    abstract fun epicDao(): EpicDao
}
