/** Таблица реакций на сообщения, посты и комментарии. */
package com.whatsmax.data.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object ReactionsTable : Table("reactions") {
    val id         = uuid("id").autoGenerate()
    val entityType = varchar("entity_type", 16)
    val entityId   = uuid("entity_id")
    val userId     = varchar("user_id", 128).references(UsersTable.uid)
    val emoji      = varchar("emoji", 8)
    val createdAt  = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("uq_reaction_per_user", entityType, entityId, userId)
    }
}
