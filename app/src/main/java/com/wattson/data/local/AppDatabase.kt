package com.wattson.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wattson.data.local.dao.ChatMessageDao
import com.wattson.data.local.dao.RepairConversationDao
import com.wattson.data.local.entity.ChatMessageEntity
import com.wattson.data.local.entity.RepairConversationEntity

/**
 * Room database for Wattson local persistence.
 */
@Database(
    entities = [
        RepairConversationEntity::class,
        ChatMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun repairConversationDao(): RepairConversationDao
    abstract fun chatMessageDao(): ChatMessageDao
}
