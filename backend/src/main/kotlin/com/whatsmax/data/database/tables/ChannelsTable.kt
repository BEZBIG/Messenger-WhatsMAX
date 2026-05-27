/** Таблицы каналов, подписчиков, постов, комментариев и просмотров. */
package com.whatsmax.data.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object ChannelsTable : Table("channels") {
    val id          = uuid("id").autoGenerate()
    val handle      = varchar("handle", 64).uniqueIndex()
    val name        = varchar("name", 128)
    val description = text("description").nullable()
    val avatarUrl   = text("avatar_url").nullable()
    val isPublic    = bool("is_public").default(true)
    val ownerId     = varchar("owner_id", 128).references(UsersTable.uid)
    val membersCount = integer("members_count").default(0)
    val createdAt   = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}

object ChannelMembersTable : Table("channel_members") {
    val channelId  = uuid("channel_id").references(ChannelsTable.id)
    val userId     = varchar("user_id", 128).references(UsersTable.uid)
    val role       = varchar("role", 16).default("subscriber")
    val joinedAt   = datetime("joined_at").default(LocalDateTime.now())
    val isMuted    = bool("is_muted").default(false)

    override val primaryKey = PrimaryKey(channelId, userId)
}

object ChannelMessagesTable : Table("channel_messages") {
    val id        = uuid("id").autoGenerate()
    val channelId = uuid("channel_id").references(ChannelsTable.id)
    val authorId  = varchar("author_id", 128).references(UsersTable.uid)
    val content   = text("content").nullable()
    val type      = varchar("type", 16).default("text")
    val fileId    = uuid("file_id").references(FilesTable.id).nullable()
    val views     = integer("views").default(0)
    val isEdited  = bool("is_edited").default(false)
    val isDeleted = bool("is_deleted").default(false)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val durationMs = long("duration_ms").nullable()
    val waveform   = text("waveform").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, channelId, createdAt)
        index(false, fileId)
    }
}

object ChannelCommentsTable : Table("channel_comments") {
    val id        = uuid("id").autoGenerate()
    val messageId = uuid("message_id").references(ChannelMessagesTable.id)
    val authorId  = varchar("author_id", 128).references(UsersTable.uid)
    val content   = text("content")
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}

object ChannelMessageViewsTable : Table("channel_message_views") {
    val messageId = uuid("message_id").references(ChannelMessagesTable.id)
    val userId    = varchar("user_id", 128).references(UsersTable.uid)
    val viewedAt  = datetime("viewed_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(messageId, userId)
}
