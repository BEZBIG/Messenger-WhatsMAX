/** Реализация ChatRepository: direct/group-чаты и участники. */
package com.whatsmax.data.repositories

import com.whatsmax.data.database.DatabaseFactory.dbQuery
import com.whatsmax.data.database.tables.*
import com.whatsmax.domain.models.*
import com.whatsmax.domain.repositories.ChatRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inSubQuery
import java.time.LocalDateTime
import java.util.UUID

class ChatRepositoryImpl : ChatRepository {

    override suspend fun createChat(request: CreateChatRequest, creatorUid: String): Chat = dbQuery {
        val chatId = UUID.randomUUID()
        val now = java.time.LocalDateTime.now()
        ChatsTable.insert {
            it[id]          = chatId
            it[type]        = request.type
            it[name]        = request.name
            it[description] = request.description
            it[createdBy]   = creatorUid
            it[createdAt]   = now
            it[updatedAt]   = now
        }
        ChatMembersTable.insert {
            it[ChatMembersTable.chatId] = chatId
            it[userId]                  = creatorUid
            it[role]                    = "admin"
        }
        request.memberUids.filter { it != creatorUid }.forEach { uid ->
            ChatMembersTable.insert {
                it[ChatMembersTable.chatId] = chatId
                it[userId]                  = uid
                it[role]                    = "member"
            }
        }
        Chat(
            id          = chatId.toString(),
            type        = request.type,
            name        = request.name,
            description = request.description,
            createdBy   = creatorUid,
            members     = getChatMembersInternal(chatId),
            createdAt   = now.toString(),
            updatedAt   = now.toString()
        )
    }

    override suspend fun getChatById(chatId: UUID): Chat? = dbQuery {
        val chatRow = ChatsTable.select { ChatsTable.id eq chatId }.singleOrNull() ?: return@dbQuery null
        val members = getChatMembersInternal(chatId)
        Chat(
            id          = chatRow[ChatsTable.id].toString(),
            type        = chatRow[ChatsTable.type],
            name        = chatRow[ChatsTable.name],
            avatarUrl   = chatRow[ChatsTable.avatarUrl],
            description = chatRow[ChatsTable.description],
            createdBy   = chatRow[ChatsTable.createdBy],
            members     = members,
            createdAt   = chatRow[ChatsTable.createdAt].toString(),
            updatedAt   = chatRow[ChatsTable.updatedAt].toString()
        )
    }

    override suspend fun getUserChats(uid: String): List<Chat> = dbQuery {
        val chatIds = ChatMembersTable
            .select { (ChatMembersTable.userId eq uid) and (ChatMembersTable.isDeleted eq false) }
            .map { it[ChatMembersTable.chatId] }
        if (chatIds.isEmpty()) return@dbQuery emptyList<Chat>()

        val chatRows = ChatsTable.select { ChatsTable.id inList chatIds }
            .associateBy { it[ChatsTable.id] }

        val membersByChat: Map<UUID, List<ChatMember>> = (ChatMembersTable innerJoin UsersTable)
            .select { (ChatMembersTable.chatId inList chatIds) and (ChatMembersTable.isDeleted eq false) }
            .groupBy({ it[ChatMembersTable.chatId] }) { row ->
                ChatMember(
                    userId      = row[UsersTable.uid],
                    displayName = row[UsersTable.displayName],
                    avatarUrl   = row[UsersTable.avatarUrl],
                    role        = row[ChatMembersTable.role],
                    isOnline    = row[UsersTable.isOnline],
                    joinedAt    = row[ChatMembersTable.joinedAt].toString()
                )
            }

        val lastMessageRows: Map<UUID, ResultRow> = (MessagesTable innerJoin UsersTable)
            .select {
                (MessagesTable.chatId inList chatIds) and (MessagesTable.isDeleted eq false)
            }
            .orderBy(MessagesTable.createdAt, SortOrder.DESC)
            .toList()
            .groupBy { it[MessagesTable.chatId] }
            .mapValues { it.value.first() }

        val lastMsgIds = lastMessageRows.values.map { it[MessagesTable.id] }
        val readByMap: Map<UUID, List<String>> = if (lastMsgIds.isEmpty()) emptyMap()
            else MessageReadStatusTable
                .select { MessageReadStatusTable.messageId inList lastMsgIds }
                .groupBy({ it[MessageReadStatusTable.messageId] }, { it[MessageReadStatusTable.userId] })

        val unreadCounts: Map<UUID, Int> = run {
            val countExpr = MessagesTable.id.count()
            MessagesTable.join(
                MessageReadStatusTable, JoinType.LEFT,
                additionalConstraint = {
                    (MessageReadStatusTable.messageId eq MessagesTable.id) and
                    (MessageReadStatusTable.userId eq uid)
                }
            ).slice(MessagesTable.chatId, countExpr)
            .select {
                (MessagesTable.chatId inList chatIds) and
                (MessagesTable.isDeleted eq false) and
                (MessagesTable.senderId neq uid) and
                (MessageReadStatusTable.messageId.isNull())
            }
            .groupBy(MessagesTable.chatId)
            .associate { it[MessagesTable.chatId] to it[countExpr].toInt() }
        }

        chatIds.mapNotNull { chatId ->
            val chatRow = chatRows[chatId] ?: return@mapNotNull null
            val lastMsg = lastMessageRows[chatId]?.let { row ->
                val msgId = row[MessagesTable.id]
                Message(
                    id           = msgId.toString(),
                    chatId       = chatId.toString(),
                    senderId     = row[MessagesTable.senderId],
                    senderName   = row[UsersTable.displayName],
                    senderAvatar = row[UsersTable.avatarUrl],
                    content      = row[MessagesTable.content],
                    type         = row[MessagesTable.type],
                    fileId       = row[MessagesTable.fileId]?.toString(),
                    fileUrl      = null,
                    fileName     = null,
                    replyToId    = row[MessagesTable.replyToId]?.toString(),
                    isEdited     = row[MessagesTable.isEdited],
                    isDeleted    = row[MessagesTable.isDeleted],
                    readBy       = readByMap[msgId] ?: emptyList(),
                    createdAt    = row[MessagesTable.createdAt].toString(),
                    editedAt     = row[MessagesTable.editedAt]?.toString()
                )
            }
            Chat(
                id          = chatRow[ChatsTable.id].toString(),
                type        = chatRow[ChatsTable.type],
                name        = chatRow[ChatsTable.name],
                avatarUrl   = chatRow[ChatsTable.avatarUrl],
                description = chatRow[ChatsTable.description],
                createdBy   = chatRow[ChatsTable.createdBy],
                members     = membersByChat[chatId] ?: emptyList(),
                lastMessage = lastMsg,
                unreadCount = unreadCounts[chatId] ?: 0,
                createdAt   = chatRow[ChatsTable.createdAt].toString(),
                updatedAt   = chatRow[ChatsTable.updatedAt].toString()
            )
        }.sortedByDescending { it.lastMessage?.createdAt ?: it.updatedAt }
    }

    override suspend fun addMember(chatId: UUID, userId: String, role: String) = dbQuery {
        val existing = ChatMembersTable.select {
            (ChatMembersTable.chatId eq chatId) and (ChatMembersTable.userId eq userId)
        }.singleOrNull()
        if (existing == null) {
            ChatMembersTable.insert {
                it[ChatMembersTable.chatId] = chatId
                it[ChatMembersTable.userId] = userId
                it[ChatMembersTable.role]   = role
            }
        } else {
            ChatMembersTable.update({
                (ChatMembersTable.chatId eq chatId) and (ChatMembersTable.userId eq userId)
            }) { it[isDeleted] = false; it[ChatMembersTable.role] = role }
        }
        Unit
    }

    override suspend fun removeMember(chatId: UUID, userId: String) = dbQuery {
        ChatMembersTable.update({
            (ChatMembersTable.chatId eq chatId) and (ChatMembersTable.userId eq userId)
        }) { it[isDeleted] = true }
        Unit
    }

    override suspend fun getChatMembers(chatId: UUID): List<ChatMember> = dbQuery {
        getChatMembersInternal(chatId)
    }

    private fun getChatMembersInternal(chatId: UUID): List<ChatMember> {
        return (ChatMembersTable innerJoin UsersTable)
            .select {
                (ChatMembersTable.chatId eq chatId) and (ChatMembersTable.isDeleted eq false)
            }.map {
                ChatMember(
                    userId      = it[UsersTable.uid],
                    displayName = it[UsersTable.displayName],
                    avatarUrl   = it[UsersTable.avatarUrl],
                    role        = it[ChatMembersTable.role],
                    isOnline    = it[UsersTable.isOnline],
                    joinedAt    = it[ChatMembersTable.joinedAt].toString()
                )
            }
    }

    override suspend fun isUserInChat(chatId: UUID, userId: String): Boolean = dbQuery {
        ChatMembersTable.select {
            (ChatMembersTable.chatId eq chatId) and
            (ChatMembersTable.userId eq userId) and
            (ChatMembersTable.isDeleted eq false)
        }.count() > 0
    }

    override suspend fun getMemberRole(chatId: UUID, userId: String): String? = dbQuery {
        ChatMembersTable.select {
            (ChatMembersTable.chatId eq chatId) and
            (ChatMembersTable.userId eq userId) and
            (ChatMembersTable.isDeleted eq false)
        }.singleOrNull()?.get(ChatMembersTable.role)
    }

    override suspend fun findDirectChat(uid1: String, uid2: String): Chat? = dbQuery {
        val chatsOfUser1 = ChatMembersTable
            .select { (ChatMembersTable.userId eq uid1) and (ChatMembersTable.isDeleted eq false) }
            .map { it[ChatMembersTable.chatId] }.toSet()
        val chatsOfUser2 = ChatMembersTable
            .select { (ChatMembersTable.userId eq uid2) and (ChatMembersTable.isDeleted eq false) }
            .map { it[ChatMembersTable.chatId] }.toSet()
        val common = chatsOfUser1.intersect(chatsOfUser2)
        common.firstOrNull { chatId ->
            ChatsTable.select {
                (ChatsTable.id eq chatId) and (ChatsTable.type eq "direct")
            }.count() > 0
        }?.let { getChatById(it) }
    }

    override suspend fun updateChat(chatId: UUID, name: String?, description: String?, avatarUrl: String?): Chat = dbQuery {
        ChatsTable.update({ ChatsTable.id eq chatId }) {
            name?.let { v -> it[ChatsTable.name] = v }
            description?.let { v -> it[ChatsTable.description] = v }
            avatarUrl?.let { v -> it[ChatsTable.avatarUrl] = v }
            it[updatedAt] = LocalDateTime.now()
        }
        getChatById(chatId)!!
    }

    override suspend fun deleteChat(chatId: UUID) = dbQuery {
        MessageReadStatusTable.deleteWhere {
            MessageReadStatusTable.messageId inSubQuery
                MessagesTable.slice(MessagesTable.id).select { MessagesTable.chatId eq chatId }
        }
        MessagesTable.deleteWhere { MessagesTable.chatId eq chatId }
        ChatMembersTable.deleteWhere { ChatMembersTable.chatId eq chatId }
        ChatsTable.deleteWhere { ChatsTable.id eq chatId }
        Unit
    }
}
