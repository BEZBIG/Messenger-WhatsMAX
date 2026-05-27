/** Таблицы сообщений чатов и статусов прочтения. */
package com.whatsmax.data.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object MessagesTable : Table("messages") {
    val id          = uuid("id").autoGenerate()
    val chatId      = uuid("chat_id").references(ChatsTable.id)
    val senderId    = varchar("sender_id", 128).references(UsersTable.uid)
    val content     = text("content").nullable()
    val type        = varchar("type", 16).default("text")
    val fileId      = uuid("file_id").references(FilesTable.id).nullable()
    val replyToId   = uuid("reply_to_id").nullable()
    val isEdited    = bool("is_edited").default(false)
    val isDeleted   = bool("is_deleted").default(false)
    val createdAt   = datetime("created_at").default(LocalDateTime.now())
    val editedAt    = datetime("edited_at").nullable()
    val durationMs  = long("duration_ms").nullable()
    val waveform    = text("waveform").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, chatId, createdAt)
        index(false, fileId)
    }
}

object MessageReadStatusTable : Table("message_read_status") {
    val messageId = uuid("message_id").references(MessagesTable.id)
    val userId    = varchar("user_id", 128).references(UsersTable.uid)
    val readAt    = datetime("read_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(messageId, userId)
}
