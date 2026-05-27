/** Таблица чатов: direct и group. */
package com.whatsmax.data.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object ChatsTable : Table("chats") {
    val id          = uuid("id").autoGenerate()
    val type        = varchar("type", 16)
    val name        = varchar("name", 128).nullable()
    val avatarUrl   = text("avatar_url").nullable()
    val description = text("description").nullable()
    val createdBy   = varchar("created_by", 128).references(UsersTable.uid)
    val createdAt   = datetime("created_at").default(LocalDateTime.now())
    val updatedAt   = datetime("updated_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}
