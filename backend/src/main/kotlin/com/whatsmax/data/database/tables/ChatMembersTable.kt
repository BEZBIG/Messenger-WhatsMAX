/** Связка пользователей и чатов (many-to-many). */
package com.whatsmax.data.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object ChatMembersTable : Table("chat_members") {
    val chatId    = uuid("chat_id").references(ChatsTable.id)
    val userId    = varchar("user_id", 128).references(UsersTable.uid)
    val role      = varchar("role", 16).default("member")
    val joinedAt  = datetime("joined_at").default(LocalDateTime.now())
    val isDeleted = bool("is_deleted").default(false)

    override val primaryKey = PrimaryKey(chatId, userId)
}
