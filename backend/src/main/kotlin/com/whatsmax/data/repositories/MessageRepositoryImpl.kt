/** Реализация MessageRepository: CRUD сообщений с soft-delete. */
package com.whatsmax.data.repositories

import com.whatsmax.data.database.DatabaseFactory.dbQuery
import com.whatsmax.data.database.tables.*
import com.whatsmax.domain.models.*
import com.whatsmax.domain.repositories.MessageRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.UUID

class MessageRepositoryImpl : MessageRepository {

    private fun parseWaveform(s: String?): List<Int>? =
        s?.let { runCatching { Json.decodeFromString<List<Int>>(it) }.getOrNull() }

    private fun encodeWaveform(w: List<Int>?): String? =
        w?.let { Json.encodeToString(it) }

    private fun ResultRow.toMessage(
        senderName: String, senderAvatar: String?, readBy: List<String>,
        fileUrl: String?, fileName: String?, thumbUrl: String?
    ): Message = Message(
        id          = this[MessagesTable.id].toString(),
        chatId      = this[MessagesTable.chatId].toString(),
        senderId    = this[MessagesTable.senderId],
        senderName  = senderName,
        senderAvatar = senderAvatar,
        content     = this[MessagesTable.content],
        type        = this[MessagesTable.type],
        fileId      = this[MessagesTable.fileId]?.toString(),
        fileUrl     = fileUrl,
        fileName    = fileName,
        thumbUrl    = thumbUrl,
        replyToId   = this[MessagesTable.replyToId]?.toString(),
        isEdited    = this[MessagesTable.isEdited],
        isDeleted   = this[MessagesTable.isDeleted],
        readBy      = readBy,
        createdAt   = this[MessagesTable.createdAt].toString(),
        editedAt    = this[MessagesTable.editedAt]?.toString(),
        durationMs  = this[MessagesTable.durationMs],
        waveform    = parseWaveform(this[MessagesTable.waveform])
    )

    private fun fileTriple(fileRow: ResultRow?): Triple<String?, String?, String?> {
        if (fileRow == null) return Triple(null, null, null)
        val id = fileRow[FilesTable.id]
        val thumb = if (fileRow[FilesTable.thumbObjectKey] != null) "/files/$id/thumb" else null
        return Triple("/files/$id", fileRow[FilesTable.originalName], thumb)
    }

    override suspend fun sendMessage(chatId: UUID, senderId: String, request: SendMessageRequest): Message = dbQuery {
        val msgId = UUID.randomUUID()
        val now   = LocalDateTime.now()
        MessagesTable.insert {
            it[MessagesTable.id]       = msgId
            it[MessagesTable.chatId]   = chatId
            it[MessagesTable.senderId] = senderId
            it[content]                = request.content
            it[type]                   = request.type
            it[fileId]                 = request.fileId?.let { fid -> UUID.fromString(fid) }
            it[replyToId]              = request.replyToId?.let { rid -> UUID.fromString(rid) }
            it[createdAt]              = now
            it[durationMs]             = request.durationMs
            it[waveform]               = encodeWaveform(request.waveform)
        }
        ChatsTable.update({ ChatsTable.id eq chatId }) {
            it[updatedAt] = now
        }
        val sender = UsersTable.select { UsersTable.uid eq senderId }.singleOrNull()
        val fileRow = request.fileId?.let { fid ->
            FilesTable.select { FilesTable.id eq UUID.fromString(fid) }.singleOrNull()
        }
        val (fileUrl, fileName, thumbUrl) = fileTriple(fileRow)
        Message(
            id           = msgId.toString(),
            chatId       = chatId.toString(),
            senderId     = senderId,
            senderName   = sender?.get(UsersTable.displayName) ?: "Unknown",
            senderAvatar = sender?.get(UsersTable.avatarUrl),
            content      = request.content,
            type         = request.type,
            fileId       = request.fileId,
            fileUrl      = fileUrl,
            fileName     = fileName,
            thumbUrl     = thumbUrl,
            replyToId    = request.replyToId,
            isEdited     = false,
            isDeleted    = false,
            readBy       = emptyList(),
            createdAt    = now.toString(),
            editedAt     = null,
            durationMs   = request.durationMs,
            waveform     = request.waveform
        )
    }

    override suspend fun getChatMessages(chatId: UUID, limit: Int, before: UUID?): List<Message> = dbQuery {
        val query = MessagesTable.select { MessagesTable.chatId eq chatId }
        if (before != null) {
            val beforeTime = MessagesTable.select { MessagesTable.id eq before }
                .singleOrNull()?.get(MessagesTable.createdAt)
            if (beforeTime != null) {
                query.andWhere { MessagesTable.createdAt less beforeTime }
            }
        }
        val rows = query.orderBy(MessagesTable.createdAt, SortOrder.DESC).limit(limit).toList()
        if (rows.isEmpty()) return@dbQuery emptyList<Message>()

        val msgIds    = rows.map { it[MessagesTable.id] }
        val senderIds = rows.map { it[MessagesTable.senderId] }.distinct()
        val fileIds   = rows.mapNotNull { it[MessagesTable.fileId] }.distinct()

        val senders: Map<String, ResultRow> = UsersTable
            .select { UsersTable.uid inList senderIds }
            .associateBy { it[UsersTable.uid] }

        val readByMap: Map<UUID, List<String>> = MessageReadStatusTable
            .select { MessageReadStatusTable.messageId inList msgIds }
            .groupBy({ it[MessageReadStatusTable.messageId] }, { it[MessageReadStatusTable.userId] })

        val files: Map<UUID, ResultRow> = if (fileIds.isEmpty()) emptyMap()
                                          else FilesTable.select { FilesTable.id inList fileIds }
                                                  .associateBy { it[FilesTable.id] }

        rows.map { row ->
            val sender = senders[row[MessagesTable.senderId]]
            val readBy = readByMap[row[MessagesTable.id]] ?: emptyList()
            val fileInfo = row[MessagesTable.fileId]?.let { files[it] }
            val (fileUrl, fileName, thumbUrl) = fileTriple(fileInfo)
            row.toMessage(
                senderName   = sender?.get(UsersTable.displayName) ?: "Unknown",
                senderAvatar = sender?.get(UsersTable.avatarUrl),
                readBy       = readBy,
                fileUrl      = fileUrl,
                fileName     = fileName,
                thumbUrl     = thumbUrl
            )
        }.reversed()
    }

    override suspend fun getMessageById(messageId: UUID): Message? = dbQuery {
        val row = MessagesTable.select { MessagesTable.id eq messageId }.singleOrNull() ?: return@dbQuery null
        val sender = UsersTable.select { UsersTable.uid eq row[MessagesTable.senderId] }.singleOrNull()
        val readBy = MessageReadStatusTable.select { MessageReadStatusTable.messageId eq messageId }
            .map { it[MessageReadStatusTable.userId] }
        val fileInfo = row[MessagesTable.fileId]?.let { fid ->
            FilesTable.select { FilesTable.id eq fid }.singleOrNull()
        }
        val (fileUrl, fileName, thumbUrl) = fileTriple(fileInfo)
        row.toMessage(
            senderName   = sender?.get(UsersTable.displayName) ?: "Unknown",
            senderAvatar = sender?.get(UsersTable.avatarUrl),
            readBy       = readBy,
            fileUrl      = fileUrl,
            fileName     = fileName,
            thumbUrl     = thumbUrl
        )
    }

    override suspend fun getChatIdOf(messageId: UUID): UUID? = dbQuery {
        MessagesTable.slice(MessagesTable.chatId)
            .select { MessagesTable.id eq messageId }
            .singleOrNull()?.get(MessagesTable.chatId)
    }

    override suspend fun editMessage(messageId: UUID, content: String): Message = dbQuery {
        MessagesTable.update({ MessagesTable.id eq messageId }) {
            it[MessagesTable.content]  = content
            it[isEdited]               = true
            it[editedAt]               = LocalDateTime.now()
        }
        getMessageById(messageId)!!
    }

    override suspend fun deleteMessage(messageId: UUID) = dbQuery {
        MessagesTable.update({ MessagesTable.id eq messageId }) { it[isDeleted] = true }
        Unit
    }

    override suspend fun markAsRead(messageId: UUID, userId: String) = dbQuery {
        val exists = MessageReadStatusTable.select {
            (MessageReadStatusTable.messageId eq messageId) and
            (MessageReadStatusTable.userId eq userId)
        }.count() > 0
        if (!exists) {
            MessageReadStatusTable.insert {
                it[MessageReadStatusTable.messageId] = messageId
                it[MessageReadStatusTable.userId]    = userId
            }
        }
        Unit
    }

    override suspend fun getUnreadCount(chatId: UUID, userId: String): Int = dbQuery {
        MessagesTable.join(
            MessageReadStatusTable, JoinType.LEFT,
            additionalConstraint = {
                (MessageReadStatusTable.messageId eq MessagesTable.id) and
                (MessageReadStatusTable.userId eq userId)
            }
        ).select {
            (MessagesTable.chatId eq chatId) and
            (MessagesTable.senderId neq userId) and
            (MessagesTable.isDeleted eq false) and
            (MessageReadStatusTable.messageId.isNull())
        }.count().toInt()
    }
}
