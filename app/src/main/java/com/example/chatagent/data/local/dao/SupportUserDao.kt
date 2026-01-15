package com.example.chatagent.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chatagent.data.local.entity.SupportUserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupportUserDao {

    @Query("SELECT * FROM support_users ORDER BY lastActiveAt DESC")
    fun getAllUsers(): Flow<List<SupportUserEntity>>

    @Query("SELECT * FROM support_users WHERE id = :userId")
    suspend fun getUserById(userId: String): SupportUserEntity?

    @Query("SELECT * FROM support_users WHERE email = :email")
    suspend fun getUserByEmail(email: String): SupportUserEntity?

    @Query("SELECT * FROM support_users WHERE subscription = :subscription")
    suspend fun getUsersBySubscription(subscription: String): List<SupportUserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: SupportUserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<SupportUserEntity>)

    @Query("UPDATE support_users SET totalTickets = totalTickets + 1, lastActiveAt = :timestamp WHERE id = :userId")
    suspend fun incrementUserTickets(userId: String, timestamp: Long)

    @Query("UPDATE support_users SET resolvedTickets = resolvedTickets + 1, lastActiveAt = :timestamp WHERE id = :userId")
    suspend fun incrementUserResolvedTickets(userId: String, timestamp: Long)

    @Query("DELETE FROM support_users WHERE id = :userId")
    suspend fun deleteUser(userId: String)

    @Query("DELETE FROM support_users")
    suspend fun deleteAllUsers()

    @Query("SELECT COUNT(*) FROM support_users")
    suspend fun getUsersCount(): Int
}
