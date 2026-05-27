/**
 * data/local/AppDatabase.kt
 * Room-БД для локального кэша сообщений и чатов — оффлайн-просмотр истории.
 * Версия 1: таблицы cached_messages и cached_chats.
 */
package com.whatsmax.data.local

import androidx.room.*

@Entity(tableName = "cached_messages")
data class CachedMessage(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val content: String?,
    val type: String,
    val createdAt: String,
    val isDeleted: Boolean = false
)

@Dao
interface MessageDao {
    @Query("SELECT * FROM cached_messages WHERE chatId = :chatId ORDER BY createdAt ASC")
    suspend fun getMessagesForChat(chatId: String): List<CachedMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<CachedMessage>)

    @Query("DELETE FROM cached_messages WHERE chatId = :chatId")
    suspend fun clearChat(chatId: String)
}

@Database(entities = [CachedMessage::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    companion object {
        const val DATABASE_NAME = "whatsmax_cache.db"
    }
}
