package com.example.chatagent.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chatagent.data.local.entity.SupportTicketEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupportTicketDao {

    @Query("SELECT * FROM support_tickets ORDER BY createdAt DESC")
    fun getAllTickets(): Flow<List<SupportTicketEntity>>

    @Query("SELECT * FROM support_tickets WHERE id = :ticketId")
    suspend fun getTicketById(ticketId: String): SupportTicketEntity?

    @Query("SELECT * FROM support_tickets WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getTicketsByUserId(userId: String): List<SupportTicketEntity>

    @Query("SELECT * FROM support_tickets WHERE status = :status ORDER BY createdAt DESC")
    suspend fun getTicketsByStatus(status: String): List<SupportTicketEntity>

    @Query("SELECT * FROM support_tickets WHERE category = :category ORDER BY createdAt DESC")
    suspend fun getTicketsByCategory(category: String): List<SupportTicketEntity>

    @Query("SELECT * FROM support_tickets WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    suspend fun searchTickets(query: String): List<SupportTicketEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: SupportTicketEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTickets(tickets: List<SupportTicketEntity>)

    @Query("DELETE FROM support_tickets WHERE id = :ticketId")
    suspend fun deleteTicket(ticketId: String)

    @Query("DELETE FROM support_tickets")
    suspend fun deleteAllTickets()

    @Query("SELECT COUNT(*) FROM support_tickets")
    suspend fun getTicketsCount(): Int

    @Query("SELECT COUNT(*) FROM support_tickets WHERE status = :status")
    suspend fun getTicketsCountByStatus(status: String): Int
}
