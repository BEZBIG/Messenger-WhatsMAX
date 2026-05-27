/** Таблица пользователей, PK — Firebase UID. */
package com.whatsmax.data.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object UsersTable : Table("users") {
    val uid         = varchar("uid", 128)
    val username    = varchar("username", 64).uniqueIndex()
    val email       = varchar("email", 255).nullable()
    val phone       = varchar("phone", 32).nullable()
    val displayName = varchar("display_name", 128)
    val avatarUrl   = text("avatar_url").nullable()
    val bio         = varchar("bio", 500).nullable()
    val isOnline    = bool("is_online").default(false)
    val lastSeen    = datetime("last_seen").default(LocalDateTime.now())
    val createdAt   = datetime("created_at").default(LocalDateTime.now())
    val fcmToken    = text("fcm_token").nullable()

    override val primaryKey = PrimaryKey(uid)
}
