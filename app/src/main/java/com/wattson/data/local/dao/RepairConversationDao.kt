package com.wattson.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wattson.data.local.entity.RepairConversationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for repair conversation operations.
 */
@Dao
interface RepairConversationDao {

    @Query("SELECT * FROM repair_conversations WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getConversationsForUser(userId: String): Flow<List<RepairConversationEntity>>

    @Query("SELECT * FROM repair_conversations WHERE id = :conversationId")
    suspend fun getConversationById(conversationId: String): RepairConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: RepairConversationEntity)

    @Update
    suspend fun updateConversation(conversation: RepairConversationEntity)

    @Query("DELETE FROM repair_conversations WHERE id = :conversationId")
    suspend fun deleteConversationById(conversationId: String)
}
