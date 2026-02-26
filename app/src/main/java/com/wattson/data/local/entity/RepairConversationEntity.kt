package com.wattson.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for a repair chat conversation.
 */
@Entity(tableName = "repair_conversations")
data class RepairConversationEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)
