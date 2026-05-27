/** Реализация UserRepository поверх Exposed. */
package com.whatsmax.data.repositories

import com.whatsmax.data.database.DatabaseFactory.dbQuery
import com.whatsmax.data.database.tables.UsersTable
import com.whatsmax.domain.models.*
import com.whatsmax.domain.repositories.UserRepository
import com.whatsmax.utils.Validation
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime

class UserRepositoryImpl : UserRepository {

    private fun ResultRow.toUser() = User(
        uid         = this[UsersTable.uid],
        username    = this[UsersTable.username],
        email       = this[UsersTable.email],
        phone       = this[UsersTable.phone],
        displayName = this[UsersTable.displayName],
        avatarUrl   = this[UsersTable.avatarUrl],
        bio         = this[UsersTable.bio],
        isOnline    = this[UsersTable.isOnline],
        lastSeen    = this[UsersTable.lastSeen].toString(),
        createdAt   = this[UsersTable.createdAt].toString()
    )

    override suspend fun createUser(uid: String, request: CreateUserRequest): User = dbQuery {
        UsersTable.insert {
            it[UsersTable.uid]         = uid
            it[UsersTable.username]    = request.username
            it[UsersTable.displayName] = request.displayName
            it[UsersTable.email]       = request.email
            it[UsersTable.phone]       = request.phone
            it[fcmToken]               = request.fcmToken
        }
        UsersTable.select { UsersTable.uid eq uid }.single().toUser()
    }

    override suspend fun getUserByUid(uid: String): User? = dbQuery {
        UsersTable.select { UsersTable.uid eq uid }.singleOrNull()?.toUser()
    }

    override suspend fun getUserByUsername(username: String): User? = dbQuery {
        UsersTable.select { UsersTable.username eq username }.singleOrNull()?.toUser()
    }

    override suspend fun searchUsers(query: String, limit: Int): List<User> = dbQuery {
        val escaped = Validation.escapeLike(query)
        UsersTable.select {
            (UsersTable.username    like "%$escaped%") or
            (UsersTable.displayName like "%$escaped%")
        }.limit(limit).map { it.toUser() }
    }

    override suspend fun updateUser(uid: String, request: UpdateUserRequest): User = dbQuery {
        UsersTable.update({ UsersTable.uid eq uid }) {
            request.displayName?.let { v -> it[displayName] = v }
            request.bio?.let { v -> it[bio] = v }
            request.username?.let { v -> it[username] = v }
            request.fcmToken?.let { v -> it[fcmToken] = v }
            request.avatarUrl?.let { v -> it[avatarUrl] = v }
        }
        UsersTable.select { UsersTable.uid eq uid }.single().toUser()
    }

    override suspend fun updateOnlineStatus(uid: String, isOnline: Boolean) = dbQuery {
        UsersTable.update({ UsersTable.uid eq uid }) {
            it[UsersTable.isOnline] = isOnline
            it[lastSeen] = LocalDateTime.now()
        }
        Unit
    }

    override suspend fun updateFcmToken(uid: String, token: String) = dbQuery {
        UsersTable.update({ UsersTable.uid eq uid }) { it[fcmToken] = token }
        Unit
    }

    override suspend fun deleteUser(uid: String) = dbQuery {
        UsersTable.deleteWhere { UsersTable.uid eq uid }
        Unit
    }
}
